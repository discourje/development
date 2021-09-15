package discourje.core.ctl.formulas.temporal;

import discourje.core.ctl.formulas.And;
import discourje.core.ctl.formulas.Not;
import discourje.core.ctl.formulas.atomic.Init;
import discourje.core.lts.Action;
import discourje.core.ctl.State;
import discourje.core.ctl.Model;
import discourje.core.ctl.Formula;
import discourje.core.ctl.formulas.Temporal;
import discourje.core.ctl.formulas.atomic.True;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

public class AP extends Temporal {
    private final Formula arg;
    private final int hash;

    public AP(Formula arg) {
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
            Formula ap = new AS(True.INSTANCE, arg);
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
