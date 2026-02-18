package dslab.entity.binding;

import dslab.entity.Queue;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class FanoutBindingStorage implements BindingStorage {
    private final List<Queue> queues = new CopyOnWriteArrayList<>();

    @Override
    public Queue addBinding(String key, Queue queue) {
        queues.add(queue);
        return queue;
    }

    @Override
    public List<Queue> getQueuesByRoutingKey(String routingKey) {
        return queues;
    }
}
