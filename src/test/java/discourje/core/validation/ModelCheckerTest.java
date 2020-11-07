package discourje.core.validation;

import clojure.java.api.Clojure;
import clojure.lang.IFn;
import clojure.lang.Var;
import discourje.core.lts.LTS;
import java.util.List;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ModelCheckerTest<Spec> {

    private static IFn require;
    private static IFn makeLts;
    public static final String NS_VALIDATION = "discourje.core.validation.validation-tests";

    @BeforeAll
    public static void setUp() {
        require = Clojure.var("clojure.core", "require");
        require.invoke(Clojure.read("discourje.core.validation.validation-tests"));
        require.invoke(Clojure.read(NS_VALIDATION));
        makeLts = Clojure.var("discourje.core.validation.validation-tests", "make-lts");
    }

    @Test
    public void testSendAfterCloseTrivialCorrect() {
        List<String> result = getModelCheckerResult("send-after-close-trivial-correct");
        assertTrue(result.isEmpty());
    }

    @Test
    public void testSendAfterCloseTrivialIncorrect() {
        List<String> result = getModelCheckerResult("send-after-close-trivial-incorrect");
        assertFalse(result.isEmpty());
    }

    @Test
    public void testSendAfterCloseNonTrivialCorrect() {
        List<String> result = getModelCheckerResult("send-after-close-non-trivial-correct");
        assertTrue(result.isEmpty());
    }

    @Test
    public void testSendAfterCloseNonTrivialIncorrect() {
        List<String> result = getModelCheckerResult("send-after-close-non-trivial-incorrect");
        assertFalse(result.isEmpty());
    }

    private List<String> getModelCheckerResult(String name) {
        IFn var = Clojure.var(NS_VALIDATION, name);
        LTS<Spec> lts = (LTS<Spec>) ((Var) var).get();

        ModelChecker modelChecker = new ModelChecker(lts);

        List<String> result = modelChecker.checkModel();
        return result;
    }
}