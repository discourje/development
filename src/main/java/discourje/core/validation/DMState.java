package discourje.core.validation;

import discourje.core.lts.Action;
import discourje.core.lts.State;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Objects;

public class DMState<Spec> {
    private final State<Spec> state;
    private final Action action;
    private Collection<DMState<Spec>> transitions = new ArrayList<>();

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

    public Collection<DMState<Spec>> getTransitions() {
        return Collections.unmodifiableCollection(transitions);
    }

    public boolean addTransition(DMState<Spec> target) {
        return transitions.add(target);
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
}
