package discourje.core.validation;

import discourje.core.lts.Action;
import discourje.core.lts.State;
import discourje.core.validation.operators.CtlOperator;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Objects;

public class DMState<Spec> {
    private final State<Spec> state;
    private final Action action;
    private final Collection<DMState<Spec>> transitions = new ArrayList<>();
    private final Collection<CtlOperator> labels = new ArrayList<>();

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

    public boolean addLabel(CtlOperator operator) {
        if (labels.contains(operator)) {
            return false;
        } else {
            return labels.add(operator);
        }
    }

    public boolean hasLabel(CtlOperator operator) {
        return labels.contains(operator);
    }

    public boolean successorsExistAndAllHaveLabel(CtlOperator operator) {
        long successorsWithLabelCount = transitions.stream()
                .filter(s -> s.hasLabel(operator))
                .count();
        int numSuccessors = transitions.size();
        return numSuccessors > 0 && numSuccessors == successorsWithLabelCount;
    }

    public boolean anySuccessorHasLabel(CtlOperator operator) {
        return transitions.stream()
                .anyMatch(s -> s.hasLabel(operator));
    }

    public Collection<CtlOperator> getLabels() {
        return Collections.unmodifiableCollection(labels);
    }
}
