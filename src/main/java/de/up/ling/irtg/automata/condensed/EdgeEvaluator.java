/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package de.up.ling.irtg.automata.condensed;

/**
 * EdgeEvaluator returns an evaluation of a state in the left automaton of an 
 * intersection and a state in the right automaton. 
 * One possible application is to evaluate a state using the A*-Algorithm.
 * If an additional value is needed for the calculation, the argument 'extra' 
 * can be used.
 * 
 * @author Johannes Gontrum <gontrum@uni-potsdam.de>
 */
public interface EdgeEvaluator {
    public abstract double evaluate(int leftState, int rightState, double extra);
}