package discourje.core.validation.operators;

import discourje.core.validation.DMState;
import discourje.core.validation.DiscourjeModel;
import java.util.Objects;
import static discourje.core.validation.operators.CtlOperators.AP;
import static discourje.core.validation.operators.CtlOperators.not;

public class EH implements CtlOperator {
    private final CtlOperator arg;
    private final int hash;

    public EH(CtlOperator arg) {
        this.arg = arg;
        hash = Objects.hash(this.arg);
    }

    @Override
    public void label(DiscourjeModel<?> model) {
        if (!model.isLabelledBy(this)) {
            int labelIndex = model.setLabelledBy(this);
            CtlOperator eh = not(AP(not(arg)));
            eh.label(model);
            int ehLabelIndex = model.getLabelIndex(eh);
            for (DMState<?> state : model.getStates()) {
                if (state.hasLabel(ehLabelIndex)) {
                    state.addLabel(labelIndex);
                }
            }
        }
    }

    @Override
    public String toString() {
        return "EH(" + arg + ")";
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        EH that = (EH) o;
        return arg.equals(that.arg);
    }

    @Override
    public int hashCode() {
        return hash;
    }
}
