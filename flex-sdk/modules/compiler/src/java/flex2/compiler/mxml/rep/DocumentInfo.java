////////////////////////////////////////////////////////////////////////////////
//
//  ADOBE SYSTEMS INCORPORATED
//  Copyright 2005-2007 Adobe Systems Incorporated
//  All Rights Reserved.
//
//  NOTICE: Adobe permits you to use, modify, and distribute this file
//  in accordance with the terms of the license agreement accompanying it.
//
////////////////////////////////////////////////////////////////////////////////

package flex2.compiler.mxml.rep;

import flex2.compiler.mxml.dom.ApplicationNode;
import flex2.compiler.mxml.lang.StandardDefs;
import flex2.compiler.util.QName;

import java.util.*;

/**
 * Accumulates the results of interface compilation.
 * Note: our contract includes managing relationships between stored items - e.g.
 * import names are accumulated as class names come in through various setters
 */
public class DocumentInfo
{
	public static final String OUTER_DOCUMENT_PROP = "outerDocument";

	private ApplicationNode rootNode;

	private final String path;
	private String className;
	private String packageName;
	private QName qname;
	private String qualifiedSuperClassName;
	private Set interfaceNames;
	private Set importNames;
	private Map varDecls;	// key=id, value=className
	private List scripts;
	private List metadata;

	public DocumentInfo(String path)
	{
		this.path = path;
	}

	/**
	 * set root document node.
	 * <br>Happens in InterfaceCompiler.createDocumentInfo(), right after initial syntax check in parse()
	 */
	public void setRootNode(ApplicationNode rootNode, int line)
	{
		assert this.rootNode == null;
		this.rootNode = rootNode;

		//	declare outerDocument property, if specified by root node
		String outerDocumentClassName = rootNode.getOuterDocumentClassName();
		if (outerDocumentClassName != null)
		{
			addVarDecl(OUTER_DOCUMENT_PROP, outerDocumentClassName, line);
		}
	}

	/**
	 * get root document node
	 */
	public ApplicationNode getRootNode()
	{
		assert rootNode != null;
		return rootNode;
	}

	/**
	 * set document class name.
	 * <br>Happens in InterfaceCompiler.createDocumentInfo(), right after initial syntax check in parse()
	 */
	public void setClassName(String className)
	{
		assert this.className == null;
		this.className = className;
	}

	/**
	 * get document class name
	 */
	public String getClassName()
	{
		assert className != null;
		return className;
	}

	/**
	 * set document package name.
	 * <br>Happens in InterfaceCompiler.createDocumentInfo(), right after initial syntax check in parse()
	 */
	public void setPackageName(String packageName)
	{
		assert this.packageName == null;
		this.packageName = packageName;
	}

	/**
	 * get document package name
	 */
	public String getPackageName()
	{
		assert packageName != null;
		return packageName;
	}

    public QName getQName()
    {
        return qname != null ? qname : (qname = new QName(packageName, className));
    }

	/**
	 * set document superclass name. adds name to import list.
	 * <br>Happens in InterfaceCompiler.createDocumentInfo(), right after initial syntax check in parse()
	 */
	public void setQualifiedSuperClassName(String qualifiedSuperClassName, int line)
	{
		assert this.qualifiedSuperClassName == null;
		this.qualifiedSuperClassName = qualifiedSuperClassName;
		addImportName(qualifiedSuperClassName, line);
	}

	/**
	 * get document superclass name
	 */
	public String getQualifiedSuperClassName()
	{
		assert qualifiedSuperClassName != null;
		return qualifiedSuperClassName;
	}

	/**
	 * add interface ('implements') name. adds name to import list.
	 * <br>Happens in InterfaceCompiler.createDocumentInfo(), right after initial syntax check in parse()
	 */
	public void addInterfaceName(String interfaceName, int line)
	{
		(interfaceNames != null ? interfaceNames : (interfaceNames = new TreeSet())).add(new NameInfo(interfaceName, line));
		addImportName(interfaceName, line);
	}

	/**
	 * get Set of document interface ('implements') names
	 */
	public Set getInterfaceNames()
	{
		return interfaceNames != null ? interfaceNames : Collections.EMPTY_SET;
	}

	/**
	 * add definition name to import set.
	 * <li>- base set of MXML imports is added in InterfaceCompiler.createDocumentInfo()
	 * <li>- various names are added to imports as side effects of their respective setters here:
	 * superclass, interfaces, id-to-classname entries. These are all invoked from InterfaceCompiler.InterfaceAnalyzer,
	 * which traverses the DOM collecting public signature items.
	 * <li>- tag-backing classes, and support classes for built-in tags, are imported as the DOM is traversed
	 * in InterfaceCompiler.DependencyAnalyzer
	 */
	public void addImportName(String importName, int line)
	{
		if (!importName.equals("*") && !StandardDefs.isBuiltInTypeName(importName))
		{
			(importNames != null ? importNames : (importNames = new TreeSet())).add(new NameInfo(importName, line));
		}
	}

	/**
	 * add definition names to import set
	 */
	public void addImportNames(Collection names, int line)
	{
		for (Iterator iter = names.iterator(); iter.hasNext(); )
			addImportName((String)iter.next(), line);
	}

	/**
	 * get document import names
	 */
	public Collection getImportNames()
	{
		return importNames != null ? importNames : Collections.EMPTY_SET;
	}

	/**
	 * add name -> className mapping.
	 * <br>This set is built as InterfaceCompiler.InterfaceAnalyzer traverses the DOM, collecting items needed
	 * to generate the public signature of the class to be generated.
	 */
	public void addVarDecl(String name, String className, int line)
	{
		VarDecl ref = new VarDecl(name, className, line);
		(varDecls != null ? varDecls : (varDecls = new LinkedHashMap())).put(name, ref);
		addImportName(className, line);
	}

	/**
	 * get id to class name map
	 */
	public Map getVarDecls()
	{
		return varDecls != null ? varDecls : Collections.EMPTY_MAP;
	}

	/**
	 * return true iff id is present in map
	 */
	public boolean containsVarDecl(String id)
	{
		return getVarDecls().containsKey(id);
	}

	/**
	 * add script.
	 * <br>The script list is built as InterfaceCompiler.InterfaceAnalyzer traverses the DOM, collecting items needed
	 * to generate the public signature of the class to be generated.
	 */
	public void addScript(Script script)
	{
		(scripts != null ? scripts : (scripts = new ArrayList())).add(script);
	}

	/**
	 * get script list
	 */
	public List getScripts()
	{
		return scripts != null ? scripts : Collections.EMPTY_LIST;
	}

	/**
	 * add metadata script..
	 * <br>The metadata script list is built as InterfaceCompiler.InterfaceAnalyzer traverses the DOM, collecting items needed
	 * to generate the public signature of the class to be generated.
	 */
	public void addMetadata(Script metadatum)
	{
		(metadata != null ? metadata : (metadata = new ArrayList())).add(metadatum);
	}

	/**
	 * get metadata script list
	 */
	public List getMetadata()
	{
		return metadata != null ? metadata : Collections.EMPTY_LIST;
	}

	/**
	 *
	 */
	public static class VarDecl
	{
		public final String name, className;
		public final int line;

		VarDecl(String name, String className, int line)
		{
			this.name = name;
			this.className = className;
			this.line = line;
		}
	}

	/**
	 *
	 */
	public static class NameInfo implements Comparable
	{
		NameInfo(String name, int line)
		{
			this.name = name;
			this.line = line;
		}

		public int compareTo(Object o)
		{
			return o instanceof NameInfo ? name.compareTo(((NameInfo) o).name) : 0;
		}

		public String toString()
		{
			return name;
		}

        public String getName()
        {
            return name;
        }

        public int getLine()
        {
            return line;
        }

		private final String name;
		private final int line;
	}
}
