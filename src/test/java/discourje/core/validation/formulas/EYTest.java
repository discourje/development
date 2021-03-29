package discourje.core.validation.formulas;

import discourje.core.lts.Action;
import discourje.core.validation.DMState;
import discourje.core.validation.DiscourjeModel;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EYTest<S> extends AbstractCtlFormulaTest<S> {

    @Test
    public void testEYLine() {
        DMState<S> s1 = createState(Action.Type.SEND, "a", "b");
        DMState<S> s2 = createState(Action.Type.SEND, "b", "c");
        DMState<S> s3 = createState(Action.Type.CLOSE, "a", "b");
        DMState<S> s4 = createState(Action.Type.CLOSE, "b", "c");
        s2.addNextState(s1);
        s3.addNextState(s2);
        s4.addNextState(s3);
        DiscourjeModel<S> model = createModel(s1, s2, s3, s4);

        EY ey = new EY(new Close("a", "b"));
        ey.label(model);

        assertFalse(s1.hasLabel(model.getLabelIndex(ey)));
        assertTrue(s2.hasLabel(model.getLabelIndex(ey)));
        assertFalse(s3.hasLabel(model.getLabelIndex(ey)));
        assertFalse(s4.hasLabel(model.getLabelIndex(ey)));
    }

    @Test
    public void testEYAllPaths() {
        DMState<S> s1 = createState(Action.Type.SEND, "a", "b");
        DMState<S> s2a = createState(Action.Type.CLOSE, "a", "b");
        DMState<S> s2b = createState(Action.Type.CLOSE, "a", "b");
        s2a.addNextState(s1);
        s2b.addNextState(s1);
        DiscourjeModel<S> model = createModel(s1, s2a, s2b);

        EY ey = new EY(new Close("a", "b"));
        ey.label(model);

        assertTrue(s1.hasLabel(model.getLabelIndex(ey)));
        assertFalse(s2a.hasLabel(model.getLabelIndex(ey)));
        assertFalse(s2b.hasLabel(model.getLabelIndex(ey)));
    }

    @Test
    public void testEYOnePath() {
        DMState<S> s1 = createState(Action.Type.SEND, "a", "b");
        DMState<S> s2a = createState(Action.Type.CLOSE, "a", "b");
        DMState<S> s2b = createState(Action.Type.CLOSE, "b", "c");
        s2a.addNextState(s1);
        s2b.addNextState(s1);
        DiscourjeModel<S> model = createModel(s1, s2a, s2b);

        EY ey = new EY(new Close("a", "b"));
        ey.label(model);

        assertTrue(s1.hasLabel(model.getLabelIndex(ey)));
        assertFalse(s2a.hasLabel(model.getLabelIndex(ey)));
        assertFalse(s2b.hasLabel(model.getLabelIndex(ey)));
    }

    @Test
    public void testEYNoPath() {
        DMState<S> s1 = createState(Action.Type.SEND, "a", "b");
        DMState<S> s2a = createState(Action.Type.CLOSE, "b", "c");
        DMState<S> s2b = createState(Action.Type.CLOSE, "b", "c");
        s2a.addNextState(s1);
        s2b.addNextState(s1);
        DiscourjeModel<S> model = createModel(s1, s2a, s2b);

        EY ey = new EY(new Close("a", "b"));
        ey.label(model);

        assertFalse(s1.hasLabel(model.getLabelIndex(ey)));
        assertFalse(s2a.hasLabel(model.getLabelIndex(ey)));
        assertFalse(s2b.hasLabel(model.getLabelIndex(ey)));
    }
}