/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package de.up.ling.irtg.semiring;

import de.saar.basic.Pair;
import de.up.ling.irtg.automata.Rule;

/**
 * Viterbi with multiplications. Stores backpointer to best rule.
 * @author koller
 */
public class ViterbiWithBackpointerSemiring implements Semiring<Pair<Double,Rule>> {
    // max
    public Pair<Double, Rule> add(Pair<Double, Rule> x, Pair<Double, Rule> y) {
        if( x.left > y.left ) {
            return x;
        } else {
            return y;
        }
    }

    // Multiply. Rule backpointer is passed on from first argument.
    public Pair<Double, Rule> multiply(Pair<Double, Rule> x, Pair<Double, Rule> y) {
        return new Pair<Double, Rule>(x.left * y.left, x.right);
    }

    public Pair<Double, Rule> zero() {
        return new Pair<Double, Rule>(Double.NEGATIVE_INFINITY, null);
    }
    
}
