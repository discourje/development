package discourje.core.validation;

import discourje.core.lts.Action;
import discourje.core.lts.State;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.Collection;
import java.util.Collections;
import java.util.Objects;

public class DMState<Spec> {
    private final State<Spec> state;
    private final Action action;
    private final Collection<DMState<Spec>> nextStates = new ArrayList<>();
    private final Collection<DMState<Spec>> previousStates = new ArrayList<>();
    private final BitSet labels = new BitSet();

    public DMState(State<Spec> state, Action action) {
        this.state = state;
        this.action = action;
    }

    public State<Spec> getState() {
        return state;
    }

    public Action getAction() {
        return action;
    }

    public Collection<DMState<Spec>> getNextStates() {
        return Collections.unmodifiableCollection(nextStates);
    }

    public void addNextState(DMState<Spec> state) {
        nextStates.add(state);
        state.previousStates.add(this);
    }

    public Collection<DMState<Spec>> getPreviousStates() {
        return Collections.unmodifiableCollection(previousStates);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        DMState<?> dmState = (DMState<?>) o;
        return state.equals(dmState.state) &&
                Objects.equals(action, dmState.action);
    }

    @Override
    public int hashCode() {
        return Objects.hash(state, action);
    }

    public boolean addLabel(int labelIndex) {
        if (labels.get(labelIndex)) {
            return false;
        } else {
            labels.set(labelIndex);
            return true;
        }
    }

    public boolean hasLabel(int labelIndex) {
        return labels.get(labelIndex);
    }

    public boolean successorsExistAndAllHaveLabel(int labelIndex) {
        long successorsWithLabelCount = nextStates.stream()
                .filter(s -> s.hasLabel(labelIndex))
                .count();
        int numSuccessors = nextStates.size();
        return numSuccessors > 0 && numSuccessors == successorsWithLabelCount;
    }

    public boolean anySuccessorHasLabel(int labelIndex) {
        return nextStates.stream()
                .anyMatch(s -> s.hasLabel(labelIndex));
    }

    public boolean predecessorsExistAndAllHaveLabel(int labelIndex) {
        long precedersWithLabelCount = previousStates.stream()
                .filter(s -> s.hasLabel(labelIndex))
                .count();
        int numPreceders = previousStates.size();
        return numPreceders > 0 && numPreceders == precedersWithLabelCount;
    }

    public boolean anyPredecessorHasLabel(int labelIndex) {
        return previousStates.stream()
                .anyMatch(s -> s.hasLabel(labelIndex));
    }
}
