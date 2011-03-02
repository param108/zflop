package flex.tools.dependencychecker.nodes;

import org.xml.sax.Attributes;


public class DependencyNodeFactory {
	public DependencyNode createElementNode(String namespaceURI,
            String lName, // local name
            String qName, // qualified name
            DependencyNode parent,
            Attributes attrs) {
		
		if (qName.equalsIgnoreCase("libraries"))
		{	
			return new LibrariesNode(parent, attrs);
		}
		else if (qName.equalsIgnoreCase("library"))
		{	
			return new LibraryNode(parent, attrs);
		}
		else if (qName.equalsIgnoreCase("script"))
		{	
			return new ScriptNode(parent, attrs);
		}
		else if (qName.equalsIgnoreCase("dep"))
		{	
			return new DepNode(parent, attrs);
		}
		else if (qName.equalsIgnoreCase("def"))
		{	
			return new DefNode(parent, attrs);
		}
		else
		{
			return null;
		}
	}
}
