package discourje.core.ctl.formulas;

import discourje.core.lts.Action;
import discourje.core.ctl.State;
import discourje.core.ctl.Model;
import static org.mockito.Mockito.*;

public class AbstractCtlFormulaTest<S> {

    private int stateIndex = 0;

    @SuppressWarnings("unchecked")
    protected State<S> createState(Action.Type type, String a, String a2) {
        Action action = new Action("name", type, null, a, a2);
        return new State<S>(mock(discourje.core.lts.State.class), action, stateIndex++);
    }

    @SuppressWarnings("unchecked")
    protected State<S> createState(Action action) {
        return new State<S>(mock(discourje.core.lts.State.class), action, stateIndex++);
    }

    @SafeVarargs
    @SuppressWarnings("unchecked")
    protected final Model<S> createModel(State<S>... states) {
        return new Model<S>(states);
    }
}
