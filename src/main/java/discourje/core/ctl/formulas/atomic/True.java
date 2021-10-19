package discourje.core.ctl.formulas.atomic;

import discourje.core.ctl.Labels;
import discourje.core.ctl.State;
import discourje.core.ctl.Model;
import discourje.core.ctl.formulas.Atomic;

public class True extends Atomic {
    public static final True INSTANCE = new True();
    public static final int hash = True.class.hashCode();

    private True() {
    }

    @Override
    public Labels label(Model<?> model) {
        Labels labels = new Labels();
        for (State<?> state : model.getStates()) {
            labels.setLabel(state);
        }
        return labels;
    }

    public String toString() {
        return "true";
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
