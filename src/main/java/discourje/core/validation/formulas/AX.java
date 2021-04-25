package discourje.core.validation.formulas;

import discourje.core.validation.State;
import discourje.core.validation.Model;
import java.util.Objects;

public class AX implements CtlFormula {
    private final CtlFormula arg;
    private final int hash;

    public AX(CtlFormula arg) {
        this.arg = arg;
        hash = Objects.hash(this.arg);
    }

    @Override
    public void label(Model<?> model) {
        if (!model.isLabelledBy(this)) {
            int labelIndex = model.setLabelledBy(this);
            arg.label(model);
            int argLabelIndex = model.getLabelIndex(arg);
            for (State<?> state : model.getStates()) {
                if (state.successorsExistAndAllHaveLabel(argLabelIndex)) {
                    state.addLabel(labelIndex);
                }
            }
        }
    }

    @Override
    public String toMCRL2() {
        if (arg.isActionFormula()) {
            return "<true>true && [true]<" + arg.toMCRL2() + ">true";
        } else {
            return "<true>true && [true](" + arg.toMCRL2() + ")";
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
