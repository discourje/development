package discourje.core.validation.formulas;

public class CtlFormulas {

    //
    // Atoms
    //

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


    //
    // Temporal operators
    //





    /**
     * Label each state that is the first state in the LTS.
     */
    public static CtlFormula first() {
        return First.INSTANCE;
    }

    /**
     * Label each state that is reached by sending and receiving to the same role r1.
     */
    public static CtlFormula self(String r1) {
        return new Self(r1);
    }

    /**
     * Label each state where the last action is a send by r1 and a receive by r2.
     */
    public static CtlFormula msg(String r1, String r2) {
        return new Message(r1, r2);
    }

    /**
     * Label each state that is labelled by all the arguments.
     */
    public static CtlFormula and(CtlFormula... args) {
        return new And(args);
    }

    /**
     * Label each state that is labelled by at least one of the arguments.
     */
    public static CtlFormula or(CtlFormula... args) {
        return new Or(args);
    }

    /**
     * Label each state that is not labelled by the argument.
     */
    public static CtlFormula not(CtlFormula arg) {
        return new Not(arg);
    }

    /**
     * Label each state that is either labelled by rhs,
     * or not labelled by lhs.
     */
    public static CtlFormula implies(CtlFormula lhs, CtlFormula rhs) {
        return new Implies(lhs, rhs);
    }

    /**
     * Label each state where all directly following states are labelled by arg.
     * Such a state must exist.
     */
    public static AX AX(CtlFormula arg) {
        return new AX(arg);
    }

    /**
     * Label each state where at all paths a state appears that is labelled by arg.
     */
    public static CtlFormula AF(CtlFormula arg) {
        return new AF(arg);
    }

    /**
     * Label each state where all following states on all paths are labelled by arg.
     */
    public static CtlFormula AG(CtlFormula arg) {
        return new AG(arg);
    }

    /**
     * Label each state where all next states are labelled by lhs, until a state is labelled by rhs.
     * A state labelled by rhs must occur in each path.
     */
    public static CtlFormula AU(CtlFormula lhs, CtlFormula rhs) {
        return new AU(lhs, rhs);
    }

    /**
     * Label each state where at least one directly following state is labelled by arg.
     */
    public static CtlFormula EX(CtlFormula arg) {
        return new EX(arg);
    }

    /**
     * Label each state where for at least one path a state appears that is labelled by arg.
     */
    public static CtlFormula EF(CtlFormula arg) {
        return new EF(arg);
    }

    /**
     * Label each state where all at least one path exists where all following states are labelled by arg.
     */
    public static CtlFormula EG(CtlFormula arg) {
        return new EG(arg);
    }

    /**
     * Label each state where a path exists where all next states are labelled by lhs, until a state is labelled by rhs.
     * A state labelled by rhs must occur in this path.
     */
    public static CtlFormula EU(CtlFormula lhs, CtlFormula rhs) {
        return new EU(lhs, rhs);
    }

    /**
     * Label each state where all directly preceding states are labelled by arg.
     * Such a state must exist.
     */
    public static CtlFormula AY(CtlFormula arg) {
        return new AY(arg);
    }

    /**
     * Label each state where at all paths a previous state exists that is labelled by arg.
     */
    public static CtlFormula AP(CtlFormula arg) {
        return new AP(arg);
    }

    /**
     * Label each state where all previous states on all paths are labelled by arg.
     */
    public static CtlFormula AH(CtlFormula arg) {
        return new AH(arg);
    }

    /**
     * Label each state where all previous states are labelled by lhs, until a state that is labelled by rhs.
     * A state labelled by rhs must occur in each path.
     */
    public static CtlFormula AS(CtlFormula lhs, CtlFormula rhs) {
        return new AS(lhs, rhs);
    }

    /**
     * Label each state where at least one directly preceding state is labelled by arg.
     */
    public static CtlFormula EY(CtlFormula arg) {
        return new EY(arg);
    }

    /**
     * Label each state where for at least one path a previous state exists that is labelled by arg.
     */
    public static CtlFormula EP(CtlFormula arg) {
        return new EP(arg);
    }

    /**
     * Label each state where at least one path exists where all previous states are labelled by arg.
     */
    public static CtlFormula EH(CtlFormula arg) {
        return new EH(arg);
    }

    /**
     * Label each state where a path exists where all previous states are labelled by lhs, since a state that is labelled by rhs.
     * A state labelled by rhs must occur in this path.
     */
    public static CtlFormula ES(CtlFormula lhs, CtlFormula rhs) {
        return new ES(lhs, rhs);
    }
}
