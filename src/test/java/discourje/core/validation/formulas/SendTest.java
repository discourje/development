package discourje.core.validation.formulas;

import discourje.core.lts.Action;
import discourje.core.validation.DMState;
import discourje.core.validation.DiscourjeModel;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SendTest<S> extends AbstractCtlFormulaTest<S> {

    @Test
    public void testSend() {
        DMState<S> s1 = createState(Action.Type.SYNC, "a", "b");
        DMState<S> s2 = createState(Action.Type.SEND, "a", "b");
        DMState<S> s3 = createState(Action.Type.RECEIVE, "a", "b");
        DMState<S> s4 = createState(Action.Type.CLOSE, "a", "b");
        DMState<S> s5 = createState(Action.Type.SYNC, "b", "a");
        DMState<S> s6 = createState(Action.Type.SEND, "b", "a");
        DMState<S> s7 = createState(Action.Type.RECEIVE, "b", "a");
        DMState<S> s8 = createState(Action.Type.CLOSE, "b", "a");
        DiscourjeModel<S> model = createModel(s1, s2, s3, s4, s5, s6, s7, s8);

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