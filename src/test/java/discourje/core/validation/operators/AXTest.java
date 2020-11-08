package discourje.core.validation.operators;

import discourje.core.lts.Action;
import discourje.core.validation.DMState;
import discourje.core.validation.DiscourjeModel;
import org.junit.jupiter.api.Test;
import static discourje.core.validation.operators.CtlFormulas.close;
import static org.junit.jupiter.api.Assertions.*;

class AXTest<S> extends AbstractOperatorTest<S> {

    @Test
    public void testSelfAndAllSuccessors() {
        DMState<S> s1 = createState(Action.Type.CLOSE, "a", "b");
        DMState<S> s2a = createState(Action.Type.CLOSE, "a", "b");
        DMState<S> s2b = createState(Action.Type.CLOSE, "a", "b");

        s1.addTransition(s2a);
        s1.addTransition(s2b);

        DiscourjeModel<S> model = createModel(s1, s2a, s2b);

        AX ax = new AX(close("a", "b"));
        ax.label(model);

        assertTrue(s1.hasLabel(ax));
    }

    @Test
    public void testNotSelfButAllSuccessors() {
        DMState<S> s1 = createState(Action.Type.SEND, "a", "b");
        DMState<S> s2a = createState(Action.Type.CLOSE, "a", "b");
        DMState<S> s2b = createState(Action.Type.CLOSE, "a", "b");

        s1.addTransition(s2a);
        s1.addTransition(s2b);

        DiscourjeModel<S> model = createModel(s1, s2a, s2b);

        AX ax = new AX(close("a", "b"));
        ax.label(model);

        assertTrue(s1.hasLabel(ax));
    }

    @Test
    public void testNotAllSuccessors() {
        DMState<S> s1 = createState(Action.Type.CLOSE, "a", "b");
        DMState<S> s2a = createState(Action.Type.SEND, "a", "b");
        DMState<S> s2b = createState(Action.Type.CLOSE, "a", "b");

        s1.addTransition(s2a);
        s1.addTransition(s2b);

        DiscourjeModel<S> model = createModel(s1, s2a, s2b);

        AX ax = new AX(close("a", "b"));
        ax.label(model);

        assertFalse(s1.hasLabel(ax));
    }

    @Test
    public void testNoSuccessors() {
        DMState<S> s1 = createState(Action.Type.CLOSE, "a", "b");
        DMState<S> s2a = createState(Action.Type.SEND, "a", "b");
        DMState<S> s2b = createState(Action.Type.SEND, "a", "b");

        s1.addTransition(s2a);
        s1.addTransition(s2b);

        DiscourjeModel<S> model = createModel(s1, s2a, s2b);

        AX ax = new AX(close("a", "b"));
        ax.label(model);

        assertFalse(s1.hasLabel(ax));
    }
}