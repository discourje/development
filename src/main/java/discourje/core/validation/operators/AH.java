package discourje.core.validation.operators;

import discourje.core.validation.DMState;
import discourje.core.validation.DiscourjeModel;
import static discourje.core.validation.operators.CtlOperators.EP;
import static discourje.core.validation.operators.CtlOperators.not;

public class AH implements CtlOperator {
    private final CtlOperator arg;

    public AH(CtlOperator arg) {
        this.arg = arg;
    }

    @Override
    public void label(DiscourjeModel<?> model) {
        CtlOperator ah = not(EP(not(arg)));
        ah.label(model);
        for (DMState<?> state : model.getStates()) {
            if (state.hasLabel(ah)) {
                state.addLabel(this);
            }
        }
    }

    @Override
    public String toString() {
        return "AH(" + arg + ")";
    }
}
