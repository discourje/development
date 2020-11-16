/** This package contains code to validate {@link discourje.core.lts.LTS} objects against a set of rules.
 * The validation makes use of Computation Tree Logic (CTL) to check conformation to the rules.
 * A good explanation of this technique is given in 'Model Checking' by Madhavan Mukund (https://www.cmi.ac.in/~madhavan/papers/pdf/resonance-jul2009.pdf).
 *
 * Entrance of this package is {@link discourje.core.validation.ModelChecker}.
 * The ModelChecker checks by default against the rules given in {@link discourje.core.validation.ModelChecker#DEFAULT_RULES}.
 * This behaviour can be overruled by providing your own set of rules in {@link discourje.core.validation.ModelChecker#ModelChecker(discourje.core.lts.LTS, java.util.Collection)}.
 *
 * You can provide your own rules.
 * They can be created by implementing {@link discourje.core.validation.CtlFormula}.
 * The actual rule can be constructed using the method on {@link discourje.core.validation.operators.CtlOperators}.
 */
package discourje.core.validation;
