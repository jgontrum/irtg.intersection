/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package de.up.ling.irtg.automata.astar;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

/**
 *
 * @author Johannes Gontrum <gontrum@uni-potsdam.de>
 */
public class SXAlgebraStructureSummary implements AlgebraStructureSummary<SXInside, SXOutside> {

    @Override
    public void forEachRuleOutside(SXOutside outsideSummary, int symbol, int arity, int position, BiConsumer<SXOutside, SXInside[]> fn) {
        List<int[]> itemsLeft = new ArrayList<>();
        SXInside[] rhs = new SXInside[arity];

        // iterate over length of the tuple. min. 1, that covers the whole left span
        // and max. the length of the left span with a score of 1 each.
        for (int i = 0; i < position; ++i) {
            generate(0, outsideSummary.getWordsLeft(), new int[i+1], tuple -> {
                System.err.println(Arrays.toString(tuple));
                itemsLeft.add(tuple);
            });
        }
        
        // now the items  on the right...
        itemsLeft.forEach(leftTuple -> {
            int maxRightArity = arity - leftTuple.length - 1; // -1 = subtract the current outsideSummary
            
            // sum up the left length
            int lengthLeft = 0;
            for (int item : leftTuple) lengthLeft += item;
            final int lengthLeftFinal = lengthLeft;  // to be used with lambda expression
            
            // copy inside summaries from the left part to the return array
            for (int i = 0; i < leftTuple.length; ++i) {
                rhs[i] = new SXInside(leftTuple[i]);
            }
            rhs[leftTuple.length] = null;

            generate(0, outsideSummary.getWordsRight(), new int[maxRightArity], rightTuple -> {

                // sum up the right length
                int lengthRight = 0;
                for (int item : rightTuple) lengthRight += item;

                // copy inside summaries from the right part to the return array
                for (int j = 0; j < rightTuple.length; ++j) {
                    rhs[leftTuple.length+1+j] = new SXInside(rightTuple[j]);
                }

                // Create outside summary on the lhs of this 'rule'
                int osBegin = outsideSummary.getWordsLeft() - lengthLeftFinal;
                int osEnd = outsideSummary.getWordsRight() - lengthRight;

                fn.accept(new SXOutside(osBegin, osEnd), rhs);

            });
        });
        
        // cover the case, that position is 0 and there is nothing left of the os
        if (itemsLeft.isEmpty()) { 
            generate(0, outsideSummary.getWordsRight(), new int[arity - 1], rightTuple -> {

                // sum up the right length
                int lengthRight = 0;
                for (int item : rightTuple) {
                    lengthRight += item;
                }

                // copy inside summaries from the right part to the return array
                for (int j = 0; j < rightTuple.length; ++j) {
                    rhs[1 + j] = new SXInside(rightTuple[j]);
                }

                // Create outside summary on the lhs of this 'rule'
                int osBegin = outsideSummary.getWordsLeft();
                int osEnd = outsideSummary.getWordsRight() - lengthRight;

                fn.accept(new SXOutside(osBegin, osEnd), rhs);
            });
        }
        
//
//        
//        // outsideSummary is left child
//        // e.g.: for forEachRule((3,2), *, 2, 0, todo), call “todo” on 
//        //      (3,1) -> *((3,2), 1); 
//        //      (3,0) -> *((3,2), 2); etc.
//        for (int i = 1; i <= outsideSummary.getWordsRight(); ++i) {
//            todo.accept(
//                    // Outside Summary for the parent state, assuming the given outside summary is the LEFT child
//                    new SXOutside(outsideSummary.getWordsLeft(), outsideSummary.getWordsRight() - i),
//                    // Inside Summary for the state at the second position (= 1), because we assume the given outside summary is the LEFT child
//                    new Pair<>(new SXInside(i), 1) 
//            );
//        }
//
//        // outsideSummary is right child
//        // e.g.:for  forEachRule((3, 2), *, 2, 1, todo) calls “todo" on
//        //      (2, 2) -> *(1, (3,2)); 
//        //      (1, 2) -> *(2, (3,2)); etc.
//        for (int i = 1; i <= outsideSummary.getWordsLeft(); ++i) {
//            todo.accept(
//                    // Outside Summary for the parent state, assuming the given outside summary is the RIGHT child
//                    new SXOutside(outsideSummary.getWordsLeft() - i, outsideSummary.getWordsRight()),
//                    // Inside Summary for the state at the first position (= 0), because we assume the given outside summary is the RIGHT child
//                    new Pair<>(new SXInside(i), 0) 
//            );        
//        }
    }
    
    
    public static void generate(int nextPos, int remainingScore, int[] tuple, Consumer<int[]> fn) {
        if (nextPos < tuple.length) {
            for (int i = 1; i <= remainingScore; ++i) {
                tuple[nextPos] = i;
                generate(nextPos + 1, remainingScore - i, Arrays.copyOf(tuple, tuple.length), fn);
            }
        } else {
            fn.accept(tuple);
        }
    }
    
    private SXInside[] intTupleToInsideTuple(int[] tuple) {
        SXInside[] ret  = new SXInside[tuple.length];
        for (int i = 0; i < tuple.length; i++) {
            ret[i] = new SXInside(tuple[i]);
        }
        return ret;
    }

    @Override
    public void forEachRuleInside(SXInside insideSummary, int arity, BiConsumer<SXInside, SXInside> todo) {
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
