package discourje.core.validation.formulas;

import discourje.core.lts.Action;
import discourje.core.validation.State;
import discourje.core.validation.Model;
import org.junit.jupiter.api.Test;
import static discourje.core.validation.formulas.CtlFormulas.close;
import static discourje.core.validation.formulas.CtlFormulas.send;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ESTest<S> extends AbstractCtlFormulaTest<S> {

    @Test
    public void testValidOnAllPathsEarlySplit() {
        State<S> s1 = createState(Action.Type.SEND, "a", "b");
        State<S> s2a = createState(Action.Type.SEND, "a", "b");
        State<S> s2b = createState(Action.Type.SEND, "a", "b");
        State<S> s3a = createState(Action.Type.CLOSE, "a", "b");
        State<S> s3b = createState(Action.Type.CLOSE, "a", "b");

        s2a.addNextState(s1);
        s2b.addNextState(s1);
        s3a.addNextState(s2a);
        s3b.addNextState(s2b);

        Model<S> model = createModel(s1, s2a, s2b, s3a, s3b);

        ES es = new ES(send("a", null), close("a", "b"));
        es.label(model);

        assertTrue(s1.hasLabel(model.getLabelIndex(es)));
    }

    @Test
    public void testValidOnAllPathsLateSplit() {
        State<S> s1 = createState(Action.Type.SEND, "a", "b");
        State<S> s2 = createState(Action.Type.SEND, "a", "b");
        State<S> s3a = createState(Action.Type.CLOSE, "a", "b");
        State<S> s3b = createState(Action.Type.CLOSE, "a", "b");

        s2.addNextState(s1);
        s3a.addNextState(s2);
        s3b.addNextState(s2);

        Model<S> model = createModel(s1, s2, s3a, s3b);

        ES es = new ES(send("a", null), close("a", "b"));
        es.label(model);

        assertTrue(s1.hasLabel(model.getLabelIndex(es)));
    }

    @Test
    public void testValidOnOnePath() {
        State<S> s1 = createState(Action.Type.SEND, "a", "b");
        State<S> s2a = createState(Action.Type.SEND, "a", "b");
        State<S> s2b = createState(Action.Type.SEND, "a", "b");
        State<S> s3a = createState(Action.Type.CLOSE, "a", "b");
        State<S> s3b = createState(Action.Type.CLOSE, "a", "c");

        s2a.addNextState(s1);
        s2b.addNextState(s1);
        s3a.addNextState(s2a);
        s3b.addNextState(s2b);

        Model<S> model = createModel(s1, s2a, s2b, s3a, s3b);

        ES es = new ES(send("a", null), close("a", "b"));
        es.label(model);

        assertTrue(s1.hasLabel(model.getLabelIndex(es)));
    }

    @Test
    public void testValidOnNoPath() {
        State<S> s1 = createState(Action.Type.SEND, "a", "b");
        State<S> s2a = createState(Action.Type.SEND, "a", "b");
        State<S> s2b = createState(Action.Type.SEND, "a", "b");
        State<S> s3a = createState(Action.Type.CLOSE, "a", "c");
        State<S> s3b = createState(Action.Type.CLOSE, "a", "c");

        s2a.addNextState(s1);
        s2b.addNextState(s1);
        s3a.addNextState(s2a);
        s3b.addNextState(s2b);

        Model<S> model = createModel(s1, s2a, s2b, s3a, s3b);

        ES es = new ES(send("a", null), close("a", "b"));
        es.label(model);

        assertFalse(s1.hasLabel(model.getLabelIndex(es)));
    }
}