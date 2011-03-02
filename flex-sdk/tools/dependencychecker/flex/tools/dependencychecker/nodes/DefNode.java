package flex.tools.dependencychecker.nodes;


import java.util.Map;
import java.util.Set;

import org.xml.sax.Attributes;

import flex.tools.dependencychecker.DependencyRules;


public class DefNode implements DependencyNode {
	
	private DependencyNode parent;
	
	private String id;
	
	public DefNode(DependencyNode parent,
            Attributes attrs)
	{
		setParent(parent);
		
		if (attrs != null)
		{
			id = attrs.getValue("id");
		}
	}

	public void addChild(DependencyNode child) {
		// no children
		throw new RuntimeException("Defnode should have no children");
	}

	public DependencyNode getParent() {
		return parent;
	}

	public void setParent(DependencyNode parent) {
		this.parent = parent;
	}

	public void visit(Map/*<String, Set<String>>*/ dependencies, DependencyRules rules) {
		// TODO Auto-generated method stub
		
	}
	
	public String getID() {
		return id;
	}

}
