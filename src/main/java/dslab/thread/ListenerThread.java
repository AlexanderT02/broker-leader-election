package dslab.thread;


import lombok.Builder;
import lombok.RequiredArgsConstructor;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Function;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * {@code ListenerThread} provides a class for handling client connections in a server.
 * It accepts incoming client connections and delegates each connection to a separate task in a thread pool.
 * This allows for efficient and scalable processing of multiple client connections.
 *
 * <p>This class can now be constructed using the builder pattern, providing a more flexible and convenient way
 * to configure and instantiate the thread.</p>
 *
 * <p>The class manages the lifecycle of client connections by performing the following tasks:
 * <ul>
 *     <li>Accepting client connections using a {@link ServerSocket}.</li>
 *     <li>Delegating each connection to a task in a thread pool for processing.</li>
 *     <li>Handling client connections as virtual threads for lightweight, efficient management of concurrent connections.</li>
 *     <li>Gracefully shutting down and cleaning up resources, including closing client and server sockets.</li>
 * </ul>
 *
 * <p>Usage example using the builder pattern:</p>
 * <pre>{@code
 * ListenerThread listener = ListenerThread.builder()
 *     .componentId("MyServer")
 *     .serverSocket(new ServerSocket(8080))
 *     .clientConnectionRunnableProvider(socket -> () -> {
 *         // Handle socket communication
 *     })
 *     .build();
 * listener.start();
 * }</pre>
 *
 * <p>Call {@link #shutdown()} to stop the server and clean up resources.</p>
 */

@RequiredArgsConstructor
@Builder
public class ListenerThread extends Thread {
    private static final Logger LOG = Logger.getLogger(ListenerThread.class.getName());
    private final String componentId;
    private final ServerSocket serverSocket;
    private final Function<Socket, Runnable> clientConnectionRunnable;
    private final ExecutorService threadPool = Executors.newCachedThreadPool(Thread.ofVirtual().factory());
    private final List<Socket> activeClientConnections = new LinkedList<>();

    @Override
    public void run() {
        LOG.log(Level.INFO, String.format("%s started, now listening on port %d", componentId, serverSocket.getLocalPort()));
        while (!serverSocket.isClosed()) {
            try {
                Socket socket = serverSocket.accept();
                activeClientConnections.add(socket);
                threadPool.execute(clientConnectionRunnable.apply(socket));
            } catch (SocketException ignored) {
            } catch (IOException e) {
                LOG.log(Level.SEVERE, "Error while accepting connection from client", e);
            }
        }
    }

    public void shutdown() {
        threadPool.shutdown();
        if (!serverSocket.isClosed()) {
            try {
                serverSocket.close();
            } catch (IOException e) {
                LOG.log(Level.WARNING, "Error while closing server socket", e);
            }
        }

        for (Socket clientSocket : activeClientConnections) {
            if (!clientSocket.isClosed()) {
                try {
                    clientSocket.close();
                } catch (IOException e) {
                    LOG.log(Level.WARNING, "Error while closing client socket", e);
                }
            }
        }

        LOG.info(String.format("%s shutdown complete",this.componentId));
    }

}
