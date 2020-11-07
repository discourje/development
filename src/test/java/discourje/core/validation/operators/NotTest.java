package discourje.core.validation.operators;

import discourje.core.lts.Action;
import discourje.core.validation.DMState;
import discourje.core.validation.DiscourjeModel;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class NotTest<S> extends AbstractOperatorTest<S> {

    @Test
    public void testNot() {
        DMState<S> s1 = createState(Action.Type.SEND, "a", "b");
        DMState<S> s2 = createState(Action.Type.SEND, "b", "a");
        DiscourjeModel<S> model = createModel(s1, s2);

        Not not = new Not(new Send("a"));
        not.label(model);

        assertFalse(s1.hasLabel(not));
        assertTrue(s2.hasLabel(not));
    }
}