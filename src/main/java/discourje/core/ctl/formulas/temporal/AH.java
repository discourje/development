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

import static discourje.core.ctl.Formulas.EP;
import static discourje.core.ctl.Formulas.and;
import static discourje.core.ctl.Formulas.init;
import static discourje.core.ctl.Formulas.not;

public class AH extends Temporal {
    private final Formula arg;
    private final int hash;

    public AH(Formula arg) {
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
        Formula ah = not(EP(and(not(arg), not(init()))));
        return model.calculateLabels(ah);
    }

    @Override
    public String toString() {
        return "AH(" + arg + ")";
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        AH that = (AH) o;
        return arg.equals(that.arg);
    }

    @Override
    public int hashCode() {
        return hash;
    }
}
