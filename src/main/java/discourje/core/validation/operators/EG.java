package discourje.core.validation.operators;

import discourje.core.validation.DMState;
import discourje.core.validation.DiscourjeModel;
import java.util.Objects;
import static discourje.core.validation.operators.CtlOperators.not;
import static discourje.core.validation.operators.CtlOperators.AF;

public class EG implements CtlOperator {
    private final CtlOperator arg;
    private final int hash;

    public EG(CtlOperator arg) {
        this.arg = arg;
        hash = Objects.hash(this.arg);
    }

    @Override
    public void label(DiscourjeModel<?> model) {
        if (!model.isLabelledBy(this)) {
            CtlOperator eg = not(AF(not(arg)));
            eg.label(model);
            for (DMState<?> state : model.getStates()) {
                if (state.hasLabel(eg)) {
                    state.addLabel(this);
                }
            }
            model.setLabelledBy(this);
        }
    }

    @Override
    public String toString() {
        return "EG(" + arg + ")";
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        EG that = (EG) o;
        return arg.equals(that.arg);
    }

    @Override
    public int hashCode() {
        return hash;
    }
}
