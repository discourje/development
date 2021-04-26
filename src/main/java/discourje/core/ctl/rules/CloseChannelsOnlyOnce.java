package discourje.core.ctl.rules;

import discourje.core.ctl.Rule;
import discourje.core.ctl.Formula;
import static discourje.core.ctl.Formulas.AG;
import static discourje.core.ctl.Formulas.EF;
import static discourje.core.ctl.Formulas.EX;
import static discourje.core.ctl.Formulas.close;
import static discourje.core.ctl.Formulas.implies;
import static discourje.core.ctl.Formulas.not;

public class CloseChannelsOnlyOnce extends Rule {

    @Override
    public String createErrorDescription(String r1, String r2) {
        return String.format("A channel from %s to %s is closed, but this channel has already been closed before.", r1, r2);
    }

    @Override
    public Formula createCtlFormula(String r1, String r2) {
        return AG(implies(close(r1,r2), not(EX(EF(close(r1, r2))))));
    }
}
