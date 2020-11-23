package discourje.core.validation.operators;

import discourje.core.validation.DMState;
import discourje.core.validation.DiscourjeModel;
import static discourje.core.validation.operators.CtlOperators.not;
import static discourje.core.validation.operators.CtlOperators.AF;

public class EG implements CtlOperator {
    private final CtlOperator arg;

    public EG(CtlOperator arg) {
        this.arg = arg;
    }

    @Override
    public void label(DiscourjeModel<?> model) {
        CtlOperator eg = not(AF(not(arg)));
        eg.label(model);
        for (DMState<?> state : model.getStates()) {
            if (state.hasLabel(eg)) {
                state.addLabel(this);
            }
        }
    }

    @Override
    public String toString() {
        return "EG(" + arg + ")";
    }
}
