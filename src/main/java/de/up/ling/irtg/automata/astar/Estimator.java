/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package de.up.ling.irtg.automata.astar;

import de.up.ling.irtg.automata.Rule;
import de.up.ling.irtg.automata.TreeAutomaton;
import java.util.Set;
import java.util.function.BiConsumer;
import javafx.util.Pair;

/**
 *
 * @author Johannes Gontrum <gontrum@uni-potsdam.de>
 * @param <State>
 * @param <OutsideSummary>
 * @param <InsideSummary>
 */
public interface Estimator<State, InsideSummary, OutsideSummary> {
    void forEachRuleOutside(OutsideSummary outsideSummary, int symbol, int arity, int position, BiConsumer<OutsideSummary, Pair<InsideSummary, Integer>> todo);
    void forEachRuleInside(InsideSummary insideSummary, BiConsumer<InsideSummary, InsideSummary>  todo);    
    
    boolean isOutsideSummaryComplete(OutsideSummary outsideSummary);
    boolean isInsideSummaryTerminal(InsideSummary insideSummary);
    
    boolean isTerminal(int state);
    Set<Rule> getRulesForRHSState(int state);
    TreeAutomaton<State> getGrammar();
    
    
    InsideSummary getSummary(int span);

}
