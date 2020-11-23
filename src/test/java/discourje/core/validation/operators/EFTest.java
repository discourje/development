package discourje.core.validation.operators;

import discourje.core.lts.Action;
import discourje.core.validation.DMState;
import discourje.core.validation.DiscourjeModel;
import org.junit.jupiter.api.Test;
import static discourje.core.validation.operators.CtlOperators.close;
import static org.junit.jupiter.api.Assertions.*;

class EFTest<S> extends AbstractOperatorTest<S> {

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

        EF ef = new EF(close("a", "b"));
        ef.label(model);

        assertTrue(s1.hasLabel(ef));
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

        EF ef = new EF(close("a", "b"));
        ef.label(model);

        assertTrue(s1.hasLabel(ef));
    }

    @Test
    public void testValidOnOneShortPath() {
        DMState<S> s1 = createState(Action.Type.SEND, "a", "b");
        DMState<S> s2a = createState(Action.Type.CLOSE, "a", "b");
        DMState<S> s2b = createState(Action.Type.SEND, "a", "b");

        s1.addNextState(s2a);
        s1.addNextState(s2b);

        DiscourjeModel<S> model = createModel(s1, s2a, s2b);

        EF ef = new EF(close("a", "b"));
        ef.label(model);

        assertTrue(s1.hasLabel(ef));
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

        EF ef = new EF(close("a", "b"));
        ef.label(model);

        assertTrue(s1.hasLabel(ef));
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

        EF ef = new EF(close("a", "b"));
        ef.label(model);

        assertFalse(s1.hasLabel(ef));
    }
}