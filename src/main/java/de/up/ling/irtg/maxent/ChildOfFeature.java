package de.up.ling.irtg.maxent;

import de.up.ling.irtg.automata.Rule;

/**
 *
 * @author Danilo Baumgarten
 */
public class ChildOfFeature extends FeatureFunction<String> {
    private String parentLabel;
    private String childLabel;

    public ChildOfFeature(String parentLabel, String childLabel) {
        this.parentLabel = parentLabel;
        this.childLabel = childLabel;
    }

    public String getParentLabel() {
        return parentLabel;
    }

    public String getChildLabel() {
        return childLabel;
    }

    @Override
    public double evaluate(Rule rule){
        String pLabel = this.getLabelFor(rule.getParent());
        if (pLabel.startsWith(parentLabel)) {
            for (Object child : rule.getChildren()) {
                String cLabel = this.getLabelFor(child);
                if (cLabel.startsWith(childLabel)) {
                    return 1.0;
                }
            }
        }
        return 0.0;
    }
}
