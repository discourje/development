package discourje.core.validation.formulas;

import discourje.core.lts.Action;
import discourje.core.validation.State;
import discourje.core.validation.Model;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CloseTest<S> extends AbstractCtlFormulaTest<S> {

    @Test
    public void testClose() {
        // setup
        State<S> s1 = createState(Action.Type.CLOSE, "a", "b");
        State<S> s2 = createState(Action.Type.CLOSE, "b", "a");

        Model<S> model = createModel(s1, s2);

        // execute
        CtlFormula close = new Close("a", "b");
        close.label(model);

        // verify
        assertTrue(s1.hasLabel(model.getLabelIndex(close)));
        assertFalse(s2.hasLabel(model.getLabelIndex(close)));
    }

}