package discourje.core.validation.operators;

import discourje.core.validation.DMState;
import discourje.core.validation.DiscourjeModel;
import java.util.Arrays;
import java.util.stream.Collectors;

class And implements CtlOperator {
    private final CtlOperator[] args;

    And(CtlOperator... args) {
        this.args = args;
    }

    @Override
    public void label(DiscourjeModel<?> model) {
        Arrays.stream(args).forEach(a -> a.label(model));

        for (DMState<?> state : model.getStates()) {
            if (Arrays.stream(args).allMatch(state::hasLabel)) {
                state.addLabel(this);
            }
        }
    }

    public String toString() {
        return String.format("(%s)",
                Arrays.stream(args).map(Object::toString).collect(Collectors.joining(" and "))
        );
    }
}
