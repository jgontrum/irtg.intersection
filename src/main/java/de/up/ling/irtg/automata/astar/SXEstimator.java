/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package de.up.ling.irtg.automata.astar;

import java.util.function.BiConsumer;
import javafx.util.Pair;

/**
 *
 * @author Johannes Gontrum <gontrum@uni-potsdam.de>
 */
public class SXEstimator implements Estimator<SXInside, SXOutside> {

    @Override
    public void forEachRuleOutside(SXOutside outsideSummary, int symbol, int arity, int position, BiConsumer<SXOutside, Pair<SXInside, Integer>> todo) {
        // outsideSummary is left child
        // e.g.: for forEachRule((3,2), *, 2, 0, todo), call “todo” on 
        //      (3,1) -> *((3,2), 1); 
        //      (3,0) -> *((3,2), 2); etc.
        for (int i = 1; i <= outsideSummary.getWordsRight(); ++i) {
            todo.accept(
                    // Outside Summary for the parent state, assuming the given outside summary is the LEFT child
                    new SXOutside(outsideSummary.getWordsLeft(), outsideSummary.getWordsRight() - i),
                    // Inside Summary for the state at the second position (= 1), because we assume the given outside summary is the LEFT child
                    new Pair<>(new SXInside(i), 1) 
            );
        }

        // outsideSummary is right child
        // e.g.:for  forEachRule((3, 2), *, 2, 1, todo) calls “todo" on
        //      (2, 2) -> *(1, (3,2)); 
        //      (1, 2) -> *(2, (3,2)); etc.
        for (int i = 1; i <= outsideSummary.getWordsLeft(); ++i) {
            todo.accept(
                    // Outside Summary for the parent state, assuming the given outside summary is the RIGHT child
                    new SXOutside(outsideSummary.getWordsLeft() - i, outsideSummary.getWordsRight()),
                    // Inside Summary for the state at the first position (= 0), because we assume the given outside summary is the RIGHT child
                    new Pair<>(new SXInside(i), 0) 
            );        
        }
    }

    @Override
    public void forEachRuleInside(SXInside insideSummary, BiConsumer<SXInside, SXInside> todo) {
//        System.err.println("forEachRuleInside: running for " + insideSummary);
        for (int split = 1; split < insideSummary.getSpan(); ++split) {
//            System.err.println("forEachRuleInside: current split = " + split);
//            System.err.println("forEachRuleInside: creating " + new SXInside(split));
//            System.err.println("forEachRuleInside: creating " + new SXInside(insideSummary.getSpan() - split));

            todo.accept(new SXInside(split), new SXInside(insideSummary.getSpan() - split));

        }
    }
    
    @Override
    public boolean isOutsideSummaryComplete(SXOutside outsideSummary) {
        return outsideSummary.getWordsLeft() == 0 && outsideSummary.getWordsRight() == 0;
    }

    @Override
    public boolean isInsideSummaryTerminal(SXInside insideSummary) {
        return insideSummary.getSpan() == 1;
    }

}
