package dslab.thread;


import dslab.protocol.Protocol;
import dslab.protocol.SdpProtocol;
import dslab.util.IOReadWrite;
import lombok.RequiredArgsConstructor;

import java.io.IOException;
import java.net.Socket;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
@RequiredArgsConstructor
public class SdpTcpThread implements Runnable {
    private final ConcurrentHashMap<String, String> dnsEntries;
    private final Socket socket;

    @Override
    public void run() {
        Protocol protocol = new SdpProtocol(dnsEntries);
        try (IOReadWrite ioReadWrite = new IOReadWrite(socket)) {

            ioReadWrite.writeSocketResponse("ok SDP");
            String clientRequest;
            while ((clientRequest = ioReadWrite.readRequest()) != null) {
                String[] command = clientRequest.split(" ");
                Optional<String> response = protocol.processCommand(command);
                response.ifPresent(ioReadWrite::writeSocketResponse);
                if("exit".equals(clientRequest)) break;
            }

        }  catch (IOException ignored) {}
    }

}
