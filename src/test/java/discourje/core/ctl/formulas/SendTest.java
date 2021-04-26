package discourje.core.ctl.formulas;

import discourje.core.lts.Action;
import discourje.core.ctl.State;
import discourje.core.ctl.Model;
import discourje.core.ctl.formulas.atomic.Send;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SendTest<S> extends AbstractCtlFormulaTest<S> {

    @Test
    public void testSend() {
        State<S> s1 = createState(Action.Type.SYNC, "a", "b");
        State<S> s2 = createState(Action.Type.SEND, "a", "b");
        State<S> s3 = createState(Action.Type.RECEIVE, "a", "b");
        State<S> s4 = createState(Action.Type.CLOSE, "a", "b");
        State<S> s5 = createState(Action.Type.SYNC, "b", "a");
        State<S> s6 = createState(Action.Type.SEND, "b", "a");
        State<S> s7 = createState(Action.Type.RECEIVE, "b", "a");
        State<S> s8 = createState(Action.Type.CLOSE, "b", "a");
        Model<S> model = createModel(s1, s2, s3, s4, s5, s6, s7, s8);

        Send snd = new Send("a", null);
        snd.label(model);

        int labelIndex = model.getLabelIndex(snd);
        assertTrue(s1.hasLabel(labelIndex));
        assertTrue(s2.hasLabel(labelIndex));
        //assertTrue(s3.hasLabel(labelIndex));
        assertFalse(s4.hasLabel(labelIndex));
        assertFalse(s5.hasLabel(labelIndex));
        assertFalse(s6.hasLabel(labelIndex));
        assertFalse(s7.hasLabel(labelIndex));
        assertFalse(s8.hasLabel(labelIndex));
    }
}