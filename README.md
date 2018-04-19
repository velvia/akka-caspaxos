# akka-caspaxos
Experimental Akka implementation of the [CAS-Paxos](https://arxiv.org/pdf/1802.07000.pdf) Replicated State Machine (RSM) distributed protocol. 

CAS-Paxos has the potential to be a very lightweight, easy to understand, and performant protocol for distributed state machines requiring a high degree of consistency.  

It is a good match for Akka and Scala:
* Like Akka Cluster, CAS-Paxos is peer to peer by nature
* Its improvement over RAFT and Paxos is the use of a change function by proposers to change the state.  Since Scala is functional and functions can easily be serialized over the wire, this makes implementation very easy
* Actors are relatively lightweight so we can spin up a large number of independent RSMs with individual actors for the proposers and acceptors for each RSM.

## Initial Implementation Goals

* A per-node `CASPaxosManager` actor capable of spinning up and managing RSMs
* Implementation of the protocol for growing and changing acceptors
    - TODO: should this be global or per-RSM?
    - How to agree on the changes in acceptors?  The list of acceptors has to be consistent across ALL proposers or else consensus does not work.
* Base RSM implementations
    - The compare-and-set register described in the paper
    - RSM for a Set (used for acceptor membership)
    - RSM for an ordered list
    - RSM for a map (or should this be individual RSMs per shard/hash key?)
* Global RSMs for the list of other RSMs and set of acceptors
    - Plus a mechanism to update all proposers with the agreed set of acceptors etc.
* Use of Akka Cluster to provide initial seed list of acceptors, when the cluster is small

## Ballot/Counter

Use of Snowflake for individual node IDs?

## Non-Goals
