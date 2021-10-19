package discourje.core.ctl.formulas;

import discourje.core.lts.Action;
import discourje.core.ctl.State;
import discourje.core.ctl.Model;
import discourje.core.ctl.formulas.atomic.Close;
import discourje.core.ctl.formulas.temporal.EX;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class EXTest<S> extends AbstractCtlFormulaTest<S> {

    @Test
    public void testEXLine() {
        State<S> s1 = createState(Action.Type.SEND, "a", "b");
        State<S> s2 = createState(Action.Type.SEND, "b", "c");
        State<S> s3 = createState(Action.Type.CLOSE, "a", "b");
        State<S> s4 = createState(Action.Type.CLOSE, "b", "c");
        s1.addNextState(s2);
        s2.addNextState(s3);
        s3.addNextState(s4);
        Model<S> model = createModel(s1, s2, s3, s4);

        EX ex = new EX(new Close("a", "b"));
        model.calculateLabels(ex);

        assertFalse(model.hasLabel(s1, ex));
        assertTrue(model.hasLabel(s2, ex));
        assertFalse(model.hasLabel(s3, ex));
        assertFalse(model.hasLabel(s4, ex));
    }

    @Test
    public void testEXAllPaths() {
        State<S> s1 = createState(Action.Type.SEND, "a", "b");
        State<S> s2a = createState(Action.Type.CLOSE, "a", "b");
        State<S> s2b = createState(Action.Type.CLOSE, "a", "b");
        s1.addNextState(s2a);
        s1.addNextState(s2b);
        Model<S> model = createModel(s1, s2a, s2b);

        EX ex = new EX(new Close("a", "b"));
        model.calculateLabels(ex);

        assertTrue(model.hasLabel(s1, ex));
        assertFalse(model.hasLabel(s2a, ex));
        assertFalse(model.hasLabel(s2a, ex));
    }

    @Test
    public void testEXOnePath() {
        State<S> s1 = createState(Action.Type.SEND, "a", "b");
        State<S> s2a = createState(Action.Type.CLOSE, "a", "b");
        State<S> s2b = createState(Action.Type.CLOSE, "b", "c");
        s1.addNextState(s2a);
        s1.addNextState(s2b);
        Model<S> model = createModel(s1, s2a, s2b);

        EX ex = new EX(new Close("a", "b"));
        model.calculateLabels(ex);

        assertTrue(model.hasLabel(s1, ex));
        assertFalse(model.hasLabel(s2a, ex));
        assertFalse(model.hasLabel(s2a, ex));
    }

    @Test
    public void testEXNoPath() {
        State<S> s1 = createState(Action.Type.SEND, "a", "b");
        State<S> s2a = createState(Action.Type.CLOSE, "b", "c");
        State<S> s2b = createState(Action.Type.CLOSE, "b", "c");
        s1.addNextState(s2a);
        s1.addNextState(s2b);
        Model<S> model = createModel(s1, s2a, s2b);

        EX ex = new EX(new Close("a", "b"));
        model.calculateLabels(ex);

        assertFalse(model.hasLabel(s1, ex));
        assertFalse(model.hasLabel(s2a, ex));
        assertFalse(model.hasLabel(s2a, ex));
    }
}