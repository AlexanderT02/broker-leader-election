package dslab.dns;

import dslab.ComponentFactory;
import dslab.config.DNSServerConfig;
import dslab.thread.ListenerThread;
import dslab.thread.SdpTcpThread;

import java.io.IOException;
import java.net.ServerSocket;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

public class DNSServer implements IDNSServer {
    private static final Logger LOG = Logger.getLogger(DNSServer.class.getName());
    private final ConcurrentHashMap<String, String> dnsEntries = new ConcurrentHashMap<>();
    private final DNSServerConfig config;
    private ListenerThread listenerThread;

    public DNSServer(DNSServerConfig config) {
        this.config = config;
    }

    @Override
    public void run() {
        try {
            listenerThread = ListenerThread.builder()
                    .componentId("SDP-Listener")
                    .serverSocket(new ServerSocket(config.port()))
                    .clientConnectionRunnable(socket -> new SdpTcpThread(dnsEntries, socket))
                    .build();

            listenerThread.start();
            LOG.info("Started DNS server: %s".formatted(config.componentId()));
        } catch (IOException e) {
            LOG.log(Level.SEVERE, "Error while creating server socket", e);
        }
    }

    @Override
    public void shutdown() {
        LOG.info(String.format("Attempting to shutdown DNS server: %s", config.componentId()));
        listenerThread.shutdown();
        LOG.info(String.format("Dns Server %s shutdown complete.", config.componentId()));
    }

    public static void main(String[] args) {
        ComponentFactory.createDNSServer(args[0]).run();
    }
}
