/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package de.up.ling.irtg.corpus;

import de.up.ling.irtg.InterpretedTreeAutomaton;
import de.up.ling.irtg.algebra.ParserException;
import de.up.ling.irtg.automata.TreeAutomaton;
import de.up.ling.tree.Tree;
import de.up.ling.tree.TreeParser;
import de.saar.basic.ZipIterator;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * An (annotated or unannotated) corpus of input objects. You may attach a
 * collection of parse charts for these input objects to the chart using the
 * {@link Charts} class. See the examples to see the exact file format for
 * corpora.
 *
 * @author koller
 */
public class Corpus implements Iterable<Instance> {
    private static String CORPUS_VERSION = "1.0";
    private static Pattern WHITESPACE_PATTERN = Pattern.compile("\\s*");
    private static Pattern UNANNOTATED_CORPUS_DECLARATION_PATTERN = Pattern.compile("\\s*#\\s*IRTG unannotated corpus file, v(\\S+).*", Pattern.CASE_INSENSITIVE);
    private static Pattern ANNOTATED_CORPUS_DECLARATION_PATTERN = Pattern.compile("\\s*#\\s*IRTG annotated corpus file, v(\\S+).*", Pattern.CASE_INSENSITIVE);
    private static Pattern COMMENT_PATTERN = Pattern.compile("\\s*#.*");
    private static Pattern INTERPRETATION_DECLARATION_PATTERN = Pattern.compile("\\s*#\\s*interpretation\\s+([^: ]+)\\s*:\\s*(\\S+).*", Pattern.CASE_INSENSITIVE);
    private List<Instance> instances;
    private Charts charts;
    private boolean isAnnotated;
    private static final boolean DEBUG = false;

    public Corpus() {
        instances = new ArrayList<Instance>();
        charts = null;
        isAnnotated = false;
    }

    public boolean isAnnotated() {
        return isAnnotated;
    }

    public boolean hasCharts() {
        return charts != null;
    }

    public void attachCharts(Charts charts) {
        this.charts = charts;
    }

    /**
     * Reads charts from a file and attaches them to this corpus.
     *
     * @param filename
     * @throws IOException
     */
    public void attachCharts(String filename) throws IOException {
        attachCharts(new Charts(new FileInputStreamSupplier(new File(filename))));
    }

    public int getNumberOfInstances() {
        return instances.size();
    }

    public Iterator<Instance> iterator() {
        if (hasCharts()) {
            return new ZipIterator<Instance, TreeAutomaton, Instance>(instances.iterator(), charts.iterator()) {
                @Override
                public Instance zip(Instance left, TreeAutomaton right) {
                    return left.withChart(right);
                }
            };
        } else {
            return instances.iterator();
        }
    }

    public void addInstance(Instance instance) {
        instances.add(instance);

        if (instance.getDerivationTree() != null) {
            isAnnotated = true;
        }
    }

    public static String makeHeader(InterpretedTreeAutomaton irtg, List<String> interpretationsInOrder, boolean annotated) {
        StringBuffer buf = new StringBuffer();

        buf.append("# IRTG " + (annotated ? "" : "un") + "annotated corpus file, v" + CORPUS_VERSION + "\n");
        buf.append("# \n");

        for (String interp : interpretationsInOrder) {
            buf.append("# interpretation " + interp + ": " + irtg.getInterpretations().get(interp).getAlgebra().getClass() + "\n");
        }

        return buf.toString();
    }

    public void writeCorpus(Writer writer, InterpretedTreeAutomaton irtg, List<String> interpretationsInOrder) throws IOException {
        writer.write(makeHeader(irtg, interpretationsInOrder, isAnnotated) + "\n");

        for (Instance inst : instances) {
            for (String interp : interpretationsInOrder) {
                writer.write(inst.getInputObjects().get(interp).toString() + "\n");
            }

            if (isAnnotated) {
                writer.write(irtg.getAutomaton().getSignature().resolve(inst.getDerivationTree()) + "\n");
            }

            writer.write("\n");
        }
    }

    public static Corpus readCorpus(Reader reader, InterpretedTreeAutomaton irtg) throws IOException, CorpusReadingException {
        Corpus ret = new Corpus();
        boolean annotated = false;

        BufferedReader br = new BufferedReader(reader);
        List<String> interpretationOrder = new ArrayList<String>();
        Map<String, Object> currentInputs = new HashMap<String, Object>();
        int currentInterpretationIndex = 0;
        int lineNumber = 0;
        String line = null;

        // read and check header
        while (true) {
            line = readNextLine(br);
            lineNumber++;

            if (line == null) {
                return ret;
            }

            Matcher unannoMatcher = UNANNOTATED_CORPUS_DECLARATION_PATTERN.matcher(line);
            if (unannoMatcher.matches()) {
                annotated = false;
                if (!CORPUS_VERSION.equals(unannoMatcher.group(1))) {
                    throw new CorpusReadingException("Expecting corpus file format version " + CORPUS_VERSION + ", but file is version " + unannoMatcher.group(1));
                }
                continue;
            }

            Matcher annoMatcher = ANNOTATED_CORPUS_DECLARATION_PATTERN.matcher(line);
            if (annoMatcher.matches()) {
                annotated = true;
                if (!CORPUS_VERSION.equals(annoMatcher.group(1))) {
                    throw new CorpusReadingException("Expecting corpus file format version " + CORPUS_VERSION + ", but file is version " + annoMatcher.group(1));
                }
                continue;
            }

            Matcher interpretationMatcher = INTERPRETATION_DECLARATION_PATTERN.matcher(line);
            if (interpretationMatcher.matches()) {
                String interpretationName = interpretationMatcher.group(1);

                if (!irtg.getInterpretations().containsKey(interpretationName)) {
                    throw new CorpusReadingException("Corpus file specified interpretation '" + interpretationName + "', which is not declared in IRTG");
                }

                interpretationOrder.add(interpretationName);
                continue;
            }

            Matcher commentMatcher = COMMENT_PATTERN.matcher(line);
            if (commentMatcher.matches()) {
                continue;
            }

            // first non-comment, non-empty, non-metadata-declaring line => finished reading metadata
            break;
        }

        // check metadata
        if (interpretationOrder.size() != irtg.getInterpretations().size()) {
            throw new CorpusReadingException("Corpus file specified interpretation order incompletely: " + interpretationOrder);
        }

        ret.isAnnotated = annotated;

        // read actual corpus
        while (true) {
            if (line == null) {
                return ret;
            }

            Matcher commentMatcher = COMMENT_PATTERN.matcher(line);
            if (commentMatcher.matches()) {
                continue;
            }

            String current = interpretationOrder.get(currentInterpretationIndex++);

            try {
                Object inputObject = irtg.parseString(current, line);
                currentInputs.put(current, inputObject);
            } catch (ParserException ex) {
                throw new CorpusReadingException("An error occurred while parsing " + reader + ", line " + lineNumber + ": " + ex.getMessage());
            }

            if (currentInterpretationIndex == interpretationOrder.size()) {
                Instance inst = new Instance();
                inst.setInputObjects(currentInputs);

                if (annotated) {
                    String annoLine = readNextLine(br);
                    lineNumber++;

                    if (annoLine == null) {
                        throw new CorpusReadingException("Expected an annotation in line " + lineNumber);
                    }

                    Tree<String> derivationTree = null;
                    try {
                        derivationTree = TreeParser.parse(annoLine);
                    } catch (Exception ex) {
                        throw new CorpusReadingException("An error occurred while reading the derivation tree in line " + lineNumber + ": " + ex.getMessage());
                    }

                    inst.setDerivationTree(irtg.getAutomaton().getSignature().addAllSymbols(derivationTree));
                }

                ret.instances.add(inst);
                currentInputs = new HashMap<String, Object>();
                currentInterpretationIndex = 0;
            }

            line = readNextLine(br);
            lineNumber++;
        }
    }

    /*
     private static Corpus readCorpus(Reader reader, InterpretedTreeAutomaton irtg, boolean annotated) throws IOException, CorpusReadingException {
     Corpus ret = new Corpus();
     ret.isAnnotated = annotated;

     BufferedReader br = new BufferedReader(reader);
     List<String> interpretationOrder = new ArrayList<String>();
     Map<String, Object> currentInputs = new HashMap<String, Object>();
     int currentInterpretationIndex = 0;
     int lineNumber = 0;

     while (true) {
     String line = readNextLine(br);
     lineNumber++;

     if (line == null) {
     return ret;
     }

     if (lineNumber - 1 < irtg.getInterpretations().size()) {
     if (DEBUG) {
     System.err.println("-> interp " + line);
     }
     interpretationOrder.add(line);
     } else {
     String current = interpretationOrder.get(currentInterpretationIndex);

     try {
     if (DEBUG) {
     System.err.println("-> input " + line);
     }
     Object inputObject = irtg.parseString(current, line);
     currentInputs.put(current, inputObject);
     } catch (ParserException ex) {
     throw new CorpusReadingException("An error occurred while parsing " + reader + ", line " + lineNumber + ": " + ex.getMessage());
     }

     currentInterpretationIndex++;

     if (currentInterpretationIndex == interpretationOrder.size()) {
     Instance inst = new Instance();
     inst.setInputObjects(currentInputs);

     if (annotated) {
     String annoLine = readNextLine(br);
     lineNumber++;

     if (annoLine == null) {
     throw new CorpusReadingException("Expected an annotation in line " + lineNumber);
     }


     Tree<String> derivationTree = TreeParser.parse(annoLine);
     inst.setDerivationTree(derivationTree);
     }

     ret.instances.add(inst);

     if (DEBUG) {
     System.err.println("-> read instance: " + currentInputs);
     }

     currentInputs = new HashMap<String, Object>();
     currentInterpretationIndex = 0;
     }
     }
     }
     }

     */
    private static String readNextLine(BufferedReader br) throws IOException {
        String ret = null;

        do {
            ret = br.readLine();
        } while (ret != null && WHITESPACE_PATTERN.matcher(ret).matches());

        if (DEBUG) {
            System.err.println("**read line: " + ret);
        }

        return ret;
    }
}
