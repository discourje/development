package discourje.core.validation.formulas;

import discourje.core.validation.DMState;
import discourje.core.validation.DiscourjeModel;
import java.util.Objects;

public class EF implements CtlFormula {
    private final CtlFormula arg;
    private final int hash;

    public EF(CtlFormula arg) {
        this.arg = arg;
        hash = Objects.hash(this.arg);
    }

    @Override
    public void label(DiscourjeModel<?> model) {
        if (!model.isLabelledBy(this)) {
            int labelIndex = model.setLabelledBy(this);
            CtlFormula ef = new EU(True.TRUE, arg);
            ef.label(model);
            int efLabelIndex = model.getLabelIndex(ef);
            for (DMState<?> state : model.getStates()) {
                if (state.hasLabel(efLabelIndex)) {
                    state.addLabel(labelIndex);
                }
            }
        }
    }

    @Override
    public String toString() {
        return "EF(" + arg + ")";
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        EF that = (EF) o;
        return arg.equals(that.arg);
    }

    @Override
    public int hashCode() {
        return hash;
    }
}
