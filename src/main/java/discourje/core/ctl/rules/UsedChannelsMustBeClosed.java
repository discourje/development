package discourje.core.ctl.rules;

import discourje.core.ctl.Rule;
import discourje.core.ctl.Formula;
import static discourje.core.ctl.Formulas.AF;
import static discourje.core.ctl.Formulas.AG;
import static discourje.core.ctl.Formulas.close;
import static discourje.core.ctl.Formulas.implies;
import static discourje.core.ctl.Formulas.send;

public class UsedChannelsMustBeClosed extends Rule {

    @Override
    public String createErrorDescription(String r1, String r2) {
        return String.format("A message is sent from %s to %s, but the channel is not closed afterwards.", r1, r2);
    }

    @Override
    public Formula createCtlFormula(String r1, String r2) {
        return AG(implies(send(r1, r2), AF(close(r1, r2))));
    }
}
