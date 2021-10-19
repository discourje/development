package discourje.core.ctl.formulas;

import discourje.core.lts.Action;
import discourje.core.ctl.State;
import discourje.core.ctl.Model;
import discourje.core.ctl.formulas.atomic.True;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ImpliesTest<S> extends AbstractCtlFormulaTest<S> {

    @Test
    public void testTrueImpliesTrueIsTrue() {
        State<S> s1 = createState(Action.Type.SEND, "a", "b");
        Model<S> model = createModel(s1);

        Implies implies = new Implies(True.INSTANCE, True.INSTANCE);
        model.calculateLabels(implies);

        assertTrue(model.hasLabel(s1, implies));
    }

    @Test
    public void testTrueImpliesFalseIsFalse() {
        State<S> s1 = createState(Action.Type.SEND, "a", "b");
        Model<S> model = createModel(s1);

        Implies implies = new Implies(True.INSTANCE, new Not(True.INSTANCE));
        model.calculateLabels(implies);

        assertFalse(model.hasLabel(s1, implies));
    }

    @Test
    public void testFalseImpliesTrueIsTrue() {
        State<S> s1 = createState(Action.Type.SEND, "a", "b");
        Model<S> model = createModel(s1);

        Implies implies = new Implies(True.INSTANCE, True.INSTANCE);
        model.calculateLabels(implies);

        assertTrue(model.hasLabel(s1, implies));
    }

    @Test
    public void testFalseImpliesFalseIsTrue() {
        State<S> s1 = createState(Action.Type.SEND, "a", "b");
        Model<S> model = createModel(s1);

        Implies implies = new Implies(True.INSTANCE, True.INSTANCE);
        model.calculateLabels(implies);

        assertTrue(model.hasLabel(s1, implies));
    }
}