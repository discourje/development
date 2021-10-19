package discourje.core.ctl.rules;

import discourje.core.ctl.Formula;
import discourje.core.ctl.Rule;
import static discourje.core.ctl.Formulas.AG;
import static discourje.core.ctl.Formulas.sendOrHandshake;
import static discourje.core.ctl.Formulas.not;

public class DoNotSendToSelf extends Rule {

    @Override
    public String createErrorDescription(String r1, String r2) {
        return String.format("A message is sent from %s to %s.", r1, r1);
    }

    @Override
    public Formula createCtlFormula(String r1, String r2) {
        return AG(not(sendOrHandshake(r1, r1)));
    }
}
