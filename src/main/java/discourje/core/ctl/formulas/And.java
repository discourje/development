package discourje.core.ctl.formulas;

import discourje.core.ctl.Formula;
import discourje.core.ctl.Labels;
import discourje.core.ctl.Model;
import discourje.core.ctl.State;
import discourje.core.lts.Action;
import java.util.Arrays;
import java.util.Collection;
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
            if (!model.hasLabel(source, arg)) {
                return arg.extractWitness(model, source);
            }
        }

        throw new IllegalArgumentException();
    }

    @Override
    public Labels label(Model<?> model) {
        Labels labels = new Labels();
        Collection<Labels> argLabels = Arrays.stream(args)
                .map(model::calculateLabels)
                .collect(Collectors.toList());

        model.getStates().stream()
                .filter(s -> argLabels.stream().allMatch(arg -> arg.hasLabel(s)))
                .forEach(labels::setLabel);
        return labels;
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
