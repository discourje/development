package discourje.core.validation.formulas;

import discourje.core.lts.Action;
import discourje.core.validation.DMState;
import discourje.core.validation.DiscourjeModel;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class TrueTest<S> extends AbstractCtlFormulaTest<S> {

    @Test
    public void testTrue() {
        DMState<S> s1 = createState(Action.Type.SYNC, "a", "b");
        DiscourjeModel<S> model = createModel(s1);

        Send snd = new Send("a", null);
        snd.label(model);

        assertTrue(s1.hasLabel(model.getLabelIndex(snd)));
    }
}