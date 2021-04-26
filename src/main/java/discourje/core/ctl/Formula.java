package discourje.core.ctl;

import discourje.core.lts.Action;

import java.util.Collections;
import java.util.List;

public interface Formula {

    void label(Model<?> model);

    default List<List<Action>> extractWitness(Model<?> model) {
        var i = model.getLabelIndex(this);
        for (var s : model.getInitialStates()) {
            if (!s.hasLabel(i)) {
                return extractWitness(model, s);
            }
        }
        throw new IllegalArgumentException();
    }

    List<List<Action>> extractWitness(Model<?> model, State<?> source);

    default boolean isAction() {
        return false;
    }

    boolean isTemporal();

    default List<Formula> split() {
        return Collections.singletonList(this);
    }

    default String toMCRL2() {
        throw new UnsupportedOperationException(getClass().getSimpleName());
    }
}
