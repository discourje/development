package discourje.core.validation.formulas;

import discourje.core.lts.Action;
import discourje.core.validation.DMState;
import discourje.core.validation.DiscourjeModel;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ReceiveTest<S> extends AbstractCtlFormulaTest<S> {

    @Test
    public void testReceive() {
        DMState<S> s1 = createState(Action.Type.SYNC, "a", "b");
        DMState<S> s2 = createState(Action.Type.SEND, "a", "b");
        DMState<S> s3 = createState(Action.Type.RECEIVE, "a", "b");
        DMState<S> s4 = createState(Action.Type.CLOSE, "a", "b");
        DMState<S> s5 = createState(Action.Type.SYNC, "b", "a");
        DMState<S> s6 = createState(Action.Type.SEND, "b", "a");
        DMState<S> s7 = createState(Action.Type.RECEIVE, "b", "a");
        DMState<S> s8 = createState(Action.Type.CLOSE, "b", "a");
        DiscourjeModel<S> model = createModel(s1, s2, s3, s4, s5, s6, s7, s8);

        Receive rcv = new Receive(null, "a");
        rcv.label(model);

        assertFalse(s1.hasLabel(model.getLabelIndex(rcv)));
        assertFalse(s2.hasLabel(model.getLabelIndex(rcv)));
        assertFalse(s3.hasLabel(model.getLabelIndex(rcv)));
        assertFalse(s4.hasLabel(model.getLabelIndex(rcv)));
        assertTrue(s5.hasLabel(model.getLabelIndex(rcv)));
        //assertTrue(s6.hasLabel(model.getLabelIndex(rcv)));
        assertTrue(s7.hasLabel(model.getLabelIndex(rcv)));
        assertFalse(s8.hasLabel(model.getLabelIndex(rcv)));
    }
}