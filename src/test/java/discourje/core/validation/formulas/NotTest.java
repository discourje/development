package discourje.core.validation.formulas;

import discourje.core.lts.Action;
import discourje.core.validation.DMState;
import discourje.core.validation.DiscourjeModel;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class NotTest<S> extends AbstractCtlFormulaTest<S> {

    @Test
    public void testNot() {
        DMState<S> s1 = createState(Action.Type.SEND, "a", "b");
        DMState<S> s2 = createState(Action.Type.SEND, "b", "a");
        DiscourjeModel<S> model = createModel(s1, s2);

        Not not = new Not(new Send("a"));
        not.label(model);

        assertFalse(s1.hasLabel(model.getLabelIndex(not)));
        assertTrue(s2.hasLabel(model.getLabelIndex(not)));
    }
}