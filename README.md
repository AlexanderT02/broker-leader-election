
# Distributed Broker & Leader Election System
![Java](https://img.shields.io/badge/Java-21-blue)
![Gradle](https://img.shields.io/badge/build-Gradle-green)
![Distributed Systems](https://img.shields.io/badge/topic-distributed--systems-orange)

A Java-based **distributed messaging system** that combines a message broker,
topic routing with wildcards, distributed service discovery, and multiple
leader election algorithms.

The system demonstrates core distributed systems concepts such as:

- distributed message routing
- service discovery
- leader election
- failure detection with heartbeats
- monitoring with UDP
- custom TCP protocols
- concurrent network servers

The architecture resembles simplified concepts used in systems like
RabbitMQ, Kafka, or distributed coordination services.

---

# System Architecture

The system consists of several cooperating components:

- **Clients** that publish or subscribe to messages
- **Broker nodes** that route and deliver messages
- **DNS / Service Discovery server** used by brokers
- **Monitoring server** collecting routing statistics

```mermaid
flowchart LR
    P[Publisher Client]
    S[Subscriber Client]

    B1[Broker Node 1]
    B2[Broker Node 2]
    B3[Broker Node 3]

    DNS[DNS / Service Discovery]
    MON[Monitoring Server]

    P -->|SMQP| B1
    S -->|SMQP| B2

    B1 <-->|LEP| B2
    B2 <-->|LEP| B3
    B1 <-->|LEP| B3

    B1 -->|register| DNS
    B2 -->|register| DNS
    B3 -->|register| DNS

    B1 -->|UDP stats| MON
    B2 -->|UDP stats| MON
    B3 -->|UDP stats| MON
```

Each broker can operate independently but also cooperates with other brokers
in a cluster to elect a leader and coordinate system behavior.

---

# Broker Architecture

Each broker contains several internal components responsible for
handling client communication, routing messages, and participating in
cluster coordination.

```mermaid
flowchart TB

Client[Client]

SMQP[SMQP Protocol Handler]

Exchange[Exchange]
Router[Routing Logic]
Queue[Queue]

Election[Leader Election]

Client --> SMQP
SMQP --> Exchange
Exchange --> Router
Router --> Queue

Election --- Exchange
```

Responsibilities of a broker:

- receive messages from clients via SMQP
- manage exchanges and queues
- route messages using routing keys
- deliver messages to subscribed clients
- participate in leader election
- register its address via service discovery
- report message statistics to the monitoring server

---

# Exchange Types

The broker supports multiple exchange types similar to RabbitMQ.

| Exchange | Description |
|--------|-------------|
| direct | routes messages using exact routing keys |
| fanout | broadcasts messages to all queues |
| topic | routes messages using wildcard patterns |
| default | implicit direct exchange |

---

# Wildcard Topic Routing

Topic exchanges support wildcard routing patterns.

| Wildcard | Meaning |
|--------|---------|
| * | matches exactly one word |
| # | matches zero or more words |

Example:

Publisher routing key:

hotel.room.duvet

Subscriber binding:

hotel.*.duvet

Result:

hotel.room.duvet → delivered  
hotel.bed.duvet → delivered  
hotel.room.clean → ignored

---

# Message Flow

The following diagram shows how a message travels through the system.

```mermaid
flowchart LR
Publisher --> Exchange
Exchange --> Router
Router --> Queue
Queue --> Subscriber
```

1. A publisher sends a message to the broker.
2. The message is received by an exchange.
3. The exchange forwards the message to the routing logic.
4. The router determines the correct queue using routing keys.
5. The message is delivered to subscribers.

---

# Leader Election

Broker nodes form a distributed cluster and must agree on **exactly one leader**.

The leader is responsible for registering the active broker address via
service discovery and coordinating cluster state.

Brokers communicate using a custom **Leader Election Protocol (LEP)**.

Each broker can be in one of three states:

- follower
- candidate
- leader

---

# Heartbeat Mechanism

The leader periodically sends heartbeat messages to followers.

Leader → ping  
Follower → pong

If a follower does not receive a heartbeat within a configured timeout:

1. the follower assumes the leader failed
2. an election is started
3. a new leader is chosen

---

# Leader Election Algorithms

The cluster supports three election algorithms.

| Algorithm | Description |
|---------|-------------|
| Bully | node with highest ID becomes leader |
| Ring | election message circulates in a ring |
| Raft-style | voting based leader election |

Example election flow:

```mermaid
sequenceDiagram
    participant B1 as Broker 1
    participant B2 as Broker 2
    participant B3 as Broker 3

    B1->>B2: elect 1
    B2->>B3: elect 2
    B3->>B1: elect 3
    B3-->>B1: declare leader
    B3-->>B2: declare leader
```

---

# Monitoring Server

The system includes a **UDP-based monitoring server** that collects
statistics about messages routed by the broker nodes.

Whenever a broker routes a message, it sends a small monitoring packet
containing its address and the routing key.

UDP is used because monitoring data is **non-critical** and should not
delay normal broker communication.

---

# Custom Network Protocols

The system defines three lightweight text-based protocols.

| Protocol | Purpose |
|--------|--------|
| SMQP | client ↔ broker messaging |
| LEP | broker ↔ broker leader election |
| SDP | service discovery / DNS |

Example publish message:

PUBLISH
exchange: hotel-events
routing-key: hotel.room.duvet
payload: "new duvet delivered"

Broker response:

OK
message routed

---

# Running the Application

## Build

Using Gradle:

./gradlew build

Windows:

gradlew build

---

## Run

java -jar build/libs/<jar-file>.jar

The application can run in different modes:

- broker node
- DNS / service discovery server
- monitoring server
- client

Multiple brokers can run simultaneously to form a cluster.

---

# Running Tests

Run the test suite with:

./gradlew test

---

# Concepts Demonstrated

This project demonstrates core distributed systems concepts:

- distributed messaging systems
- leader election algorithms
- service discovery
- failure detection
- wildcard topic routing
- TCP and UDP protocol design
- concurrent server architectures

# Technologies

| Technology | Purpose |
|-----------|--------|
| Java 21 | Core implementation |
| Gradle | Build system |
| TCP Sockets | Client and broker communication |
| UDP | Monitoring statistics |
| Concurrent Programming | Multi-client handling |
| Distributed Algorithms | Leader election |

