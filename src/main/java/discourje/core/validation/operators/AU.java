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
            lhs.label(model);
            rhs.label(model);

            Queue<DMState<?>> states = new LinkedList<>(model.getStates());
            while (!states.isEmpty()) {
                DMState<?> dmState = states.remove();
                if (dmState.hasLabel(rhs) ||
                        (dmState.hasLabel(lhs) && dmState.successorsExistAndAllHaveLabel(this))) {
                    if (dmState.addLabel(this)) {
                        states.addAll(dmState.getPreviousStates());
                    }
                }
            }
            model.setLabelledBy(this);
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
