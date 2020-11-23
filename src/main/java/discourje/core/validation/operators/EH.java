package discourje.core.validation.operators;

import discourje.core.validation.DMState;
import discourje.core.validation.DiscourjeModel;
import static discourje.core.validation.operators.CtlOperators.AP;
import static discourje.core.validation.operators.CtlOperators.not;

public class EH implements CtlOperator {
    private final CtlOperator arg;

    public EH(CtlOperator arg) {
        this.arg = arg;
    }

    @Override
    public void label(DiscourjeModel<?> model) {
        CtlOperator eh = not(AP(not(arg)));
        eh.label(model);
        for (DMState<?> state : model.getStates()) {
            if (state.hasLabel(eh)) {
                state.addLabel(this);
            }
        }
    }

    @Override
    public String toString() {
        return "EH(" + arg + ")";
    }
}
