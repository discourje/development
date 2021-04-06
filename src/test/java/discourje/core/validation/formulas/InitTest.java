package discourje.core.validation.formulas;

import discourje.core.lts.Action;
import discourje.core.lts.State;
import discourje.core.validation.DMState;
import discourje.core.validation.DiscourjeModel;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class InitTest<S> extends AbstractCtlFormulaTest<S> {

    @Test
    public void testFirst() {
        DMState<S> firstState = new DMState<S>(mock(State.class), null);
        DMState<S> secondState = createState(Action.Type.SEND, "a", "b");
        DiscourjeModel<S> model = createModel(firstState, secondState);

        Init init = Init.INSTANCE;
        init.label(model);

        assertTrue(firstState.hasLabel(model.getLabelIndex(init)));
        assertFalse(secondState.hasLabel(model.getLabelIndex(init)));
    }
}