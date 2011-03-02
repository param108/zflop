package flex.tools.dependencychecker.nodes;


import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import org.xml.sax.Attributes;

import flex.tools.dependencychecker.DependencyRules;


public class LibrariesNode implements DependencyNode {
	
	private DependencyNode parent;
	
	private Map/*<String, LibraryNode>*/ childMap;
	
	public LibrariesNode(DependencyNode parent,
            Attributes attrs)
	{
		setParent(parent);
		
		childMap = new HashMap/*<String, LibraryNode>*/();
	}

	public void addChild(DependencyNode child) {
		if (child instanceof LibraryNode)
		{
			LibraryNode ln = (LibraryNode) child;
			childMap.put(ln.getSWF(), ln);
		}
	}

	public DependencyNode getParent() {
		return parent;
	}

	public void setParent(DependencyNode parent) {
		this.parent = parent;
	}

	public void visit(Map/*<String, Set<String>>*/ dependencies, DependencyRules rules) throws IOException {
		/*for (LibraryNode child : childMap.values()) {*/LibraryNode child; for (Iterator iter = childMap.values().iterator(); iter.hasNext(); ) {  child = (LibraryNode)iter.next(); 
			child.visit(dependencies, rules);
		}
	}

}
