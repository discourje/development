package discourje.core.validation.operators;

import discourje.core.validation.DMState;
import discourje.core.validation.DiscourjeModel;

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

        boolean newLabels = true;
        while (newLabels) {
            newLabels = false;
            for (DMState<?> dmState : model.getStates()) {
                if (dmState.hasLabel(rhs) ||
                        (dmState.hasLabel(lhs) && dmState.anyPredecessorHasLabel(this))) {
                    newLabels = newLabels || dmState.addLabel(this);
                }
            }
        }

    }

    @Override
    public String toString() {
        return "E(" + lhs + " S " + rhs + ")";
    }
}
