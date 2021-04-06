package discourje.core.validation.formulas;

import discourje.core.lts.Action;
import discourje.core.validation.DMState;
import discourje.core.validation.DiscourjeModel;
import org.junit.jupiter.api.Test;
import static discourje.core.validation.formulas.CtlFormulas.close;
import static discourje.core.validation.formulas.CtlFormulas.send;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EUTest<S> extends AbstractCtlFormulaTest<S> {

    @Test
    public void testValidOnAllPathsEarlySplit() {
        DMState<S> s1 = createState(Action.Type.SEND, "a", "b");
        DMState<S> s2a = createState(Action.Type.SEND, "a", "b");
        DMState<S> s2b = createState(Action.Type.SEND, "a", "b");
        DMState<S> s3a = createState(Action.Type.CLOSE, "a", "b");
        DMState<S> s3b = createState(Action.Type.CLOSE, "a", "b");

        s1.addNextState(s2a);
        s1.addNextState(s2b);
        s2a.addNextState(s3a);
        s2b.addNextState(s3b);

        DiscourjeModel<S> model = createModel(s1, s2a, s2b, s3a, s3b);

        EU eu = new EU(send("a", null), close("a", "b"));
        eu.label(model);

        assertTrue(s1.hasLabel(model.getLabelIndex(eu)));
    }

    @Test
    public void testValidOnAllPathsLateSplit() {
        DMState<S> s1 = createState(Action.Type.SEND, "a", "b");
        DMState<S> s2 = createState(Action.Type.SEND, "a", "b");
        DMState<S> s3a = createState(Action.Type.CLOSE, "a", "b");
        DMState<S> s3b = createState(Action.Type.CLOSE, "a", "b");

        s1.addNextState(s2);
        s2.addNextState(s3a);
        s2.addNextState(s3b);

        DiscourjeModel<S> model = createModel(s1, s2, s3a, s3b);

        EU eu = new EU(send("a", null), close("a", "b"));
        eu.label(model);

        assertTrue(s1.hasLabel(model.getLabelIndex(eu)));
    }

    @Test
    public void testValidOnOnePath() {
        DMState<S> s1 = createState(Action.Type.SEND, "a", "b");
        DMState<S> s2a = createState(Action.Type.SEND, "a", "b");
        DMState<S> s2b = createState(Action.Type.SEND, "a", "b");
        DMState<S> s3a = createState(Action.Type.CLOSE, "a", "b");
        DMState<S> s3b = createState(Action.Type.CLOSE, "a", "c");

        s1.addNextState(s2a);
        s1.addNextState(s2b);
        s2a.addNextState(s3a);
        s2b.addNextState(s3b);

        DiscourjeModel<S> model = createModel(s1, s2a, s2b, s3a, s3b);

        EU eu = new EU(send("a", null), close("a", "b"));
        eu.label(model);

        assertTrue(s1.hasLabel(model.getLabelIndex(eu)));
    }

    @Test
    public void testValidOnNoPath() {
        DMState<S> s1 = createState(Action.Type.SEND, "a", "b");
        DMState<S> s2a = createState(Action.Type.SEND, "a", "b");
        DMState<S> s2b = createState(Action.Type.SEND, "a", "b");
        DMState<S> s3a = createState(Action.Type.CLOSE, "a", "c");
        DMState<S> s3b = createState(Action.Type.CLOSE, "a", "c");

        s1.addNextState(s2a);
        s1.addNextState(s2b);
        s2a.addNextState(s3a);
        s2b.addNextState(s3b);

        DiscourjeModel<S> model = createModel(s1, s2a, s2b, s3a, s3b);

        EU eu = new EU(send("a", null), close("a", "b"));
        eu.label(model);

        assertFalse(s1.hasLabel(model.getLabelIndex(eu)));
    }
}