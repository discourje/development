package discourje.core.validation.operators;

import discourje.core.lts.Action;
import discourje.core.validation.DMState;
import discourje.core.validation.DiscourjeModel;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MessageTest<S> extends AbstractOperatorTest<S> {

    @Test
    public void testMessage() {
        DMState<S> s1 = createState(Action.Type.SYNC, "a", "b");
        DMState<S> s2 = createState(Action.Type.SEND, "a", "b");
        DMState<S> s3 = createState(Action.Type.RECEIVE, "a", "b");
        DMState<S> s4 = createState(Action.Type.CLOSE, "a", "b");
        DMState<S> s5 = createState(Action.Type.SYNC, "a", "a");
        DMState<S> s6 = createState(Action.Type.SEND, "a", "a");
        DMState<S> s7 = createState(Action.Type.RECEIVE, "a", "a");
        DMState<S> s8 = createState(Action.Type.SYNC, "b", "a");
        DMState<S> s9 = createState(Action.Type.SEND, "b", "a");
        DMState<S> s10 = createState(Action.Type.RECEIVE, "b", "a");
        DiscourjeModel<S> model = createModel(s1, s2, s3, s4, s5, s6, s7, s8, s9, s10);

        Message msg = new Message("a", "b");
        msg.label(model);

        assertTrue(s1.hasLabel(model.getLabelIndex(msg)));
        assertTrue(s2.hasLabel(model.getLabelIndex(msg)));
        assertTrue(s3.hasLabel(model.getLabelIndex(msg)));
        assertFalse(s4.hasLabel(model.getLabelIndex(msg)));
        assertFalse(s5.hasLabel(model.getLabelIndex(msg)));
        assertFalse(s6.hasLabel(model.getLabelIndex(msg)));
        assertFalse(s7.hasLabel(model.getLabelIndex(msg)));
        assertFalse(s8.hasLabel(model.getLabelIndex(msg)));
        assertFalse(s9.hasLabel(model.getLabelIndex(msg)));
        assertFalse(s10.hasLabel(model.getLabelIndex(msg)));
    }
}