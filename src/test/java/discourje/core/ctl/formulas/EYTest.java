package discourje.core.ctl.formulas;

import discourje.core.lts.Action;
import discourje.core.ctl.State;
import discourje.core.ctl.Model;
import discourje.core.ctl.formulas.atomic.Close;
import discourje.core.ctl.formulas.temporal.EY;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EYTest<S> extends AbstractCtlFormulaTest<S> {

    @Test
    public void testEYLine() {
        State<S> s1 = createState(Action.Type.SEND, "a", "b");
        State<S> s2 = createState(Action.Type.SEND, "b", "c");
        State<S> s3 = createState(Action.Type.CLOSE, "a", "b");
        State<S> s4 = createState(Action.Type.CLOSE, "b", "c");
        s2.addNextState(s1);
        s3.addNextState(s2);
        s4.addNextState(s3);
        Model<S> model = createModel(s1, s2, s3, s4);

        EY ey = new EY(new Close("a", "b"));
        ey.label(model);

        assertFalse(s1.hasLabel(model.getLabelIndex(ey)));
        assertTrue(s2.hasLabel(model.getLabelIndex(ey)));
        assertFalse(s3.hasLabel(model.getLabelIndex(ey)));
        assertFalse(s4.hasLabel(model.getLabelIndex(ey)));
    }

    @Test
    public void testEYAllPaths() {
        State<S> s1 = createState(Action.Type.SEND, "a", "b");
        State<S> s2a = createState(Action.Type.CLOSE, "a", "b");
        State<S> s2b = createState(Action.Type.CLOSE, "a", "b");
        s2a.addNextState(s1);
        s2b.addNextState(s1);
        Model<S> model = createModel(s1, s2a, s2b);

        EY ey = new EY(new Close("a", "b"));
        ey.label(model);

        assertTrue(s1.hasLabel(model.getLabelIndex(ey)));
        assertFalse(s2a.hasLabel(model.getLabelIndex(ey)));
        assertFalse(s2b.hasLabel(model.getLabelIndex(ey)));
    }

    @Test
    public void testEYOnePath() {
        State<S> s1 = createState(Action.Type.SEND, "a", "b");
        State<S> s2a = createState(Action.Type.CLOSE, "a", "b");
        State<S> s2b = createState(Action.Type.CLOSE, "b", "c");
        s2a.addNextState(s1);
        s2b.addNextState(s1);
        Model<S> model = createModel(s1, s2a, s2b);

        EY ey = new EY(new Close("a", "b"));
        ey.label(model);

        assertTrue(s1.hasLabel(model.getLabelIndex(ey)));
        assertFalse(s2a.hasLabel(model.getLabelIndex(ey)));
        assertFalse(s2b.hasLabel(model.getLabelIndex(ey)));
    }

    @Test
    public void testEYNoPath() {
        State<S> s1 = createState(Action.Type.SEND, "a", "b");
        State<S> s2a = createState(Action.Type.CLOSE, "b", "c");
        State<S> s2b = createState(Action.Type.CLOSE, "b", "c");
        s2a.addNextState(s1);
        s2b.addNextState(s1);
        Model<S> model = createModel(s1, s2a, s2b);

        EY ey = new EY(new Close("a", "b"));
        ey.label(model);

        assertFalse(s1.hasLabel(model.getLabelIndex(ey)));
        assertFalse(s2a.hasLabel(model.getLabelIndex(ey)));
        assertFalse(s2b.hasLabel(model.getLabelIndex(ey)));
    }
}