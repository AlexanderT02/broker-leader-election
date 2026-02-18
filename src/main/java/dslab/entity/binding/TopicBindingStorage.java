package dslab.entity.binding;

import dslab.entity.Queue;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

public class TopicBindingStorage implements BindingStorage {
    private final TrieNode root = new TrieNode();

    @Override
    public Queue addBinding(String key, Queue queue) {
        TrieNode currentNode = root;
        String[] parts = key.split("\\.");
        for (String part : parts) {
            currentNode = currentNode.children.computeIfAbsent(part, k -> new TrieNode());
        }
        currentNode.queues.add(queue);
        return queue;
    }

    @Override
    public List<Queue> getQueuesByRoutingKey(String routingKey) {
        List<Queue> result = new LinkedList<>();
        String[] parts = routingKey.split("\\.");
        searchTopicMatches(root, parts, 0, result);
        return result;
    }

    private void searchTopicMatches(TrieNode node, String[] parts, int index, List<Queue> result) {
        if (index == parts.length) {
            result.addAll(node.queues);

            Optional.ofNullable(node.children.get("#"))
                .ifPresent(wildcardNode -> searchTopicMatches(wildcardNode, parts, index, result));
            return;
        }

        Optional.ofNullable(node.children.get("#"))
            .ifPresent(wildcardNode -> {
                for (int i = index; i <= parts.length; i++) {
                    searchTopicMatches(wildcardNode, parts, i, result);
                }
            });

        Optional.ofNullable(node.children.get("*"))
            .filter(starNode -> index < parts.length)
            .ifPresent(starNode -> searchTopicMatches(starNode, parts, index + 1, result));

        Optional.ofNullable(node.children.get(parts[index]))
            .ifPresent(partNode -> searchTopicMatches(partNode, parts, index + 1, result));
    }

    private static class TrieNode {
        private final Map<String, TrieNode> children = new ConcurrentHashMap<>();
        private final List<Queue> queues = new CopyOnWriteArrayList<>();
    }
}
