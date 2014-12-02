/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.up.ling.irtg.automata.condensed;

import de.saar.basic.Pair;
import de.up.ling.irtg.Interpretation;
import de.up.ling.irtg.InterpretedTreeAutomaton;
import de.up.ling.irtg.algebra.Algebra;
import de.up.ling.irtg.algebra.ParserException;
import de.up.ling.irtg.automata.Rule;
import de.up.ling.irtg.automata.TreeAutomaton;
import de.up.ling.irtg.automata.astar.AStarEstimator;
import de.up.ling.irtg.automata.astar.AlgebraStructureSummary;
import de.up.ling.irtg.automata.astar.SXAlgebraStructureSummary;
import de.up.ling.irtg.automata.astar.AStarEdgeEvaluator;
import de.up.ling.irtg.automata.astar.SXOutside;
import de.up.ling.irtg.automata.astar.SXSummarizer;
import de.up.ling.irtg.automata.astar.Summarizer;
import de.up.ling.irtg.codec.InputCodec;
import de.up.ling.irtg.hom.Homomorphism;
import de.up.ling.irtg.signature.IdentitySignatureMapper;
import de.up.ling.irtg.signature.SignatureMapper;
import de.up.ling.irtg.util.ArrayInt2IntMap;
import de.up.ling.irtg.util.IntBinaryHeapPriorityQueue;
import de.up.ling.irtg.util.IntInt2IntMap;
import de.up.ling.irtg.util.IntPriorityQueue;
import de.up.ling.irtg.util.Util;
import it.unimi.dsi.fastutil.ints.Int2DoubleMap;
import it.unimi.dsi.fastutil.ints.Int2DoubleOpenHashMap;
import it.unimi.dsi.fastutil.ints.Int2IntMap;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.ints.IntSet;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.management.ManagementFactory;
import java.lang.management.ThreadMXBean;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 *
 * @author koller
 */
public class CondensedBestFirstIntersectionAutomaton<LeftState, RightState> extends TreeAutomaton<Pair<LeftState, RightState>> {

    private final TreeAutomaton<LeftState> left;
    private final CondensedTreeAutomaton<RightState> right;
    private final Int2IntMap stateToLeftState;
    private final Int2IntMap stateToRightState;
    private final SignatureMapper leftToRightSignatureMapper;
    private final IntInt2IntMap stateMapping;
    private final EdgeEvaluator edgeEvaluator;
    private final Int2DoubleMap viterbiScore;

    public CondensedBestFirstIntersectionAutomaton(TreeAutomaton<LeftState> left, CondensedTreeAutomaton<RightState> right, SignatureMapper sigMapper, EdgeEvaluator edgeEvaluator) {
        super(left.getSignature()); // TODO = should intersect this with the (remapped) right signature

        this.left = left;
        this.right = right;
        
        this.edgeEvaluator = edgeEvaluator;

        stateToLeftState = new ArrayInt2IntMap();
        stateToRightState = new ArrayInt2IntMap();

        this.leftToRightSignatureMapper = sigMapper;
        stateMapping = new IntInt2IntMap();
        
        viterbiScore = new Int2DoubleOpenHashMap();
    }

    @Override
    public void makeAllRulesExplicit() {
        if (!isExplicit) {
            isExplicit = true;

            getStateInterner().setTrustingMode(true);
            
            right.makeAllRulesCondensedExplicit();

            IntPriorityQueue agenda = new IntBinaryHeapPriorityQueue();

            IntSet seenStates = new IntOpenHashSet();

            // initialize agenda with nullary rules
            int[] emptyChildren = new int[0];

            for (CondensedRule rightRule : right.getCondensedRulesBottomUpFromExplicit(emptyChildren)) {
//                System.err.println("right: " + rightRule.toString(right));
                
                IntSet rightLabels = rightRule.getLabels(right);
                for (int rightLabel : rightLabels) {
                    int leftLabel = leftToRightSignatureMapper.remapBackward(rightLabel);
                    for (Rule leftRule : left.getRulesBottomUp(leftLabel, emptyChildren)) {
//                        System.err.println("left: " + leftRule.toString(left));
                        Rule rule = combineRules(leftRule, rightRule);
                        storeRule(rule);
                        viterbiScore.put(rule.getParent(), rule.getWeight());

                        double estimate = edgeEvaluator.evaluate(leftRule.getParent(), rightRule.getParent(), rule.getWeight());
                        
                        agenda.add(rule.getParent(), estimate);
                        seenStates.add(rule.getParent());
                    }
                }
            }

            // iterate until agenda is empty
            List<IntSet> remappedChildren = new ArrayList<IntSet>();

            while (!agenda.isEmpty()) {
//                System.err.println("ag: " + agenda);

                int statePairID = agenda.removeFirst();
                int rightState = stateToRightState.get(statePairID);
                double currentViterbiScore = viterbiScore.get(statePairID);

//                System.err.println("pop: " + statePairID + " = " + left.getStateForId(stateToLeftState.get(statePairID)) + ", " + right.getStateForId(stateToRightState.get(statePairID)));

                rightRuleLoop:
                for (CondensedRule rightRule : right.getCondensedRulesForRhsState(rightState)) {
                    remappedChildren.clear();

                    // iterate over all children in the right rule
                    for (int i = 0; i < rightRule.getArity(); ++i) {
                        IntSet partners = getPartners(rightRule.getChildren()[i]);

                        if (partners == null) {
                            continue rightRuleLoop;
                        } else {
                            remappedChildren.add(partners);
                        }
                    }

                    left.foreachRuleBottomUpForSets(rightRule.getLabels(right), remappedChildren, leftToRightSignatureMapper, leftRule -> {
                        Rule rule = combineRules(leftRule, rightRule);
                        storeRule(rule);
                        
                        double score = currentViterbiScore * rule.getWeight();
                        double estimate = edgeEvaluator.evaluate(leftRule.getParent(), rightRule.getParent(), score);

                        // Update viterbi score if needed (Problem with different rule? TODO)
                        if (estimate > viterbiScore.getOrDefault(rule.getParent(), Double.NEGATIVE_INFINITY)) {
                            viterbiScore.put(rule.getParent(), score);
                        }
                        
                        seenStates.add(rule.getParent());
                        agenda.relaxPriority(rule.getParent(), estimate);
                    });
                }
            }

            finalStates = null;
        }
    }

    @Override
    public IntSet getFinalStates() {
        if (finalStates == null) {
            getAllStates(); // initialize data structure for addState
            finalStates = new IntOpenHashSet();
            collectStatePairs(left.getFinalStates(), right.getFinalStates(), finalStates);
        }

        return finalStates;
    }
    
    private void collectStatePairs(IntSet leftStates, IntSet rightStates, IntSet outStates) {
        for( int l : leftStates ) {
            for( int r : rightStates ) {
                int pair = stateMapping.get(r, l);
                if( pair != 0 ) {
                    outStates.add(pair);
                }
            }
        }
    }

    private IntSet getPartners(int rightState) {
        Int2IntMap leftMap = stateMapping.get(rightState);

        if (leftMap == null) {
            return null;
        } else {
            return leftMap.keySet();
        }
    }

    private Rule combineRules(Rule leftRule, CondensedRule rightRule) {
        int[] childStates = new int[leftRule.getArity()];

        for (int i = 0; i < leftRule.getArity(); i++) {
            childStates[i] = addStatePair(leftRule.getChildren()[i], rightRule.getChildren()[i]);
        }

        int parentState = addStatePair(leftRule.getParent(), rightRule.getParent());

        return createRule(parentState, leftRule.getLabel(), childStates, leftRule.getWeight() * rightRule.getWeight());
    }

    private int addStatePair(int leftState, int rightState) {
        int ret = stateMapping.get(rightState, leftState);

        if (ret == 0) {
            ret = addState(new Pair(left.getStateForId(leftState), right.getStateForId(rightState)));

//            System.err.println("new state " + ret + ": " + getStateForId(ret));

            stateMapping.put(rightState, leftState, ret);
            stateToLeftState.put(ret, leftState);
            stateToRightState.put(ret, rightState);
        }

        return ret;
    }

    @Override
    public Iterable<Rule> getRulesBottomUp(int labelId, int[] childStates) {
        makeAllRulesExplicit();
        return getRulesBottomUpFromExplicit(labelId, childStates);
    }

    @Override
    public Iterable<Rule> getRulesTopDown(int labelId, int parentState) {
        makeAllRulesExplicit();
        return getRulesTopDownFromExplicit(labelId, parentState);
    }

    @Override
    public boolean isBottomUpDeterministic() {
        return left.isBottomUpDeterministic() && right.isBottomUpDeterministic();
    }
    
    /**
     * Function to test the efficiency of this intersection algorithm by parsing
     * each sentence in a text file with a given IRTG. Args:
     * /path/to/grammar.irtg, /path/to/sentences, interpretation,
     * /path/to/result_file, "Additional comment"
     *
     * @param args CMD arguments
     * @param showViterbiTrees
     * @param icall what intersection should be used?
     * @throws FileNotFoundException
     * @throws ParseException
     * @throws IOException
     * @throws ParserException
     * @throws AntlrIrtgBuilder.ParseException
     */
    public static void main(String[] args) throws FileNotFoundException, de.up.ling.tree.ParseException, IOException, ParserException, de.up.ling.irtg.codec.ParseException {
        if (args.length != 5) {
            System.err.println("1. IRTG\n"
                    + "2. Sentences\n"
                    + "3. Interpretation\n"
                    + "4. Output file\n"
                    + "5. Comments");
            System.exit(1);
        }

        boolean showViterbiTrees = false;
        String irtgFilename = args[0];
        String sentencesFilename = args[1];
        String interpretation = args[2];
        String outputFile = args[3];
        String comments = args[4];
        long totalChartTime = 0;
        long totalViterbiTime = 0;

        // initialize CPU-time benchmarking
        long[] timestamp = new long[10];
        ThreadMXBean benchmarkBean = ManagementFactory.getThreadMXBean();
        boolean useCPUTime = benchmarkBean.isCurrentThreadCpuTimeSupported();
        if (useCPUTime) {
            System.err.println("Using CPU time for measuring the results.");
        }

        System.err.print("Reading the IRTG...");

        updateBenchmark(timestamp, 0, useCPUTime, benchmarkBean);

        InputCodec<InterpretedTreeAutomaton> codec = InputCodec.getInputCodecByExtension(Util.getFilenameExtension(irtgFilename));
        InterpretedTreeAutomaton irtg = codec.read(new FileInputStream(new File(irtgFilename)));
        Interpretation interp = irtg.getInterpretation(interpretation);
        Homomorphism hom = interp.getHomomorphism();
        Algebra alg = irtg.getInterpretation(interpretation).getAlgebra();

        updateBenchmark(timestamp, 1, useCPUTime, benchmarkBean);

//        irtg.getAutomaton().analyze();
        System.err.println(" Done in " + ((timestamp[1] - timestamp[0]) / 1000000) + "ms");
        try {
            File oFile = new File(outputFile);
            FileWriter outstream = new FileWriter(oFile);
            BufferedWriter out = new BufferedWriter(outstream);
            out.write("Testing IntersectionAutomaton with A* condensed intersection ...\n"
                    + "IRTG-File  : " + irtgFilename + "\n"
                    + "Input-File : " + sentencesFilename + "\n"
                    + "Output-File: " + outputFile + "\n"
                    + "Comments   : " + comments + "\n"
                    + "CPU-Time   : " + useCPUTime + "\n\n");
            out.flush();

            try {
                // setting up inputstream for the sentences
                FileInputStream instream = new FileInputStream(new File(sentencesFilename));
                DataInputStream in = new DataInputStream(instream);
                BufferedReader br = new BufferedReader(new InputStreamReader(in));
                String sentence;
                int times = 0;
                int sentences = 0;

                // A* objects
                AlgebraStructureSummary<Integer, SXOutside> structureSummarizer = new SXAlgebraStructureSummary();
                AStarEstimator<String, Integer, SXOutside> astar = new AStarEstimator(structureSummarizer, irtg.getAutomaton());

                while ((sentence = br.readLine()) != null) {
                    ++sentences;
                    System.err.println("\nSentence #" + sentences);
                    System.err.println("Current sentence: " + sentence);
                    updateBenchmark(timestamp, 2, useCPUTime, benchmarkBean);

                    // intersect
                    TreeAutomaton decomp = alg.decompose(alg.parseString(sentence));
                    CondensedTreeAutomaton inv = decomp.inverseCondensedHomomorphism(hom);

                    // A*
                    Summarizer summarizer = new SXSummarizer(Arrays.asList(sentence.split(" ")));
                    EdgeEvaluator edgeEvaluator = new AStarEdgeEvaluator(astar, summarizer, decomp);

                    TreeAutomaton result
                            = new CondensedBestFirstIntersectionAutomaton<>(
                                    irtg.getAutomaton(),
                                    inv,
                                    new IdentitySignatureMapper(irtg.getAutomaton().getSignature()),
                                    edgeEvaluator
                            );
                    result.makeAllRulesExplicit();

                    updateBenchmark(timestamp, 3, useCPUTime, benchmarkBean);

                    long thisChartTime = (timestamp[3] - timestamp[2]);
                    totalChartTime += thisChartTime;
                    System.err.println("-> Chart " + (thisChartTime / 1000000) + "ms, cumulative " + totalChartTime / 1000000 + "ms");
                    out.write("Parsed \n" + sentence + "\nIn " + ((timestamp[3] - timestamp[2]) / 1000000) + "ms.\n\n");
                    out.flush();

                    if (result.getFinalStates().isEmpty()) {
                        System.err.println("**** EMPTY ****\n");
                    } else if (showViterbiTrees) {
                        System.err.println(result.viterbi());
                        updateBenchmark(timestamp, 4, useCPUTime, benchmarkBean);
                        long thisViterbiTime = timestamp[4] - timestamp[3];
                        totalViterbiTime += thisViterbiTime;

                        System.err.println("-> Viterbi " + thisViterbiTime / 1000000 + "ms, cumulative " + totalViterbiTime / 1000000 + "ms");
                    }

                    times += (timestamp[3] - timestamp[2]) / 1000000;
                }
                out.write("~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~\n Parsed " + sentences + " sentences in " + times + "ms. \n");
                out.flush();
            } catch (IOException ex) {
                System.err.println("Error while reading the Sentences-file: " + ex.getMessage());
            }
        } catch (Exception ex) {
            System.out.println("Error while writing to file:" + ex.getMessage());
            ex.printStackTrace(System.err);
        }

    }

    // Saves the current time / CPU time in the timestamp-variable
    private static void updateBenchmark(long[] timestamp, int index, boolean useCPU, ThreadMXBean bean) {
        if (useCPU) {
            timestamp[index] = bean.getCurrentThreadCpuTime();
        } else {
            timestamp[index] = System.nanoTime();
        }
    }

}