package org.velvia.akka_caspaxos

import scala.concurrent.duration._

import akka.actor.{Actor, ActorRef, Props}
import com.typesafe.scalalogging.StrictLogging


object Proposer {
  def props[T](initialAcceptors: Seq[ActorRef],
               timeout: FiniteDuration): Props = Props(new Proposer[T](initialAcceptors, timeout))
}

/**
 * CAS-Paxos proposer
 * Responsible for two-phase preparation and submission of change requests to acceptors
 * It is a state machine.  During the two phase approach it will not accept new proposals from clients.
 */
class Proposer[T](initialAcceptors: Seq[ActorRef],
                  timeout: FiniteDuration) extends Actor with StrictLogging {
  var acceptors = initialAcceptors
  var nextBallot = 1
  var currentBallot = 0
  var confirmationsNeeded = -1
  var currentChangeFunc: Option[T] => T = _
  val prepareConfirms = new collection.mutable.ArrayBuffer[PrepareConfirm[T]]
  var myID = 0   // TODO: get a unique ID from snowflake

  def prepareReceive: Receive = {
    case Proposal(changeFunc, Quorum)[T] =>
      // Default number of acceptors to accept responses from are F + 1, assuming 2F + 1 acceptors
      confirmationsNeeded = acceptors.length / 2 + 1
      currentChangeFunc = changeFunc
      sendPrepareMessages()
      context.become(waitAndAccept)

    case Proposal[T](changeFunc, ALL) =>
      confirmationsNeeded = acceptors.length
      currentChangeFunc = changeFunc
      sendPrepareMessages()
      context.become(waitAndAccept)
  }

  def waitAndAccept: Receive = {
    // Now, wait for confirmations
    case Proposal[T](changeFunc, _) =>
      logger.info(s"Sorry, cannot accept proposals while we are already proposing another change")
      sender() ! AlreadyProposed

    case PrepareConflict[T](newBallot, valueOpt) =>
      logger.info(s"Received conflict from ${sender()} with $newBallot, retrying with higher ballot")
      // TODO: what to do here?   update our ballot to one past the latest and try again?  Maybe back off for a while?
      nextBallot = newBallot.ballotNum
      // TODO: check new value, maybe accepted value is OK, then we don't need to do anything?
      sendPrepareMessages()

    case p @ PrepareConfirm[T](ballot, valueOpt) =>
      prepareConfirms += p
      logger.debug(s"Received prepare confirmation with $ballot and $valueOpt...")
      if (prepareConfirms.length == confirmationsNeeded) applyChangeFuncToLatest()
  }

  def confirmAccept: Receive = {
    // Now, wait for confirmations of accept messages
    case Proposal[T](changeFunc, _) =>
      logger.info(s"Sorry, cannot accept proposals while we are already proposing another change")
      sender() ! AlreadyProposed
  }

  private def sendPrepareMessages(): Unit = {
    val prepMessage = Prepare(Ballot(nextBallot, myID))
    logger.debug(s"Sending Prepare() with ballot number $nextBallot and expecting $confirmationsNeeded confirmations")
    currentBallot = nextBallot
    nextBallot += 1
    acceptors foreach (_ ! prepMessage)
    prepareConfirms.clear()
    // TODO: send myself a timeout message.  What to do if we don't receive confirmations by the timeout?  Retry?
  }

  private def applyChangeFuncToLatest(): Unit = {
    val nonEmptyValues = prepareConfirms.filter(_.acceptedValue.nonEmpty)
    val newValue = if (nonEmptyValues.isEmpty) {
      logger.debug(s"All accepted values are empty")
      currentChangeFunc(None)
    } else {
      val latestConfirm = nonEmptyValues.reduce { case (a, b) =>
                            if (a.ballot.compare(b.ballot) > 0) a else b
                          }
      logger.debug(s"Found latest accepted confirmation is $latestConfirm")
      currentChangeFunc(latestConfirm.acceptedValue.get)
    }
    val acceptMsg = AcceptMessage(Ballot(currentBallot, myID), newValue)
    logger.debug(s"Preparing to send $acceptMsg to ${acceptors.length} acceptors")
    acceptors foreach (_ ! acceptMsg)
    context.become(confirmAccept)
  }

  def receive = prepareReceive
}