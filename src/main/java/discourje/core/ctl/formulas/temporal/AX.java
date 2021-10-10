package discourje.core.ctl.formulas.temporal;

import discourje.core.ctl.Labels;
import discourje.core.lts.Action;
import discourje.core.ctl.State;
import discourje.core.ctl.Model;
import discourje.core.ctl.Formula;
import discourje.core.ctl.formulas.Temporal;

import java.util.*;

public class AX extends Temporal {
    private final Formula arg;
    private final int hash;

    public AX(Formula arg) {
        this.arg = arg;
        hash = Objects.hash(this.arg);
    }

    @Override
    public List<List<Action>> extractWitness(Model<?> model, State<?> source) {
        if (!model.hasLabel(source, this)) {
            var argLabels = model.getLabels(arg);
            for (var next : source.getNextStates()) {
                if (!argLabels.hasLabel(next)) {
                    var segments = new ArrayList<List<Action>>();
                    segments.add(Collections.singletonList(next.getAction()));
                    segments.addAll(arg.extractWitness(model, next));
                    return segments;
                }
            }
        }
        throw new IllegalArgumentException();
    }

    @Override
    public Labels label(Model<?> model) {
        Labels labels = new Labels();
        Labels argLabels = model.calculateLabels(arg);
        for (State<?> state : model.getStates()) {
            if (!state.getNextStates().isEmpty() && argLabels.allHaveLabel(state.getNextStates())) {
                labels.setLabel(state);
            }
        }
        return labels;
    }

    @Override
    public String toMCRL2() {
        if (arg.isAction()) {
            return "<true>true && [true]<" + arg.toMCRL2() + ">true";
        } else {
            return "<true>true && [true](" + arg.toMCRL2() + ")";
        }
    }

    @Override
    public String toString() {
        return "AX(" + arg + ")";
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        AX that = (AX) o;
        return arg.equals(that.arg);
    }

    @Override
    public int hashCode() {
        return hash;
    }
}
