package discourje.core.validation.formulas;

import discourje.core.validation.DMState;
import discourje.core.validation.DiscourjeModel;
import java.util.Objects;
import static discourje.core.validation.formulas.CtlFormulas.not;
import static discourje.core.validation.formulas.CtlFormulas.AF;

public class EG implements CtlFormula {
    private final CtlFormula arg;
    private final int hash;

    public EG(CtlFormula arg) {
        this.arg = arg;
        hash = Objects.hash(this.arg);
    }

    @Override
    public void label(DiscourjeModel<?> model) {
        if (!model.isLabelledBy(this)) {
            int labelIndex = model.setLabelledBy(this);
            CtlFormula eg = not(AF(not(arg)));
            eg.label(model);
            int egLabelIndex = model.getLabelIndex(eg);
            for (DMState<?> state : model.getStates()) {
                if (state.hasLabel(egLabelIndex)) {
                    state.addLabel(labelIndex);
                }
            }
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
