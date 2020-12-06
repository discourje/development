package discourje.core.validation.operators;

import discourje.core.validation.DMState;
import discourje.core.validation.DiscourjeModel;
import java.util.Objects;
import static discourje.core.validation.operators.CtlOperators.EF;
import static discourje.core.validation.operators.CtlOperators.not;

public class AG implements CtlOperator {
    private final CtlOperator arg;
    private final int hash;

    public AG(CtlOperator arg) {
        this.arg = arg;
        hash = Objects.hash(this.arg);
    }

    @Override
    public void label(DiscourjeModel<?> model) {
        if (!model.isLabelledBy(this)) {
            int labelIndex = model.setLabelledBy(this);
            CtlOperator ag = not(EF(not(arg)));
            ag.label(model);
            int agLabelIndex = model.getLabelIndex(ag);
            for (DMState<?> state : model.getStates()) {
                if (state.hasLabel(agLabelIndex)) {
                    state.addLabel(labelIndex);
                }
            }
        }
    }

    @Override
    public String toString() {
        return "AG(" + arg + ")";
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        AG that = (AG) o;
        return arg.equals(that.arg);
    }

    @Override
    public int hashCode() {
        return hash;
    }
}
