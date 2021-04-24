package discourje.core.validation.formulas;

public class CtlFormulas {

    //
    // Atoms
    //

    public static CtlFormula init() {
        return Init.INSTANCE;
    }

    public static CtlFormula fin() {
        return Fin.INSTANCE;
    }

    public static CtlFormula send(String sender, String receiver) {
        return new Send(sender, receiver);
    }

    public static CtlFormula receive(String sender, String receiver) {
        return new Receive(sender, receiver);
    }

    public static CtlFormula close(String sender, String receive) {
        return new Close(sender, receive);
    }

    //
    // Propositional operators
    //

    public static CtlFormula and(CtlFormula... args) {
        return new And(args);
    }

    public static CtlFormula or(CtlFormula... args) {
        return new Or(args);
    }

    public static CtlFormula not(CtlFormula arg) {
        return new Not(arg);
    }

    public static CtlFormula implies(CtlFormula arg1, CtlFormula arg2) {
        return new Implies(arg1, arg2);
    }

    //
    // Temporal operators - future
    //

    public static CtlFormula AX(CtlFormula arg) {
        return new AX(arg);
    }

    public static CtlFormula AF(CtlFormula arg) {
        return new AF(arg);
    }

    public static CtlFormula AG(CtlFormula arg) {
        return new AG(arg);
    }

    public static CtlFormula AU(CtlFormula arg1, CtlFormula arg2) {
        return new AU(arg1, arg2);
    }

    public static CtlFormula EX(CtlFormula arg) {
        return new EX(arg);
    }

    public static CtlFormula EF(CtlFormula arg) {
        return new EF(arg);
    }

    public static CtlFormula EG(CtlFormula arg) {
        return new EG(arg);
    }

    public static CtlFormula EU(CtlFormula arg1, CtlFormula arg2) {
        return new EU(arg1, arg2);
    }

    //
    // Temporal operators - past
    //

    /**
     * "In all paths, yesterday"
     */
    public static CtlFormula AY(CtlFormula arg) {
        return new AY(arg);
    }

    /**
     * "In all paths, sometime in the past"
     */
    public static CtlFormula AP(CtlFormula arg) {
        return new AP(arg);
    }

    /**
     * "In all paths, always in the past
     */
    public static CtlFormula AH(CtlFormula arg) {
        return new AH(arg);
    }

    /**
     * "In all paths, since"
     */
    public static CtlFormula AS(CtlFormula lhs, CtlFormula rhs) {
        return new AS(lhs, rhs);
    }

    /**
     * "For some path, yesterday"
     */
    public static CtlFormula EY(CtlFormula arg) {
        return new EY(arg);
    }

    /**
     * "For some path, sometime in the past"
     */
    public static CtlFormula EP(CtlFormula arg) {
        return new EP(arg);
    }

    /**
     * "For some path, always in the past"
     */
    public static CtlFormula EH(CtlFormula arg) {
        return new EH(arg);
    }

    /**
     * "For some path, since"
     */
    public static CtlFormula ES(CtlFormula lhs, CtlFormula rhs) {
        return new ES(lhs, rhs);
    }
}
