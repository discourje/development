package discourje.core.ctl;

import discourje.core.lts.LTS;
import discourje.core.ctl.rules.Causality;
import discourje.core.ctl.rules.CloseChannelsOnlyOnce;
import discourje.core.ctl.rules.ClosedChannelMustBeUsedInPath;
import discourje.core.ctl.rules.ClosedChannelMustBeUsedInProtocol;
import discourje.core.ctl.rules.DoNotSendAfterClose;
import discourje.core.ctl.rules.DoNotSendToSelf;
import discourje.core.ctl.rules.UsedChannelsMustBeClosed;
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

    private final Model<?> dmModel;
    private final Collection<Rule> rules;

    public ModelChecker(LTS<?> lts) {
        this.dmModel = new Model<>(lts);
        rules = DEFAULT_RULES;
    }

    public ModelChecker(LTS<?> lts, Collection<Rule> rules) {
        this.dmModel = new Model<>(lts);
        this.rules = rules;
    }

    public List<String> checkModel() {
        return rules.stream()
                .flatMap(r -> r.getValidationErrors(dmModel).stream())
                .collect(Collectors.toList());
    }

    public static boolean check(LTS<?> lts, Formula f) {
        var model = new Model<>(lts);
        f.label(model);
        var i = model.getLabelIndex(f);
        return !model.getInitialStates().stream().anyMatch(s -> !s.hasLabel(i));
    }
}
