package flex.tools.dependencychecker;
import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.UnsupportedEncodingException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import flex.tools.dependencychecker.nodes.DependencyNode;
import flex.tools.dependencychecker.nodes.DependencyNodeFactory;

/**
 * This class reads in a swcFile and a list of exceptions.  It extracts the catalog.xml file from
 * the swc and looks at the dependencies brought in.  It uses the SAX processor to examine the 
 * catalog.xml file.  We actually create a make-shift Tree from the XML for the nodes we are 
 * interested.  Each of these nodes is of type DependencyNode (created by DependencyNodeFactory).
 * 
 * After reading in the tree, we call a visit() method on each node to do the appropriate work.  In 
 * this case, the only work that's done is on the depenencies (DepNode).  DepNode adds the dependencies 
 * it finds that aren't proper to the Map that gets sent in.  When we are done visiting all the nodes, we 
 * look at the map, cross out ones on our exceptions list, and then output the others.
 * 
 * To see if a dependency is kosher or not, we check our DependencyRules.  This is created by 
 * DependencyRulesFactory, which takes in the name of the swc file to generate the rules for.
 * To add more swc files, create a DependencyRules class, and add it to the DependencyRulesFactory.
 * 
 * @author rfrishbe
 *
 */
public class DependencyChecker extends DefaultHandler {

	/**
	 * Reads in the swc, finds catalog.xml.  Starts up the SAX processor.
	 * 
	 * @param args [swcFile exceptionsFile]
	 * @throws SAXException 
	 * @throws ParserConfigurationException 
	 * @throws IOException 
	 */
	public static void main(String[] args) throws ParserConfigurationException, SAXException, IOException {
		if (args.length != 2) {
            System.err.println("Usage: cmd swcFile exceptionsFile");
            System.exit(1);
        }
        
        // Use an instance of ourselves as the SAX event handler
        DefaultHandler handler = new DependencyChecker(args[0], args[1]);
        
        // Use the default (non-validating) parser
        SAXParserFactory factory = SAXParserFactory.newInstance();
        
        // Parse the input
        SAXParser saxParser = factory.newSAXParser();
        InputStream catalogStream = getCatalogInputStream(args[0]);
        
        if (catalogStream == null)
        {
        	System.err.println("Could not find catalog.xml in the zip file you passed in");
            System.exit(1);
        }
        
        saxParser.parse(catalogStream, handler);
        
        catalogStream.close();
	}
	
	private static InputStream getCatalogInputStream(String zipFile) throws IOException
	{
		FileInputStream fis = new FileInputStream(zipFile);
		ZipInputStream zis = new ZipInputStream(new BufferedInputStream(fis));
		ZipEntry entry;
		
		while((entry = zis.getNextEntry()) != null)
		{
			if (entry.getName().equals("catalog.xml"))
			{
				return zis;
			}
			else
			{
				// don't do anything
		    }
		}
		
		return null;
	}
	
	public DependencyChecker(String swcFile, String exceptionsList) throws UnsupportedEncodingException
	{
		factory = new DependencyNodeFactory();
		
        // Set up output stream
        out = new OutputStreamWriter(System.out, "UTF8");
        
        dependencyMap = new HashMap/*<String, Set<String>>*/();
        
        topLevelNodes = new ArrayList/*<DependencyNode>*/();
        
        previousExceptions = new HashSet/*<String>*/();
        
        DependencyRulesFactory rulesFactory = new DependencyRulesFactory();
        
        rules = rulesFactory.createDependencyRules(swcFile);
        
		try {
			Reader fis = new FileReader(exceptionsList);
			BufferedReader reader = new BufferedReader(fis);
			
			String exception;
			while ((exception = reader.readLine()) != null)
			{
				previousExceptions.add(exception);
			}
			
			reader.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
        
	}
	
    //===========================================================
    // SAX DocumentHandler methods
    //===========================================================

    public void startDocument()
    throws SAXException
    {
        
    }

    public void endDocument()
    throws SAXException
    {
    	try {
			doVisit();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
    }

    public void startElement(String namespaceURI,
                             String lName, // local name
                             String qName, // qualified name
                             Attributes attrs)
    throws SAXException
    {
    	DependencyNode newNode = factory.createElementNode(namespaceURI, lName, qName, current, attrs);
    	
    	if (newNode != null)
		{
    		if (current != null)
    			current.addChild(newNode);
    		else
    			topLevelNodes.add(newNode);
    		current = newNode;
		}	
    }

    public void endElement(String namespaceURI,
                           String sName, // simple name
                           String qName  // qualified name
                          )
    throws SAXException
    {
        if (current != null)
        {
        	current = current.getParent();
        }
    }
    
    private void doVisit() throws IOException
    {
    	boolean newException = false;

    	/*for (DependencyNode node : topLevelNodes) {*/DependencyNode node; for (Iterator iter = topLevelNodes.iterator(); iter.hasNext(); ) {  node = (DependencyNode)iter.next();
    		node.visit(dependencyMap, rules);
	    	
	    	SortedSet/*<String>*/ sortedIds = new TreeSet/*<String>*/();
	    	
	    	/*for (String dependency : dependencyMap.keySet()) {*/String dependency; for (Iterator iter2 = dependencyMap.keySet().iterator(); iter2.hasNext(); ) {  dependency = (String)iter2.next();
	    		sortedIds.add(dependency);
	    	}
	    	
	    	/*for (String sortedId : sortedIds) {*/String sortedId; for (Iterator iter2 = sortedIds.iterator(); iter2.hasNext(); ) {  sortedId = (String)iter2.next();
	    		Set/*<String>*/ mappers = (Set)dependencyMap.get(sortedId);
	    		/*for (String parent : mappers) {*/String parent; for (Iterator iter3 = mappers.iterator(); iter3.hasNext(); ) {  parent = (String)iter3.next();
	    			String lineOutput = sortedId + " is imported into " + parent;
	    			
	    			if (!previousExceptions.contains(lineOutput))
	    			{
		    			newException = true;
		    			out.write(lineOutput);
		    			Utilities.outputNewLine(out);
	    			}
	    		}
	    	}
    	}
		out.flush();
		if (newException)
		{
	    		System.exit(1);
		}
    }
    
	private Writer out;
	
	// maps dependencies to classes that bring them in
	private Map/*<String, Set<String>>*/ dependencyMap;
	
	private List/*<DependencyNode>*/ topLevelNodes;

	private DependencyNodeFactory factory;
	
	private DependencyNode current = null;
	
	private Set/*<String>*/ previousExceptions;
	
	private DependencyRules rules;
}
