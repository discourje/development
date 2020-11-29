package discourje.core.validation.operators;

import discourje.core.validation.DMState;
import discourje.core.validation.DiscourjeModel;
import java.util.Objects;

public class EF implements CtlOperator {
    private final CtlOperator arg;
    private final int hash;

    public EF(CtlOperator arg) {
        this.arg = arg;
        hash = Objects.hash(this.arg);
    }

    @Override
    public void label(DiscourjeModel<?> model) {
        if (!model.isLabelledBy(this)) {
            CtlOperator ef = new EU(True.TRUE, arg);
            ef.label(model);
            for (DMState<?> state : model.getStates()) {
                if (state.hasLabel(ef)) {
                    state.addLabel(this);
                }
            }
            model.setLabelledBy(this);
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
