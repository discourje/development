package discourje.core.ctl.formulas;

import discourje.core.ctl.Formula;
import discourje.core.ctl.Labels;
import discourje.core.ctl.Model;
import discourje.core.ctl.State;
import discourje.core.lts.Action;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class Or implements Formula {
    private final Formula[] args;
    private final int hash;

    public Or(Formula... args) {
        this.args = args;
        hash = Arrays.hashCode(args);
    }

    @Override
    public boolean isTemporal() {
        return Arrays.stream(args).anyMatch(Formula::isTemporal);
    }

    @Override
    public List<List<Action>> extractWitness(Model<?> model, State<?> source) {

        if (Arrays.stream(args).filter(Formula::isTemporal).count() > 1) {
            throw new IllegalStateException();
        }

        if (model.hasLabel(source, this)) {
            throw new IllegalArgumentException();
        }

        for (var arg : args) {
            if (arg.isTemporal()) {
                return arg.extractWitness(model, source);
            }
        }

        return Collections.singletonList(Collections.emptyList());
    }

    @Override
    public Labels label(Model<?> model) {
        Labels labels = new Labels();
        Collection<Labels> argLabels = Arrays.stream(args)
                .map(model::calculateLabels)
                .collect(Collectors.toList());

        model.getStates().stream()
                .filter(s -> argLabels.stream().anyMatch(arg -> arg.hasLabel(s)))
                .forEach(labels::setLabel);
        return labels;
    }

    @Override
    public String toMCRL2() {
        String s = "false";
        for (var arg : args) {
            if (arg.isAction()) {
                s += " || <" + arg.toMCRL2() + ">true";
            } else {
                s += " || (" + arg.toMCRL2() + ")";
            }
        }
        return s;
    }

    public String toString() {
        return String.format("(%s)",
                Arrays.stream(args).map(Object::toString).collect(Collectors.joining(" || "))
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
