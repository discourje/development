package discourje.core.ctl.formulas.temporal;

import discourje.core.lts.Action;
import discourje.core.ctl.State;
import discourje.core.ctl.Model;
import discourje.core.ctl.Formula;
import discourje.core.ctl.formulas.Temporal;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

import static discourje.core.ctl.Formulas.AP;
import static discourje.core.ctl.Formulas.not;

public class EH extends Temporal {
    private final Formula arg;
    private final int hash;

    public EH(Formula arg) {
        this.arg = arg;
        hash = Objects.hash(this.arg);
    }

    @Override
    public List<List<Action>> extractWitness(Model<?> model, State<?> source) {
        var i = model.getLabelIndex(this);
        if (source.hasLabel(i)) {
            throw new IllegalArgumentException();
        }

        return Collections.emptyList();
    }

    @Override
    public void label(Model<?> model) {
        if (!model.isLabelledBy(this)) {
            int labelIndex = model.setLabelledBy(this);
            Formula eh = not(AP(not(arg)));
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
