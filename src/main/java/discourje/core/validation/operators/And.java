package discourje.core.validation.operators;

import discourje.core.validation.DMState;
import discourje.core.validation.DiscourjeModel;
import java.util.Arrays;
import java.util.stream.Collectors;

class And implements CtlOperator {
    private final CtlOperator[] args;
    private final int hash;

    And(CtlOperator... args) {
        this.args = args;
        hash = Arrays.hashCode(args);
    }

    @Override
    public void label(DiscourjeModel<?> model) {
        if (!model.isLabelledBy(this)) {
            Arrays.stream(args).forEach(a -> a.label(model));

            for (DMState<?> state : model.getStates()) {
                if (Arrays.stream(args).allMatch(state::hasLabel)) {
                    state.addLabel(this);
                }
            }
            model.setLabelledBy(this);
        }
    }

    public String toString() {
        return String.format("(%s)",
                Arrays.stream(args).map(Object::toString).collect(Collectors.joining(" and "))
        );
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        And that = (And) o;
        return Arrays.equals(args, that.args);
    }

    @Override
    public int hashCode() {
        return hash;
    }
}
