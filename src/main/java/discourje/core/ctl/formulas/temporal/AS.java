package discourje.core.ctl.formulas.temporal;

import discourje.core.lts.Action;
import discourje.core.ctl.State;
import discourje.core.ctl.Model;
import discourje.core.ctl.Formula;
import discourje.core.ctl.formulas.Temporal;

import java.util.*;

public class AS extends Temporal {
    private final Formula lhs;
    private final Formula rhs;
    private final int hash;

    public AS(Formula lhs, Formula rhs) {
        this.lhs = lhs;
        this.rhs = rhs;
        hash = Objects.hash(this.lhs, this.rhs);
    }

    @Override
    public List<List<Action>> extractWitness(Model<?> model, State<?> source) {
        var i = model.getLabelIndex(this);
        if (source.hasLabel(i)) {
            throw new IllegalArgumentException();
        }

        return Collections.emptyList();
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
                        (state.hasLabel(lhsLabelIndex) && state.predecessorsExistAndAllHaveLabel(labelIndex))) {
                    if (state.addLabel(labelIndex)) {
                        states.addAll(state.getNextStates());
                    }
                }
            }
        }
    }

    public String toString() {
        return "AS(" + lhs + "," + rhs + ")";
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        AS as = (AS) o;
        return lhs.equals(as.lhs) &&
                rhs.equals(as.rhs);
    }

    @Override
    public int hashCode() {
        return hash;
    }
}
