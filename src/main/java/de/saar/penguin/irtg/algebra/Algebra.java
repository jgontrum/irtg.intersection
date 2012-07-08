/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package de.saar.penguin.irtg.algebra;

import de.saar.penguin.irtg.automata.TreeAutomaton;
import de.up.ling.tree.Tree;

/**
 *
 * @author koller
 */
public interface Algebra<E> {
    public E evaluate(Tree<String> t);
    public TreeAutomaton decompose(E value);
    public E parseString(String representation) throws ParserException;
}
