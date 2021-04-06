package discourje.core.validation.rules;

import discourje.core.validation.Rule;
import discourje.core.validation.formulas.CtlFormula;
import static discourje.core.validation.formulas.CtlFormulas.AG;
import static discourje.core.validation.formulas.CtlFormulas.EP;
import static discourje.core.validation.formulas.CtlFormulas.close;
import static discourje.core.validation.formulas.CtlFormulas.implies;
import static discourje.core.validation.formulas.CtlFormulas.send;

public class ClosedChannelMustBeUsedInPath extends Rule {

    @Override
    public String createErrorDescription(String r1, String r2) {
        return String.format("A channel from %s to %s is closed, but this channel is never used in the path.", r1, r2);
    }

    @Override
    public CtlFormula createCtlFormula(String r1, String r2) {
        return AG(implies(close(r1, r2), EP(send(r1, r2))));
    }
}
