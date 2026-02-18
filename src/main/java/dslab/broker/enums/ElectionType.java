package dslab.broker.enums;

public enum ElectionType {
    NONE,
    RING,
    BULLY,
    RAFT;

    public static ElectionType fromString(String electionType) {
        try {
            return ElectionType.valueOf(electionType.toUpperCase());
        } catch (IllegalArgumentException | NullPointerException e) {
            return NONE;
        }
    }
}
