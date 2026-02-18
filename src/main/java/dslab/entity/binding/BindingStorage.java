package dslab.entity.binding;

import dslab.entity.Queue;
import dslab.util.ExchangeType;

import java.util.List;

/**
 * The {@code BindingStorage} interface defines the methods for managing the relationships between
 * routing keys and queues in a message broker system. These relationships are essential for routing
 * messages to the correct queues based on exchange types such as {@link ExchangeType#FANOUT},
 * {@link ExchangeType#DIRECT}, and {@link ExchangeType#TOPIC}.
 *
 * <p>Each implementation of {@code BindingStorage} provides a way to bind queues to routing keys
 * and retrieve the queues associated with a given routing key. This ensures messages are routed
 * to the appropriate queues according to the exchange type.</p>
 */
public interface BindingStorage {

    /**
     * Adds a new binding between the given routing key parts and the specified queue.
     *
     * @param key A string representing the routing key.
     * @param queue The {@link Queue} to associate with the routing key.
     * @return the provided {@link Queue} after the binding is successfully added.
     */
    Queue addBinding(String key, Queue queue);

    /**
     * Retrieves a list of queues associated with the given routing key for the specified exchange type.
     * The routing key could be used with a variety of exchange types (e.g., FANOUT, DIRECT, TOPIC).
     *
     * @param routingKey The routing key for which to retrieve the associated queues.
     * @return A list of {@link Queue} objects associated with the routing key.
     */
    List<Queue> getQueuesByRoutingKey(String routingKey);

}
