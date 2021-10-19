package discourje.core.ctl.formulas.atomic;

import discourje.core.ctl.Labels;
import discourje.core.lts.Action;
import discourje.core.ctl.State;
import discourje.core.ctl.Model;
import discourje.core.ctl.formulas.Atomic;

public class Init extends Atomic {
    public static final Init INSTANCE = new Init();
    public static final int hash = Init.class.hashCode();

    private Init() {
    }

    @Override
    public Labels label(Model<?> model) {
        Labels labels = new Labels();
        for (State<?> state : model.getStates()) {
            Action action = state.getAction();
            if (action == null) {
                labels.setLabel(state);
            }
        }
        return labels;
    }

    @Override
    public String toString() {
        return "init";
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
