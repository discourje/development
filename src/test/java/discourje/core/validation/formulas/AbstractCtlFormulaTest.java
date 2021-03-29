package discourje.core.validation.formulas;

import discourje.core.lts.Action;
import discourje.core.lts.State;
import discourje.core.validation.DMState;
import discourje.core.validation.DiscourjeModel;
import static org.mockito.Mockito.*;

public class AbstractCtlFormulaTest<S> {

    @SuppressWarnings("unchecked")
    protected DMState<S> createState(Action.Type type, String a, String a2) {
        Action action = new Action("name", type, null, a, a2);
        return new DMState<S>(mock(State.class), action);
    }

    @SuppressWarnings("unchecked")
    protected DMState<S> createState(Action action) {
        return new DMState<S>(mock(State.class), action);
    }

    @SafeVarargs
    @SuppressWarnings("unchecked")
    protected final DiscourjeModel<S> createModel(DMState<S>... states) {
        return new DiscourjeModel<S>(states);
    }
}
