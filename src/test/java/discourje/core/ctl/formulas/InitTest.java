package discourje.core.ctl.formulas;

import discourje.core.lts.Action;
import discourje.core.ctl.State;
import discourje.core.ctl.Model;
import discourje.core.ctl.formulas.atomic.Init;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class InitTest<S> extends AbstractCtlFormulaTest<S> {

    @Test
    public void testFirst() {
        State<S> firstState = createState(null);
        State<S> secondState = createState(Action.Type.SEND, "a", "b");
        Model<S> model = createModel(firstState, secondState);

        Init init = Init.INSTANCE;
        model.calculateLabels(init);

        assertTrue(model.hasLabel(firstState, init));
        assertFalse(model.hasLabel(secondState, init));
    }
}