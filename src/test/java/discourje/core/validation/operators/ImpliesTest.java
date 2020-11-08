package discourje.core.validation.operators;

import discourje.core.lts.Action;
import discourje.core.validation.DMState;
import discourje.core.validation.DiscourjeModel;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ImpliesTest<S> extends AbstractOperatorTest<S> {

    @Test
    public void testTrueImpliesTrueIsTrue() {
        DMState<S> s1 = createState(Action.Type.SEND, "a", "b");
        DiscourjeModel<S> model = createModel(s1);

        Implies implies = new Implies(True.TRUE, True.TRUE);
        implies.label(model);

        assertTrue(s1.hasLabel(implies));
    }

    @Test
    public void testTrueImpliesFalseIsFalse() {
        DMState<S> s1 = createState(Action.Type.SEND, "a", "b");
        DiscourjeModel<S> model = createModel(s1);

        Implies implies = new Implies(True.TRUE, new Not(True.TRUE));
        implies.label(model);

        assertFalse(s1.hasLabel(implies));
    }

    @Test
    public void testFalseImpliesTrueIsTrue() {
        DMState<S> s1 = createState(Action.Type.SEND, "a", "b");
        DiscourjeModel<S> model = createModel(s1);

        Implies implies = new Implies(True.TRUE, True.TRUE);
        implies.label(model);

        assertTrue(s1.hasLabel(implies));
    }

    @Test
    public void testFalseImpliesFalseIsTrue() {
        DMState<S> s1 = createState(Action.Type.SEND, "a", "b");
        DiscourjeModel<S> model = createModel(s1);

        Implies implies = new Implies(True.TRUE, True.TRUE);
        implies.label(model);

        assertTrue(s1.hasLabel(implies));
    }
}