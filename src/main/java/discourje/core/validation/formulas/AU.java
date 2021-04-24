package discourje.core.validation.formulas;

import discourje.core.validation.State;
import discourje.core.validation.Model;
import java.util.LinkedList;
import java.util.Objects;
import java.util.Queue;

class AU implements CtlFormula {
    private final CtlFormula lhs;
    private final CtlFormula rhs;
    private final int hash;

    AU(CtlFormula lhs, CtlFormula rhs) {
        this.lhs = lhs;
        this.rhs = rhs;
        hash = Objects.hash(this.lhs, this.rhs);
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
                        (state.hasLabel(lhsLabelIndex) && state.successorsExistAndAllHaveLabel(labelIndex))) {
                    if (state.addLabel(labelIndex)) {
                        states.addAll(state.getPreviousStates());
                    }
                }
            }
        }
    }

    public String toString() {
        return "A(" + lhs + " U " + rhs + ")";
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
