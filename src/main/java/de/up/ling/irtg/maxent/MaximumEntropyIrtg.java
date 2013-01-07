package de.up.ling.irtg.maxent;

import cc.mallet.optimize.LimitedMemoryBFGS;
import cc.mallet.optimize.Optimizable;
import de.up.ling.irtg.corpus.AnnotatedCorpus;
import de.up.ling.irtg.InterpretedTreeAutomaton;
import de.up.ling.irtg.IrtgParser;
import de.up.ling.irtg.ParseException;
import de.up.ling.irtg.automata.TreeAutomaton;
import de.up.ling.irtg.automata.Rule;
import de.up.ling.shell.CallableFromShell;
import de.up.ling.tree.Tree;
import de.saar.basic.StringTools;
import de.up.ling.irtg.Interpretation;
import de.up.ling.irtg.algebra.ParserException;
import de.up.ling.irtg.algebra.StringAlgebra;
import java.io.*;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author koller
 * @author Danilo Baumgarten
 */
public class MaximumEntropyIrtg extends InterpretedTreeAutomaton {
    public static final boolean TRAINING = false;

    /**
     * For testing only
     */
    public static void main(String[] args) throws ParseException, IOException, ParserException {
        String prefix = (args.length > 0) ? args[0] : "examples/ptb-test";
        MaximumEntropyIrtg.log.log(Level.INFO, "Starting {0} of MaximumEntropyIrtg...", (TRAINING ? "training" : "evaluation"));
        MaximumEntropyIrtg.log.info("Reading grammar...");
//        MaximumEntropyIrtg i = (MaximumEntropyIrtg) IrtgParser.parse(new FileReader("examples/maxent-test.irtg"));
        MaximumEntropyIrtg i = (MaximumEntropyIrtg) IrtgParser.parse(new FileReader(prefix + "-grammar.irtg"));
        MaximumEntropyIrtg.log.info("Reading corpus...");
        String TRAIN_STR = "i\n"
                + "john watches the woman with the telescope\n"
//                + "r1(r7,r4(r8,r2(r9,r3(r10,r6(r12,r2(r9,r11))))))\n";
                + "r1(r7,r5(r4(r8,r2(r9,r10)),r6(r12,r2(r9,r11))))\n";
//        AnnotatedCorpus anCo = i.readAnnotatedCorpus(new StringReader(TRAIN_STR));
        AnnotatedCorpus anCo = i.readAnnotatedCorpus(new FileReader(prefix + "-corpus.txt"));
        if (TRAINING) {
            i.train(anCo);
            i.writeWeights(new FileWriter(prefix + "-weights.props"));
        } else {
            AnnotatedCorpus.Instance instance = anCo.getInstances().get(0);
            String sentence = StringTools.join((List<String>) instance.inputObjects.get("i"), " ");
            MaximumEntropyIrtg.log.info("Reading feature weights...");
            i.readWeights(new FileReader(prefix + "-weights.props"));
            i.precomputeFeatures();
            Map<String, Reader> test = new HashMap<String, Reader>();
            test.put("i", new StringReader(sentence));
            TreeAutomaton chart = i.parseInput(instance.inputObjects);
            System.err.println(chart.viterbi().toString());
        }
    }

    private List<String> featureNames;                    // list of names of feature functions
    private FeatureFunction[] features;                   // list of feature functions
    private double[] weights;                             // weights for feature functions
    private String strAlgebraInterp = "";
    private List<TreeAutomaton> cachedCharts;
    private static final double INITIAL_WEIGHT = 0.01; // initial value for a feature's weight 
    private Map<String, double[]> f;
    private static final String NL = System.getProperty("line.separator");
    private static final Logger log = Logger.getLogger( MaximumEntropyIrtg.class.getName() );


    /**
     * Constructor
     *
     * @param automaton the TreeAutomaton build by grammar rules
     * @param featureMap the map contains feature functions accessed by their
     * names. These functions are used to calculate probabilities for the RTG
     */
    public MaximumEntropyIrtg(TreeAutomaton<String> automaton, final Map<String, FeatureFunction> featureMap) {
        super(automaton);

        log.setLevel(Level.ALL);

        // instantiate member variables
        featureNames = new ArrayList<String>();
        cachedCharts = new ArrayList<TreeAutomaton>();
        // store the features
        setFeatures(featureMap);
    }

    /**
     * Sets the feature functions
     *
     * @param featureMap the mapping of names to feature functions
     */
    public final void setFeatures(final Map<String, FeatureFunction> featureMap) {
        if (featureMap == null) {
            features = null;
            weights = null;
            return;
        }
        // the names of the feature functions are the keys of featureMap
        featureNames.addAll(featureMap.keySet());
        // the size of the names list is used to initialize the arrays for the feature functions
        features = new FeatureFunction[this.featureNames.size()];
        // and their corresponding weights
        weights = new double[featureNames.size()];

        for (int i = 0; i < featureNames.size(); i++) {
            // fill the array for the feature functions using the parameter featureMap
            features[i] = featureMap.get(featureNames.get(i));
            // and the weights array using a default value
            weights[i] = INITIAL_WEIGHT;
        }
    }

    /**
     * Precompute f_i(r) for every known rule
     */
    private void precomputeFeatures() {
        f = new HashMap<String, double[]>();
        if (features == null) {
            throw new NullPointerException("No features present to precompute.");
        }
        MaximumEntropyIrtg.log.info("Compute f_i for every rule...");
        Set<Rule<String>> ruleSet = (Set<Rule<String>>) automaton.getRuleSet();
        int numOfFeatures = getNumFeatures();
        for (Rule r : ruleSet) {
            double[] fi = new double[numOfFeatures];
            for (int i = 0; i < numOfFeatures; i++) {
                FeatureFunction ff = features[i];
                fi[i] = ff.evaluate(r);
            }
            f.put(r.getLabel(), fi);
        }
        MaximumEntropyIrtg.log.info("Done.");
    }

    /**
     * Returns the list of the feature function names
     *
     * @return List<String>() containing the names of all feature functions
     */
    public List<String> getFeatureNames() {
        return featureNames;
    }

    /**
     * Returns the array of the feature function weights
     *
     * @return double[] containing the weights of all feature functions
     */
    public double[] getFeatureWeights() {
        if (featureNames.isEmpty()) {
            throw new NullPointerException("No features functions set yet.");
        }
        return weights;
    }

    /**
     * Returns the feature function referenced by name
     *
     * @param name the name of the feature function
     * @return the feature function with the name <tt>name</tt> if no
     * corresponding function is found
     * @throws ArrayIndexOutOfBoundsException if no feature function for
     * <tt>name</tt> can be found
     */
    public FeatureFunction getFeatureFunction(String name) throws ArrayIndexOutOfBoundsException {
        int index = featureNames.indexOf(name);
        return getFeatureFunction(index);
    }

    /**
     * Returns the feature function referenced by index
     *
     * @param index the index of the feature function
     * @return the feature function with the <tt>index</tt>
     * @throws ArrayIndexOutOfBoundsException if <tt>index</tt> is negative or
     * not less the number of feature functions
     */
    private FeatureFunction getFeatureFunction(int index) throws ArrayIndexOutOfBoundsException {
        if (featureNames.isEmpty()) {
            throw new NullPointerException("No features functions set yet.");
        }
        return features[index];
    }

    private int getNumFeatures() {
        return featureNames.size();
    }

    private String getInterpName() {
        if (strAlgebraInterp.isEmpty()) {
            Set<Entry<String, Interpretation>> entrySet = this.getInterpretations().entrySet();
            for (Entry<String, Interpretation> i : entrySet) {
                if (i.getValue().getAlgebra() instanceof StringAlgebra) {
                    strAlgebraInterp = i.getKey();
                    break;
                }
            }
        }
        if (strAlgebraInterp.isEmpty()) {
            throw new NullPointerException("No interpretation for StringAlgebra found...");
        }
        return strAlgebraInterp;
    }

    private TreeAutomaton parseInput(Map<String, Object> inputs) {
        String interpName = getInterpName();
        Interpretation interp = interpretations.get(interpName);
        List<String> input = (List<String>) inputs.get(interpName);
        
        String s = StringTools.join(input, " ");
        MaximumEntropyIrtg.log.log(Level.INFO, "Compute chart for \"{0}\" ...", s);
        
        Object[] bottomRules = new Object[input.size()];
        int i = 0;
        for (String terminal : input) {
            Set<String> ruleNames = interp.getHomomorphism().getTerminalRuleNames(terminal);
            bottomRules[i++] = ruleNames;
        }
        TreeAutomaton ret = de.up.ling.irtg.automata.ChartAutomaton.create(bottomRules, automaton);
        
        ret = ret.reduceBottomUp();
        setWeightsOnChart(ret);
        
        MaximumEntropyIrtg.log.info("Done.");
//        System.err.println(ret.toString());
        return ret;
    }

    private void setWeightsOnChart(TreeAutomaton chart) {
        Set<Rule> ruleSet = (Set<Rule>) chart.getRuleSet();
        int numFeatures = getNumFeatures();
        if (features != null) {
            for (Rule rule : ruleSet) {
                double weight = 0.0;
                double[] fi = f.get(rule.getLabel());

                for (int i = 0; i < numFeatures; i++) {
                    weight += fi[i] * weights[i];
                }

                rule.setWeight(Math.exp(weight));
            }
        }
    }
    
    /**
     * Trains the weights for the rules according to the training data
     *
     * @param corpus the training data containing sentences and their parse tree
     */
    public void train(AnnotatedCorpus corpus) {
        if (featureNames.isEmpty()) {
            throw new NullPointerException("No features functions set yet.");
        }
        precomputeFeatures();
        LimitedMemoryBFGS bfgs = new LimitedMemoryBFGS(new MaxEntIrtgOptimizable(corpus));
        try {
            bfgs.optimize();
        } catch (cc.mallet.optimize.OptimizationException e) {
            System.err.println(e);
        }
    }

    /**
     * Reads the feature function weights from a reader, e.g., string or file
     * The data must be formatted as Java properties
     * 
     * @param reader the reader to read the data from
     * @throws IOException if the reader cannot read its stream properly
     */
    @CallableFromShell(name = "weights")
    public void readWeights(Reader reader) throws IOException {
        Properties props = new Properties();
        props.load(reader);

        for (Map.Entry<Object, Object> entry : props.entrySet()) {
            String key = String.valueOf(entry.getKey());
            double weight = Double.valueOf(String.valueOf(entry.getValue()));
            int index = getFeatureNames().indexOf(key);

            if (index >= 0) { // no need to check the upper bound, because both collections have the same size
                getFeatureWeights()[index] = weight;
            }

        }

    }

    /**
     * Writes the feature function weights to a writer, e.g., string or file
     * The data will be formatted as Java properties
     * 
     * @param writer the writer to store the data into
     * @throws IOException if the writer cannot store the data properly
     */
    public void writeWeights(Writer writer) throws IOException {
        Properties props = new Properties();

        for (int i = 0; i < weights.length; i++) {
            if (weights[i] != INITIAL_WEIGHT) {
                props.put(featureNames.get(i), String.valueOf(weights[i]));
            }
        }

        props.store(writer, null);
    }

    /**
     * Returns a string representing the object and its elements
     * 
     * @return String the string representing the object
     */
    @Override
    public String toString() {
        StringWriter ret = new StringWriter();
        ret.append(super.toString());

        for (int i = 0; i < featureNames.size(); i++) {
            ret.append("feature ");
            ret.append(featureNames.get(i));
            ret.append(": ");
            ret.append(features[i].toString());
            ret.append(NL);
        }

        return ret.toString();
    }

    public void checkForLoops() {
        List<Rule<String>> rulesWithOneChild = new ArrayList<Rule<String>>();
        Set<Rule<String>> rulesSet = getAutomaton().getRuleSet();
        for (Rule<String> r : rulesSet) {
            if (r.getArity() == 1) {
                rulesWithOneChild.add(r);
            }
        }
        List<Rule<String>> loopRules = new ArrayList<Rule<String>>();
        boolean hasLoops = false;
        for (Rule<String> r : rulesWithOneChild) {
            if (hasLoop(r, r, rulesWithOneChild, 0)) {
                loopRules.add(r);
                if (!hasLoops) {
                    log.warning("Loop found.");
                    hasLoops = true;
                }
            }
        }
        if (hasLoops) {
            StringBuilder out = new StringBuilder();
            for (Rule<String> r : loopRules) {
                out.append(NL);
                out.append(r.toString());
            }
            log.log(Level.WARNING, "These are the rules leading to or are part of a loop:{0}", out.toString());
        } else {
            log.info("No loops found.");
        }
    }

    private boolean hasLoop(Rule<String> start, Rule<String> current, List<Rule<String>> rules, int depth) {
        if (depth > (rules.size() + 20)) {
            return true;
        }
        for (Rule<String> r : rules) {
            Object[] children = current.getChildren();
            if (((String)children[0]).equals(r.getParent())) {
                if (hasLoop(start, r, rules, depth+1)) {
                    return true;
                }
            }
        }
        return false;
    }
    
    /**
     * Internal class for training the feature function weights. We use the mallet framework
     * for training so this class implements a mallet interface
     */
    private class MaxEntIrtgOptimizable implements Optimizable.ByGradientValue {

        private boolean cachedStale = true;
        private double cachedValue;
        private double[] cachedGradient;
        private AnnotatedCorpus trainingData;

        /**
         * Constructor
         *
         * @param corpus the annotated training data
         * @param interp the training data may contain multiple interpretations.
         * This parameter tells us which one to use
         */
        public MaxEntIrtgOptimizable(AnnotatedCorpus corpus) {
            cachedStale = true;
            trainingData = corpus;
            cachedGradient = new double[getNumFeatures()];
        }

        /**
         * Primarily this function returns the computed log-likelihood for
         * the optimization. Beyond that it computes the also needed gradient.
         */
        @Override
        public double getValue() {
            /**
             * log-likelihood:
             * L(Lambda) = sum_x,y(p~(x,y)*sum_i(lambda_i*f_i(x,y)) - sum_x(p~(x)*log(sum_y(e^(sum_i(lambda_i*f_i(x,y))))))
             * sum_x,y : sum over every instance of training data
             * p~(x,y) : 1/N
             * sum_i(lambda_i*f_i(x,y)) : log(chart.getWeights(y))
             * 
             * sum_x : sum over every instance of training data
             * p~(x) : 1/N
             * sum_y(e^(sum_i(lambda_i*f_i(x,y)))) : inside(S)
             * 
             * gradient (<f~i> - <fi>):
             * g_i = sum_x,y(p~(x,y)*f_i(x,y)) - sum_x,y(p~(x)*p_lambda(y|x)*f_i(x,y))
             * sum_x,y : in both cases sum over every instance of training data
             * f_i(x,y) : sum over all f_i(r) with Rule r used in a node of the tree
             * p_lambda(y|x)*f_i(x,y) : E(f_i|S) (Chiang, 04)
             * E(f_i|S) = sum_r(f_i(r)*E(r))
             * sum_r : sum over all rules of the parse chart
             * E(r) = outside(A)*p(r)*inside(B)*inside(C) / inside(S) | for r(A -> B C)
             * p(r) : r.getWeight()
             */
            if (cachedStale) {
                // recompute
                MaximumEntropyIrtg.log.info("(Re)compute log-likelihood and gradient...");
                int n = trainingData.getInstances().size();
                double sum1 = 0.0; // sum_x,y(sum_i(lambda_i*f_i(x,y))
                double sum2 = 0.0; // sum_x(log(sum_y(e^(sum_i(lambda_i*f_i(x,y))))))
                double[] fiY = new double[cachedGradient.length]; // sum_x,y(f_i(x,y))
                double[] expectation = new double[cachedGradient.length]; // sum_x,y(E(f_i|S))

                for (int j = 0; j < n; j++) {
                    AnnotatedCorpus.Instance instance = trainingData.getInstances().get(j);
                    TreeAutomaton chart = null;
                    if (cachedCharts.size() > j) {
                        chart = cachedCharts.get(j);
                        setWeightsOnChart(chart);
                    } else {
                        chart = parseInput(instance.inputObjects);
                        cachedCharts.add(chart);
                    }
                    assert (chart != null);

                    // compute inside & outside for the states of the parse chart
                    MaximumEntropyIrtg.log.log(Level.INFO, "Compute inside & outside");
                    Map<Object, Double> inside = chart.inside();
                    Map<Object, Double> outside = chart.outside(inside);
                    double insideS = 0.0;
                    // compute inside(S) : the inside value of the starting states
                    Set finalStates = chart.getFinalStates();
                    for (Object start : finalStates) {
                        insideS += inside.get(start);
                    }
                    MaximumEntropyIrtg.log.info("Done.");

                    // compute parts of the log-likelihood
                    // L(Lambda) = sum1/n - sum2/n
                    double tmp = chart.getWeight(instance.tree);
                    System.err.println(tmp);
                    sum1 += Math.log(tmp);
                    System.err.println(sum1);
                    sum2 += Math.log(insideS);
                    System.err.println(sum2);

                    //compute parts of the gradient
                    MaximumEntropyIrtg.log.info("Compute expectations for gradient...");
                    Set<Rule> ruleSet = (Set<Rule>) chart.getRuleSet();
                    for (Rule r : ruleSet) {
                        double expect_r; // E(r)
                        Double outVal = outside.get(r.getParent());
                        if (outVal != null) {
                            double insideOutside = outVal * r.getWeight(); // outside(A)*p(r)

                            for (Object state : r.getChildren()) {
                                Double inVal = inside.get(state);
                                if (inVal != null) {
                                    insideOutside *= inVal; // (...)*inside(B)*inside(C)
                                } else {
                                    insideOutside = 0.0;
                                }
                            }

                            expect_r = insideOutside / insideS; // (...) / inside(S)
                        } else {
                            expect_r = 0.0;
                        }
                        double[] fi = f.get(r.getLabel());
                        for (int i = 0; i < fi.length; i++) {
                            expectation[i] += fi[i] * expect_r; // (...)*f_i(r)
                        }
                    }
                    MaximumEntropyIrtg.log.info("Done.");
                    
                    // compute f_i(x,y)
                    MaximumEntropyIrtg.log.info("Compute f_i for the tree of the training instance...");
                    getFiFor(instance.tree, fiY);
                    MaximumEntropyIrtg.log.info("Done.");
                }

                // L(Lambda) = sum1/n - sum2/n
                cachedValue = (sum1 - sum2) / n;

                for (int i = 0; i < cachedGradient.length; i++) {
                    // g_i = sum_x,y(f_i(x,y))/n - sum_x,y(E(f_i|S))/n
                    cachedGradient[i] = (fiY[i] - expectation[i]) / n;
                }

                cachedStale = false;
                MaximumEntropyIrtg.log.info("log-likelihood: " + cachedValue);
            }

            return cachedValue;
        }

        private void getFiFor(final Tree tree, double[] fiY) {
            double[] fi = f.get((String) tree.getLabel());
            for (int i = 0; i < fi.length; i++) {
                // add f_i for this rule
                fiY[i] += fi[i];
            }

            List<Tree> children = (List<Tree>) tree.getChildren();
            for (Tree child : children) {
                getFiFor(child, fiY);
            }
        }

        @Override
        public void getValueGradient(double[] gradient) {
            // we compute the gradient together with the value
            if (cachedStale) {
                getValue();
            }
            assert (gradient != null && gradient.length == cachedGradient.length);
            System.arraycopy(cachedGradient, 0, gradient, 0, cachedGradient.length);
        }

        @Override
        public int getNumParameters() {
            return weights.length;
        }

        @Override
        public void getParameters(double[] doubles) {
            System.arraycopy(weights, 0, doubles, 0, weights.length);
        }

        @Override
        public double getParameter(int i) throws ArrayIndexOutOfBoundsException {
            return weights[i];
        }

        @Override
        public void setParameters(double[] doubles) {
            weights = doubles;
//            System.err.print("new lambdas: ");
//            printArray(weights);
            cachedStale = true;
        }

        @Override
        public void setParameter(int i, double d) throws ArrayIndexOutOfBoundsException {
            weights[i] = d;
            cachedStale = true;
        }
    }

    /**
     * TESTING
     * Helper for readable array output
     */
    private void printArray(double[] x) {
        for (int i = 0; i < x.length; i++) {
            System.err.print(x[i] + " ");
        }
        System.err.println();
    }

}
