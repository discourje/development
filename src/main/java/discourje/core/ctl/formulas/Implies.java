package discourje.core.ctl.formulas;

import discourje.core.lts.Action;
import discourje.core.ctl.Formula;
import discourje.core.ctl.State;
import discourje.core.ctl.Model;

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
        var i = model.getLabelIndex(this);
        if (source.hasLabel(i)) {
            throw new IllegalArgumentException();
        }

        return rhs.extractWitness(model, source);
    }

    @Override
    public void label(Model<?> model) {
        if (!model.isLabelledBy(this)) {
            int labelIndex = model.setLabelledBy(this);
            lhs.label(model);
            rhs.label(model);
            int lhsLabelIndex = model.getLabelIndex(lhs);
            int rhsLabelIndex = model.getLabelIndex(rhs);

            for (State<?> state : model.getStates()) {
                if (!state.hasLabel(lhsLabelIndex) || state.hasLabel(rhsLabelIndex)) {
                    state.addLabel(labelIndex);
                }
            }
        }
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
