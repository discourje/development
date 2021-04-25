package discourje.core.validation.formulas;

import discourje.core.validation.State;
import discourje.core.validation.Model;

import java.util.Objects;

class Not implements CtlFormula {
    private final CtlFormula arg;
    private final int hash;

    Not(CtlFormula args) {
        this.arg = args;
        hash = Objects.hash(this.arg);
    }

    @Override
    public void label(Model<?> model) {
        if (!model.isLabelledBy(this)) {
            int labelIndex = model.setLabelledBy(this);
            arg.label(model);
            int argLabelIndex = model.getLabelIndex(arg);

            for (State<?> state : model.getStates()) {
                if (!state.hasLabel(argLabelIndex)) {
                    state.addLabel(labelIndex);
                }
            }
        }
    }

    @Override
    public String toMCRL2() {
        if (arg.isActionFormula()) {
            return "[" + arg.toMCRL2() + "]false";
        } else {
            return "!(" + arg.toMCRL2() + ")";
        }
    }

    public String toString() {
        return "not(" + arg + ")";
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Not that = (Not) o;
        return arg.equals(that.arg);
    }

    @Override
    public int hashCode() {
        return hash;
    }
}
