package discourje.core.validation;

import discourje.core.lts.LTS;
import discourje.core.validation.formulas.Causality;
import discourje.core.validation.formulas.UsedChannelsMustBeClosed;
import discourje.core.validation.formulas.CloseChannelsOnlyOnce;
import discourje.core.validation.formulas.ClosedChannelMustBeUsedInProtocol;
import discourje.core.validation.formulas.DoNotSendAfterClose;
import discourje.core.validation.formulas.DoNotSendToSelf;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class ModelChecker {
    private final DiscourjeModel<?> dmModel;

    public ModelChecker(LTS<?> lts) {
        this.dmModel = new DiscourjeModel<>(lts);
    }

    public List<String> checkModel() {
        return Arrays.stream(new CtlFormula[]{
                new Causality(),
                new UsedChannelsMustBeClosed(),
                new ClosedChannelMustBeUsedInProtocol(),
                new CloseChannelsOnlyOnce(),
                new DoNotSendAfterClose(),
                new DoNotSendToSelf()
        })
                .flatMap(r -> r.getValidationErrors(dmModel).stream())
                .collect(Collectors.toList());
    }
}
