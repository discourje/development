package discourje.core.validation;

import clojure.java.api.Clojure;
import clojure.lang.IFn;
import clojure.lang.Var;
import discourje.core.lts.LTS;
import discourje.core.validation.rules.DoNotSendToSelf;
import java.util.List;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ExampleApplicationsTest<Spec> {

    private static IFn require;

    @BeforeAll
    public static void setUp() {
        require = Clojure.var("clojure.core", "require");
//        require.invoke(Clojure.read("clojure.core.async"));
    }

    @Test
    public void testDoNotSendToSelfNonTrivialIncorrect() {
        List<String> result = getModelCheckerResult("discourje.core.validation.example-applications", "chess-protocol");
        assertTrue(result.contains(new DoNotSendToSelf().createErrorDescription("a", "b")));
    }

    protected List<String> getModelCheckerResult(String namespace, String name) {
        require.invoke(Clojure.read(namespace));
        IFn var = Clojure.var(namespace, name);
        @SuppressWarnings("unchecked")
        LTS<Spec> lts = (LTS<Spec>) ((Var) var).get();
        return new ModelChecker(lts).checkModel();
    }
}