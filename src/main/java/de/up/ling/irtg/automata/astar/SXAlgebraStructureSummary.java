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
 * Provides methods to find the next Inside/Outside objects for a given Inside
 * or Outside object.
 * Since this class works for strings, the Inside object is simply an Integer.
 * It represents the length of a span.
 * @author Johannes Gontrum <gontrum@uni-potsdam.de>
 */
public class SXAlgebraStructureSummary implements AlgebraStructureSummary<Integer, SXOutside> {
    
    /**
     * Iterates over all possible Outside and Inside objects for the given input data.
     * The Outside object represents the LHS and the array of Inside objects the LHS,
     * where the given outsideSummary is on position 'position'.
     * 
     * SXOutside_fn -> SXInside_0, ..., outsideSummary_position, ..., SXInside_arity-1
     * Note: SXInside is represented as a simple Integer. The position of outsideSummary
     * can be 0 or 'arity-1' as well.
     * @param outsideSummary    //< The OS on the RHS
     * @param symbol            //< Not used
     * @param arity             //< Arity of the rule
     * @param position          //< The position of outsideSummary on the RHS
     * @param fn            
     */
    @Override
    public void forEachRuleOutside(SXOutside outsideSummary, int symbol, int arity, int position, BiConsumer<SXOutside, Integer[]> fn) {

        List<int[]> itemsLeft = new ArrayList<>();  ///< List of arrays of 'InsideSummaries' that are left to the current Outside Summary
        Integer[] rhs = new Integer[arity];         ///< The actual RHS that we are building. The size does not change, but its content may do.

        // generate all possible ways to split the value of getWordsLeft() into 
        // an array of size position (that is the number of all inside summaries
        // left to the position of our outside summary
        generate(0, outsideSummary.getWordsLeft(), new int[position], false, tuple -> {
            itemsLeft.add(Arrays.copyOf(tuple, tuple.length));
        });

        // now the items on the right...
        itemsLeft.forEach(leftTuple -> {
            int maxRightArity = arity - position - 1; // -1 = subtract the current outsideSummary

            // Sum up the value of the left array. 
            // We need this value to create the outside summary on the lhs.
            int lengthLeft = 0;
            for (int item : leftTuple) {
                lengthLeft += item;
            }
            final int lengthLeftFinal = lengthLeft;  // to be used with lambda expression

            
            // Now copy inside summaries from the left part to the return array
            for (int i = 0; i < leftTuple.length; ++i) {
                rhs[i] = leftTuple[i];
            }
            
            // Set the index = null, where the current outside summary is.
            rhs[leftTuple.length] = null;
                        
            // Generate all possible ways to split the value of getWordsRight() into arrays
            // of length arity - position - 1.
            // These are all the inside summaries right to the current outside summary.
            generate(0, outsideSummary.getWordsRight(), new int[maxRightArity], false, rightTuple -> {

                // Sum up the value of the right array. 
                // We need this value to create the outside summary on the lhs.
                int lengthRight = 0;
                for (int item : rightTuple) {
                    lengthRight += item;
                }

                // Copy inside summaries from the right part to the return array
                for (int j = 0; j < rightTuple.length; ++j) {
                    rhs[leftTuple.length + 1 + j] = rightTuple[j];
                }
                
                // Create outside summary on the lhs of this 'rule'
                int osBegin = outsideSummary.getWordsLeft() - lengthLeftFinal;
                int osEnd = outsideSummary.getWordsRight() - lengthRight;

                fn.accept(new SXOutside(osBegin, osEnd), rhs);

            });
        });
    }

    /**
     * Iterates over all possible splits of insideSummary into Inside Summaries 
     * (Integers). The number of possible splits is equal to 'arity'.
     * @param insideSummary
     * @param arity
     * @param fn
     */
    @Override
    public void forEachRuleInside(Integer insideSummary, int arity, Consumer<Integer[]> fn) {
        // SXInsides will be returned if and only if:
        //      * The number of returned items is equal to the given arity 
        //      * The lengths of all items for a rule sum up to the length of the given spans
        //      * No item has the length 0
        generate(0, insideSummary, new int[arity], true, tuple -> {
            Integer[] ret = new Integer[arity];
            for (int i = 0; i < tuple.length; i++) {
                ret[i] = tuple[i];
            }
            fn.accept(ret);
        });

    }
    
    /**
     * Fills a given tuple recursively with values. 
     * The sum of the values may not be larger than the value of 'remainingScore'
     * and no value may be 0. 
     * If completeOnly is true, the values sum up to exactly 'remainingScore',
     * if false, their sum can be smaller. 
     * @param nextPos The next position in the tuple. Set this to 0 to start the recursion.
     * @param remainingScore The score, that should be split into the tuple. 
     * @param tuple An empty tuple, that the values will be written into.
     * @param completeOnly If true, the values of the tuple sum up to the value of 'remainingScore'.
     * @param fn 
     */
    private static void generate(int nextPos, int remainingScore, int[] tuple, boolean completeOnly, Consumer<int[]> fn) {
        if (nextPos < tuple.length) {
            for (int i = 1; i <= remainingScore; ++i) {
                tuple[nextPos] = i;
                generate(nextPos + 1, remainingScore - i, tuple, completeOnly, fn);
            }
        } else {
            if (completeOnly) {
                if (remainingScore == 0) {
                    fn.accept(tuple);
                }
            } else {
                fn.accept(tuple);
            }
        }
    }
    
    @Override
    public boolean isOutsideSummaryComplete(SXOutside outsideSummary) {
        return outsideSummary.getWordsLeft() == 0 && outsideSummary.getWordsRight() == 0;
    }

    @Override
    public boolean isInsideSummaryTerminal(Integer insideSummary) {
        return insideSummary == 1;
    }

}
