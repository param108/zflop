package flex.tools.dependencychecker.nodes;


import java.io.IOException;
import java.util.Map;
import java.util.Set;

import flex.tools.dependencychecker.DependencyRules;


public interface DependencyNode {
	public void addChild(DependencyNode child);
	public void visit(Map/*<String, Set<String>>*/ dependencies, DependencyRules rules) throws IOException ;
	public void setParent(DependencyNode parent);
	public DependencyNode getParent();
}
