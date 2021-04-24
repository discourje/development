package discourje.core.validation.formulas;

import discourje.core.validation.State;
import discourje.core.validation.Model;
import java.util.LinkedList;
import java.util.Objects;
import java.util.Queue;

public class ES implements CtlFormula {
    private final CtlFormula lhs;
    private final CtlFormula rhs;
    private final int hash;

    public ES(CtlFormula lhs, CtlFormula rhs) {
        this.lhs = lhs;
        this.rhs = rhs;
        hash = Objects.hash(this.lhs, this.rhs);
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
        return "E(" + lhs + " S " + rhs + ")";
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
