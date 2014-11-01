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
import javafx.util.Pair;

/**
 *
 * @author Johannes Gontrum <gontrum@uni-potsdam.de>
 */
public class SXAlgebraStructureSummary implements AlgebraStructureSummary<SXInside, SXOutside> {

    @Override
    public void forEachRuleOutside(SXOutside outsideSummary, int symbol, int arity, int position, BiConsumer<SXOutside, SXInside[]> fn) {
        
        
// Build two Lists of restult items to compare the old and the new method. TODO Remove this!
        List<Pair<SXOutside, SXInside[]>> a = new ArrayList<>();
        List<Pair<SXOutside, SXInside[]>> b = new ArrayList<>();

        
        List<int[]> iLtest = new ArrayList<>();
        SXInside[] rhsTest = new SXInside[arity];

        // iterate over length of the tuple. min. 1, that covers the whole left span
        // and max. the length of the left span with a score of 1 each.
        for (int i = 0; i < position; ++i) {
            generate(0, outsideSummary.getWordsLeft(), new int[i+1], false, tuple -> {
                System.err.println(Arrays.toString(tuple));
                iLtest.add(tuple);
            });
        }
        
        // now the items  on the right...
        iLtest.forEach(leftTuple -> {
            int maxRightArity = arity - leftTuple.length - 1; // -1 = subtract the current outsideSummary
            
            // sum up the left length
            int lengthLeft = 0;
            for (int item : leftTuple) lengthLeft += item;
            final int lengthLeftFinal = lengthLeft;  // to be used with lambda expression
            
            // copy inside summaries from the left part to the return array
            for (int i = 0; i < leftTuple.length; ++i) {
                rhsTest[i] = new SXInside(leftTuple[i]);
            }
            rhsTest[leftTuple.length] = null;

            generate(0, outsideSummary.getWordsRight(), new int[maxRightArity],false, rightTuple -> {

                // sum up the right length
                int lengthRight = 0;
                for (int item : rightTuple) lengthRight += item;

                // copy inside summaries from the right part to the return array
                for (int j = 0; j < rightTuple.length; ++j) {
                    rhsTest[leftTuple.length+1+j] = new SXInside(rightTuple[j]);
                }

                // Create outside summary on the lhs of this 'rule'
                int osBegin = outsideSummary.getWordsLeft() - lengthLeftFinal;
                int osEnd = outsideSummary.getWordsRight() - lengthRight;

                a.add(new Pair<>(new SXOutside(osBegin, osEnd), rhsTest));

            });
        });
        
                // cover the case, that position is 0 and there is nothing left of the os
        if (position == 0) {
            generate(0, outsideSummary.getWordsRight(), new int[arity - 1], false, rightTuple -> {

                // sum up the right length
                int lengthRight = 0;
                for (int item : rightTuple) {
                    lengthRight += item;
                }
                rhsTest[0] = null;
                // copy inside summaries from the right part to the return array
                for (int j = 0; j < rightTuple.length; ++j) {
                    rhsTest[1 + j] = new SXInside(rightTuple[j]);
                }

                // Create outside summary on the lhs of this 'rule'
                int osBegin = outsideSummary.getWordsLeft();
                int osEnd = outsideSummary.getWordsRight() - lengthRight;

                a.add(new Pair<>(new SXOutside(osBegin, osEnd), rhsTest));
            });
        }
        

        if (position == 0) {
                // outsideSummary is left child
            // e.g.: for forEachRule((3,2), *, 2, 0, todo), call “todo” on 
            //      (3,1) -> *((3,2), 1); 
            //      (3,0) -> *((3,2), 2); etc.
            for (int i = 1; i <= outsideSummary.getWordsRight(); ++i) {
                SXInside[] tup = new SXInside[2];
                tup[0] = null;
                tup[1] = new SXInside(i);

                b.add(new Pair<>(new SXOutside(outsideSummary.getWordsLeft(), outsideSummary.getWordsRight() - i), tup));
            }

        } else {
                  // outsideSummary is right child
            // e.g.:for  forEachRule((3, 2), *, 2, 1, todo) calls “todo" on
            //      (2, 2) -> *(1, (3,2)); 
            //      (1, 2) -> *(2, (3,2)); etc.
            for (int i = 1; i <= outsideSummary.getWordsLeft(); ++i) {
                SXInside[] tup = new SXInside[2];
                tup[0] = new SXInside(i);
                tup[1] = null;

                b.add(new Pair<>(new SXOutside(outsideSummary.getWordsLeft() - i, outsideSummary.getWordsRight()), tup));

            }
        }
    
  
        
//        System.err.println("p: " + position + " " + outsideSummary);
//        a.forEach(p -> System.err.println("a: " + p.getKey() + "  " + Arrays.toString(p.getValue())));
//        b.forEach(p -> System.err.println("b: " + p.getKey() + "  " + Arrays.toString(p.getValue())));
//        System.err.println("c:\n");
        
        
        assert a.equals(b);

        
        
        
        
        // Valid code starting here: 
        List<int[]> itemsLeft = new ArrayList<>();
        SXInside[] rhs = new SXInside[arity];

        // iterate over length of the tuple. min. 1, that covers the whole left span
        // and max. the length of the left span with a score of 1 each.
        for (int i = 0; i < position; ++i) {
            generate(0, outsideSummary.getWordsLeft(), new int[i + 1], false, tuple -> {
                itemsLeft.add(tuple);
            });
        }

        // now the items on the right...
        itemsLeft.forEach(leftTuple -> {
            int maxRightArity = arity - leftTuple.length - 1; // -1 = subtract the current outsideSummary

            // sum up the left length
            int lengthLeft = 0;
            for (int item : leftTuple) {
                lengthLeft += item;
            }
            final int lengthLeftFinal = lengthLeft;  // to be used with lambda expression

            // copy inside summaries from the left part to the return array
            for (int i = 0; i < leftTuple.length; ++i) {
                rhs[i] = new SXInside(leftTuple[i]);
            }
            rhs[leftTuple.length] = null;

            generate(0, outsideSummary.getWordsRight(), new int[maxRightArity], false, rightTuple -> {

                // sum up the right length
                int lengthRight = 0;
                for (int item : rightTuple) {
                    lengthRight += item;
                }

                // copy inside summaries from the right part to the return array
                for (int j = 0; j < rightTuple.length; ++j) {
                    rhs[leftTuple.length + 1 + j] = new SXInside(rightTuple[j]);
                }

                // Create outside summary on the lhs of this 'rule'
                int osBegin = outsideSummary.getWordsLeft() - lengthLeftFinal;
                int osEnd = outsideSummary.getWordsRight() - lengthRight;

                fn.accept(new SXOutside(osBegin, osEnd), rhs);

            });
        });

        // cover the case, that position is 0 and there is nothing left of the os
        if (position == 0) {
            generate(0, outsideSummary.getWordsRight(), new int[arity - 1], false, rightTuple -> {

                // sum up the right length
                int lengthRight = 0;
                for (int item : rightTuple) {
                    lengthRight += item;
                }
                rhs[0] = null;
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
    public void forEachRuleInside(SXInside insideSummary, int arity, Consumer<SXInside[]> fn) {
        // Build two Lists of restult items to compare the old and the new method. TODO Remove this!
        List<SXInside[]> a = new ArrayList<>();
        List<SXInside[]> b = new ArrayList<>();

        generate(0, insideSummary.getSpan(), new int[arity], true, tuple -> {
            SXInside[] ret = new SXInside[arity];
            for (int i = 0; i < tuple.length; i++) {
                ret[i] = new SXInside(tuple[i]);
            }
            a.add(ret);
        });

        for (int split = 1; split < insideSummary.getSpan(); ++split) {
            SXInside[] tup = new SXInside[2];
            tup[0]= new SXInside(split);
            tup[1] = new SXInside(insideSummary.getSpan() - split);
            b.add(tup);
        }
        
        assert a.equals(b);
        
        // New, valid code starting here:
        
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
    
    @Override
    public boolean isOutsideSummaryComplete(SXOutside outsideSummary) {
        return outsideSummary.getWordsLeft() == 0 && outsideSummary.getWordsRight() == 0;
    }

    @Override
    public boolean isInsideSummaryTerminal(SXInside insideSummary) {
        return insideSummary.getSpan() == 1;
    }

}
