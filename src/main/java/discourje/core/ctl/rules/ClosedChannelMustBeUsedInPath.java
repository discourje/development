package discourje.core.ctl.rules;

import discourje.core.ctl.Rule;
import discourje.core.ctl.Formula;
import static discourje.core.ctl.Formulas.AG;
import static discourje.core.ctl.Formulas.EP;
import static discourje.core.ctl.Formulas.close;
import static discourje.core.ctl.Formulas.implies;
import static discourje.core.ctl.Formulas.send;

public class ClosedChannelMustBeUsedInPath extends Rule {

    @Override
    public String createErrorDescription(String r1, String r2) {
        return String.format("A channel from %s to %s is closed, but this channel is never used in the path.", r1, r2);
    }

    @Override
    public Formula createCtlFormula(String r1, String r2) {
        return AG(implies(close(r1, r2), EP(send(r1, r2))));
    }
}
