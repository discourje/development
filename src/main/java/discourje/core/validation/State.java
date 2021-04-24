package discourje.core.validation;

import discourje.core.lts.Action;

import java.util.*;
import java.util.stream.Collectors;

public class State<Spec> {
    private final discourje.core.lts.State state;
    private final Action action;
    private final Collection<State<Spec>> nextStates = new LinkedHashSet<>();
    private final Collection<State<Spec>> previousStates = new LinkedHashSet<>();
    private final BitSet labels = new BitSet();

    public State(discourje.core.lts.State state, Action action) {
        this.state = state;
        this.action = action;
    }

    public discourje.core.lts.State getState() {
        return state;
    }

    public Action getAction() {
        return action;
    }

    public Collection<State<Spec>> getNextStates() {
        return Collections.unmodifiableCollection(nextStates);
    }

    public void addNextState(State<Spec> state) {
        nextStates.add(state);
        state.previousStates.add(this);
    }

    public Collection<State<Spec>> getPreviousStates() {
        return Collections.unmodifiableCollection(previousStates);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        State<?> state = (State<?>) o;
        return this.state.equals(state.state) &&
                Objects.equals(action, state.action);
    }

    @Override
    public int hashCode() {
        return Objects.hash(state, action);
    }

    @Override
    public String toString() {
        return previousStates.stream().map(State::getState).collect(Collectors.toList()) + "(" + previousStates.size() + ")" +
                " -> " + action + "," + state + " -> " +
                nextStates.stream().map(State::getState).collect(Collectors.toList());
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
