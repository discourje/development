package discourje.core.ctl.formulas.atomic;

import discourje.core.ctl.Labels;
import discourje.core.ctl.Model;
import discourje.core.ctl.State;
import discourje.core.ctl.formulas.Atomic;

public class Fin extends Atomic {
    public static final Fin INSTANCE = new Fin();
    public static final int hash = Fin.class.hashCode();

    private Fin() {
    }

    @Override
    public Labels label(Model<?> model) {
        Labels labels = new Labels();
        for (State<?> state : model.getStates()) {
            if (state.getNextStates().isEmpty()) {
                labels.setLabel(state);
            }
        }
        return labels;
    }

    @Override
    public String toMCRL2() {
        return "[true]false";
    }

    @Override
    public String toString() {
        return "fin";
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        return o != null && getClass() == o.getClass();
    }

    @Override
    public int hashCode() {
        return hash;
    }
}
