package discourje.core.validation.formulas;

import discourje.core.validation.State;
import discourje.core.validation.Model;

import java.util.Objects;

public class Implies implements CtlFormula {
    private final CtlFormula lhs;
    private final CtlFormula rhs;
    private final int hash;

    public Implies(CtlFormula lhs, CtlFormula rhs) {
        this.lhs = lhs;
        this.rhs = rhs;
        hash = Objects.hash(this.lhs, this.rhs);
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
        if (lhs.isActionFormula() && rhs.isActionFormula()) {
            throw new UnsupportedOperationException();
        } else if (lhs.isActionFormula() && !rhs.isActionFormula()) {
            return "[" + lhs.toMCRL2() + "](" + rhs.toMCRL2() + ")";
        } else if (!lhs.isActionFormula() && rhs.isActionFormula()) {
            return "(" + lhs.toMCRL2() + ") => <" + rhs.toMCRL2() + ">true";
        } else if (!lhs.isActionFormula() && !rhs.isActionFormula()) {
            return "(" + lhs.toMCRL2() + ") => (" + rhs.toMCRL2() + ")";
        } else {
            throw new UnsupportedOperationException();
        }
    }

    @Override
    public String toString() {
        return lhs + " --> " + rhs;
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
