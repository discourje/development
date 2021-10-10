package discourje.core.ctl.formulas.temporal;

import discourje.core.ctl.Labels;
import discourje.core.lts.Action;
import discourje.core.ctl.State;
import discourje.core.ctl.Model;
import discourje.core.ctl.Formula;
import discourje.core.ctl.formulas.Temporal;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

import static discourje.core.ctl.Formulas.AP;
import static discourje.core.ctl.Formulas.and;
import static discourje.core.ctl.Formulas.init;
import static discourje.core.ctl.Formulas.not;

public class EH extends Temporal {
    private final Formula arg;
    private final int hash;

    public EH(Formula arg) {
        this.arg = arg;
        hash = Objects.hash(this.arg);
    }

    @Override
    public List<List<Action>> extractWitness(Model<?> model, State<?> source) {
        if (model.hasLabel(source, this)) {
            throw new IllegalArgumentException();
        }

        return Collections.emptyList();
    }

    @Override
    public Labels label(Model<?> model) {
        Formula eh = not(AP(and(not(arg), not(init()))));
        return model.calculateLabels(eh);
    }

    @Override
    public String toString() {
        return "EH(" + arg + ")";
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        EH that = (EH) o;
        return arg.equals(that.arg);
    }

    @Override
    public int hashCode() {
        return hash;
    }
}
