package dslab.thread;


import dslab.config.BrokerConfig;
import dslab.entity.BrokerStateManager;
import dslab.protocol.LepProtocol;
import dslab.util.IOReadWrite;
import lombok.RequiredArgsConstructor;

import java.io.IOException;
import java.net.Socket;
import java.util.Optional;

@RequiredArgsConstructor
public class LepTcpThread implements Runnable  {
    private final Socket socket;
    private final BrokerConfig brokerConfig;
    private final BrokerStateManager brokerStateManager;

    @Override
    public void run() {
        try (IOReadWrite io = new IOReadWrite(socket);
             LepProtocol lepProtocol = new LepProtocol(brokerConfig, brokerStateManager)) {
            String message;
            io.writeSocketResponse("ok LEP");
            while ((message = io.readRequest()) != null){
                String[] command = message.split(" ");
                Optional<String> response = lepProtocol.processCommand(command, io);
                response.ifPresent(io::writeSocketResponse);
                if (response.isPresent() && "error usage: <command> <args>".equals(response.get())) {
                    break;
                }
            }

        } catch (IOException ignored) {}
    }

}
