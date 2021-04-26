package discourje.core.ctl.formulas.temporal;

import discourje.core.lts.Action;
import discourje.core.ctl.State;
import discourje.core.ctl.Model;
import discourje.core.ctl.Formula;
import discourje.core.ctl.formulas.Temporal;

import java.util.*;

public class ES extends Temporal {
    private final Formula lhs;
    private final Formula rhs;
    private final int hash;

    public ES(Formula lhs, Formula rhs) {
        this.lhs = lhs;
        this.rhs = rhs;
        hash = Objects.hash(this.lhs, this.rhs);
    }

    @Override
    public List<List<Action>> extractWitness(Model<?> model, State<?> source) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void label(Model<?> model) {
        if (!model.isLabelledBy(this)) {
            int labelIndex = model.setLabelledBy(this);
            lhs.label(model);
            int lhsIndex = model.getLabelIndex(lhs);
            rhs.label(model);
            int rhsIndex = model.getLabelIndex(rhs);

            Queue<State<?>> states = new LinkedList<>(model.getStates());
            while (!states.isEmpty()) {
                State<?> state = states.remove();
                if (state.hasLabel(rhsIndex) ||
                        (state.hasLabel(lhsIndex) && state.anyPredecessorHasLabel(labelIndex))) {
                    if (state.addLabel(labelIndex)) {
                        states.addAll(state.getNextStates());
                    }
                }
            }
        }
    }

    @Override
    public String toString() {
        return "ES(" + lhs + "," + rhs + ")";
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ES as = (ES) o;
        return lhs.equals(as.lhs) &&
                rhs.equals(as.rhs);
    }

    @Override
    public int hashCode() {
        return hash;
    }
}
