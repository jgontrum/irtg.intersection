/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package de.up.ling.irtg.automata.condensed;

/**
 *
 * @author gontrum
 */
public interface EdgeEvaluator {
    public abstract double evaluate(int leftState, int rightState, double extra);
}