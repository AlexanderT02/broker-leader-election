# README

## Overview
The Broker Leader Election project is designed to ensure a robust method for leader selection in distributed systems. It leverages consensus algorithms to determine a leader, minimizing downtime and maximizing efficiency.

## System Requirements
- **Java Runtime Environment**: Version 21 or higher
- **Memory**: Minimum 1GB RAM
- **Disk Space**: At least 100MB free

## Installation Steps
1. **Clone the repository:**  
   ```bash
   git clone https://github.com/AlexanderT02/broker-leader-election.git
   cd broker-leader-election
   ```  

2. **Build the project:**  
   ```bash
   ./gradlew build
   ```  

3. **Run the application:**  
   ```bash
   ./gradlew run
   ```

## Detailed Component Descriptions
- **Leader Election Module**: Implements the core logic for selecting the leader.
- **Communication Module**: Handles communication between nodes.
- **Data Storage**: Manages persistent storage of state and configurations.

## Protocol Specifications
The system operates on the Raft consensus protocol, ensuring that all nodes agree on a single source of truth, with the following phases:
- **Candidate Phase**: A node requests votes from others to become the leader.
- **Leader Phase**: The elected leader manages all client requests, replicating log entries to followers.
- **Follower Phase**: Nodes acknowledge the leader's log entries and reply with their state.

## Testing Information
To ensure reliability, unit tests are included:
- Use JUnit for testing various components.
- Run tests using the following command:
  ```bash
  ./gradlew test
  ```

## Troubleshooting
Common issues include:
- **Network Connectivity**: Ensure all nodes can communicate over the specified ports.
- **Version Conflicts**: Ensure all nodes are using compatible software versions.

## Project Structure
```
/broker-leader-election
├── src
│   ├── main
│   ├── test
├── build.gradle
├── settings.gradle
└── README.md
```
