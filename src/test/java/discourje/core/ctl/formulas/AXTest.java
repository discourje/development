package discourje.core.ctl.formulas;

import discourje.core.lts.Action;
import discourje.core.ctl.State;
import discourje.core.ctl.Model;
import discourje.core.ctl.formulas.temporal.AX;
import org.junit.jupiter.api.Test;
import static discourje.core.ctl.Formulas.close;
import static org.junit.jupiter.api.Assertions.*;

class AXTest<S> extends AbstractCtlFormulaTest<S> {

    @Test
    public void testSelfAndAllSuccessors() {
        State<S> s1 = createState(Action.Type.CLOSE, "a", "b");
        State<S> s2a = createState(Action.Type.CLOSE, "a", "b");
        State<S> s2b = createState(Action.Type.CLOSE, "a", "b");

        s1.addNextState(s2a);
        s1.addNextState(s2b);

        Model<S> model = createModel(s1, s2a, s2b);

        AX ax = new AX(close("a", "b"));
        model.calculateLabels(ax);

        assertTrue(model.hasLabel(s1, ax));
    }

    @Test
    public void testNotSelfButAllSuccessors() {
        State<S> s1 = createState(Action.Type.SEND, "a", "b");
        State<S> s2a = createState(Action.Type.CLOSE, "a", "b");
        State<S> s2b = createState(Action.Type.CLOSE, "a", "b");

        s1.addNextState(s2a);
        s1.addNextState(s2b);

        Model<S> model = createModel(s1, s2a, s2b);

        AX ax = new AX(close("a", "b"));
        model.calculateLabels(ax);

        assertTrue(model.hasLabel(s1, ax));
    }

    @Test
    public void testNotAllSuccessors() {
        State<S> s1 = createState(Action.Type.CLOSE, "a", "b");
        State<S> s2a = createState(Action.Type.SEND, "a", "b");
        State<S> s2b = createState(Action.Type.CLOSE, "a", "b");

        s1.addNextState(s2a);
        s1.addNextState(s2b);

        Model<S> model = createModel(s1, s2a, s2b);

        AX ax = new AX(close("a", "b"));
        model.calculateLabels(ax);

        assertFalse(model.hasLabel(s1, ax));
    }

    @Test
    public void testNoSuccessors() {
        State<S> s1 = createState(Action.Type.CLOSE, "a", "b");
        State<S> s2a = createState(Action.Type.SEND, "a", "b");
        State<S> s2b = createState(Action.Type.SEND, "a", "b");

        s1.addNextState(s2a);
        s1.addNextState(s2b);

        Model<S> model = createModel(s1, s2a, s2b);

        AX ax = new AX(close("a", "b"));
        model.calculateLabels(ax);

        assertFalse(model.hasLabel(s1, ax));
    }
}