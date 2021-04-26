package discourje.core.ctl.formulas;

import discourje.core.lts.Action;
import discourje.core.ctl.Formula;
import discourje.core.ctl.State;
import discourje.core.ctl.Model;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

public class Not implements Formula {
    private final Formula arg;
    private final int hash;

    public Not(Formula args) {
        this.arg = args;
        hash = Objects.hash(this.arg);
    }

    @Override
    public boolean isTemporal() {
        return arg.isTemporal();
    }

    @Override
    public List<List<Action>> extractWitness(Model<?> model, State<?> source) {
        var i = model.getLabelIndex(this);
        if (!isTemporal() && !source.hasLabel(i)) {
            return Collections.singletonList(Collections.emptyList());
        } else {
            throw new IllegalArgumentException();
        }
    }

    @Override
    public void label(Model<?> model) {
        if (!model.isLabelledBy(this)) {
            int labelIndex = model.setLabelledBy(this);
            arg.label(model);
            int argLabelIndex = model.getLabelIndex(arg);

            for (State<?> state : model.getStates()) {
                if (!state.hasLabel(argLabelIndex)) {
                    state.addLabel(labelIndex);
                }
            }
        }
    }

    @Override
    public String toMCRL2() {
        if (arg.isAction()) {
            return "[" + arg.toMCRL2() + "]false";
        } else {
            return "!(" + arg.toMCRL2() + ")";
        }
    }

    public String toString() {
        return "!(" + arg + ")";
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Not that = (Not) o;
        return arg.equals(that.arg);
    }

    @Override
    public int hashCode() {
        return hash;
    }
}
