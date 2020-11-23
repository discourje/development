package discourje.core.validation.operators;

import discourje.core.validation.DMState;
import discourje.core.validation.DiscourjeModel;

class AS implements CtlOperator {
    private final CtlOperator lhs;
    private final CtlOperator rhs;

    AS(CtlOperator lhs, CtlOperator rhs) {
        this.lhs = lhs;
        this.rhs = rhs;
    }

    @Override
    public void label(DiscourjeModel<?> model) {
        lhs.label(model);
        rhs.label(model);

        boolean newLabels;
        do {
            newLabels = false;
            for (DMState<?> dmState : model.getStates()) {
                if (dmState.hasLabel(rhs) ||
                        (dmState.hasLabel(lhs) && dmState.predecessorsExistAndAllHaveLabel(this))) {
                    newLabels = newLabels || dmState.addLabel(this);
                }
            }
        } while (newLabels);
    }

    public String toString() {
        return "A(" + lhs + " S " + rhs + ")";
    }
}
