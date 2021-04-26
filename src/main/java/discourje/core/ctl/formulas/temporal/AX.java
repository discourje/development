package discourje.core.ctl.formulas.temporal;

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
        var i = model.getLabelIndex(this);
        if (!source.hasLabel(i)) {
            var ii = model.getLabelIndex(arg);
            for (var next : source.getNextStates()) {
                if (!next.hasLabel(ii)) {
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
    public void label(Model<?> model) {
        if (!model.isLabelledBy(this)) {
            int labelIndex = model.setLabelledBy(this);
            arg.label(model);
            int argLabelIndex = model.getLabelIndex(arg);
            for (State<?> state : model.getStates()) {
                if (state.successorsExistAndAllHaveLabel(argLabelIndex)) {
                    state.addLabel(labelIndex);
                }
            }
        }
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
