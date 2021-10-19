package discourje.core.ctl.formulas.temporal;

import discourje.core.ctl.Formula;
import discourje.core.ctl.Labels;
import discourje.core.ctl.Model;
import discourje.core.ctl.State;
import discourje.core.ctl.formulas.Temporal;
import discourje.core.lts.Action;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.Queue;

public class AU extends Temporal {
    private final Formula lhs;
    private final Formula rhs;
    private final int hash;

    public AU(Formula lhs, Formula rhs) {
        this.lhs = lhs;
        this.rhs = rhs;
        hash = Objects.hash(this.lhs, this.rhs);
    }

    @Override
    public List<List<Action>> extractWitness(Model<?> model, State<?> source) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Labels label(Model<?> model) {
        Labels labels = new Labels();
        Labels lhsLabels = model.calculateLabels(lhs);
        Labels rhsLabels = model.calculateLabels(rhs);
        Queue<State<?>> states = new LinkedList<>(model.getStates());
        while (!states.isEmpty()) {
            State<?> state = states.remove();
            if (rhsLabels.hasLabel(state) ||
                    (lhsLabels.hasLabel(state) && !state.getNextStates().isEmpty() && labels.allHaveLabel(state.getNextStates()))) {
                if (labels.setLabel(state)) {
                    states.addAll(state.getPreviousStates());
                }
            }
        }
        return labels;
    }

    public String toString() {
        return "AU(" + lhs + "," + rhs + ")";
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        AU as = (AU) o;
        return lhs.equals(as.lhs) &&
                rhs.equals(as.rhs);
    }

    @Override
    public int hashCode() {
        return hash;
    }
}
