package org.velvia.akka_caspaxos

sealed trait PaxosMessage
trait PaxosConflict

sealed trait Consistency
case object Quorum extends Consistency
case object ALL extends Consistency

/**
 * Sent from the client to a proposer with a change function on current state
 * @param changeFunc the change function, given an input T and a new output T
 * @param consistency whether the Proposer should wait for confirmations from F+1 or 2F+1 (all) acceptors
 */
final case class Proposal[T](changeFunc: Option[T] => T, consistency: Consistency = Quorum) extends PaxosMessage

/**
 * Proposer is already in the middle of proposing a change.  Try again later (response to client)
 */
case object AlreadyProposed

final case class Ballot(ballotNum: Int, proposerID: Int) extends PaxosMessage {
  def compare(other: Ballot): Int = {
    val ballotCmp = other.ballotNum compare ballotNum
    if (ballotCmp == 0) { other.proposerID compare proposerID } else ballotCmp
  }
}

/**
 * Sent from the proposer to acceptors during the prepare phase
 */
final case class Prepare(ballot: Ballot) extends PaxosMessage

/**
 * Returns a conflict if it already saw a greater ballot number
 * from Acceptor back to Proposer
 *
 * @param ballotNum the higher ballot number seen by an Acceptor (would be higher than the one in Prepare)
 */
final case class PrepareConflict[T](ballot: Ballot, acceptedValue: Option[T]) extends PaxosConflict

/**
 * Confirmation of a successful prepare message.  Returns the existing accepted value and its accepted ballot number.
 * This confirmation means the ballot number of the prepare is being held as a promise.
 */
final case class PrepareConfirm[T](ballot: Ballot, acceptedValue: Option[T]) extends PaxosMessage

final case class AcceptMessage[T](ballot: Ballot, newValue: T) extends PaxosMessage

/**
 * Conflict seen when receiving an AcceptMessage but a higher ballot has already been seen
 */
final case class AcceptConflict(ballot: Ballot) extends PaxosConflict

final case class AcceptConfirm(ballot: Ballot) extends PaxosMessage