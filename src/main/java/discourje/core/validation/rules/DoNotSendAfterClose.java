package discourje.core.validation.rules;

import discourje.core.validation.Rule;
import discourje.core.validation.formulas.CtlFormula;
import static discourje.core.validation.formulas.CtlFormulas.AG;
import static discourje.core.validation.formulas.CtlFormulas.close;
import static discourje.core.validation.formulas.CtlFormulas.implies;
import static discourje.core.validation.formulas.CtlFormulas.msg;
import static discourje.core.validation.formulas.CtlFormulas.not;

public class DoNotSendAfterClose extends Rule {

    @Override
    public String createErrorDescription(String r1, String r2) {
        return String.format("A message is sent from %s to %s, after the channel between the two is closed.", r1, r2);
    }

    @Override
    public CtlFormula createCtlFormula(String r1, String r2) {
        return AG(implies(close(r1, r2), AG(not(msg(r1, r2)))));
    }
}
