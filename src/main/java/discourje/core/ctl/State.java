package discourje.core.ctl;

import discourje.core.lts.Action;

import java.util.*;
import java.util.stream.Collectors;

public class State<Spec> {
    private final discourje.core.lts.State state;
    private final Action action;
    private final Collection<State<Spec>> nextStates = new LinkedHashSet<>();
    private final Collection<State<Spec>> previousStates = new LinkedHashSet<>();
    private final int index;

    public State(discourje.core.lts.State state, Action action, int index) {
        this.state = state;
        this.action = action;
        this.index = index;
    }

    public discourje.core.lts.State getState() {
        return state;
    }

    public Action getAction() {
        return action;
    }

    public Collection<State<?>> getNextStates() {
        return Collections.unmodifiableCollection(nextStates);
    }

    public void addNextState(State<Spec> state) {
        nextStates.add(state);
        state.previousStates.add(this);
    }

    public Collection<State<?>> getPreviousStates() {
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

    public int getIndex() {
        return index;
    }
}
