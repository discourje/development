package discourje.core.validation.rules;

import discourje.core.validation.Rule;
import discourje.core.validation.formulas.CtlFormula;
import static discourje.core.validation.formulas.CtlFormulas.AG;
import static discourje.core.validation.formulas.CtlFormulas.not;
import static discourje.core.validation.formulas.CtlFormulas.self;

public class DoNotSendToSelf extends Rule {

    @Override
    public String createDescription(String r1, String r2) {
        return String.format("A message is sent from %s to %s.", r1, r1);
    }

    @Override
    public CtlFormula createCtlOperator(String r1, String r2) {
        return AG(not(self(r1)));
    }
}
