package discourje.core.validation;

import discourje.core.lts.LTS;
import discourje.core.validation.formulas.Causality;
import discourje.core.validation.formulas.CloseChannelsOnlyOnce;
import discourje.core.validation.formulas.ClosedChannelMustBeUsedInPath;
import discourje.core.validation.formulas.ClosedChannelMustBeUsedInProtocol;
import discourje.core.validation.formulas.DoNotSendAfterClose;
import discourje.core.validation.formulas.DoNotSendToSelf;
import discourje.core.validation.formulas.UsedChannelsMustBeClosed;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

public class ModelChecker {
    public static final List<CtlFormula> DEFAULT_RULES = Arrays.asList(new Causality(),
            new UsedChannelsMustBeClosed(),
            new ClosedChannelMustBeUsedInPath(),
            new ClosedChannelMustBeUsedInProtocol(),
            new CloseChannelsOnlyOnce(),
            new DoNotSendAfterClose(),
            new DoNotSendToSelf());

    private final DiscourjeModel<?> dmModel;
    private final Collection<CtlFormula> rules;

    public ModelChecker(LTS<?> lts) {
        this.dmModel = new DiscourjeModel<>(lts);
        rules = DEFAULT_RULES;
    }

    public ModelChecker(LTS<?> lts, Collection<CtlFormula> rules) {
        this.dmModel = new DiscourjeModel<>(lts);
        this.rules = rules;
    }

    public List<String> checkModel() {
        return rules.stream()
                .flatMap(r -> r.getValidationErrors(dmModel).stream())
                .collect(Collectors.toList());
    }
}
