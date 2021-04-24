package discourje.core.validation.formulas;

import discourje.core.lts.Action;
import discourje.core.validation.State;
import discourje.core.validation.Model;
import org.junit.jupiter.api.Test;
import static discourje.core.validation.formulas.CtlFormulas.close;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AYTest<S> extends AbstractCtlFormulaTest<S> {

    @Test
    public void testSelfAndAllSuccessors() {
        State<S> s1 = createState(Action.Type.CLOSE, "a", "b");
        State<S> s2a = createState(Action.Type.CLOSE, "a", "b");
        State<S> s2b = createState(Action.Type.CLOSE, "a", "b");

        s2a.addNextState(s1);
        s2b.addNextState(s1);

        Model<S> model = createModel(s1, s2a, s2b);

        AY ay = new AY(close("a", "b"));
        ay.label(model);

        assertTrue(s1.hasLabel(model.getLabelIndex(ay)));
    }

    @Test
    public void testNotSelfButAllSuccessors() {
        State<S> s1 = createState(Action.Type.SEND, "a", "b");
        State<S> s2a = createState(Action.Type.CLOSE, "a", "b");
        State<S> s2b = createState(Action.Type.CLOSE, "a", "b");

        s2a.addNextState(s1);
        s2b.addNextState(s1);

        Model<S> model = createModel(s1, s2a, s2b);

        AY ay = new AY(close("a", "b"));
        ay.label(model);

        assertTrue(s1.hasLabel(model.getLabelIndex(ay)));
    }

    @Test
    public void testNotAllSuccessors() {
        State<S> s1 = createState(Action.Type.CLOSE, "a", "b");
        State<S> s2a = createState(Action.Type.SEND, "a", "b");
        State<S> s2b = createState(Action.Type.CLOSE, "a", "b");

        s2a.addNextState(s1);
        s2b.addNextState(s1);

        Model<S> model = createModel(s1, s2a, s2b);

        AY ay = new AY(close("a", "b"));
        ay.label(model);

        assertFalse(s1.hasLabel(model.getLabelIndex(ay)));
    }

    @Test
    public void testNoSuccessors() {
        State<S> s1 = createState(Action.Type.CLOSE, "a", "b");
        State<S> s2a = createState(Action.Type.SEND, "a", "b");
        State<S> s2b = createState(Action.Type.SEND, "a", "b");

        s2a.addNextState(s1);
        s2b.addNextState(s1);

        Model<S> model = createModel(s1, s2a, s2b);

        AY ay = new AY(close("a", "b"));
        ay.label(model);

        assertFalse(s1.hasLabel(model.getLabelIndex(ay)));
    }
}