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

        List<int[]> itemsLeft = new ArrayList<>(); ///< List of arrays of 'InsideSummaries' that are left to the current Outside Summary
        SXInside[] rhs = new SXInside[arity]; ///< The actual RHS that we are building. The size does not change, but its content may do.

        // generate all possible ways to split the value of getWordsLeft() into 
        // an array of size position (that is the number of all inside summaries
        // left to the position of our outside summary
        generate(0, outsideSummary.getWordsLeft(), new int[position], false, tuple -> {
            itemsLeft.add(tuple);
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
                rhs[i] = new SXInside(leftTuple[i]);
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
                    rhs[leftTuple.length + 1 + j] = new SXInside(rightTuple[j]);
                }
                
                // Create outside summary on the lhs of this 'rule'
                int osBegin = outsideSummary.getWordsLeft() - lengthLeftFinal;
                int osEnd = outsideSummary.getWordsRight() - lengthRight;

                fn.accept(new SXOutside(osBegin, osEnd), rhs);

            });
        });

    }

    @Override
    public void forEachRuleInside(SXInside insideSummary, int arity, Consumer<SXInside[]> fn) {
        // SXInsides will be returned if and only if:
        //      * The number of returned items is equal to the given arity 
        //      * The lengths of all items for a rule sum up to the length of the given spans
        //      * No item has the length 0
        generate(0, insideSummary.getSpan(), new int[arity], true, tuple -> {
            SXInside[] ret = new SXInside[arity];
            for (int i = 0; i < tuple.length; i++) {
                ret[i] = new SXInside(tuple[i]);
            }
            fn.accept(ret);
        });

    }
    
    public static void generate(int nextPos, int remainingScore, int[] tuple, boolean completeOnly, Consumer<int[]> fn) {
        if (nextPos < tuple.length) {
            for (int i = 1; i <= remainingScore; ++i) {
                tuple[nextPos] = i;
                generate(nextPos + 1, remainingScore - i, Arrays.copyOf(tuple, tuple.length), completeOnly, fn);
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
    public boolean isInsideSummaryTerminal(SXInside insideSummary) {
        return insideSummary.getSpan() == 1;
    }

}
