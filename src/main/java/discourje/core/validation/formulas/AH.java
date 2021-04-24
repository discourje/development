package discourje.core.validation.formulas;

import discourje.core.validation.State;
import discourje.core.validation.Model;
import java.util.Objects;
import static discourje.core.validation.formulas.CtlFormulas.EP;
import static discourje.core.validation.formulas.CtlFormulas.not;

public class AH implements CtlFormula {
    private final CtlFormula arg;
    private final int hash;

    public AH(CtlFormula arg) {
        this.arg = arg;
        hash = Objects.hash(this.arg);
    }

    @Override
    public void label(Model<?> model) {
        if (!model.isLabelledBy(this)) {
            int labelIndex = model.setLabelledBy(this);
            CtlFormula ah = not(EP(not(arg)));
            ah.label(model);
            int ahLabelIndex = model.getLabelIndex(ah);
            for (State<?> state : model.getStates()) {
                if (state.hasLabel(ahLabelIndex)) {
                    state.addLabel(labelIndex);
                }
            }
        }
    }

    @Override
    public String toString() {
        return "AH(" + arg + ")";
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        AH that = (AH) o;
        return arg.equals(that.arg);
    }

    @Override
    public int hashCode() {
        return hash;
    }
}
