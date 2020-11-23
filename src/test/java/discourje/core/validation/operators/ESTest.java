package discourje.core.validation.operators;

import discourje.core.lts.Action;
import discourje.core.validation.DMState;
import discourje.core.validation.DiscourjeModel;
import org.junit.jupiter.api.Test;
import static discourje.core.validation.operators.CtlOperators.close;
import static discourje.core.validation.operators.CtlOperators.snd;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ESTest<S> extends AbstractOperatorTest<S> {

    @Test
    public void testValidOnAllPathsEarlySplit() {
        DMState<S> s1 = createState(Action.Type.SEND, "a", "b");
        DMState<S> s2a = createState(Action.Type.SEND, "a", "b");
        DMState<S> s2b = createState(Action.Type.SEND, "a", "b");
        DMState<S> s3a = createState(Action.Type.CLOSE, "a", "b");
        DMState<S> s3b = createState(Action.Type.CLOSE, "a", "b");

        s1.addPreviousState(s2a);
        s1.addPreviousState(s2b);
        s2a.addPreviousState(s3a);
        s2b.addPreviousState(s3b);

        DiscourjeModel<S> model = createModel(s1, s2a, s2b, s3a, s3b);

        ES es = new ES(snd("a"), close("a", "b"));
        es.label(model);

        assertTrue(s1.hasLabel(es));
    }

    @Test
    public void testValidOnAllPathsLateSplit() {
        DMState<S> s1 = createState(Action.Type.SEND, "a", "b");
        DMState<S> s2 = createState(Action.Type.SEND, "a", "b");
        DMState<S> s3a = createState(Action.Type.CLOSE, "a", "b");
        DMState<S> s3b = createState(Action.Type.CLOSE, "a", "b");

        s1.addPreviousState(s2);
        s2.addPreviousState(s3a);
        s2.addPreviousState(s3b);

        DiscourjeModel<S> model = createModel(s1, s2, s3a, s3b);

        ES es = new ES(snd("a"), close("a", "b"));
        es.label(model);

        assertTrue(s1.hasLabel(es));
    }

    @Test
    public void testValidOnOnePath() {
        DMState<S> s1 = createState(Action.Type.SEND, "a", "b");
        DMState<S> s2a = createState(Action.Type.SEND, "a", "b");
        DMState<S> s2b = createState(Action.Type.SEND, "a", "b");
        DMState<S> s3a = createState(Action.Type.CLOSE, "a", "b");
        DMState<S> s3b = createState(Action.Type.CLOSE, "a", "c");

        s1.addPreviousState(s2a);
        s1.addPreviousState(s2b);
        s2a.addPreviousState(s3a);
        s2b.addPreviousState(s3b);

        DiscourjeModel<S> model = createModel(s1, s2a, s2b, s3a, s3b);

        ES es = new ES(snd("a"), close("a", "b"));
        es.label(model);

        assertTrue(s1.hasLabel(es));
    }

    @Test
    public void testValidOnNoPath() {
        DMState<S> s1 = createState(Action.Type.SEND, "a", "b");
        DMState<S> s2a = createState(Action.Type.SEND, "a", "b");
        DMState<S> s2b = createState(Action.Type.SEND, "a", "b");
        DMState<S> s3a = createState(Action.Type.CLOSE, "a", "c");
        DMState<S> s3b = createState(Action.Type.CLOSE, "a", "c");

        s1.addPreviousState(s2a);
        s1.addPreviousState(s2b);
        s2a.addPreviousState(s3a);
        s2b.addPreviousState(s3b);

        DiscourjeModel<S> model = createModel(s1, s2a, s2b, s3a, s3b);

        ES es = new ES(snd("a"), close("a", "b"));
        es.label(model);

        assertFalse(s1.hasLabel(es));
    }
}