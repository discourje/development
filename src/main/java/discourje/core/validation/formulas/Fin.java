package discourje.core.validation.formulas;

import discourje.core.validation.Model;
import discourje.core.validation.State;

import java.util.Objects;

class Fin implements CtlFormula {

    public static final Fin INSTANCE = new Fin();

    private Fin() {
    }

    @Override
    public void label(Model<?> model) {
        if (!model.isLabelledBy(this)) {
            int labelIndex = model.setLabelledBy(this);
            for (State<?> state : model.getStates()) {
                if (state.getNextStates().isEmpty()) {
                    state.addLabel(labelIndex);
                }
            }
        }
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
        return Objects.hash(this.getClass());
    }
}
