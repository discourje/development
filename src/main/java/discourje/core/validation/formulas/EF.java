package discourje.core.validation.formulas;

import discourje.core.validation.State;
import discourje.core.validation.Model;
import java.util.Objects;

public class EF implements CtlFormula {
    private final CtlFormula arg;
    private final int hash;

    public EF(CtlFormula arg) {
        this.arg = arg;
        hash = Objects.hash(this.arg);
    }

    @Override
    public void label(Model<?> model) {
        if (!model.isLabelledBy(this)) {
            int labelIndex = model.setLabelledBy(this);
            CtlFormula ef = new EU(True.TRUE, arg);
            ef.label(model);
            int efLabelIndex = model.getLabelIndex(ef);
            for (State<?> state : model.getStates()) {
                if (state.hasLabel(efLabelIndex)) {
                    state.addLabel(labelIndex);
                }
            }
        }
    }

    @Override
    public String toMCRL2() {
        if (arg.isActionFormula()) {
            return "<true*><" + arg.toMCRL2() + ">true";
        } else {
            return "<true*>(" + arg.toMCRL2() + ")";
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
