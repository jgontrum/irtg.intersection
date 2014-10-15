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
 * @param <OutsideSummary>
 * @param <InsideSummary>
 */
public interface AlgebraStructureSummary<InsideSummary extends Inside, OutsideSummary extends Outside> {
    void forEachRuleOutside(OutsideSummary outsideSummary, int symbol, int arity, int position, BiConsumer<OutsideSummary, Pair<InsideSummary, Integer>> todo);
    void forEachRuleInside(InsideSummary insideSummary, BiConsumer<InsideSummary, InsideSummary>  todo);    
        
    boolean isOutsideSummaryComplete(OutsideSummary outsideSummary);
    boolean isInsideSummaryTerminal(InsideSummary insideSummary);

}
