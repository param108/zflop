package flex.tools.dependencychecker.nodes;


import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import org.xml.sax.Attributes;

import flex.tools.dependencychecker.DependencyRules;


public class LibraryNode implements DependencyNode {
	private DependencyNode parent;
	private String swf;
	private Map/*<String, ScriptNode>*/ childMap;
	
	public LibraryNode(DependencyNode parent,
            Attributes attrs)
	{
		setParent(parent);
		
		if (attrs != null)
		{
			swf = attrs.getValue("path");
		}
		
		childMap = new HashMap/*<String, ScriptNode>*/();
	}
	
	public void addChild(DependencyNode child) {
		if (child instanceof ScriptNode)
		{
			ScriptNode sn = (ScriptNode) child;
			childMap.put(sn.getName(), sn);
		}
	}

	public DependencyNode getParent() {
		return parent;
	}

	public void setParent(DependencyNode parent) {
		this.parent = parent;
	}

	public void visit(Map/*<String, Set<String>>*/ dependencies, DependencyRules rules) throws IOException {
		/*for (ScriptNode child : childMap.values()) {*/ScriptNode child; for (Iterator iter = childMap.values().iterator(); iter.hasNext(); ) {  child = (ScriptNode)iter.next();
			child.visit(dependencies, rules);
		}
	}
	
	public String getSWF() {
		return swf;
	}

}
