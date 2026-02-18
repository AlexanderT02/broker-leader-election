package dslab.protocol;

import dslab.entity.Exchange;
import dslab.entity.Queue;
import dslab.util.ExchangeType;
import dslab.util.IOReadWrite;
import lombok.RequiredArgsConstructor;

import java.io.Closeable;
import java.util.Arrays;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@RequiredArgsConstructor
public class SmqpProtocol implements Protocol, Closeable {
    private final ConcurrentHashMap<String, Exchange> exchanges;
    private final ConcurrentHashMap<String, Queue> queues;
    private Exchange currentExchange;
    private Queue currentQueue;
    private Thread subscription;

    @Override
    public Optional<String> processCommand(String[] command, Object... additionalParams) {
        if(subscription != null && !subscription.isInterrupted()) {
            if ("stop".equals(command[0])) {
                this.subscription.interrupt();
            }
            return Optional.empty();
        }
        String returner = switch (command[0]) {
            case "exchange" -> handleExchangeCommand(command);
            case "queue" -> handleQueueCommand(command);
            case "bind" -> handleBindCommand(command);
            case "publish" -> handlePublishCommand(command);
            case "subscribe" -> handleSubscribeCommand((IOReadWrite) additionalParams[0]);
            case "exit" -> handleExitCommand();
            default -> "error usage: <command> <args>";
        };
        return Optional.ofNullable(returner);
    }

    private String handleExchangeCommand(String[] parts) {
        if (parts.length != 3) return "error usage: exchange <type> <name>";
        ExchangeType type = ExchangeType.validType(parts[1]);
        if(type == null) return "error invalid exchange type. Valid types: %s".formatted(ExchangeType.validTypes());

        currentExchange = exchanges.computeIfAbsent(parts[2], routingKey -> new Exchange(type, routingKey));

        return currentExchange.getType().equals(type)
            ? "ok"
            : "error exchange already exists with a different type";
    }

    private String handleQueueCommand(String[] parts) {
        if (parts.length != 2)
            return "error usage: queue <name>";

        currentQueue = queues.computeIfAbsent(parts[1], queueName ->
            exchanges.get("default")
                .getBindingStorage()
                .addBinding(queueName, new Queue(queueName))
        );

        return "ok";
    }

    private String handleBindCommand(String[] parts) {
        if (parts.length != 2) return "error usage: bind <binding-key>";
        if (currentExchange == null) return "error no exchange declared";
        if (currentQueue == null) return "error no queue declared";

        currentExchange.getBindingStorage()
            .addBinding(parts[1], currentQueue);
        return "ok";
    }

    private String handlePublishCommand(String[] parts) {
        if (parts.length < 3) return "error usage: publish <routing-key> <message>";
        if (currentExchange == null) return "error no exchange declared";
        String message = String.join(" ", Arrays.copyOfRange(parts, 2, parts.length));

        currentExchange.getBindingStorage()
            .getQueuesByRoutingKey(parts[1])
            .forEach(queue -> queue.publishMessage(message));

        return "ok";
    }

    private String handleSubscribeCommand(IOReadWrite ioReadWrite) {
        if (currentQueue == null) return "error no queue declared";
        this.subscription = Thread.ofVirtual()
            .start(() -> currentQueue.startMessageDispatch(ioReadWrite));
        return null;
    }

    private String handleExitCommand() { return "ok bye"; }

    @Override
    public void close() {
        Optional.ofNullable(subscription).ifPresent(Thread::interrupt);
    }


}
