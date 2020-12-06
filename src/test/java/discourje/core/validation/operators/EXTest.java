package discourje.core.validation.operators;

import discourje.core.lts.Action;
import discourje.core.validation.DMState;
import discourje.core.validation.DiscourjeModel;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class EXTest<S> extends AbstractOperatorTest<S> {

    @Test
    public void testEXLine() {
        DMState<S> s1 = createState(Action.Type.SEND, "a", "b");
        DMState<S> s2 = createState(Action.Type.SEND, "b", "c");
        DMState<S> s3 = createState(Action.Type.CLOSE, "a", "b");
        DMState<S> s4 = createState(Action.Type.CLOSE, "b", "c");
        s1.addNextState(s2);
        s2.addNextState(s3);
        s3.addNextState(s4);
        DiscourjeModel<S> model = createModel(s1, s2, s3, s4);

        EX ex = new EX(new Close("a", "b"));
        ex.label(model);

        assertFalse(s1.hasLabel(model.getLabelIndex(ex)));
        assertTrue(s2.hasLabel(model.getLabelIndex(ex)));
        assertFalse(s3.hasLabel(model.getLabelIndex(ex)));
        assertFalse(s4.hasLabel(model.getLabelIndex(ex)));
    }

    @Test
    public void testEXAllPaths() {
        DMState<S> s1 = createState(Action.Type.SEND, "a", "b");
        DMState<S> s2a = createState(Action.Type.CLOSE, "a", "b");
        DMState<S> s2b = createState(Action.Type.CLOSE, "a", "b");
        s1.addNextState(s2a);
        s1.addNextState(s2b);
        DiscourjeModel<S> model = createModel(s1, s2a, s2b);

        EX ex = new EX(new Close("a", "b"));
        ex.label(model);

        assertTrue(s1.hasLabel(model.getLabelIndex(ex)));
        assertFalse(s2a.hasLabel(model.getLabelIndex(ex)));
        assertFalse(s2b.hasLabel(model.getLabelIndex(ex)));
    }

    @Test
    public void testEXOnePath() {
        DMState<S> s1 = createState(Action.Type.SEND, "a", "b");
        DMState<S> s2a = createState(Action.Type.CLOSE, "a", "b");
        DMState<S> s2b = createState(Action.Type.CLOSE, "b", "c");
        s1.addNextState(s2a);
        s1.addNextState(s2b);
        DiscourjeModel<S> model = createModel(s1, s2a, s2b);

        EX ex = new EX(new Close("a", "b"));
        ex.label(model);

        assertTrue(s1.hasLabel(model.getLabelIndex(ex)));
        assertFalse(s2a.hasLabel(model.getLabelIndex(ex)));
        assertFalse(s2b.hasLabel(model.getLabelIndex(ex)));
    }

    @Test
    public void testEXNoPath() {
        DMState<S> s1 = createState(Action.Type.SEND, "a", "b");
        DMState<S> s2a = createState(Action.Type.CLOSE, "b", "c");
        DMState<S> s2b = createState(Action.Type.CLOSE, "b", "c");
        s1.addNextState(s2a);
        s1.addNextState(s2b);
        DiscourjeModel<S> model = createModel(s1, s2a, s2b);

        EX ex = new EX(new Close("a", "b"));
        ex.label(model);

        assertFalse(s1.hasLabel(model.getLabelIndex(ex)));
        assertFalse(s2a.hasLabel(model.getLabelIndex(ex)));
        assertFalse(s2b.hasLabel(model.getLabelIndex(ex)));
    }
}