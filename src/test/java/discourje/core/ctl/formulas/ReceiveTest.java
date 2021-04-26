package discourje.core.ctl.formulas;

import discourje.core.lts.Action;
import discourje.core.ctl.State;
import discourje.core.ctl.Model;
import discourje.core.ctl.formulas.atomic.Receive;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ReceiveTest<S> extends AbstractCtlFormulaTest<S> {

    @Test
    public void testReceive() {
        State<S> s1 = createState(Action.Type.SYNC, "a", "b");
        State<S> s2 = createState(Action.Type.SEND, "a", "b");
        State<S> s3 = createState(Action.Type.RECEIVE, "a", "b");
        State<S> s4 = createState(Action.Type.CLOSE, "a", "b");
        State<S> s5 = createState(Action.Type.SYNC, "b", "a");
        State<S> s6 = createState(Action.Type.SEND, "b", "a");
        State<S> s7 = createState(Action.Type.RECEIVE, "b", "a");
        State<S> s8 = createState(Action.Type.CLOSE, "b", "a");
        Model<S> model = createModel(s1, s2, s3, s4, s5, s6, s7, s8);

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