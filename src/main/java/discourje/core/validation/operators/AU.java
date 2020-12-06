package discourje.core.validation.operators;

import discourje.core.validation.DMState;
import discourje.core.validation.DiscourjeModel;
import java.util.LinkedList;
import java.util.Objects;
import java.util.Queue;

class AU implements CtlOperator {
    private final CtlOperator lhs;
    private final CtlOperator rhs;
    private final int hash;

    AU(CtlOperator lhs, CtlOperator rhs) {
        this.lhs = lhs;
        this.rhs = rhs;
        hash = Objects.hash(this.lhs, this.rhs);
    }

    @Override
    public void label(DiscourjeModel<?> model) {
        if (!model.isLabelledBy(this)) {
            int labelIndex = model.setLabelledBy(this);
            lhs.label(model);
            rhs.label(model);
            int lhsLabelIndex = model.getLabelIndex(lhs);
            int rhsLabelIndex = model.getLabelIndex(rhs);

            Queue<DMState<?>> states = new LinkedList<>(model.getStates());
            while (!states.isEmpty()) {
                DMState<?> dmState = states.remove();
                if (dmState.hasLabel(rhsLabelIndex) ||
                        (dmState.hasLabel(lhsLabelIndex) && dmState.successorsExistAndAllHaveLabel(labelIndex))) {
                    if (dmState.addLabel(labelIndex)) {
                        states.addAll(dmState.getPreviousStates());
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
