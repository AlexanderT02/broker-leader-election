# Distributed Broker & Leader Election System

A Java-based distributed messaging system that combines a **message
broker**, **topic routing with wildcards**, and **multiple leader
election algorithms**.

The system also implements **custom TCP protocols** for communication
between clients and brokers.

This project demonstrates core concepts of **distributed systems**,
including:

-   leader election
-   distributed coordination
-   message routing
-   wildcard topic exchanges
-   custom TCP protocols

------------------------------------------------------------------------

# System Architecture

``` mermaid
flowchart LR
    P[Publisher Client]
    S[Subscriber Client]

    B1[Broker Node 1]
    B2[Broker Node 2]
    B3[Broker Node 3]

    DNS[DNS / Service Discovery]

    P -->|SMQP| B1
    S -->|SMQP| B2

    B1 <-->|LEP| B2
    B2 <-->|LEP| B3
    B1 <-->|LEP| B3

    B1 -->|SDP| DNS
    B2 -->|SDP| DNS
    B3 -->|SDP| DNS
```

The system consists of:


- Clients that publish or subscribe to messages via the SMQP protocol
- Broker nodes forming a distributed cluster
- A DNS / service discovery server used by brokers
- Leader election between brokers using the LEP protocol

------------------------------------------------------------------------

# Broker Architecture

Each broker contains several core components.

``` mermaid
flowchart TB

Client[Client]

SMQP[SMQP Protocol Handler]

Exchange[Exchange]
Router[Topic Router]
Queue[Queue]

Election[Leader Election]

Client --> SMQP
SMQP --> Exchange
Exchange --> Router
Router --> Queue

Election --- Exchange
```

Responsibilities of a broker:

- receive messages from clients via the SMQP protocol
- route messages through exchanges and routing logic
- deliver messages to queues
- communicate with other brokers in the cluster
- participate in leader election

------------------------------------------------------------------------

# Exchange Types

The broker supports multiple exchange types similar to **RabbitMQ**.

  Exchange   Description
  ---------- ------------------------------------------
  Direct     Routes messages using exact routing keys
  Fanout     Broadcasts messages to all queues
  Topic      Routes messages using wildcard patterns

------------------------------------------------------------------------

# Wildcard Topic Routing

Topic exchanges support wildcard routing keys.

  Wildcard   Meaning
  ---------- ----------------------------
  `*`        matches exactly one word
  `#`        matches zero or more words

Example:

Publisher routing key:

    hotel.room.duvet

Subscriber binding:

    hotel.*.duvet

Result:

    hotel.room.duvet  -> delivered
    hotel.bed.duvet   -> delivered
    hotel.room.clean  -> ignored

## Message Flow

The following diagram shows how a message moves through the broker from
a publisher to a subscriber.

``` mermaid
flowchart LR
Publisher --> Exchange
Exchange --> Router
Router --> Queue
Queue --> Subscriber
```

1.  A publisher sends a message to the broker.
2.  The message is received by an exchange.
3.  The exchange forwards the message to the routing logic.
4.  The router determines the correct queue using routing keys and
    bindings.
5.  The message is delivered to the subscriber.


------------------------------------------------------------------------

# Leader Election Algorithms

The broker cluster implements **three leader election algorithms**.

  Algorithm    Description
  ------------ ----------------------------------------
  Bully        Node with highest ID becomes leader
  Ring         Election message circulates in a ring
  Raft-style   Voting-based consensus leader election

------------------------------------------------------------------------

# Leader Election Flow Example

``` mermaid
sequenceDiagram
    participant B1 as Broker 1
    participant B2 as Broker 2
    participant B3 as Broker 3

    B1->>B2: Election message
    B2->>B3: Forward election
    B3->>B1: Highest ID detected
    B3-->>B1: Leader announcement
    B3-->>B2: Leader announcement
```

When the leader fails:

1.  brokers detect missing heartbeat
2.  an election is started
3.  nodes exchange election messages
4.  the new leader is announced

------------------------------------------------------------------------

# Custom TCP Protocols

The system defines its own communication protocols over TCP.

  Protocol   Purpose
  ---------- -------------------------------
  SMQP       Client ↔ Broker messaging
  LEP        Leader election communication
  SDP        Service discovery (DNS-like)

------------------------------------------------------------------------

# Protocol Example

Example SMQP publish message:

    PUBLISH
    exchange: hotel-events
    routing-key: hotel.room.duvet
    payload: "new duvet delivered"

Broker response:

    OK
    message routed

------------------------------------------------------------------------

# Running the Application

## Build

Build the project using Gradle:

``` bash
./gradlew build
```

Windows:

``` bash
gradlew build
```

------------------------------------------------------------------------

## Start the Application

Run the compiled jar:

``` bash
java -jar build/libs/<your-jar-file>.jar
```

The application can run as:

-   broker node
-   DNS / service discovery server
-   client

Multiple brokers can be started to form a **cluster**.

They automatically:

-   discover each other
-   participate in leader election
-   route messages across the system

------------------------------------------------------------------------

# Running Tests

Run all unit and integration tests with:

``` bash
./gradlew test
```

Gradle will build the project and execute the complete test suite.

------------------------------------------------------------------------

# Educational Purpose

This project demonstrates how real distributed systems work internally,
including concepts used in systems such as:

-   RabbitMQ
-   Kafka
-   distributed coordination services

It serves as a practical example of:

-   distributed messaging
-   leader election algorithms
-   network protocol design
-   broker-based architectures
