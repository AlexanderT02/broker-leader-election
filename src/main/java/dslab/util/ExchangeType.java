package dslab.util;

import java.util.EnumSet;

/**
 * The {@code ExchangeType} enum defines types of message exchanges, specifying how messages
 * are routed to queues. Supported types include:
 * <ul>
 *     <li>{@link #DEFAULT} - Default direct matching for routing keys.</li>
 *     <li>{@link #DIRECT} - Routes messages directly to queues with exact matching keys.</li>
 *     <li>{@link #FANOUT} - Broadcasts messages to all bound queues, ignoring routing keys.</li>
 *     <li>{@link #TOPIC} - Supports hierarchical key matching with wildcards for flexible routing.</li>
 * </ul>
 *
 * <p>Provides a {@link #toString()} method for lower-case conversion of enum names, and
 * {@link #validTypes()} to retrieve all types as a lowercase string array.
 */
public enum ExchangeType {
    DEFAULT,
    DIRECT,
    FANOUT,
    TOPIC;

    @Override
    public String toString() {
        return name().toLowerCase();
    }

    public static String validTypes() {
        return String.join(", ", EnumSet.allOf(ExchangeType.class).stream()
            .map(Enum::name)
            .toList());
    }
    public static ExchangeType validType(String type) {
        try {
            return ExchangeType.valueOf(type.toUpperCase());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

}
