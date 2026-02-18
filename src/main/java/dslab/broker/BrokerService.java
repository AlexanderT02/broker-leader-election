package dslab.broker;

import dslab.broker.enums.ElectionState;
import dslab.broker.enums.ElectionType;
import dslab.config.BrokerConfig;
import dslab.entity.BrokerStateManager;
import dslab.util.IOReadWrite;

import java.io.IOException;
import java.net.ConnectException;
import java.util.List;
import java.util.Optional;
import java.util.logging.Level;
import java.util.stream.IntStream;
/**
 * Manages the core services of a broker, including leader election, heartbeat management,
 * and DNS registration. Encapsulates logic for communication with peer brokers and the DNS server.
 */
public class BrokerService {
    private final BrokerConfig brokerConfig;
    private Thread heartbeatThread;
    private final BrokerStateManager brokerState;

    public BrokerService(BrokerConfig brokerConfig, BrokerStateManager brokerState) {
        this.brokerConfig = brokerConfig;
        this.brokerState = brokerState;
    }

    /**
     * Declares this broker as the leader in the network.
     * This method notifies peers about the new leadership,
     * establishes peer connections, starts sending periodic heartbeat messages,
     * and registers the broker's domain with the DNS server.
     *
     * <p>Steps performed by this method:</p>
     * <ol>
     *     <li>Notifies peers of the leadership change based on the election type
     *         (e.g., RING, BULLY, or RAFT).</li>
     *     <li>Establishes TCP connections to peers for communication.</li>
     *     <li>Starts a periodic heartbeat task to maintain leader status and detect state changes.</li>
     *     <li>Registers the broker's domain with the DNS server.</li>
     * </ol>
     *
     * <p>If the broker's state changes from LEADER, the heartbeat task will be canceled,
     * and all peer connections will be closed.</p>
     */
    public void declareLeader() {
        String declareMessage = String.format("declare %d", brokerConfig.electionId());
        switch (brokerState.getElectionType() ) {
            case RING -> forwardToPeersAndAwaitResponse(declareMessage, "ack");
            case BULLY, RAFT -> forwardToAllPeersAndAwaitResponse(declareMessage, "ack");
        }
        Broker.LOG.info("Broker %d is now the leader".formatted(brokerConfig.electionId()));

        String[] peerHosts = brokerConfig.electionPeerHosts();
        int[] peerPorts = brokerConfig.electionPeerPorts();
        int boundedLength = Math.min(peerHosts.length, peerPorts.length);

        List<IOReadWrite> peerConnections = IntStream.range(0, boundedLength)
                .mapToObj(i -> IOReadWrite.createConnection(peerHosts[i], peerPorts[i]))
                .flatMap(Optional::stream)
                .toList();

        heartbeatThread = Thread.ofVirtual().start(() -> {
            try {
                while (!Thread.currentThread().isInterrupted() && brokerState.getElectionState() == ElectionState.LEADER) {
                    peerConnections.forEach(io -> io.writeSocketResponse("ping"));
                    Thread.sleep(brokerConfig.electionHeartbeatTimeoutMs());
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } finally {
                peerConnections.forEach(IOReadWrite::close);
            }
        });

        registerBrokerDomain();
    }


    /**
     * Forwards a message to all peers and waits for a response that starts with the expected response string.
     * If any peer returns a matching response, the method returns immediately.
     *
     * @param message          the message to be sent to all peers.
     * @param expectedResponse the response prefix to look for in peer responses.
     */
    public void forwardToPeersAndAwaitResponse(String message, String expectedResponse) {
        String[] peerHosts = brokerConfig.electionPeerHosts();
        int[] peerPorts = brokerConfig.electionPeerPorts();
        int boundedLength = Math.min(peerPorts.length, peerHosts.length);
        for (int i = 0; i < boundedLength; i++) {
            boolean receivedExpectedResponse = sendMessageAndAwaitResponse(message, expectedResponse, peerHosts[i], peerPorts[i]);
            if(receivedExpectedResponse) return;
        }
    }

    /**
     * Forwards a message to all peers and waits for any response that starts with the expected response string.
     *
     * @param message          the message to be sent to the peers.
     * @param expectedResponse the response prefix to look for in peer responses.
     */
    public void forwardToAllPeersAndAwaitResponse(String message, String expectedResponse) {
        String[] peerHosts = brokerConfig.electionPeerHosts();
        int[] peerPorts = brokerConfig.electionPeerPorts();
        int boundedLength = Math.min(peerPorts.length, peerHosts.length);
        for (int i = 0; i < boundedLength; i++) {
            sendMessageAndAwaitResponse(message, expectedResponse, peerHosts[i], peerPorts[i]);
        }

    }

    /**
     * Forwards a message to peers with higher election IDs only, and waits for any response that starts with the expected response string.
     *
     * @param message            the message to be sent to the peers.
     * @param expectedResponse   the response prefix to look for in peer responses.
     * @return true if any peer with a higher ID returns a response starting with the expected response; false otherwise.
     */
    public boolean forwardToHigherIdPeersAndAwaitResponse(String message, String expectedResponse) {
        String[] peerHosts = brokerConfig.electionPeerHosts();
        int[] peerPorts = brokerConfig.electionPeerPorts();
        int[] peerIds = brokerConfig.electionPeerIds();
        int boundedLength = Math.min(Math.min(peerPorts.length, peerHosts.length), peerIds.length);
        return IntStream.range(0, boundedLength)
                .filter(i -> peerIds[i] > brokerConfig.electionId())
                .mapToObj(i -> sendMessageAndAwaitResponse(message, expectedResponse, peerHosts[i], peerPorts[i]))
                .anyMatch(response -> response);
    }

    /**
     * Sends a message to all peers and checks if the majority of them respond with a valid election vote.
     * A valid vote is a response where the election ID matches this node's election ID.
     *
     * @param message the election message to be sent to the peers.
     * @return true if the majority of peers respond with valid votes; false otherwise.
     */
    public boolean hasPeerMajorityVotes(String message) {
        String[] peerHosts = brokerConfig.electionPeerHosts();
        int[] peerPorts = brokerConfig.electionPeerPorts();
        int boundedLength = Math.min(peerPorts.length, peerHosts.length);
        return IntStream.range(0, boundedLength)
                .mapToObj(i -> isPeerVoteAccepted(message, peerHosts[i], peerPorts[i]))
                .filter(Boolean::booleanValue)
                .count() > boundedLength / 2;
    }

    public void registerBrokerDomain() {
        String domain = brokerState.getElectionType() == ElectionType.NONE
                ? brokerConfig.domain()
                : brokerConfig.electionDomain();

        try (IOReadWrite ioReadWrite = new IOReadWrite(brokerConfig.dnsHost(), brokerConfig.dnsPort())) {
            ioReadWrite.writeSocketResponse(String.format("register %s %s:%s", domain, brokerConfig.host(), brokerConfig.port()));
            if("ok".equals(ioReadWrite.readRequest())) {
                Broker.LOG.info("Successfully registered broker domain: %s".formatted(domain));
            }
        } catch (ConnectException e) {
            Broker.LOG.log(Level.SEVERE, "Domain registration failed for broker-domain: {0}. Unable to connect to DNS server at {1}:{2}.",
                    new Object[]{domain, brokerConfig.dnsHost(), brokerConfig.dnsPort()});
        } catch (IOException ignored) {}
    }

    public void shutdown() {
        Optional.ofNullable(heartbeatThread).ifPresent(Thread::interrupt);
    }

    private boolean sendMessageAndAwaitResponse(String message, String expectedResponse, String peerHost, int peerPort) {
        try (IOReadWrite io = new IOReadWrite(peerHost, peerPort)) {
            io.readRequest();
            io.writeSocketResponse(message);
            String response = io.readRequest();
            if (response != null && response.startsWith(expectedResponse)) {
                return true;
            }
        } catch (IOException ignored) {}
        return false;
    }
    private boolean isPeerVoteAccepted(String message, String peerHost, int peerPort) {
        try (IOReadWrite io = new IOReadWrite(peerHost, peerPort)) {
            io.readRequest();
            io.writeSocketResponse(message);
            String response =  io.readRequest();
            String[] responseParts = response != null ? response.split(" ") : new String[0];
            return responseParts.length > 2 && String.valueOf(brokerConfig.electionId()).equals(responseParts[2]);

        } catch (IOException e) {
            return false;
        }
    }

}
