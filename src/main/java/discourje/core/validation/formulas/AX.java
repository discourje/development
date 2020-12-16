package discourje.core.validation.formulas;

import discourje.core.validation.DMState;
import discourje.core.validation.DiscourjeModel;
import java.util.Objects;

public class AX implements CtlFormula {
    private final CtlFormula arg;
    private final int hash;

    public AX(CtlFormula arg) {
        this.arg = arg;
        hash = Objects.hash(this.arg);
    }

    @Override
    public void label(DiscourjeModel<?> model) {
        if (!model.isLabelledBy(this)) {
            int labelIndex = model.setLabelledBy(this);
            arg.label(model);
            int argLabelIndex = model.getLabelIndex(arg);
            for (DMState<?> state : model.getStates()) {
                if (state.successorsExistAndAllHaveLabel(argLabelIndex)) {
                    state.addLabel(labelIndex);
                }
            }
        }
    }

    @Override
    public String toString() {
        return "AX(" + arg + ")";
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        AX that = (AX) o;
        return arg.equals(that.arg);
    }

    @Override
    public int hashCode() {
        return hash;
    }
}
