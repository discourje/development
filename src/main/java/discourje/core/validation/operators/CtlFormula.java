package discourje.core.validation.operators;

import discourje.core.validation.DMState;
import discourje.core.validation.DiscourjeModel;

public class CtlFormula implements CtlOperator {
    private final String name;
    private final String description;
    private final CtlOperator formula;

    public CtlFormula(String name, String description, CtlOperator formula) {
        this.name = name;
        this.description = description;
        this.formula = formula;
    }

    @Override
    public void label(DiscourjeModel<?> model) {
        formula.label(model);
        model.getStates().stream()
                .filter(s -> s.hasLabel(formula))
                .forEach(s -> s.addLabel(this));
    }

    public String getDescription() {
        return description;
    }
}
