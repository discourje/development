package discourje.core.validation.formulas;

import discourje.core.validation.State;
import discourje.core.validation.Model;
import java.util.Objects;

public class AP implements CtlFormula {
    private final CtlFormula arg;
    private final int hash;

    public AP(CtlFormula arg) {
        this.arg = arg;
        hash = Objects.hash(this.arg);
    }

    @Override
    public void label(Model<?> model) {
        if (!model.isLabelledBy(this)) {
            int labelIndex = model.setLabelledBy(this);
            CtlFormula ap = new AS(True.TRUE, arg);
            ap.label(model);
            int apLabelIndex = model.getLabelIndex(ap);
            for (State<?> state : model.getStates()) {
                if (state.hasLabel(apLabelIndex)) {
                    state.addLabel(labelIndex);
                }
            }
        }
    }

    @Override
    public String toString() {
        return "AP(" + arg + ")";
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        AP that = (AP) o;
        return arg.equals(that.arg);
    }

    @Override
    public int hashCode() {
        return hash;
    }
}
