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

public class AY extends Temporal {
    private final Formula arg;
    private final int hash;

    public AY(Formula arg) {
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
        Labels labels = new Labels();
        Labels argLabels = model.calculateLabels(arg);
        for (State<?> state : model.getStates()) {
            if (!state.getPreviousStates().isEmpty() && argLabels.allHaveLabel(state.getPreviousStates())) {
                labels.setLabel(state);
            }
        }
        return labels;
    }

    @Override
    public String toString() {
        return "AY(" + arg + ")";
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        AY that = (AY) o;
        return arg.equals(that.arg);
    }

    @Override
    public int hashCode() {
        return hash;
    }
}
