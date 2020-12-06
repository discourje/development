package discourje.core.validation.operators;

import discourje.core.lts.Action;
import discourje.core.validation.DMState;
import discourje.core.validation.DiscourjeModel;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CloseTest<S> extends AbstractOperatorTest<S> {

    @Test
    public void testClose() {
        // setup
        DMState<S> s1 = createState(Action.Type.CLOSE, "a", "b");
        DMState<S> s2 = createState(Action.Type.CLOSE, "b", "a");

        DiscourjeModel<S> model = createModel(s1, s2);

        // execute
        CtlOperator close = new Close("a", "b");
        close.label(model);

        // verify
        assertTrue(s1.hasLabel(model.getLabelIndex(close)));
        assertFalse(s2.hasLabel(model.getLabelIndex(close)));
    }

}