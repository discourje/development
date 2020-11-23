package discourje.core.validation.operators;

import discourje.core.validation.DMState;
import discourje.core.validation.DiscourjeModel;
import static discourje.core.validation.operators.CtlOperators.EF;
import static discourje.core.validation.operators.CtlOperators.not;

public class AG implements CtlOperator {
    private final CtlOperator arg;

    public AG(CtlOperator arg) {
        this.arg = arg;
    }

    @Override
    public void label(DiscourjeModel<?> model) {
        CtlOperator ag = not(EF(not(arg)));
        ag.label(model);
        for (DMState<?> state : model.getStates()) {
            if (state.hasLabel(ag)) {
                state.addLabel(this);
            }
        }
    }

    @Override
    public String toString() {
        return "AG(" + arg + ")";
    }
}
