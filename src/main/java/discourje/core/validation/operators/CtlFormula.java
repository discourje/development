package discourje.core.validation.operators;

public class CtlFormula implements CtlOperator {
    private final String name;
    private final String description;
    private final CtlOperator formula;

    public CtlFormula(String name, String description, CtlOperator formula) {
        this.name = name;
        this.description = description;
        this.formula = formula;
    }
}
