package flex.tools.dependencychecker.nodes;


import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import org.xml.sax.Attributes;

import flex.tools.dependencychecker.DependencyRules;


public class ScriptNode implements DependencyNode {
	
	private DependencyNode parent;
	private String name;
	
	private DefNode definition;
	private Collection/*<DepNode>*/ depNodes;
	
	public ScriptNode(DependencyNode parent,
            Attributes attrs)
	{
		setParent(parent);
		
		if (attrs != null)
		{
			name = attrs.getValue("name");
		}
		
		depNodes = new ArrayList/*<DepNode>*/();
	}

	public void addChild(DependencyNode child) {
		if (child instanceof DefNode)
		{
			DefNode defNode = (DefNode) child;
			
			assert name.equals(defNode.getID());
			assert definition == null : child;
			
			definition = defNode;
		}
		
		if (child instanceof DepNode)
		{
			DepNode depNode = (DepNode) child;
			
			depNodes.add(depNode);
		}
		
	}

	public DependencyNode getParent() {
		return parent;
	}

	public void setParent(DependencyNode parent) {
		this.parent = parent;
	}

	public void visit(Map/*<String, Set<String>>*/ dependencies, DependencyRules rules) throws IOException {
		assert definition != null : this;
		
		definition.visit(dependencies, rules);
		
		/*for (DepNode depNode : depNodes) {*/DepNode depNode; for (Iterator iter = depNodes.iterator(); iter.hasNext(); ) {  depNode = (DepNode)iter.next();
			depNode.visit(dependencies, rules);
		}
	}
	
	public String getName() {
		String nameWithAllDots = name.replace('/', '.');
		if (nameWithAllDots.lastIndexOf(".") != -1)
		{
			return nameWithAllDots.substring(0, nameWithAllDots.lastIndexOf(".")) + 
				   ":" + 
				   nameWithAllDots.substring(nameWithAllDots.lastIndexOf(".") + 1);
		}
		else
		{
			return nameWithAllDots;
		}
	}

}
