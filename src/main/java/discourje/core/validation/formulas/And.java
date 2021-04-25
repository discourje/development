package discourje.core.validation.formulas;

import discourje.core.lts.Action;
import discourje.core.validation.State;
import discourje.core.validation.Model;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

class And implements CtlFormula {
    private final CtlFormula[] args;
    private final int hash;

    And(CtlFormula... args) {
        this.args = args;
        hash = Arrays.hashCode(args);
    }

    @Override
    public void label(Model<?> model) {
        if (!model.isLabelledBy(this)) {
            int labelIndex = model.setLabelledBy(this);
            Arrays.stream(args).forEach(a -> a.label(model));

            for (State<?> state : model.getStates()) {
                if (Arrays.stream(args).allMatch(arg -> state.hasLabel(model.getLabelIndex(arg)))) {
                    state.addLabel(labelIndex);
                }
            }
        }
    }

    @Override
    public List<Action> getCounterexample(Model<?> model) {

        for (State<?> s : model.getInitialStates()) {
            for (CtlFormula f : args) {
                var i = model.getLabelIndex(f);
                if (!s.hasLabel(i) && f instanceof AG) {
                    return f.getCounterexample(model);
                }
            }
        }

        throw new UnsupportedOperationException();
    }

    @Override
    public String toMCRL2() {
        String s = "true";
        for (var arg : args) {
            if (arg.isActionFormula()) {
                s += " && <" + arg.toMCRL2() + ">true";
            } else {
                s += " && (" + arg.toMCRL2() + ")";
            }
        }
        return s;
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
