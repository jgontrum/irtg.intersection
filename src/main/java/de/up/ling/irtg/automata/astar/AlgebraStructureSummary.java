/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package de.up.ling.irtg.automata.astar;

import java.util.function.BiConsumer;
import java.util.function.Consumer;

/**
 * Provides methods to find the next Inside/Outside objects for a given Inside
 * or Outside object.
 * @author Johannes Gontrum <gontrum@uni-potsdam.de>
 * @param <OutsideSummary>
 * @param <InsideSummary>
 */
public interface AlgebraStructureSummary<InsideSummary, OutsideSummary> {
    void forEachRuleOutside(OutsideSummary outsideSummary, int symbol, int arity, int position, BiConsumer<OutsideSummary, InsideSummary[]> fn);
    void forEachRuleInside(InsideSummary insideSummary, int arity, Consumer<InsideSummary[]>  fn);    
        
    boolean isOutsideSummaryComplete(OutsideSummary outsideSummary);
    boolean isInsideSummaryTerminal(InsideSummary insideSummary);

}
