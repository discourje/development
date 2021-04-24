package discourje.core.validation.formulas;

import discourje.core.validation.State;
import discourje.core.validation.Model;
import java.util.Arrays;
import java.util.stream.Collectors;

class Or implements CtlFormula {
    private final CtlFormula[] args;
    private final int hash;

    Or(CtlFormula... args) {
        this.args = args;
        hash = Arrays.hashCode(args);
    }

    @Override
    public void label(Model<?> model) {
        if (!model.isLabelledBy(this)) {
            int labelIndex = model.setLabelledBy(this);
            Arrays.stream(args).forEach(a -> a.label(model));

            for (State<?> state : model.getStates()) {
                if (Arrays.stream(args).anyMatch(arg -> state.hasLabel(model.getLabelIndex(arg)))) {
                    state.addLabel(labelIndex);
                }
            }
        }
    }

    public String toString() {
        return String.format("(%s)",
                Arrays.stream(args).map(Object::toString).collect(Collectors.joining(" or "))
        );
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Or that = (Or) o;
        return Arrays.equals(args, that.args);
    }

    @Override
    public int hashCode() {
        return hash;
    }
}
