package dslab.monitoring;

import dslab.ComponentFactory;
import dslab.config.MonitoringServerConfig;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.util.HashMap;
import java.util.Map;

public class MonitoringServer implements IMonitoringServer {
    private final Map<String, Map<String, Integer>> statistics = new HashMap<>();
    private final MonitoringServerConfig config;
    private DatagramSocket datagramSocket;

    public MonitoringServer(MonitoringServerConfig config) {
        this.config = config;
    }

    @Override
    public void run() {
        try  {
            this.datagramSocket = new DatagramSocket(config.monitoringPort());
            byte[] buffer = new byte[1024];
            while (!datagramSocket.isClosed()) {
                DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                datagramSocket.receive(packet);
                String[] parts = new String(packet.getData(), 0, packet.getLength()).split(" ");
                if (parts.length == 2) {
                    statistics.computeIfAbsent(parts[0], k -> new HashMap<>())
                            .merge(parts[1], 1, Integer::sum);
                }
            }
        } catch (IOException ignored) {}
    }

    @Override
    public void shutdown() {
        datagramSocket.close();
    }

    @Override
    public int receivedMessages() {
        return statistics.values().stream()
                .flatMap(routingKeys -> routingKeys.values().stream())
                .mapToInt(Integer::intValue)
                .sum();
    }

    @Override
    public String getStatistics() {
        StringBuilder sb = new StringBuilder();
        statistics.forEach((serverName, routingKeys) -> {
            sb.append("Server ").append(serverName).append("\n");
            routingKeys.forEach((routingKey, count) ->
                sb.append("  ").append(routingKey).append(" ").append(count).append("\n")
            );
        });
        return sb.toString();
    }

    public static void main(String[] args) {
        ComponentFactory.createMonitoringServer(args[0]).run();
    }
}
