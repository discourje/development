package discourje.core.ctl.formulas.temporal;

import discourje.core.ctl.Formula;
import discourje.core.ctl.Labels;
import discourje.core.ctl.Model;
import discourje.core.ctl.State;
import discourje.core.ctl.formulas.Temporal;
import discourje.core.ctl.formulas.atomic.True;
import discourje.core.lts.Action;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

public class EP extends Temporal {
    private final Formula arg;
    private final int hash;

    public EP(Formula arg) {
        this.arg = arg;
        hash = Objects.hash(this.arg);
    }

    @Override
    public List<List<Action>> extractWitness(Model<?> model, State<?> source) {
        if (model.hasLabel(source, this)) {
            throw new IllegalArgumentException();
        }

        return Collections.emptyList();
    }

    @Override
    public Labels label(Model<?> model) {
        Formula ep = new ES(True.INSTANCE, arg);
        return model.calculateLabels(ep);
    }

    @Override
    public String toString() {
        return "EP(" + arg + ")";
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        EP that = (EP) o;
        return arg.equals(that.arg);
    }

    @Override
    public int hashCode() {
        return hash;
    }
}
