package discourje.core.ctl.formulas;

import discourje.core.lts.Action;
import discourje.core.ctl.Formula;
import discourje.core.ctl.Model;
import discourje.core.ctl.State;

import java.util.Collections;
import java.util.List;

public abstract class Atomic implements Formula {

    @Override
    public List<List<Action>> extractWitness(Model<?> model, State<?> source) {
        if (model.hasLabel(source, this)) {
            throw new IllegalArgumentException();
        }

        return Collections.singletonList(Collections.emptyList());
    }

    @Override
    public final boolean isTemporal() {
        return false;
    }
}
