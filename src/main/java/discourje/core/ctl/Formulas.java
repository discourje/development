package discourje.core.ctl;

import discourje.core.ctl.formulas.And;
import discourje.core.ctl.formulas.Implies;
import discourje.core.ctl.formulas.Not;
import discourje.core.ctl.formulas.Or;
import discourje.core.ctl.formulas.atomic.*;
import discourje.core.ctl.formulas.temporal.*;

public class Formulas {

    //
    // Atoms
    //

    public static Formula init() {
        return Init.INSTANCE;
    }

    public static Formula fin() {
        return Fin.INSTANCE;
    }

    public static Formula send(String sender, String receiver) {
        return new Send(sender, receiver);
    }

    public static Formula receive(String sender, String receiver) {
        return new Receive(sender, receiver);
    }

    public static Formula close(String sender, String receive) {
        return new Close(sender, receive);
    }

    public static Formula act(String role) {
        return new Act(role);
    }

    //
    // Propositional operators
    //

    public static Formula and(Formula... args) {
        return new And(args);
    }

    public static Formula or(Formula... args) {
        return new Or(args);
    }

    public static Formula not(Formula arg) {
        return new Not(arg);
    }

    public static Formula implies(Formula arg1, Formula arg2) {
        return new Implies(arg1, arg2);
    }

    //
    // Temporal operators - future
    //

    public static Formula AX(Formula arg) {
        return new AX(arg);
    }

    public static Formula AF(Formula arg) {
        return new AF(arg);
    }

    public static Formula AG(Formula arg) {
        return new AG(arg);
    }

    public static Formula AU(Formula arg1, Formula arg2) {
        return new AU(arg1, arg2);
    }

    public static Formula EX(Formula arg) {
        return new EX(arg);
    }

    public static Formula EF(Formula arg) {
        return new EF(arg);
    }

    public static Formula EG(Formula arg) {
        return new EG(arg);
    }

    public static Formula EU(Formula arg1, Formula arg2) {
        return new EU(arg1, arg2);
    }

    //
    // Temporal operators - past
    //

    /**
     * "In all paths, yesterday"
     */
    public static Formula AY(Formula arg) {
        return new AY(arg);
    }

    /**
     * "In all paths, sometime in the past"
     */
    public static Formula AP(Formula arg) {
        return new AP(arg);
    }

    /**
     * "In all paths, always in the past
     */
    public static Formula AH(Formula arg) {
        return new AH(arg);
    }

    /**
     * "In all paths, since"
     */
    public static Formula AS(Formula lhs, Formula rhs) {
        return new AS(lhs, rhs);
    }

    /**
     * "For some path, yesterday"
     */
    public static Formula EY(Formula arg) {
        return new EY(arg);
    }

    /**
     * "For some path, sometime in the past"
     */
    public static Formula EP(Formula arg) {
        return new EP(arg);
    }

    /**
     * "For some path, always in the past"
     */
    public static Formula EH(Formula arg) {
        return new EH(arg);
    }

    /**
     * "For some path, since"
     */
    public static Formula ES(Formula lhs, Formula rhs) {
        return new ES(lhs, rhs);
    }
}
