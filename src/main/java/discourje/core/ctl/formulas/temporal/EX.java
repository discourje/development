package discourje.core.ctl.formulas.temporal;

import discourje.core.ctl.Labels;
import discourje.core.lts.Action;
import discourje.core.ctl.State;
import discourje.core.ctl.Model;
import discourje.core.ctl.Formula;
import discourje.core.ctl.formulas.Temporal;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

public class EX extends Temporal {
    private final Formula arg;
    private final int hash;

    public EX(Formula arg) {
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
        Labels labels = new Labels();
        Labels argLabels = model.calculateLabels(arg);
        for (State<?> state : model.getStates()) {
            if (!state.getNextStates().isEmpty() && argLabels.anyHaveLabel(state.getNextStates())) {
                labels.setLabel(state);
            }
        }
        return labels;
    }

    @Override
    public String toString() {
        return "EX(" + arg + ")";
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        EX that = (EX) o;
        return arg.equals(that.arg);
    }

    @Override
    public int hashCode() {
        return hash;
    }
}
