package discourje.core.validation.formulas;

import discourje.core.lts.Action;
import discourje.core.validation.State;
import discourje.core.validation.Model;
import static org.mockito.Mockito.*;

public class AbstractCtlFormulaTest<S> {

    @SuppressWarnings("unchecked")
    protected State<S> createState(Action.Type type, String a, String a2) {
        Action action = new Action("name", type, null, a, a2);
        return new State<S>(mock(discourje.core.lts.State.class), action);
    }

    @SuppressWarnings("unchecked")
    protected State<S> createState(Action action) {
        return new State<S>(mock(discourje.core.lts.State.class), action);
    }

    @SafeVarargs
    @SuppressWarnings("unchecked")
    protected final Model<S> createModel(State<S>... states) {
        return new Model<S>(states);
    }
}
