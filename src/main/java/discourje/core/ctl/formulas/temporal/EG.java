package discourje.core.ctl.formulas.temporal;

import discourje.core.lts.Action;
import discourje.core.ctl.State;
import discourje.core.ctl.Model;
import discourje.core.ctl.Formula;
import discourje.core.ctl.formulas.Temporal;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import static discourje.core.ctl.Formulas.not;
import static discourje.core.ctl.Formulas.AF;

public class EG extends Temporal {
    private final Formula arg;
    private final int hash;

    public EG(Formula arg) {
        this.arg = arg;
        hash = Objects.hash(this.arg);
    }

    @Override
    public List<List<Action>> extractWitness(Model<?> model, State<?> source) {
        var i = model.getLabelIndex(this);
        if (!source.hasLabel(i)) {
            return Collections.singletonList(Collections.emptyList());
        } else {
            throw new IllegalArgumentException();
        }
    }

    @Override
    public void label(Model<?> model) {
        if (!model.isLabelledBy(this)) {
            int labelIndex = model.setLabelledBy(this);
            Formula eg = not(AF(not(arg)));
            eg.label(model);
            int egLabelIndex = model.getLabelIndex(eg);
            for (State<?> state : model.getStates()) {
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
