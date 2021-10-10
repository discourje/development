package discourje.core.ctl.formulas.temporal;

import discourje.core.ctl.Labels;
import discourje.core.lts.Action;
import discourje.core.ctl.State;
import discourje.core.ctl.Model;
import discourje.core.ctl.Formula;
import discourje.core.ctl.formulas.Temporal;
import discourje.core.ctl.formulas.atomic.True;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

public class EF extends Temporal {
    private final Formula arg;
    private final int hash;

    public EF(Formula arg) {
        this.arg = arg;
        hash = Objects.hash(this.arg);
    }

    @Override
    public List<List<Action>> extractWitness(Model<?> model, State<?> source) {
        if (!model.hasLabel(source, this)) {
            return Collections.singletonList(Collections.emptyList());
        } else {
            throw new IllegalArgumentException();
        }
    }

    @Override
    public Labels label(Model<?> model) {
        Formula ef = new EU(True.INSTANCE, arg);
        return model.calculateLabels(ef);
    }

    @Override
    public String toMCRL2() {
        if (arg.isAction()) {
            return "<true*><" + arg.toMCRL2() + ">true";
        } else {
            return "<true*>(" + arg.toMCRL2() + ")";
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
