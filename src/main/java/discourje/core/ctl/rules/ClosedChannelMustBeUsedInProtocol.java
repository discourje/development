package discourje.core.ctl.rules;

import discourje.core.ctl.Rule;
import discourje.core.ctl.Formula;
import static discourje.core.ctl.Formulas.EF;
import static discourje.core.ctl.Formulas.close;
import static discourje.core.ctl.Formulas.implies;
import static discourje.core.ctl.Formulas.send;

public class ClosedChannelMustBeUsedInProtocol extends Rule {

    @Override
    public String createErrorDescription(String r1, String r2) {
        return String.format("A channel from %s to %s is closed, but this channel is never used in the protocol.", r1, r2);
    }

    @Override
    public Formula createCtlFormula(String r1, String r2) {
        return implies(EF(close(r1, r2)), EF(send(r1, r2)));
    }
}
