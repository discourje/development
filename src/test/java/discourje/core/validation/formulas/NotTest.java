package discourje.core.validation.formulas;

import discourje.core.lts.Action;
import discourje.core.validation.State;
import discourje.core.validation.Model;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class NotTest<S> extends AbstractCtlFormulaTest<S> {

    @Test
    public void testNot() {
        State<S> s1 = createState(Action.Type.SEND, "a", "b");
        State<S> s2 = createState(Action.Type.SEND, "b", "a");
        Model<S> model = createModel(s1, s2);

        Not not = new Not(new Send("a", null));
        not.label(model);

        assertFalse(s1.hasLabel(model.getLabelIndex(not)));
        assertTrue(s2.hasLabel(model.getLabelIndex(not)));
    }
}