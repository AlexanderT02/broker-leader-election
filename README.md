# Distributed Broker & Leader Election System

A Java-based distributed messaging system that combines a **message
broker**, **topic routing with wildcards**, and **multiple leader
election algorithms**.\
The system also implements **custom TCP protocols** for communication
between clients and brokers.

This project demonstrates key concepts of **distributed systems**, such
as coordination, routing, and fault tolerance.

------------------------------------------------------------------------

## Main Features

### Message Broker

The broker supports typical messaging patterns similar to systems like
RabbitMQ.

Supported exchange types:

-   **Direct** -- routes messages by exact routing key
-   **Fanout** -- broadcasts messages to all queues
-   **Topic** -- routes messages based on pattern matching

### Wildcard Topic Routing

Topic exchanges support routing patterns with wildcards.

  Wildcard   Meaning
  ---------- ----------------------------
  `*`        matches exactly one word
  `#`        matches zero or more words

Example:

    hotel.*.duvet

matches

    hotel.bed.duvet
    hotel.room.duvet

    hotel.#

matches any routing key starting with `hotel`.

------------------------------------------------------------------------

## Leader Election Algorithms

The broker cluster implements **three different leader election
algorithms**.

These are used when:

-   the leader fails
-   a new node joins
-   the cluster needs to re-elect a coordinator

Implemented algorithms:

1.  **Bully Algorithm**\
    Nodes with higher IDs take priority and can take over leadership.

2.  **Ring Election Algorithm**\
    Election messages circulate in a logical ring until the node with
    the highest ID is chosen.

3.  **Raft‑style Election**\
    Nodes transition between *Follower*, *Candidate*, and *Leader*
    states and elect a leader via majority voting.

------------------------------------------------------------------------

## Custom TCP Protocols

Instead of HTTP or existing messaging protocols, the system defines its
own **TCP-based protocols**.

### SMQP -- Simple Message Queue Protocol

Used for communication between **clients and brokers**.

Handles:

-   publishing messages
-   subscribing to queues
-   message delivery

### LEP -- Leader Election Protocol

Used for **broker-to-broker communication** during leader elections.

### SDP -- Service Discovery Protocol

Provides **DNS-like service discovery** so brokers and clients can
locate services.

------------------------------------------------------------------------

## Architecture

    Clients
       |
       v
    +-------------------+
    |   Message Broker  |
    |-------------------|
    | Exchanges         |
    | Queues            |
    | Topic Routing     |
    | Wildcard Matching |
    +---------+---------+
              |
              v
       Broker Cluster
       (Leader Election)

Each broker:

-   routes messages
-   participates in leader election
-   communicates with other brokers via TCP



