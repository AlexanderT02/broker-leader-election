package dslab.entity;

import dslab.entity.binding.BindingStorage;
import dslab.entity.binding.DirectBindingStorage;
import dslab.entity.binding.FanoutBindingStorage;
import dslab.entity.binding.TopicBindingStorage;
import dslab.util.ExchangeType;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;


/**
 * The {@code Exchange} class represents an exchange in a message broker system.
 * It is responsible for managing queue bindings based on different {@link ExchangeType}s
 * and routing messages to queues according to the exchange's type.
 *
 * <p>This class supports:
 * <ul>
 *     <li>Adding bindings between queues and specific routing keys.</li>
 *     <li>Retrieving queues that match a given routing key according to the exchange's type.</li>
 *     <li>Support for different exchange types: {@link ExchangeType#FANOUT}, {@link ExchangeType#TOPIC}, {@link ExchangeType#DIRECT}, and a default exchange.</li>
 * </ul>
 *
 * @see BindingStorage
 * @see TopicBindingStorage
 * @see DirectBindingStorage
 * @see FanoutBindingStorage
 */
@Getter
@AllArgsConstructor
@ToString
@EqualsAndHashCode
public class Exchange {

    private final ExchangeType type;
    private final String name;
    private BindingStorage bindingStorage;

    public Exchange(ExchangeType type, String name) {
        this.type = type;
        this.name = name;
        this.bindingStorage = switch (type) {
            case FANOUT -> new FanoutBindingStorage();
            case TOPIC -> new TopicBindingStorage();
            case DIRECT, DEFAULT -> new DirectBindingStorage();
        };
    }
}

