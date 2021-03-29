package discourje.core.validation;

import discourje.core.lts.LTS;
import discourje.core.validation.rules.Causality;
import discourje.core.validation.rules.CloseChannelsOnlyOnce;
import discourje.core.validation.rules.ClosedChannelMustBeUsedInPath;
import discourje.core.validation.rules.ClosedChannelMustBeUsedInProtocol;
import discourje.core.validation.rules.DoNotSendAfterClose;
import discourje.core.validation.rules.DoNotSendToSelf;
import discourje.core.validation.rules.UsedChannelsMustBeClosed;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

public class ModelChecker {
    public static final List<Rule> DEFAULT_RULES = Arrays.asList(new Causality(),
            new UsedChannelsMustBeClosed(),
            new ClosedChannelMustBeUsedInPath(),
            new ClosedChannelMustBeUsedInProtocol(),
            new CloseChannelsOnlyOnce(),
            new DoNotSendAfterClose(),
            new DoNotSendToSelf());

    private final DiscourjeModel<?> dmModel;
    private final Collection<Rule> rules;

    public ModelChecker(LTS<?> lts) {
        this.dmModel = new DiscourjeModel<>(lts);
        rules = DEFAULT_RULES;
    }

    public ModelChecker(LTS<?> lts, Collection<Rule> rules) {
        this.dmModel = new DiscourjeModel<>(lts);
        this.rules = rules;
    }

    public List<String> checkModel() {
        return rules.stream()
                .flatMap(r -> r.getValidationErrors(dmModel).stream())
                .collect(Collectors.toList());
    }
}
