package discourje.core.ctl.rules;

import discourje.core.ctl.Formula;
import discourje.core.ctl.Rule;
import static discourje.core.ctl.Formulas.AF;
import static discourje.core.ctl.Formulas.AG;
import static discourje.core.ctl.Formulas.sendOrHandshake;
import static discourje.core.ctl.Formulas.close;
import static discourje.core.ctl.Formulas.implies;

public class UsedChannelsMustBeClosed extends Rule {

    @Override
    public String createErrorDescription(String r1, String r2) {
        return String.format("A message is sent from %s to %s, but the channel is not closed afterwards.", r1, r2);
    }

    @Override
    public Formula createCtlFormula(String r1, String r2) {
        return AG(implies(sendOrHandshake(r1, r2), AF(close(r1, r2))));
    }
}
