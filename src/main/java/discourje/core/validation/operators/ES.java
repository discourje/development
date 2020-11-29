package discourje.core.validation.operators;

import discourje.core.validation.DMState;
import discourje.core.validation.DiscourjeModel;
import java.util.LinkedList;
import java.util.Queue;

public class ES implements CtlOperator {
    private final CtlOperator lhs;
    private final CtlOperator rhs;

    public ES(CtlOperator lhs, CtlOperator rhs) {
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
                        (dmState.hasLabel(lhs) && dmState.anyPredecessorHasLabel(this))) {
                    if (dmState.addLabel(this)) {
                        states.addAll(dmState.getNextStates());
                    }
                }
        }

    }

    @Override
    public String toString() {
        return "E(" + lhs + " S " + rhs + ")";
    }
}
