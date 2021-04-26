package discourje.core.ctl.formulas;

import discourje.core.lts.Action;
import discourje.core.ctl.Formula;
import discourje.core.ctl.State;
import discourje.core.ctl.Model;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class And implements Formula {
    private final Formula[] args;
    private final int hash;

    public And(Formula... args) {
        this.args = args;
        hash = Arrays.hashCode(args);
    }

    @Override
    public boolean isTemporal() {
        return Arrays.stream(args).anyMatch(Formula::isTemporal);
    }

    @Override
    public List<List<Action>> extractWitness(Model<?> model, State<?> source) {
        for (Formula arg : args) {
            var i = model.getLabelIndex(arg);
            if (!source.hasLabel(i)) {
                return arg.extractWitness(model, source);
            }
        }

        throw new IllegalArgumentException();
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
    public List<Formula> split() {
        return Arrays.asList(args);
    }

    @Override
    public String toMCRL2() {
        String s = "true";
        for (var arg : args) {
            if (arg.isAction()) {
                s += " && <" + arg.toMCRL2() + ">true";
            } else {
                s += " && (" + arg.toMCRL2() + ")";
            }
        }
        return s;
    }

    public String toString() {
        return String.format("(%s)",
                Arrays.stream(args).map(Object::toString).collect(Collectors.joining(" && "))
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
