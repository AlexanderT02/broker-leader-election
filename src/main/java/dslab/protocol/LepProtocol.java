package dslab.protocol;

import dslab.broker.enums.ElectionState;
import dslab.broker.BrokerService;
import dslab.broker.enums.ElectionType;
import dslab.config.BrokerConfig;
import dslab.entity.BrokerStateManager;
import dslab.util.IOReadWrite;

import java.io.Closeable;
import java.util.Optional;


public class LepProtocol implements Protocol, Closeable {
    private final BrokerConfig brokerConfig;
    private final BrokerService brokerService;
    private final BrokerStateManager brokerState;


    public LepProtocol(BrokerConfig brokerConfig, BrokerStateManager brokerState) {
        this.brokerConfig = brokerConfig;
        this.brokerState = brokerState;
        this.brokerService = new BrokerService(brokerConfig, brokerState);
    }

    @Override
    public Optional<String> processCommand(String[] command, Object... additionalParams) {
        String returner = switch (command[0]) {
            case "ping" -> handlePingCommand();
            case "elect" -> handleElectCommand(command, (IOReadWrite) additionalParams[0]);
            case "declare" -> handleDeclareCommand(command);
            default -> "error usage: <command> <args>";
        };
        return Optional.ofNullable(returner);
    }

    private String handlePingCommand() {
        brokerState.setLastHeartbeatTimestamp(System.currentTimeMillis());
        return "pong";
    }

    private String handleElectCommand(String[] parts, IOReadWrite ioReadWrite) {
        if (parts.length != 2) return "error usage: elect <id>";
        int senderId = Integer.parseInt(parts[1]);
        brokerState.setCurrentLeaderId(-1);

        return switch (brokerState.getElectionType()) {
            case RING -> handleRingElection(senderId, ioReadWrite);
            case BULLY -> handleBullyElection(senderId, ioReadWrite);
            case RAFT -> handleRaftElection(senderId);
            case NONE -> null;
        };
    }

    private String handleDeclareCommand(String[] parts) {
        if (parts.length != 2) return "error usage: declare <id>";

        int leaderId = Integer.parseInt(parts[1]);

        switch (brokerState.getElectionType()) {
            case RAFT -> {
                brokerState.setLastVotedBrokerId(-1);
                brokerState.setElectionState(ElectionState.FOLLOWER);
            }
            case BULLY -> brokerState.setElectionState(ElectionState.FOLLOWER);
            case RING -> {
                if (leaderId != brokerConfig.electionId()) {
                    brokerService.forwardToPeersAndAwaitResponse(
                            String.format("declare %d", leaderId), "ack");
                }
            }
        }

        brokerState.setCurrentLeaderId(leaderId);
        return brokerState.getElectionType() != ElectionType.NONE
                ? String.format("ack %d", brokerConfig.electionId())
                : null;
    }


    private String handleRingElection(int senderId, IOReadWrite ioReadWrite) {
        if (senderId == brokerConfig.electionId()) {
            brokerState.setElectionState(ElectionState.LEADER);
            brokerState.setCurrentLeaderId(brokerConfig.electionId());
            ioReadWrite.writeSocketResponse("ok");
            brokerService.declareLeader();
        } else {
            ioReadWrite.writeSocketResponse("ok");
            int max = Math.max(senderId, brokerConfig.electionId());
            brokerState.setElectionState(max == senderId ? ElectionState.FOLLOWER : ElectionState.CANDIDATE);
            brokerService.forwardToPeersAndAwaitResponse(
                String.format("elect %d", max),
                "ok"
            );
        }
        return null;
    }

    private String handleBullyElection(int senderId, IOReadWrite ioReadWrite) {
        ioReadWrite.writeSocketResponse("ok");
        brokerState.setElectionState(ElectionState.CANDIDATE);
        if (brokerConfig.electionId() > senderId) {
            boolean receivedResponse = brokerService.forwardToHigherIdPeersAndAwaitResponse(
                    String.format("elect %d", brokerConfig.electionId()),
                    "ok"
            );
            if (!receivedResponse) {
                brokerState.setElectionState(ElectionState.LEADER);
                brokerState.setCurrentLeaderId(brokerConfig.electionId());
                brokerService.declareLeader();
            }
        }
        return null;
    }

    private String handleRaftElection(int senderId) {
        brokerState.setElectionState(ElectionState.CANDIDATE);
        if (brokerState.compareAndSetLastVotedBrokerId(-1, senderId)) {
            return String.format("vote %d %d", brokerConfig.electionId(), senderId);
        } else {
            return String.format("vote %d %d", brokerConfig.electionId(), brokerState.getLastVotedBrokerId());
        }
    }

    @Override
    public void close() {
        brokerService.shutdown();
    }
}