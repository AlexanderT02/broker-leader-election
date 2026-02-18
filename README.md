# Broker Leader Election & UDP Monitoring

A distributed system for managing message brokers with automatic leader election and UDP-based monitoring.

## Quick Start

### Prerequisites
- Java JDK 21
- Maven 3.8+

### Compilation & Running

```bash
mvn compile
```

Run individual components:
```bash
mvn exec:java@broker-0
mvn exec:java@broker-1
mvn exec:java@broker-2
mvn exec:java@dns-0
mvn exec:java@monitoring-0
```

## Components

| Component | Port | Type |
|-----------|------|------|
| broker-0 | 20000/20001 | TCP |
| broker-1 | 20010/20011 | TCP |
| broker-2 | 20020/20021 | TCP |
| dns-0 | 18000 | TCP |
| monitoring-0 | 17000 | UDP |

## Protocols

### Leader Election Protocol (LEP)

Connected clients receive: `ok LEP`

| Command | Response |
|---------|----------|
| `elect <id>` | `ok` or `vote <sender-id> <candidate-id>` |
| `declare <id>` | `ack <sender-id>` |
| `ping` | `pong` |

Invalid commands return: `error protocol error`

## Testing

```bash
mvn test
```

Automated tests run via GitHub Actions on push.

## Manual Testing with Netcat

```bash
nc localhost 20001
# or on Windows: ncat -C localhost 20001

> elect 0
ok
> declare 0
ack 0
> ping
pong
```

## Project Structure

```
src/main/java/          <- Implementation code
src/main/resources/     <- Configuration (protected)
src/test/               <- Tests (protected)
```

**Protected files** (do not modify):
- `.github/**/*`
- `src/main/resources/**/*`
- `src/test/**/*`
- `pom.xml`