package flex.tools.dependencychecker.nodes;


import java.io.IOException;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.xml.sax.Attributes;

import flex.tools.dependencychecker.DependencyRules;


public class DepNode implements DependencyNode {
	
	private DependencyNode parent;
	
	private String id;
	
	private String type;
	// type = "n" means namespace
	// type = "i" means inherits
	// type = "s" means signature
	// type = "e" means expression
	
	public DepNode(DependencyNode parent,
            Attributes attrs)
	{
		setParent(parent);
		
		if (attrs != null)
		{
			id = attrs.getValue("id");
			type = attrs.getValue("type");
		}
	}

	public void addChild(DependencyNode child) {
		// no children
		assert false : child;
	}

	public DependencyNode getParent() {
		return parent;
	}

	public void setParent(DependencyNode parent) {
		this.parent = parent;
	}

	public void visit(Map/*<String, Set<String>>*/ dependencies, DependencyRules rules) throws IOException {
		ScriptNode snParent = (ScriptNode) parent;
		
		if (!rules.allowedDependency(snParent.getName(), id, type))
		{
			Set/*<String>*/ set;
			if (dependencies.containsKey(id)) {
				set = (Set)dependencies.get(id);
			}
			else
			{
				set = new TreeSet/*<String>*/();
				dependencies.put(id, set);
			}
			
			if (!set.contains(snParent.getName()))
			{
				set.add(snParent.getName());
			}
		}
	}
	
	public String getID() {
		return id;
	}
	
	public String getType() {
		return type;
	}

	public static boolean isSignatureDependency(String type)
	{
		return type.equals("s");
	}

	public static boolean isExpressionDependency(String type)
	{
		return type.equals("e");
	}

	public static boolean isNamespaceDependency(String type)
	{
		return type.equals("n");
	}

	public static boolean isInheritsDependency(String type)
	{
		return type.equals("i");
	}
}
