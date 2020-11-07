package discourje.core.validation.operators;

import discourje.core.lts.Action;
import discourje.core.validation.DMState;
import discourje.core.validation.DiscourjeModel;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ImpliesTest<S> extends AbstractOperatorTest<S> {

    @Test
    public void testImpliesTrueTrue() {
        DMState<S> s1 = createState(Action.Type.SEND, "a", "b");
        DiscourjeModel<S> model = createModel(s1);

        Implies implies = new Implies(new True(), new True());
        implies.label(model);

        assertTrue(s1.hasLabel(implies));
    }

    @Test
    public void testImpliesTrueFalse() {
        DMState<S> s1 = createState(Action.Type.SEND, "a", "b");
        DiscourjeModel<S> model = createModel(s1);

        Implies implies = new Implies(new True(), new Not(new True()));
        implies.label(model);

        assertFalse(s1.hasLabel(implies));
    }

    @Test
    public void testImpliesFalseTrue() {
        DMState<S> s1 = createState(Action.Type.SEND, "a", "b");
        DiscourjeModel<S> model = createModel(s1);

        Implies implies = new Implies(new True(), new True());
        implies.label(model);

        assertTrue(s1.hasLabel(implies));
    }

    @Test
    public void testImpliesFalseFalse() {
        DMState<S> s1 = createState(Action.Type.SEND, "a", "b");
        DiscourjeModel<S> model = createModel(s1);

        Implies implies = new Implies(new True(), new True());
        implies.label(model);

        assertTrue(s1.hasLabel(implies));
    }
}