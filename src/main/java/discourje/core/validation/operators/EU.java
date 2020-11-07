package discourje.core.validation.operators;

import discourje.core.validation.DMState;
import discourje.core.validation.DiscourjeModel;

class EU implements CtlOperator {
    private final CtlOperator lhs;
    private final CtlOperator rhs;

    EU(CtlOperator lhs, CtlOperator rhs) {
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
                        (dmState.hasLabel(lhs) && dmState.anySuccessorHasLabel(this))) {
                    newLabels = newLabels || dmState.addLabel(this);
                }
            }
        }
    }
}
