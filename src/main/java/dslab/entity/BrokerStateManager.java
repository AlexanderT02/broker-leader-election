package dslab.entity;

import dslab.broker.enums.ElectionState;
import dslab.broker.enums.ElectionType;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
@RequiredArgsConstructor
public class BrokerStateManager {
    @Getter
    private final ElectionType electionType;
    private final AtomicReference<ElectionState> electionStateRef = new AtomicReference<>(ElectionState.FOLLOWER);
    private final AtomicInteger currentLeaderId = new AtomicInteger(-1);
    private final AtomicInteger lastVotedBrokerId = new AtomicInteger(-1);
    private final AtomicLong lastHeartbeatTimestamp = new AtomicLong(System.currentTimeMillis());
    public ElectionState getElectionState() {
        return electionStateRef.get();
    }

    public void setElectionState(ElectionState state) {
        electionStateRef.set(state);
    }

    public int getCurrentLeaderId() {
        return currentLeaderId.get();
    }
    public boolean compareAndSetLastVotedBrokerId(int expected, int newValue) {
        return lastVotedBrokerId.compareAndSet(expected, newValue);
    }

    public void setCurrentLeaderId(int id) {
        currentLeaderId.set(id);
    }

    public int getLastVotedBrokerId() {
        return lastVotedBrokerId.get();
    }

    public void setLastVotedBrokerId(int id) {
        lastVotedBrokerId.set(id);
    }

    public long getLastHeartbeatTimestamp() {
        return lastHeartbeatTimestamp.get();
    }

    public void setLastHeartbeatTimestamp(long timestamp) {
        lastHeartbeatTimestamp.set(timestamp);
    }


}
