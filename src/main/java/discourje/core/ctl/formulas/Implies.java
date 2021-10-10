package discourje.core.ctl.formulas;

import discourje.core.ctl.Formula;
import discourje.core.ctl.Labels;
import discourje.core.ctl.Model;
import discourje.core.ctl.State;
import discourje.core.lts.Action;
import java.util.List;
import java.util.Objects;

public class Implies implements Formula {
    private final Formula lhs;
    private final Formula rhs;
    private final int hash;

    public Implies(Formula lhs, Formula rhs) {
        this.lhs = lhs;
        this.rhs = rhs;
        hash = Objects.hash(this.lhs, this.rhs);
    }

    @Override
    public boolean isTemporal() {
        return lhs.isTemporal() || rhs.isTemporal();
    }

    @Override
    public List<List<Action>> extractWitness(Model<?> model, State<?> source) {
        if (model.hasLabel(source, this)) {
            throw new IllegalArgumentException();
        }

        return rhs.extractWitness(model, source);
    }

    @Override
    public Labels label(Model<?> model) {
        Labels labels = new Labels();
        Labels lhsLabels = model.calculateLabels(lhs);
        Labels rhsLabels = model.calculateLabels(rhs);

        for (State<?> state : model.getStates()) {
            if (!lhsLabels.hasLabel(state) || rhsLabels.hasLabel(state)) {
                labels.setLabel(state);
            }
        }
        return labels;
    }

    @Override
    public String toMCRL2() {
        if (lhs.isAction() && rhs.isAction()) {
            throw new UnsupportedOperationException();
        } else if (lhs.isAction() && !rhs.isAction()) {
            return "[" + lhs.toMCRL2() + "](" + rhs.toMCRL2() + ")";
        } else if (!lhs.isAction() && rhs.isAction()) {
            return "(" + lhs.toMCRL2() + ") => <" + rhs.toMCRL2() + ">true";
        } else if (!lhs.isAction() && !rhs.isAction()) {
            return "(" + lhs.toMCRL2() + ") => (" + rhs.toMCRL2() + ")";
        } else {
            throw new UnsupportedOperationException();
        }
    }

    @Override
    public String toString() {
        return lhs + " => " + rhs;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Implies as = (Implies) o;
        return lhs.equals(as.lhs) &&
                rhs.equals(as.rhs);
    }

    @Override
    public int hashCode() {
        return hash;
    }
}
