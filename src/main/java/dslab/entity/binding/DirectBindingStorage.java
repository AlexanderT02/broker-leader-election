package dslab.entity.binding;

import dslab.entity.Queue;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
public class DirectBindingStorage implements BindingStorage {
    private final ConcurrentHashMap<String, List<Queue>> bindings = new ConcurrentHashMap<>();

    @Override
    public Queue addBinding(String key, Queue queue) {
        bindings.computeIfAbsent(key, k -> new CopyOnWriteArrayList<>())
            .add(queue);
        return queue;
    }

    @Override
    public List<Queue> getQueuesByRoutingKey(String routingKey) {
        return Optional.ofNullable(bindings.get(routingKey))
            .orElseGet(List::of);
    }
}
