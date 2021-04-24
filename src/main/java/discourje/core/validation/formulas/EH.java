package discourje.core.validation.formulas;

import discourje.core.validation.State;
import discourje.core.validation.Model;
import java.util.Objects;
import static discourje.core.validation.formulas.CtlFormulas.AP;
import static discourje.core.validation.formulas.CtlFormulas.not;

public class EH implements CtlFormula {
    private final CtlFormula arg;
    private final int hash;

    public EH(CtlFormula arg) {
        this.arg = arg;
        hash = Objects.hash(this.arg);
    }

    @Override
    public void label(Model<?> model) {
        if (!model.isLabelledBy(this)) {
            int labelIndex = model.setLabelledBy(this);
            CtlFormula eh = not(AP(not(arg)));
            eh.label(model);
            int ehLabelIndex = model.getLabelIndex(eh);
            for (State<?> state : model.getStates()) {
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
