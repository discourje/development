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
        State<S> firstState = new State<S>(mock(discourje.core.lts.State.class), null);
        State<S> secondState = createState(Action.Type.SEND, "a", "b");
        Model<S> model = createModel(firstState, secondState);

        Init init = Init.INSTANCE;
        init.label(model);

        assertTrue(firstState.hasLabel(model.getLabelIndex(init)));
        assertFalse(secondState.hasLabel(model.getLabelIndex(init)));
    }
}