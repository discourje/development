package discourje.core.ctl.formulas.temporal;

import discourje.core.lts.Action;
import discourje.core.ctl.State;
import discourje.core.ctl.Model;
import discourje.core.ctl.Formula;
import discourje.core.ctl.formulas.Temporal;

import java.util.*;

public class EU extends Temporal {
    private final Formula lhs;
    private final Formula rhs;
    private final int hash;

    public EU(Formula lhs, Formula rhs) {
        this.lhs = lhs;
        this.rhs = rhs;
        hash = Objects.hash(this.lhs, this.rhs);
    }

    @Override
    public List<List<Action>> extractWitness(Model<?> model, State<?> source) {
        var i = model.getLabelIndex(this);
        if (!source.hasLabel(i)) {
            return Collections.singletonList(Collections.emptyList());
        } else {
            throw new IllegalArgumentException();
        }
    }

    @Override
    public void label(Model<?> model) {
        if (!model.isLabelledBy(this)) {
            int labelIndex = model.setLabelledBy(this);
            lhs.label(model);
            rhs.label(model);
            int lhsLabelIndex = model.getLabelIndex(lhs);
            int rhsLabelIndex = model.getLabelIndex(rhs);

            Queue<State<?>> states = new LinkedList<>(model.getStates());
            while (!states.isEmpty()) {
                State<?> state = states.remove();
                if (state.hasLabel(rhsLabelIndex) ||
                        (state.hasLabel(lhsLabelIndex) && state.anySuccessorHasLabel(labelIndex))) {
                    if (state.addLabel(labelIndex)) {
                        states.addAll(state.getPreviousStates());
                    }
                }
            }
        }
    }

    @Override
    public String toString() {
        return "EU(" + lhs + "," + rhs + ")";
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        EU as = (EU) o;
        return lhs.equals(as.lhs) &&
                rhs.equals(as.rhs);
    }

    @Override
    public int hashCode() {
        return hash;
    }
}
