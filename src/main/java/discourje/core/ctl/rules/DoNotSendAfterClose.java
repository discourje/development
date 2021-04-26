package discourje.core.ctl.rules;

import discourje.core.ctl.Rule;
import discourje.core.ctl.Formula;
import static discourje.core.ctl.Formulas.AG;
import static discourje.core.ctl.Formulas.close;
import static discourje.core.ctl.Formulas.implies;
import static discourje.core.ctl.Formulas.send;
import static discourje.core.ctl.Formulas.not;

public class DoNotSendAfterClose extends Rule {

    @Override
    public String createErrorDescription(String r1, String r2) {
        return String.format("A message is sent from %s to %s, after the channel between the two is closed.", r1, r2);
    }

    @Override
    public Formula createCtlFormula(String r1, String r2) {
        return AG(implies(close(r1, r2), AG(not(send(r1, r2)))));
    }
}
