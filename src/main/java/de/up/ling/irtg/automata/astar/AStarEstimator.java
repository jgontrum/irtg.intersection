/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package de.up.ling.irtg.automata.astar;

import de.up.ling.irtg.automata.Rule;
import de.up.ling.irtg.automata.TreeAutomaton;
import de.up.ling.irtg.util.MutableDouble;

/**
 *
 * @author koller
 */
public class AStarEstimator<State, InsideSummary, OutsideSummary> {
    private Estimator<State, InsideSummary, OutsideSummary> estimator;
    private TreeAutomaton<? extends Object> grammar;

    public AStarEstimator(TreeAutomaton grammar, Estimator<State, InsideSummary, OutsideSummary> estimator) {
        this.estimator = estimator;
        this.grammar = grammar;
    }
    
    
    
    public double estimateOutside(int state, OutsideSummary outsideSummary) {  
        // base case for the recursion: summary is complete 
        if (estimator.isOutsideSummaryComplete(outsideSummary)) {
            // if the state is the startsymbol of the grammar, return 0
            return grammar.getFinalStates().contains(state)? 0 : Double.NEGATIVE_INFINITY;
        }
        MutableDouble score = new MutableDouble(Double.NEGATIVE_INFINITY);
        for (Rule r : rhsSymbolToRules.get(state)) {
            int position = 0;            
            for (; state == r.getChildren()[position]; position++);
            estimator.forEachRuleOutside(outsideSummary, r.getLabel(), r.getArity(), position, // forEachRuleOutside(outsideSummary, f, n, i):
                    rule -> {
                        assert rule.size() == r.getArity();
                        double currentEstimate = Math.log(r.getWeight()); // P(rule) ...

                        
                        assert rule.get(0).getClass() == outsideSummary.getClass();

                        currentEstimate += estimateOutside(r.getParent(), (OutsideSummary) rule.get(0)); // ... * estimateOutside(A,s) ...
                        for (int i = 1; i < rule.size(); i++) {
                            if (rule.get(i).getClass() != outsideSummary.getClass()) { // Calculate inside estimates only
                                currentEstimate += estimateInside(r.getChildren()[i], (SXInside) rule.get(i)); // ... * estimateInside(Bi,si) ... 
                            }
                        }
                        score.setValue((currentEstimate > score.getValue()) ? currentEstimate : score.getValue()); // maximize over weights
                    });
        }

        return score.getValue();
    }
    
    public double estimateInside(int state, InsideSummary insideSummary) {
        // if IS == 0: 
        //      if state \in terminal -> 0
        //      else -> neg infinity
        if (estimator.isInsideSummaryTerminal(insideSummary)) {
            return (terminalSymbols.contains(state) ? 0 : Double.NEGATIVE_INFINITY);
        }

        MutableDouble score = new MutableDouble(Double.NEGATIVE_INFINITY);

        // for rule state -> x y:
        //      for split in 1 -> span-1:
        //          inside(x, split)
        //          inside(y, span - split)
        //          P(rule)
        for (Rule r : grammar.getRulesTopDown(state)) {
            estimator.forEachRuleInside(insideSummary,
                    newInsideList -> {
                        assert newInsideList.size() == r.getArity();
                        double currentEstimate = Math.log(r.getWeight());

                        for (int i = 0; i < newInsideList.size(); i++) {
                            currentEstimate += estimateInside(r.getChildren()[i], newInsideList.get(i));
                        }

                        score.setValue((currentEstimate > score.getValue()) ? currentEstimate : score.getValue()); // maximize over weights 
                    });
        }

        return score.getValue();
    }
}
