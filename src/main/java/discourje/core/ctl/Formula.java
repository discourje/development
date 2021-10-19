package discourje.core.ctl;

import discourje.core.lts.Action;
import java.util.Collections;
import java.util.List;

public interface Formula {

    default boolean isAction() {
        return false;
    }

    boolean isTemporal();

    Labels label(Model<?> model);

    default List<List<Action>> extractWitness(Model<?> model) {
        for (var s : model.getInitialStates()) {
            if (!model.hasLabel(s, this)) {
                return extractWitness(model, s);
            }
        }
        throw new IllegalArgumentException();
    }

    List<List<Action>> extractWitness(Model<?> model, State<?> source);

    default List<Formula> split() {
        return Collections.singletonList(this);
    }

    default String toMCRL2() {
        throw new UnsupportedOperationException(getClass().getSimpleName());
    }
}
