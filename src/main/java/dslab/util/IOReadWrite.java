package dslab.util;

import lombok.Getter;

import java.io.BufferedReader;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * A utility class for managing input/output operations over a TCP socket.
 * It provides methods for reading client requests and sending responses.
 *
 * <p>This class simplifies socket communication by:
 * <ul>
 *     <li>Handling incoming client requests with blocking read operations.</li>
 *     <li>Sending responses with automatic line-based management.</li>
 *     <li>Managing resources efficiently with the {@link Closeable} interface.</li>
 * </ul>
 *
 * <p>Usage example:
 * <pre>{@code
 * try (IOReadWrite io = new IOReadWrite(socket)) {
 *     String request = io.readRequest();
 *     io.writeSocketResponses("Response message");
 * } catch (IOException e) {
 *     //handle exception if needed
 * }
 * }</pre>
 */
@Getter
public class IOReadWrite implements Closeable {

    private static final Logger LOG = Logger.getLogger(IOReadWrite.class.getName());
    private final PrintWriter printWriter;
    private final BufferedReader bufferedReader;
    private final Socket socket;

    public IOReadWrite(Socket socket) throws IOException {
        this.socket = socket;
        this.printWriter = new PrintWriter(socket.getOutputStream(), true);
        this.bufferedReader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
    }
    public IOReadWrite(String host, int port) throws IOException {
        this.socket = new Socket(host, port);
        this.printWriter = new PrintWriter(socket.getOutputStream(), true);
        this.bufferedReader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
    }

    public String readRequest() throws IOException {
        try {
            return bufferedReader.readLine();
        } catch (IOException e) {
            throw new IOException("Error reading from the socket", e);
        }
    }


    public void writeSocketResponse(String response) {
        printWriter.println(response);
    }


    public static Optional<IOReadWrite> createConnection(String host, int port) {
        try {
            return Optional.of(new IOReadWrite(host, port));
        } catch (IOException ignored) {
            return Optional.empty();
        }
    }

    @Override
    public void close() {
        try {
            socket.close();
        } catch (IOException e) {
            LOG.log(Level.WARNING, "Error closing the socket", e);
        } finally {
            try {
                bufferedReader.close();
            } catch (IOException e) {
                LOG.log(Level.WARNING, "Error closing the BufferedReader", e);
            }
            printWriter.close();
        }

    }
}
