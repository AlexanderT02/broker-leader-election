package dslab.thread;

import dslab.config.Config;
import dslab.entity.Exchange;
import dslab.entity.Queue;
import dslab.protocol.SmqpProtocol;
import dslab.util.IOReadWrite;
import lombok.RequiredArgsConstructor;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.Socket;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
@RequiredArgsConstructor
public class SmqpTcpThread implements Runnable {
    private final ConcurrentHashMap<String, Exchange> exchanges;
    private final ConcurrentHashMap<String, Queue> queues;
    private final Socket socket;

    @Override
    public void run() {
        try (SmqpProtocol protocol = new SmqpProtocol(exchanges, queues);
             IOReadWrite ioReadWrite = new IOReadWrite(socket)) {

            ioReadWrite.writeSocketResponse("ok SMQP");
            String clientRequest;
            while ((clientRequest = ioReadWrite.readRequest()) != null) {
                String[] command = clientRequest.split(" ");
                Optional<String> response = protocol.processCommand(command, ioReadWrite);
                response.ifPresent(ioReadWrite::writeSocketResponse);
                if ("publish".equals(command[0]) && response.filter("ok"::equals).isPresent()) {
                    sendUdpMonitoringMessage(ioReadWrite, command[1]);
                }
                else if("exit".equals(clientRequest)) break;
            }

        } catch (IOException ignored){}
    }
    private void sendUdpMonitoringMessage(IOReadWrite ioReadWrite, String routingKey){
        try (DatagramSocket datagramSocket = new DatagramSocket()) {
            String udpMessage = String.format("%s:%d %s",
                    ioReadWrite.getSocket().getLocalAddress().getHostAddress(),
                    ioReadWrite.getSocket().getLocalPort(),
                    routingKey);
            DatagramPacket packet = new DatagramPacket(
                    udpMessage.getBytes(),
                    udpMessage.length(),
                    InetAddress.getLocalHost(),
                    new Config("monitoring-0.properties").getInt("monitoring.port"));
            datagramSocket.send(packet);
        } catch (IOException ignored) {}
    }
}
