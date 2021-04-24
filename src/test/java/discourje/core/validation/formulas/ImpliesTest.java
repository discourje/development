package discourje.core.validation.formulas;

import discourje.core.lts.Action;
import discourje.core.validation.State;
import discourje.core.validation.Model;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ImpliesTest<S> extends AbstractCtlFormulaTest<S> {

    @Test
    public void testTrueImpliesTrueIsTrue() {
        State<S> s1 = createState(Action.Type.SEND, "a", "b");
        Model<S> model = createModel(s1);

        Implies implies = new Implies(True.TRUE, True.TRUE);
        implies.label(model);

        assertTrue(s1.hasLabel(model.getLabelIndex(implies)));
    }

    @Test
    public void testTrueImpliesFalseIsFalse() {
        State<S> s1 = createState(Action.Type.SEND, "a", "b");
        Model<S> model = createModel(s1);

        Implies implies = new Implies(True.TRUE, new Not(True.TRUE));
        implies.label(model);

        assertFalse(s1.hasLabel(model.getLabelIndex(implies)));
    }

    @Test
    public void testFalseImpliesTrueIsTrue() {
        State<S> s1 = createState(Action.Type.SEND, "a", "b");
        Model<S> model = createModel(s1);

        Implies implies = new Implies(True.TRUE, True.TRUE);
        implies.label(model);

        assertTrue(s1.hasLabel(model.getLabelIndex(implies)));
    }

    @Test
    public void testFalseImpliesFalseIsTrue() {
        State<S> s1 = createState(Action.Type.SEND, "a", "b");
        Model<S> model = createModel(s1);

        Implies implies = new Implies(True.TRUE, True.TRUE);
        implies.label(model);

        assertTrue(s1.hasLabel(model.getLabelIndex(implies)));
    }
}