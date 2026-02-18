package dslab.entity;

import dslab.util.IOReadWrite;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.ToString;

import java.util.concurrent.LinkedBlockingQueue;


/**
 * Represents a message queue that stores and dispatches messages to registered subscribers.
 * Subscribers receive messages in the order they are published until the connection is closed
 * or a "stop" command is received.
 *
 * <p>The queue is thread-safe and supports concurrent access using a {@link LinkedBlockingQueue} to store messages.
 * Messages are published into the queue and will be consumed by subscribers one at a time in the order they were received.
 * Each message can only be consumed by a single subscriber. Once a message is taken from the queue, it is no longer available to
 * other subscribers.</p>
 */
@ToString
@EqualsAndHashCode
@Getter
@RequiredArgsConstructor
public class Queue {
    private final String name;
    private final LinkedBlockingQueue<String> messages = new LinkedBlockingQueue<>();

    public void publishMessage(String message) {
        try {
            messages.put(message);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    public void startMessageDispatch(IOReadWrite ioReadWrite) {
        try {
            ioReadWrite.writeSocketResponse("ok");
            while (!Thread.currentThread().isInterrupted() && !ioReadWrite.getSocket().isClosed()) {
                String message = messages.take();
                ioReadWrite.writeSocketResponse(message);
            }
        }  catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
