package discourje.core.validation.operators;

import discourje.core.validation.DMState;
import discourje.core.validation.DiscourjeModel;
import java.util.Objects;

class Not implements CtlOperator {
    private final CtlOperator arg;
    private final int hash;

    Not(CtlOperator args) {
        this.arg = args;
        hash = Objects.hash(this.arg);
    }

    @Override
    public void label(DiscourjeModel<?> model) {
        if (!model.isLabelledBy(this)) {
            arg.label(model);

            for (DMState<?> state : model.getStates()) {
                if (!state.hasLabel(arg)) {
                    state.addLabel(this);
                }
            }
            model.setLabelledBy(this);
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
