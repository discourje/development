package discourje.core.validation.operators;

import discourje.core.lts.Action;
import discourje.core.validation.DMState;
import discourje.core.validation.DiscourjeModel;
import org.junit.jupiter.api.Test;
import static discourje.core.validation.operators.CtlOperators.close;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AHTest<S> extends AbstractOperatorTest<S> {

    @Test
    public void testValidOnAllPathsEarlySplit() {
        DMState<S> s1 = createState(Action.Type.CLOSE, "a", "b");
        DMState<S> s2a = createState(Action.Type.CLOSE, "a", "b");
        DMState<S> s2b = createState(Action.Type.CLOSE, "a", "b");
        DMState<S> s3a = createState(Action.Type.CLOSE, "a", "b");
        DMState<S> s3b = createState(Action.Type.CLOSE, "a", "b");

        s2a.addNextState(s1);
        s2b.addNextState(s1);
        s3a.addNextState(s2a);
        s3b.addNextState(s2b);

        DiscourjeModel<S> model = createModel(s1, s2a, s2b, s3a, s3b);

        AH ah = new AH(close("a", "b"));
        ah.label(model);

        assertTrue(s1.hasLabel(model.getLabelIndex(ah)));
    }

    @Test
    public void testValidOnAllPathsLateSplit() {
        DMState<S> s1 = createState(Action.Type.CLOSE, "a", "b");
        DMState<S> s2 = createState(Action.Type.CLOSE, "a", "b");
        DMState<S> s3a = createState(Action.Type.CLOSE, "a", "b");
        DMState<S> s3b = createState(Action.Type.CLOSE, "a", "b");

        s2.addNextState(s1);
        s3a.addNextState(s2);
        s3b.addNextState(s2);

        DiscourjeModel<S> model = createModel(s1, s2, s3a, s3b);

        AH ah = new AH(close("a", "b"));
        ah.label(model);

        assertTrue(s1.hasLabel(model.getLabelIndex(ah)));
    }

    @Test
    public void testNotValidInNextState() {
        DMState<S> s1 = createState(Action.Type.CLOSE, "a", "b");
        DMState<S> s2 = createState(Action.Type.SEND, "a", "b");
        DMState<S> s3 = createState(Action.Type.CLOSE, "a", "b");

        s2.addNextState(s1);
        s3.addNextState(s2);

        DiscourjeModel<S> model = createModel(s1, s2, s3);

        AH ah = new AH(close("a", "b"));
        ah.label(model);

        assertFalse(s1.hasLabel(model.getLabelIndex(ah)));
    }

    @Test
    public void testValidOnOnePath() {
        DMState<S> s1 = createState(Action.Type.CLOSE, "a", "b");
        DMState<S> s2a = createState(Action.Type.CLOSE, "a", "b");
        DMState<S> s2b = createState(Action.Type.CLOSE, "a", "b");
        DMState<S> s3a = createState(Action.Type.CLOSE, "a", "b");
        DMState<S> s3b = createState(Action.Type.SEND, "a", "c");

        s2a.addNextState(s1);
        s2b.addNextState(s1);
        s3a.addNextState(s2a);
        s3b.addNextState(s2b);

        DiscourjeModel<S> model = createModel(s1, s2a, s2b, s3a, s3b);

        AH ah = new AH(close("a", "b"));
        ah.label(model);

        assertFalse(s1.hasLabel(model.getLabelIndex(ah)));
    }

    @Test
    public void testValidOnNoPath1() {
        DMState<S> s1 = createState(Action.Type.SEND, "a", "b");
        DMState<S> s2a = createState(Action.Type.SEND, "a", "b");
        DMState<S> s2b = createState(Action.Type.SEND, "a", "b");
        DMState<S> s3a = createState(Action.Type.CLOSE, "a", "c");
        DMState<S> s3b = createState(Action.Type.CLOSE, "a", "c");

        s2a.addNextState(s1);
        s2b.addNextState(s1);
        s3a.addNextState(s2a);
        s3b.addNextState(s2b);

        DiscourjeModel<S> model = createModel(s1, s2a, s2b, s3a, s3b);

        AH ah = new AH(close("a", "b"));
        ah.label(model);

        assertFalse(s1.hasLabel(model.getLabelIndex(ah)));
    }

    @Test
    public void testValidOnNoPath2() {
        DMState<S> s1 = createState(Action.Type.CLOSE, "a", "b");
        DMState<S> s2a = createState(Action.Type.CLOSE, "a", "b");
        DMState<S> s2b = createState(Action.Type.CLOSE, "a", "b");
        DMState<S> s3a = createState(Action.Type.SEND, "a", "c");
        DMState<S> s3b = createState(Action.Type.SEND, "a", "c");

        s2a.addNextState(s1);
        s2b.addNextState(s1);
        s3a.addNextState(s2a);
        s3b.addNextState(s2b);

        DiscourjeModel<S> model = createModel(s1, s2a, s2b, s3a, s3b);

        AH ah = new AH(close("a", "b"));
        ah.label(model);

        assertFalse(s1.hasLabel(model.getLabelIndex(ah)));
    }
}