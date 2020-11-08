package discourje.core.validation;

import clojure.java.api.Clojure;
import clojure.lang.IFn;
import clojure.lang.Var;
import discourje.core.lts.LTS;
import discourje.core.validation.operators.CtlFormulas;
import java.util.List;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ModelCheckerTest<Spec> {

    public static final String NS_VALIDATION = "discourje.core.validation.validation-tests";

    @BeforeAll
    public static void setUp() {
        IFn require = Clojure.var("clojure.core", "require");
        require.invoke(Clojure.read("discourje.core.validation.validation-tests"));
        require.invoke(Clojure.read(NS_VALIDATION));
    }

    @Test
    public void testCausalityTrivialCorrect() {
        List<String> result = getModelCheckerResult("causality-trivial-correct");
        assertTrue(result.isEmpty());
    }

    @Test
    public void testCausalityTrivialIncorrect() {
        List<String> result = getModelCheckerResult("causality-trivial-incorrect");
        assertFalse(result.isEmpty());
    }

    @Test
    public void testCausalityNonTrivialCorrect() {
        List<String> result = getModelCheckerResult("causality-non-trivial-correct");
        assertTrue(result.isEmpty());
    }

    @Test
    public void testCausalityNonTrivialIncorrect() {
        List<String> result = getModelCheckerResult("causality-non-trivial-incorrect");
        assertFalse(result.isEmpty());
    }

    @Test
    public void testCloseOnlyOnceTrivialCorrect() {
        List<String> result = getModelCheckerResult("close-only-once-trivial-correct");
        assertTrue(result.isEmpty());
    }

    @Test
    public void testCloseOnlyOnceTrivialIncorrect() {
        List<String> result = getModelCheckerResult("close-only-once-trivial-incorrect");
        assertFalse(result.isEmpty());
    }

    @Test
    public void testCloseOnlyOnceNonTrivialCorrect() {
        List<String> result = getModelCheckerResult("close-only-once-non-trivial-correct");
        assertTrue(result.isEmpty());
    }

    @Test
    public void testCloseOnlyOnceNonTrivialIncorrect() {
        List<String> result = getModelCheckerResult("close-only-once-non-trivial-incorrect");
        assertFalse(result.isEmpty());
    }

    @Test
    public void testCloseUsedChannelsTrivialCorrect() {
        List<String> result = getModelCheckerResult("close-used-channels-trivial-correct");
        assertTrue(result.isEmpty());
    }

    @Test
    public void testCloseUsedChannelsTrivialIncorrect() {
        List<String> result = getModelCheckerResult("close-used-channels-trivial-incorrect");
        assertFalse(result.isEmpty());
    }

    @Test
    public void testCloseUsedChannelsNonTrivialCorrect() {
        List<String> result = getModelCheckerResult("close-used-channels-non-trivial-correct");
        assertTrue(result.isEmpty());
    }

    @Test
    public void testCloseUsedChannelsNonTrivialIncorrect() {
        List<String> result = getModelCheckerResult("close-used-channels-non-trivial-incorrect");
        assertFalse(result.isEmpty());
    }

//    @Test
//    public void testSendAfterCloseTrivialCorrect() {
//        List<String> result = getModelCheckerResult("send-after-close-trivial-correct");
//        assertTrue(result.isEmpty());
//    }
//
//    @Test
//    public void testSendAfterCloseTrivialIncorrect() {
//        List<String> result = getModelCheckerResult("send-after-close-trivial-incorrect");
//        assertFalse(result.isEmpty());
//    }
//
//    @Test
//    public void testSendAfterCloseNonTrivialCorrect() {
//        List<String> result = getModelCheckerResult("send-after-close-non-trivial-correct");
//        assertTrue(result.isEmpty());
//    }
//
//    @Test
//    public void testSendAfterCloseNonTrivialIncorrect() {
//        List<String> result = getModelCheckerResult("send-after-close-non-trivial-incorrect");
//        assertFalse(result.isEmpty());
//    }

    @Test
    public void testSendAfterCloseTrivialCorrect() {
        List<String> result = getModelCheckerResult("send-after-close-trivial-correct");
        assertFalse(result.contains(CtlFormulas.doNotSendAfterClose("a", "b").getDescription()));
    }

    @Test
    public void testSendAfterCloseTrivialIncorrect() {
        List<String> result = getModelCheckerResult("send-after-close-trivial-incorrect");
        assertTrue(result.contains(CtlFormulas.doNotSendAfterClose("a", "b").getDescription()));
    }

    @Test
    public void testSendAfterCloseNonTrivialCorrect() {
        List<String> result = getModelCheckerResult("send-after-close-non-trivial-correct");
        assertFalse(result.contains(CtlFormulas.doNotSendAfterClose("a", "b").getDescription()));
    }

    @Test
    public void testSendAfterCloseNonTrivialIncorrect() {
        List<String> result = getModelCheckerResult("send-after-close-non-trivial-incorrect");
        assertTrue(result.contains(CtlFormulas.doNotSendAfterClose("a", "b").getDescription()));
    }

//    @Test
//    public void testSendAfterCloseTrivialCorrect() {
//        List<String> result = getModelCheckerResult("send-after-close-trivial-correct");
//        assertTrue(result.isEmpty());
//    }
//
//    @Test
//    public void testSendAfterCloseTrivialIncorrect() {
//        List<String> result = getModelCheckerResult("send-after-close-trivial-incorrect");
//        assertFalse(result.isEmpty());
//    }
//
//    @Test
//    public void testSendAfterCloseNonTrivialCorrect() {
//        List<String> result = getModelCheckerResult("send-after-close-non-trivial-correct");
//        assertTrue(result.isEmpty());
//    }
//
//    @Test
//    public void testSendAfterCloseNonTrivialIncorrect() {
//        List<String> result = getModelCheckerResult("send-after-close-non-trivial-incorrect");
//        assertFalse(result.isEmpty());
//    }

    private List<String> getModelCheckerResult(String name) {
        IFn var = Clojure.var(NS_VALIDATION, name);
        @SuppressWarnings("uncheckedcast")
        LTS<Spec> lts = (LTS<Spec>) ((Var) var).get();

        ModelChecker modelChecker = new ModelChecker(lts);

        return modelChecker.checkModel();
    }
}