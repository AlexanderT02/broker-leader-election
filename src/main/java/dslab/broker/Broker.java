package dslab.broker;

import dslab.ComponentFactory;
import dslab.broker.enums.ElectionState;
import dslab.broker.enums.ElectionType;
import dslab.config.BrokerConfig;
import dslab.dns.DNSServer;
import dslab.entity.BrokerStateManager;
import dslab.entity.Exchange;
import dslab.entity.Queue;
import dslab.thread.ListenerThread;
import dslab.thread.LepTcpThread;
import dslab.thread.SmqpTcpThread;
import dslab.util.ExchangeType;

import java.io.IOException;
import java.net.ServerSocket;
import java.util.Optional;
import java.util.concurrent.*;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Broker implements IBroker {
    public static final Logger LOG = Logger.getLogger(DNSServer.class.getName());
    private final BrokerConfig brokerConfig;
    private ListenerThread smqpListenerThread;
    private final ConcurrentHashMap<String, Exchange> exchanges = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Queue> queues = new ConcurrentHashMap<>();

    private ListenerThread lepListenerThread;
    private final BrokerService brokerService;
    private Thread heartbeatMonitorThread;
    private final BrokerStateManager brokerState;



    public Broker(BrokerConfig config) {
        this.brokerConfig = config;
        this.brokerState = new BrokerStateManager(ElectionType.fromString(config.electionType()));
        this.brokerService = new BrokerService(brokerConfig, brokerState);

    }

    @Override
    public void run() {
        try {
            //build listener
            smqpListenerThread = ListenerThread.builder()
                    .componentId("SMQP-Listener")
                    .serverSocket(new ServerSocket(brokerConfig.port()))
                    .clientConnectionRunnable(socket -> new SmqpTcpThread(exchanges, queues, socket))
                    .build();

            lepListenerThread = ListenerThread.builder()
                    .componentId("LEP-Listener")
                    .serverSocket(new ServerSocket(brokerConfig.electionPort()))
                    .clientConnectionRunnable(socket -> new LepTcpThread(socket, brokerConfig, brokerState))
                    .build();

            exchanges.put("default", new Exchange(ExchangeType.DEFAULT, "default"));
            smqpListenerThread.start();
            lepListenerThread.start();

            //startHeartbeatMonitoring ;
            if (brokerState.getElectionType() != ElectionType.NONE) {
                heartbeatMonitorThread = Thread.ofVirtual().start(this::monitorHeartbeat);
            }else {
                brokerService.registerBrokerDomain();
            }

            LOG.info("Started broker: %s".formatted(brokerConfig.componentId()));
        } catch (IOException e) {
            LOG.log(Level.SEVERE, "Error while starting broker", e);
        }
    }

    @Override
    public int getId() {
        return brokerConfig.electionId();
    }

    @Override
    public void initiateElection() {
        brokerState.setElectionState(ElectionState.CANDIDATE);
        brokerState.setCurrentLeaderId(-1);

        String electionMessage = String.format("elect %d", brokerConfig.electionId());
        switch (brokerState.getElectionType()) {
            case RING -> initiateRingElection(electionMessage);
            case BULLY -> initiateBullyElection(electionMessage);
            case RAFT -> initiateRaftElection(electionMessage);
        }
    }

    @Override
    public int getLeader() {
        return brokerState.getCurrentLeaderId();
    }

    @Override
    public void shutdown() {
        Optional.ofNullable(heartbeatMonitorThread).ifPresent(Thread::interrupt);
        brokerService.shutdown();
        smqpListenerThread.shutdown();
        lepListenerThread.shutdown();
        LOG.info(String.format("Broker %s shutdown complete.", brokerConfig.componentId()));
    }

    private void initiateRingElection(String electionMessage) {
        LOG.info(String.format("Broker %d initiating RING election", getId()));
        brokerService.forwardToPeersAndAwaitResponse(electionMessage, "ok");
    }

    private void initiateBullyElection(String electionMessage) {
        LOG.info(String.format("Broker %d initiating BULLY election", getId()));
        boolean receivedResponse = brokerService.forwardToHigherIdPeersAndAwaitResponse(electionMessage, "ok");
        if (receivedResponse) return;

        LOG.info(String.format("Broker %d received no responses, declaring itself as leader", getId()));
        brokerState.setElectionState(ElectionState.LEADER);
        brokerState.setCurrentLeaderId(brokerConfig.electionId());
        brokerService.declareLeader();
    }

    private void initiateRaftElection(String electionMessage) {
        LOG.info(String.format("Broker %d initiating RAFT election", getId()));
        if (brokerService.hasPeerMajorityVotes(electionMessage)) {
            LOG.info(String.format("Broker %d received majority of votes, declaring itself as leader", getId()));
            brokerState.setElectionState(ElectionState.LEADER);
            brokerState.setCurrentLeaderId(brokerConfig.electionId());
            brokerService.declareLeader();
        } else {
            LOG.info(String.format("Broker %d received not enough votes for RAFT election", getId()));
        }
    }

    private void monitorHeartbeat() {
        while (!Thread.currentThread().isInterrupted()) {
            if(brokerState.getElectionState() == ElectionState.LEADER) continue;
            if ((System.currentTimeMillis() - brokerState.getLastHeartbeatTimestamp()) > brokerConfig.electionHeartbeatTimeoutMs()) {
                initiateElection();
            }
            try {
                Thread.sleep(50);
            } catch (InterruptedException e) {
               Thread.currentThread().interrupt();
            }
        }
    }


    public static void main(String[] args) {
        ComponentFactory.createBroker(args[0]).run();
    }
}
