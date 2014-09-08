/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package de.up.ling.irtg.automata.astar;

import java.util.List;
import java.util.function.Consumer;

/**
 *
 * @author Johannes Gontrum <gontrum@uni-potsdam.de>
 * @param <State>
 * @param <OutsideSummary>
 * @param <InsideSummary>
 */
public interface Estimator<State, InsideSummary, OutsideSummary> {
    void forEachRuleOutside(OutsideSummary outsideSummary, int symbol, int arity, int position, Consumer<List<Object>> todo);
    void forEachRuleInside(InsideSummary insideSummary, Consumer<List<InsideSummary>> todo);    
    
    boolean isOutsideSummaryComplete(OutsideSummary outsideSummary);
    boolean isInsideSummaryTerminal(InsideSummary insideSummary);
}
