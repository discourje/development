package discourje.core.validation.formulas;

import discourje.core.lts.Action;
import discourje.core.validation.State;
import discourje.core.validation.Model;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class TrueTest<S> extends AbstractCtlFormulaTest<S> {

    @Test
    public void testTrue() {
        State<S> s1 = createState(Action.Type.SYNC, "a", "b");
        Model<S> model = createModel(s1);

        Send snd = new Send("a", null);
        snd.label(model);

        assertTrue(s1.hasLabel(model.getLabelIndex(snd)));
    }
}