package discourje.core.validation.operators;

import discourje.core.validation.DMState;
import discourje.core.validation.DiscourjeModel;
import java.util.LinkedList;
import java.util.Queue;

class AU implements CtlOperator {
    private final CtlOperator lhs;
    private final CtlOperator rhs;

    AU(CtlOperator lhs, CtlOperator rhs) {
        this.lhs = lhs;
        this.rhs = rhs;
    }

    @Override
    public void label(DiscourjeModel<?> model) {
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
    }

    public String toString() {
        return "A(" + lhs + " U " + rhs + ")";
    }
}
