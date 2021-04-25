package discourje.core.validation.formulas;

import discourje.core.lts.Action;
import discourje.core.validation.Model;

import java.util.List;

public interface CtlFormula {

    void label(Model<?> model);

    default List<Action> getCounterexample(Model<?> model) {
        throw new UnsupportedOperationException();
    }

    default boolean isActionFormula() {
        return false;
    }

    default String toMCRL2() {
        throw new UnsupportedOperationException(getClass().getSimpleName());
    }
}
