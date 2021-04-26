package discourje.core.ctl.formulas.atomic;

import discourje.core.ctl.State;
import discourje.core.ctl.Model;
import discourje.core.ctl.formulas.Atomic;

public class True extends Atomic {
    public static final True INSTANCE = new True();
    public static final int hash = True.class.hashCode();

    private True() {
    }

    @Override
    public void label(Model<?> model) {
        if (!model.isLabelledBy(this)) {
            int labelIndex = model.setLabelledBy(this);
            for (State<?> state : model.getStates()) {
                state.addLabel(labelIndex);
            }
        }
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
