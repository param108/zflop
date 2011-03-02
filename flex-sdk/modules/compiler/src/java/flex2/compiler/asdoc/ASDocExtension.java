////////////////////////////////////////////////////////////////////////////////
//
//  ADOBE SYSTEMS INCORPORATED
//  Copyright 2006-2007 Adobe Systems Incorporated
//  All Rights Reserved.
//
//  NOTICE: Adobe permits you to use, modify, and distribute this file
//  in accordance with the terms of the license agreement accompanying it.
//
////////////////////////////////////////////////////////////////////////////////

package flex2.compiler.asdoc;

import flex2.compiler.as3.Extension;
import flex2.compiler.as3.reflect.TypeTable;
import flex2.compiler.CompilationUnit;
import flex2.compiler.Context;
import flex2.compiler.util.NameFormatter;
import flex2.compiler.util.QName;
import macromedia.asc.parser.AttributeListNode;
import macromedia.asc.parser.LiteralBooleanNode;
import macromedia.asc.parser.LiteralNullNode;
import macromedia.asc.parser.LiteralNumberNode;
import macromedia.asc.parser.LiteralStringNode;
import macromedia.asc.parser.MemberExpressionNode;
import macromedia.asc.parser.NamespaceDefinitionNode;
import macromedia.asc.parser.Node;
import macromedia.asc.parser.ProgramNode;
import macromedia.asc.parser.MetaDataEvaluator;
import macromedia.asc.parser.DocCommentNode;
import macromedia.asc.parser.MetaDataNode;
import macromedia.asc.parser.FunctionDefinitionNode;
import macromedia.asc.parser.ClassDefinitionNode;
import macromedia.asc.parser.PackageDefinitionNode;
import macromedia.asc.parser.InterfaceDefinitionNode;
import macromedia.asc.parser.VariableDefinitionNode;
import macromedia.asc.parser.VariableBindingNode;
import macromedia.asc.parser.Tokens;

import macromedia.asc.semantics.ObjectValue;
import macromedia.asc.semantics.ReferenceValue;
import macromedia.asc.semantics.Slot;

import macromedia.asc.semantics.Value;
import macromedia.asc.util.ObjectList;

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.FileOutputStream;
import java.io.File;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.LinkedHashMap;
import java.util.HashSet;
import java.util.TreeMap;

import flash.util.Trace;

/**
 * Compiler extension that creates the ASDoc xml file
 *
 * @author Brian Deitte
 */
public class ASDocExtension implements Extension
{
	private static String EXCLUDE_CLASS = "ExcludeClass";
	private static boolean useNewCode = true;
    
	private StringBuffer out;
	private String xml;
	private List excludeClasses;
	private Set includeOnly;
	private Set packages;
    
    //a map of every class (including excluded ones) for retrieving inherited docs
    private LinkedHashMap classes;
    
    private ClassTable tab;     //new

	public ASDocExtension(List excludeClasses, Set includeOnly, Set packages)
	{
		this.excludeClasses = excludeClasses;
		this.includeOnly = includeOnly;
		this.packages = packages;
        if (!useNewCode)
        {
            out = new StringBuffer();
            out.append("<asdoc>\n");
            classes = new LinkedHashMap();
        }
        else
            tab = new ClassTable();     //new
	}

	public void finish()
	{
        if (!useNewCode)
        {
            Iterator iter = classes.values().iterator();
            while(iter.hasNext())
            {
                CommentsTable temp = (CommentsTable)iter.next();
                temp.writeTopLevel(out);        
            }
            out.append("\n</asdoc>\n");
            xml = out.toString();
            out = null;
        }
        else
        {
            /*
             * This part shouldn't be exposed...the default should be a TopLevel passed in.
             */
            DocCommentGenerator g = new TopLevelGenerator();    //new
            g.generate(tab);    //new
            xml = g.toString();    //new
            g = null;    //new
        }
	}

	public void saveFile(File file)
	{
		BufferedOutputStream outputStream = null;
		try
		{
			outputStream = new BufferedOutputStream(new FileOutputStream(file));
			outputStream.write(xml.getBytes("UTF-8"));
			outputStream.flush();
		}
		catch (IOException ex)
		{
			if (Trace.error)
				ex.printStackTrace();

			throw new RuntimeException("Could not save " + file + ": " + ex);
		}
		finally
		{
			if (Trace.asdoc) System.out.println("Wrote doc file: " + file);

			if (outputStream != null)
			{
				try
				{
					outputStream.close();
				}
				catch (IOException ex) {}
			}
		}
	}

	public String getXML()
	{
		return xml;
	}

	public void parse1(CompilationUnit unit, TypeTable typeTable)
	{
	}

	public void parse2(CompilationUnit unit, TypeTable typeTable)
	{
	}

	public void analyze1(CompilationUnit unit, TypeTable typeTable)
	{
	}

	public void analyze2(CompilationUnit unit, TypeTable typeTable)
	{
	}

	public void analyze3(CompilationUnit unit, TypeTable typeTable)
	{
	}

	public void analyze4(CompilationUnit unit, TypeTable typeTable)
	{
	}

	public void generate(CompilationUnit unit, TypeTable typeTable)
	{
		// this code is similar to code in asc.  We don't go through the main asc path, though,
		// and have multiple compilation passes, so we have to have our own version of this code
		Context flexCx = unit.getContext();
		macromedia.asc.util.Context cx = (macromedia.asc.util.Context)flexCx.getAttribute("cx");
		ProgramNode node = (ProgramNode) unit.getSyntaxTree();

		String className = NameFormatter.toDot(unit.topLevelDefinitions.first());
		boolean exclude = false;
		if (includeOnly != null && ! includeOnly.contains(className))
		{
			exclude = true;
		}
		else if (excludeClasses.contains(className))
		{
			excludeClasses.remove(className);
			exclude = true;
		}
		// check the metadata for ExcludeClass.  Like Flex Builder, ASDoc uses this compiler metadata to
		// determine which classes should not be visible to the user
		else if (unit.metadata != null)
		{

			for (Iterator iterator = unit.metadata.iterator(); iterator.hasNext();)
			{
				MetaDataNode metaDataNode = (MetaDataNode)iterator.next();
				if (EXCLUDE_CLASS.equals(metaDataNode.id))
				{
					exclude = true;
					break;
				}
			}
		}
        
        HashSet inheritance = new HashSet();
        Iterator iter = unit.inheritance.iterator();
        while(iter.hasNext())
            inheritance.add(iter.next());
        
        CommentsTable table = new CommentsTable(inheritance, exclude, cx);
        
        if (! exclude && ! unit.getSource().isInternal())
		{
			if (Trace.asdoc) System.out.println("Generating XML for " + unit.getSource().getName());

			if (packages.size() != 0)
			{
				String n = unit.topLevelDefinitions.first().getNamespace();
				if (n != null)
				{
					packages.remove(n);
				}
			}

			cx.pushScope(node.frame);

			MetaDataEvaluator printer = new MetaDataEvaluator();
			node.evaluate(cx,printer);

			ObjectList comments = printer.doccomments;
			if (!useNewCode)
            {
                int numComments = comments.size();
    			for (int x = 0; x < numComments; x++)
    			{
                    table.addComment((DocCommentNode)comments.get(x));
    			}
            }
            else
                tab.addComments(unit.topLevelDefinitions.first(), comments, inheritance, false, cx);
            cx.popScope();
		}
		else
		{
			if (Trace.asdoc) System.out.println("Skipping generating XML for " + unit.getSource().getName());
            
            //But we still need the comments to retrieve inherited documentation
            if (packages.size() != 0)
            {
                String n = unit.topLevelDefinitions.first().getNamespace();
                if (n != null)
                {
                    packages.remove(n);
                }
            }
            
            cx.pushScope(node.frame);

            MetaDataEvaluator printer = new MetaDataEvaluator();
            node.evaluate(cx,printer);

            ObjectList comments = printer.doccomments;
            if (!useNewCode)
            {
                int numComments = comments.size();
                for (int x = 0; x < numComments; x++)
                {
                    table.addComment((DocCommentNode)comments.get(x));
                }
            }
            else
                tab.addComments(unit.topLevelDefinitions.first(), comments, inheritance, true, cx);
            cx.popScope();
		}
        if (!useNewCode)
            classes.put(className, table);
        
	}
    
    /**
     * CommentsTable stores the comments for both excluded and not excluded
     * classes. This allows for <code>@inheritDoc</code> processing.
     * 
     * @author Kevin Lin
     *
     */
    private class CommentsTable
    {
        //Types of definitions
        private final int PACKAGE = 0;
        private final int CLASS = 1;
        private final int INTERFACE = 2;
        private final int FUNCTION = 3;
        private final int FUNCTION_GET = 4;
        private final int FUNCTION_SET = 5;
        private final int FIELD = 6;
        
        private TreeMap docTable;
        private boolean exclude;
        private HashSet inheritance;
        private macromedia.asc.util.Context cx;
        
        /**
         * @param inheritance includes both superclasses and interfaces.
         * @param exclude
         */
        public CommentsTable(HashSet inheritance, boolean exclude, macromedia.asc.util.Context cx)
        {
            docTable = new TreeMap();
            this.exclude = exclude;
            this.inheritance = inheritance;
            this.cx = cx;
        }
        
        /** 
         * Adds a comment to the table.
         * 
         * @param comment
         * @param exclude
         */
        public void addComment(DocCommentNode comment)
        {
            CommentEntry entry = new CommentEntry(comment, exclude);
            if (!docTable.containsKey(entry.key))
                docTable.put(entry.key, entry);
        }
        
        /**
         * Finds inherited documentation to comment.
         *
         * @return Returns inherited documentation (without root tags).
         */
        private String findInheritDoc(KeyPair key)
        {
            String inheritDoc = null;
            
            Iterator iter = inheritance.iterator();
            while (iter.hasNext()){
                String nextClass = NameFormatter.toDot((QName)iter.next());
                CommentsTable t = (CommentsTable)classes.get(nextClass);
                if (t != null)
                {
                    //Class name changes for Class level Documentation
                    if (key.type == CLASS)
                        inheritDoc = t.getCommentForInherit(new KeyPair(nextClass.substring(nextClass.lastIndexOf('.')+1), CLASS));
                    else
                        inheritDoc = t.getCommentForInherit(key);
                }
                if (inheritDoc != null)
                    break;
            }
            return inheritDoc;
        }

        /**
         * Returns full description of the comment to be inherited.
         */
        public String getCommentForInherit(KeyPair key)
        {
            CommentEntry temp = (CommentEntry)docTable.get(key);
            if (temp != null)
                return temp.getValue();
            else
                return findInheritDoc(key);    //If cannot find, check inheritance for this class
        }
        
        /**
         * Writes the final output into a StringBuffer.
         * 
         * @return Returns original StringBuffer if class is excluded. Otherwise,
         * it returns a StringBuffer with all the entries of docTable appended. 
         */
        public StringBuffer writeTopLevel(StringBuffer s)
        {
            if (exclude) //excluded class so don't write anything
                return s;
            
            Iterator iter = docTable.values().iterator();
            while (iter.hasNext())
                s = ((CommentEntry)iter.next()).emit(s);
            return s;
        }
        
        /**
         * Key to retrieve individual CommentEntry's. Composed of the
         * name of the definition associated with the comment
         * and the type of definition. Implements Comparable
         * only for equality (high and low are arbitrary).
         */
        private class KeyPair implements Comparable{
            
            public String name;
            public int type;
            
            public KeyPair(String name, int type)
            {
                this.name = name;
                this.type = type;
            }
            
            public int compareTo(Object key)
            {
                return(((new Integer(type)).toString() + name ).compareTo((new Integer(((KeyPair)key).type)).toString() + ((KeyPair)key).name));
            }
        }
        
        /**
         * A CommentEntry contains the documentation for a specific definition
         * (Ex. package, class, function, etc...). It processes any inheritDoc
         * tags.
         */
        private class CommentEntry
        {
            private boolean exclude;
            private String id;
            private StringBuffer emitme;
            
            public KeyPair key;
            private boolean inherit = false;
            
            public CommentEntry(DocCommentNode comment, boolean exclude)
            {
                this.exclude = exclude;
                emitme = new StringBuffer();
                
                //prevents formatting errors
                if (comment.id == null)
                    id = "";//"<description><![CDATA[]]></description>";
                else
                    id = comment.id;
                processComment(comment);
            }
            
            private String getAccessKindFromNS(ObjectValue ns)
            {
                String access_specifier;
                switch (ns.getNamespaceKind())
                {
                    case macromedia.asc.util.Context.NS_PUBLIC:
                        access_specifier = "public";
                        break;
                    case macromedia.asc.util.Context.NS_INTERNAL:
                        access_specifier = "internal";
                        break;
                    case macromedia.asc.util.Context.NS_PROTECTED:
                        access_specifier = "protected";
                        break;
                    case macromedia.asc.util.Context.NS_PRIVATE:
                        access_specifier = "private";
                        break;
                    default:
                        // should never happen
                        access_specifier = "public";
                        break;
                }
                return access_specifier;
            }
            
            private void emitMetaDataComment(String debugName, MetaDataNode meta, boolean isAttributeOfDefinition, MetaDataNode current)
            {
                emitme.append("\n<metadata>\n");
                String tagname = meta.id;
                emitme.append("\n\t<").append(tagname).append(" ");
                emitme.append("owner='").append(debugName).append("' ");

                // write out the first keyless value, if any, as the name attribute. Output all keyValuePairs
                //  as usual.
                boolean has_name = false;
                if (meta.values != null)
                {
                    for (int i = 0; i < meta.values.length; i++)
                    {
                        Value v = meta.values[i];
                        if (v != null)
                        {
                            if (v instanceof MetaDataEvaluator.KeylessValue && has_name == false)
                            {
                                MetaDataEvaluator.KeylessValue ov = (MetaDataEvaluator.KeylessValue)v;
                                emitme.append("name='").append(ov.obj).append("' ");
                                has_name = true;
                                continue;
                            }
                            if (v instanceof MetaDataEvaluator.KeyValuePair)
                            {
                                MetaDataEvaluator.KeyValuePair kv = (MetaDataEvaluator.KeyValuePair)v;
                                emitme.append(kv.key).append("='").append(kv.obj).append("' ");
                                continue;
                            }
                        }
                    }
                }
                else if(meta.id != null)
                {
                    // metadata with an id, but no values
                    emitme.append("name='").append(meta.id).append("' ");
                }
                emitme.append(">\n");

                // [Event], [Style], and [Effect] are documented as seperate entities, rather than
                //   as elements of other entities.  In that case, we need to write out the asDoc
                //   comment here 
                if (isAttributeOfDefinition == false)
                {
                    if (current.values != null)
                    {
                        for (int i = 0; i < current.values.length; i++)
                        {
                            Value v = current.values[i];
                            if (v != null)
                            {
                                if (v instanceof MetaDataEvaluator.KeylessValue)
                                {
                                    MetaDataEvaluator.KeylessValue ov = (MetaDataEvaluator.KeylessValue)v;
                                    emitme.append(ov.obj);
                                    continue;
                                }
    
                                if (v instanceof MetaDataEvaluator.KeyValuePair)
                                {
                                    MetaDataEvaluator.KeyValuePair kv = (MetaDataEvaluator.KeyValuePair)v;
                                    emitme.append("\n\t<").append(kv.key).append(">").append(kv.obj).append("</").append(kv.key).append(">");
                                    continue;
                                }
                            }
                        }
                    }
                    else if (current.id != null)
                    {
                        // Id, but no values
                        emitme.append(current.id);
                    }
                }

                emitme.append("\n\t</").append(tagname).append(">\n");
                emitme.append("</metadata>\n");
            }
            
            /**
             * processComment() extracts the necessary information from the
             * DocCommentNode. It also processes any inheritDoc tags.
             */
            private void processComment(DocCommentNode comment)
            {               
                String name = "";
                int type;
                String tagname = "";
                String debug_name = "";
                
                //Extracts information (name and def type) for identifying a comment
                if (comment.def instanceof PackageDefinitionNode)
                {
                    type = PACKAGE;
                    PackageDefinitionNode pd = (PackageDefinitionNode)comment.def;
                    name = pd.name.id != null ? pd.name.id.pkg_part : "";
                    
                    //from asc
                    tagname = "packageRec";
                    debug_name = "";

                    emitme.append("\n<");
                    emitme.append(tagname);
                    emitme.append(" name='");
                    emitme.append((pd.name.id != null ? pd.name.id.pkg_part : ""));
                    emitme.append(".");
                    emitme.append((pd.name.id != null ? pd.name.id.def_part : ""));    
                    emitme.append("' fullname='");
                    emitme.append((pd.name.id != null ? pd.name.id.pkg_part : ""));
                    emitme.append(".");
                    emitme.append((pd.name.id != null ? pd.name.id.def_part : ""));    
                    emitme.append("'>\n");
                }
                else if (comment.def instanceof ClassDefinitionNode)
                {
                    ClassDefinitionNode cd = (ClassDefinitionNode)comment.def;
                    name = cd.name.name;
                    debug_name = cd.debug_name;
                    
                    if (cd.metaData.items.at(0) != comment)
                    {
                        type = -1;
                        name = "IGNORE";
                    }
                    else
                    {
                        InterfaceDefinitionNode idn = null;
                        if (comment.def instanceof InterfaceDefinitionNode)
                        {
                            type = INTERFACE;
                            tagname = "interfaceRec";
                            idn = (InterfaceDefinitionNode)comment.def;
                        }
                        else
                        {
                            type = CLASS;
                            tagname = "classRec";
                        }
                        
                        emitme.append("\n<");
                        emitme.append(tagname);
                        emitme.append(" name='");
                        emitme.append(cd.name.name);
                        emitme.append("' fullname='");
                        emitme.append(cd.debug_name);
                        if (cd.cx.input != null && cd.cx.input.origin.length() != 0)
                        {
                            emitme.append("' sourcefile='");
                            emitme.append(cd.cx.input.origin);
                        }
                        emitme.append("' namespace='");
                        emitme.append(cd.cframe.builder.classname.ns.name);
                        emitme.append("' access='");
                        emitme.append(getAccessKindFromNS(cd.cframe.builder.classname.ns));
                        
                        if (idn != null)
                        {
                            emitme.append("' baseClasses='");
                            if (idn.interfaces != null)
                            {
                                List values = idn.interfaces.values;
                                Value firstV = (Value)values.get(0);
                                for (int i = 0; i < values.size(); i++)
                                {
                                    ReferenceValue rv = (ReferenceValue)values.get(i);
                                    if ((Value)rv != firstV)
                                    {
                                        emitme.append(";");
                                    }
                                    Slot s = rv.getSlot(cx, Tokens.GET_TOKEN);
                                    emitme.append((s == null || s.getDebugName().length() == 0) ? rv.name : s.getDebugName());
                                }
                            }
                            else
                            {
                                emitme.append("Object");
                            }
                            emitme.append("' ");
                        }
                        else
                        {
                            emitme.append("' baseclass='");
                            if (cd.baseref != null)
                            {
                                Slot s = cd.baseref.getSlot(cx, Tokens.GET_TOKEN);
                                emitme.append( (s == null || s.getDebugName().length() == 0) ? "Object" : s.getDebugName());
                            }
                            else
                            {
                                emitme.append("Object");
                            }
                            emitme.append("' ");

                            if (cd.interfaces != null)
                            {
                                emitme.append("interfaces='");

                                List values = cd.interfaces.values;
                                Value firstV = (Value)values.get(0);
                                for (int i = 0; i < values.size(); i++)
                                {
                                    ReferenceValue rv = (ReferenceValue)values.get(i);
                                    if ((Value)rv != firstV)
                                    {
                                        emitme.append(";");
                                    }
                                    Slot s = rv.getSlot(cx, Tokens.GET_TOKEN);
                                    emitme.append((s == null || s.getDebugName().length() == 0) ? rv.name : s.getDebugName());
                                }
                                emitme.append("' ");
                            }
                        }

                        AttributeListNode attrs = cd.attrs;
                        if (attrs != null)
                        {
                            emitme.append("isFinal='");
                            emitme.append(attrs.hasFinal ? "true" : "false");
                            emitme.append("' ");

                            emitme.append("isDynamic='");
                            emitme.append(attrs.hasDynamic ? "true" : "false");
                            emitme.append("' ");
                        }
                        else
                        {
                            emitme.append("isFinal='false' ");
                            emitme.append("isDynamic='false' ");
                        }
                        emitme.append(">");
                    }
                }
                else if (comment.def instanceof FunctionDefinitionNode)
                {
                    type = FUNCTION;
                    FunctionDefinitionNode fd = (FunctionDefinitionNode)comment.def;
                    
                    int check1 = fd.fexpr.debug_name.indexOf("/get");
                    int check2 = fd.fexpr.debug_name.indexOf("/set");
                    if (check1 == fd.fexpr.debug_name.length()-4)
                        type = FUNCTION_GET;
                    else if (check2 == fd.fexpr.debug_name.length()-4)
                        type = FUNCTION_SET;
                    
                    name = fd.name.identifier.name;
                    
                    debug_name = fd.fexpr.debug_name;
                    tagname = "method";

                    emitme.append("\n<method name='");
                    emitme.append(fd.name.identifier.name);
                    emitme.append("' fullname='");
                    emitme.append(fd.fexpr.debug_name);
                    emitme.append("' ");
         
                    AttributeListNode attrs = fd.attrs;
                    if (attrs != null)
                    {
                        emitme.append("isStatic='");
                        emitme.append(attrs.hasStatic ? "true" : "false");
                        emitme.append("' ");

                        emitme.append("isFinal='");
                        emitme.append(attrs.hasFinal ? "true" : "false");
                        emitme.append("' ");

                        emitme.append("isOverride='");
                        emitme.append(attrs.hasOverride ? "true" : "false");
                        emitme.append("' ");
                    }
                    else
                    {
                        emitme.append("isStatic='false' ");
                        emitme.append("isFinal='false' ");
                        emitme.append("isOverride='false' ");
                    }
                    
                    //We will leave this call here for now (gets parameters/result)
                    fd.fexpr.signature.toCanonicalString(cx, emitme);
                    emitme.append(">");
                }
                else if (comment.def instanceof VariableDefinitionNode)
                {
                    type = FIELD;
                    VariableDefinitionNode vd = (VariableDefinitionNode)comment.def;
                    VariableBindingNode vb = (VariableBindingNode)(vd.list.items.get(0));
                    name = vb.variable.identifier.name;
                    
                    debug_name = vb.debug_name;

                    tagname = "field";
                    emitme.append("\n<");
                    emitme.append(tagname);
                    emitme.append(" name='");
                    emitme.append(vb.variable.identifier.name);
                    emitme.append("' fullname='");
                    emitme.append(vb.debug_name);
                    emitme.append("' type='");
                    if (vb.typeref != null)
                    {
                        Slot s = vb.typeref.getSlot(cx, Tokens.GET_TOKEN);
                        emitme.append((s == null || s.getDebugName().length() == 0) ? vb.typeref.name : s.getDebugName());
                    }           
                    emitme.append("' ");

                    AttributeListNode attrs = vd.attrs;
                    if (attrs != null)
                    {
                        emitme.append("isStatic='");  // bug in E4X prevents us from using reserved keywords like 'static' as attribute keys
                        emitme.append(attrs.hasStatic ? "true" : "false");
                        emitme.append("' ");
                    }
                    else
                    {
                        emitme.append("isStatic='false' ");
                    }
                    
                    Slot s = vb.ref.getSlot(cx);
                    if (s != null)
                    {
                        emitme.append("isConst='");
                        emitme.append(s.isConst() ? "true" : "false");
                        emitme.append("' ");
                    }

                    if (vb.initializer != null)
                    {
                        emitme.append("defaultValue='");
                        if (vb.initializer instanceof LiteralNumberNode)
                        {
                            emitme.append(((LiteralNumberNode)(vb.initializer)).value);
                        }
                        else if (vb.initializer instanceof LiteralStringNode)
                        {
                            emitme.append(DocCommentNode.escapeXml(((LiteralStringNode)(vb.initializer)).value));
                        }
                        else if (vb.initializer instanceof LiteralNullNode)
                        {
                            emitme.append("null");
                        }
                        else if (vb.initializer instanceof LiteralBooleanNode)
                        {
                            emitme.append((((LiteralBooleanNode)(vb.initializer)).value) ? "true" : "false");
                        }
                        else if (vb.initializer instanceof MemberExpressionNode)
                        {
                            MemberExpressionNode mb = (MemberExpressionNode)(vb.initializer);
                            Slot vs = (mb.ref != null ? mb.ref.getSlot(cx, Tokens.GET_TOKEN) : null);
                            Value v = (vs != null ? vs.getValue() : null);
                            ObjectValue ov = ((v instanceof ObjectValue) ? (ObjectValue)(v) : null);
                            // if constant evaluator has determined this has a value, use it.
                            emitme.append((ov != null) ? ov.getValue() : "unknown");
                        }
                        else
                        {
                            Slot vs = vb.ref.getSlot(cx, Tokens.GET_TOKEN);
                            Value v = (vs != null ? vs.getValue() : null);
                            ObjectValue ov = ((v instanceof ObjectValue) ? (ObjectValue)(v) : null);
                            // if constant evaluator has determined this has a value, use it.
                            emitme.append((ov != null) ? ov.getValue() : "unknown");
                        }
                        emitme.append("' ");
                    }
                    emitme.append(">");
                }
                else
                {
                    //unsupported definition
                    type = -1;
                    name = "Unsupported";
                }
                key = new KeyPair(name, type);
                
                if (key.type != -1)
                {
                    //only process inheritDoc when needed
                    inherit = hasInheritDoc();
                    if (!exclude && inherit)
                    {
                        processInheritDoc();
                    }
                    
                    Value[] values = comment.values;
                    if (values != null)
                    {
                        for (int i = 0; i < values.length; i++)
                        {
                            Value v = values[i];
                            if (v != null)
                            {
                                if (v instanceof MetaDataEvaluator.KeylessValue)
                                {
                                    MetaDataEvaluator.KeylessValue ov = (MetaDataEvaluator.KeylessValue)v;
                                    emitme.append(ov.obj);
                                    continue;
                                }
        
                                if (v instanceof MetaDataEvaluator.KeyValuePair)
                                {
                                    MetaDataEvaluator.KeyValuePair kv = (MetaDataEvaluator.KeyValuePair)v;
                                    emitme.append("\n<").append(kv.key).append(">").append(kv.obj).append("</").append(kv.key).append(">");
                                    continue;
                                }
                            }
                        }
                    }
                    else if (id != null)
                    {
                        // id, but no values
                        emitme.append(id);
                    }
                    
                    if (comment.def != null && comment.def.metaData != null)
                    {
                        int numItems = comment.def.metaData.items.size();
                        for (int x = 0; x < numItems; x++)
                        {
                            Node md = (Node)comment.def.metaData.items.at(x);
                            MetaDataNode mdi = (md instanceof MetaDataNode) ? (MetaDataNode)(md) : null;

                            // cn: why not just dump all the metaData ???
                            if (mdi != null && mdi.id != null)
                            {
                                // these metaData types can have their own DocComment associated with them, though they might also have no comment.
                                if (mdi.id.equals("Style") || mdi.id.equals("Event") || mdi.id.equals("Effect") )
                                {
                                    if (x+1 < numItems)  // if it has a comment, it will be the sequentially next DocCommentNode
                                    {
                                        Node next = (Node)comment.def.metaData.items.at(x+1);
                                        DocCommentNode metaDataComment = (next instanceof DocCommentNode) ? (DocCommentNode)next : null;

                                        if (metaDataComment != null)
                                        {
                                            emitMetaDataComment(debug_name, mdi, false, metaDataComment);
                                            x++;
                                        }
                                        else  // emit it even if it doesn't have a comment.
                                        {
                                            emitMetaDataComment(debug_name, mdi, true, null);
                                        }
                                    }
                                    else
                                    {
                                        emitMetaDataComment(debug_name, mdi, true, null);
                                    }
                                }
                                else if (mdi.id.equals("Bindable") || mdi.id.equals("Deprecated") || mdi.id.equals("Exclude") || mdi.id.equals("DefaultProperty"))
                                {
                                    emitMetaDataComment(debug_name, mdi, true, null);
                                }
                            }
                        }
                    }
                    if (!"".equals(tagname))
                    {
                        emitme.append("\n</");
                        emitme.append(tagname);
                        emitme.append(">");
                    }
                    else
                    {
                        if (comment.def instanceof NamespaceDefinitionNode)
                        {
                            NamespaceDefinitionNode nd = (NamespaceDefinitionNode)(comment.def);
                            if (nd != null)
                                emitme.append("<!-- Namespace comments not supported yet: ").append(nd.debug_name).append("-->");
                        }
                    }
                }
            }

            /**
             * Checks for existence of inheritDoc tag.
             */
            private boolean hasInheritDoc()
            {
                int begin = -1;
                begin = id.indexOf("<inheritDoc><![CDATA[", id.indexOf("]]></description>"));
                return begin >= 0 ? true : false;
            }
            
            
            
            /**
             * @return Returns inherited documentation appended to the comment.
             */
            private String addToComment(String inheritDoc)
            {
                String TEMP1 = "<description><![CDATA[";
                String TEMP2 = "]]></description>";
                String TEMP3 = "<inheritDoc><![CDATA[";
                String TEMP4 = "]]></inheritDoc>";
                String result;
                
                //Extracting inherited documentation
                int is = inheritDoc.indexOf(TEMP1) + TEMP1.length();
                int ie = inheritDoc.indexOf(TEMP2);
                
                //Inserting to comment and removing inheritDoc tags to prevent duplicates downstream
                int splice = id.indexOf(TEMP2);
                int temp4 = id.indexOf(TEMP4);
                //Must make sure that the beginning tag is not within a CDATA section of another.
                int endCDATABeforeInherit = id.substring(0, temp4).lastIndexOf("]]>");
                int begin = id.indexOf(TEMP3, endCDATABeforeInherit);
                int end = temp4 + TEMP4.length();
                result = id.substring(0, splice) + inheritDoc.substring(is, ie) 
                            + id.substring(splice, begin) + id.substring(end);
                
                //special case for functions
                //We must take care of parameters and return tags
                if (this.key.type == FUNCTION)
                {
                    //inherit return documentation if it is not defined.
                    String TEMP5 = "<return><![CDATA[";
                    String TEMP6 = "]]></return>";
                    int endret = id.indexOf(TEMP5);
                    if (endret < 0)
                    {
                        int endret2 = inheritDoc.indexOf(TEMP6);
                        if (endret2 > 0)
                        {
                            int endCDATABeforeReturn = inheritDoc.substring(0, endret2).lastIndexOf("]]>");
                            begin = inheritDoc.indexOf(TEMP5, endCDATABeforeReturn);
                            result += inheritDoc.substring(begin, endret2 + TEMP6.length());
                        }
                    }
                    
                    //Compare number of parameters...if there are more in the inherited, append
                    //the extra ones on.
                    String TEMP7 = "<param><![CDATA[";
                    String TEMP8 = "]]></param>";
                    int count1 = 0;   //# of params in current comment
                    int count2 = 0;   //# of params in inherited
                    int index = id.indexOf(TEMP8);
                    while (index >= 0)
                    {
                        count1++;
                        index = id.indexOf(TEMP8, index + 1);
                    }
                    index = inheritDoc.indexOf(TEMP8);
                    while (index >= 0)
                    {
                        count2++;
                        index = inheritDoc.indexOf(TEMP8, index + 1);
                    }
                    index = 0;
                    if (count2-count1 > 0)
                    {
                        for (int j = 0; j < count1; j++)
                            index = inheritDoc.indexOf(TEMP8, index + 1);
                        for (int i = 0; i < count2-count1; i++)
                        {
                            index = inheritDoc.indexOf(TEMP8, index + 1);
                            int endCDATABeforeParam = inheritDoc.substring(0, index).lastIndexOf("]]>");
                            int newindex = inheritDoc.indexOf(TEMP7, endCDATABeforeParam);
                            result += inheritDoc.substring(newindex, inheritDoc.indexOf(TEMP8, newindex) + TEMP8.length());
                        }
                    }
                }                
                return result;
            }
            
            /**
             * Processes inheritDoc tag
             */
            private void processInheritDoc()
            {
                String inheritDoc = findInheritDoc(this.key);
                if (inheritDoc != null)
                    id = addToComment(inheritDoc);
                else
                    if (Trace.asdoc) System.out.println("Cannot find inherited documentation for: " + this.key.name);
            }
            
            /**
             * If this comment was excluded from inheritDoc processing before,
             * do it now.
             * 
             * @return Returns the comment without root tags.
             */
            public String getValue()
            {
                //only search for inherited documentation when exclude condition
                //because we did not search for it earlier.
                if (inherit && exclude)
                {
                    String inheritDoc = findInheritDoc(this.key);
                    if (inheritDoc != null)
                        return (addToComment(inheritDoc));
                }
                return id;
            }
            
            /**
             * @param sb StringBuffer to append to
             * @return Appends the full comment onto the StringBuffer
             */
            public StringBuffer emit(StringBuffer sb)
            {
                return sb.append(emitme);
            }
        }
    }
}