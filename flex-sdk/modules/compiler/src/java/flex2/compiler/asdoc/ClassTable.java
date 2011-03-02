////////////////////////////////////////////////////////////////////////////////
//
//  ADOBE SYSTEMS INCORPORATED
//  Copyright 2007 Adobe Systems Incorporated
//  All Rights Reserved.
//
//  NOTICE: Adobe permits you to use, modify, and distribute this file
//  in accordance with the terms of the license agreement accompanying it.
//
////////////////////////////////////////////////////////////////////////////////

package flex2.compiler.asdoc;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import macromedia.asc.parser.AttributeListNode;
import macromedia.asc.parser.ClassDefinitionNode;
import macromedia.asc.parser.DocCommentNode;
import macromedia.asc.parser.FunctionDefinitionNode;
import macromedia.asc.parser.InterfaceDefinitionNode;
import macromedia.asc.parser.LiteralBooleanNode;
import macromedia.asc.parser.LiteralNullNode;
import macromedia.asc.parser.LiteralNumberNode;
import macromedia.asc.parser.LiteralStringNode;
import macromedia.asc.parser.MemberExpressionNode;
import macromedia.asc.parser.MetaDataEvaluator;
import macromedia.asc.parser.MetaDataNode;
import macromedia.asc.parser.Node;
import macromedia.asc.parser.PackageDefinitionNode;
import macromedia.asc.parser.ParameterListNode;
import macromedia.asc.parser.ParameterNode;
import macromedia.asc.parser.RestParameterNode;
import macromedia.asc.parser.Tokens;
import macromedia.asc.parser.TypeExpressionNode;
import macromedia.asc.parser.VariableBindingNode;
import macromedia.asc.parser.VariableDefinitionNode;
import macromedia.asc.semantics.ObjectValue;
import macromedia.asc.semantics.ReferenceValue;
import macromedia.asc.semantics.Slot;
import macromedia.asc.semantics.Value;
import macromedia.asc.util.Context;
import flash.util.Trace;

import flex2.compiler.util.NameFormatter;
import flex2.compiler.util.QName;
import flex2.compiler.util.ThreadLocalToolkit;

/**
 * ClassTable stores the CommentsTables containing CommentEntrys
 * in a LinkedHashMap. The key to each CommentsTable is the package 
 * name and class name in dot format. (Ex. the class Foo in the 
 * package Bar would be Bar.Foo and the class Cheese in the default
 * (empty) package would be Cheese). It also keeps a separate Map
 * containing each unique package name linked to a CommentEntry for
 * that package (if it exists). A HashSet is used to quickly check
 * for known tag names.
 * 
 * @author klin
 *
 */
public class ClassTable implements DocCommentTable {
    
    private LinkedHashMap classTable;
    private LinkedHashMap packageTable;
    private HashSet tagNames;
    
    public ClassTable()
    {
        classTable = new LinkedHashMap();
        packageTable = new LinkedHashMap();
        tagNames = new HashSet();
        tagNames.add("author");
        tagNames.add("category");
        tagNames.add("copy");
        tagNames.add("default");
        tagNames.add("deprecated");
        tagNames.add("event");
        tagNames.add("eventType");
        tagNames.add("example");
        tagNames.add("exampleText");
        tagNames.add("excludeInherited");
        tagNames.add("helpid");
        tagNames.add("import");
        tagNames.add("includeExample");
        tagNames.add("inheritDoc");
        tagNames.add("internal");
        tagNames.add("keyword");
        tagNames.add("langversion");
        tagNames.add("migration");
        tagNames.add("param");
        tagNames.add("playerversion");
        tagNames.add("private");
        tagNames.add("productversion");
        tagNames.add("return");
        tagNames.add("review");
        tagNames.add("see");
        tagNames.add("throws");
        tagNames.add("tiptext");
        tagNames.add("toolversion");
        tagNames.add("description");
    }
    
    
    /**
     * Adds comments to the table. Sorts them by package. Also, makes sure all
     * packages are present in the packageTable.
     * 
     * @param name
     * @param comments
     * @param inheritance
     * @param exclude
     * @param cx
     */
    public void addComments(QName name, List comments, 
            HashSet inheritance, boolean exclude, Context cx)
    {
        String packageName = name.getNamespace().intern();
        String className = name.getLocalPart().intern();
        //CommentNodes belonging to the public class (or function)
        List mainClass = new ArrayList();
        //CommentNodes belonging to private classes and their inheritance
        Map otherClasses = new LinkedHashMap();
        Map otherInheritance = new LinkedHashMap();
        //Whether there is a public class declaration
        boolean mainDef = false;
        //asc generated package
        String otherPackage = null;
        
        //sorting all the comments and pulling out other classes that are out of the package
        for (int i = 0; i < comments.size(); i++)
        {
            DocCommentNode current = (DocCommentNode)comments.get(i);
            String pkg = "";  //package name
            String cls = "";  //class name
            String debug;
            if (current.def instanceof PackageDefinitionNode)
            {
                mainClass.add(current);
                continue;
            }
            else if (current.def instanceof ClassDefinitionNode)
            {
                ClassDefinitionNode cd = (ClassDefinitionNode)current.def;
                
                if (cd.metaData.items.at(0) == current)
                {
                    debug = cd.debug_name;
                    int colon = debug.indexOf(':');
                    if (colon < 0) //empty package
                    {
                        pkg = "";
                        cls = debug.intern();
                    }
                    else
                    {
                        pkg = debug.substring(0, colon).intern();
                        cls = debug.substring(colon + 1).intern();
                    }
                    if (cls.equals(className) && pkg.equals(packageName))
                        mainDef = true;
                    else
                    {
                        if (otherPackage == null)
                            otherPackage = pkg;
                        
                        //if not the public class, we need to create our own inheritance set
                        HashSet inherit = new HashSet(); 
                        otherInheritance.put(cls, inherit);
                        List inherited = cd.used_def_namespaces;
                        for (int j = 0; j < inherited.size(); j++)
                        {
                            String s = inherited.get(j).toString().intern();
                            //Make sure that the inheritance list doesn't contain itself or a package.
                            if (!s.equals(debug) && !s.equals(otherPackage))
                            {
                                QName q = new QName(s);
                                if (!q.getLocalPart().equals(""))
                                {
                                    assert !((q.getLocalPart().equals(cls)) && (q.getNamespace().equals(pkg))) : "same class";
                                    inherit.add(q);
                                }
                            }
                        }
                    }
                }
                else
                    continue; //ignore duplicates caused by metadata
            }
            else if (current.def instanceof FunctionDefinitionNode)
            {
                FunctionDefinitionNode fd = (FunctionDefinitionNode)current.def;
                debug = fd.fexpr.debug_name;
                int colon = debug.indexOf(':');
                int slash = debug.indexOf('/');
                if (colon < 0)
                {
                    pkg = "";
                    if (slash < 0) //when there's only a name (Ex. debug == Foobar)
                        cls = "";
                    else  //when there happens to be a slash (Ex. debug == Class/Function)
                        cls = debug.substring(0, slash).intern();
                }
                else
                {
                    pkg = debug.substring(0, colon).intern();
                    if (slash < 0)   //when you have debug == packageName:Function
                        cls = "";
                    else if (slash < colon)  //when debug == className/private:something (mxml case)
                    {
                        pkg = "";
                        cls = debug.substring(0, slash).intern();
                    }
                    else  //when debug == packageName:className/Function
                        cls = debug.substring(colon + 1, slash).intern();
                }
            }
            else if (current.def instanceof VariableDefinitionNode)
            {
                VariableBindingNode vb = (VariableBindingNode)(((VariableDefinitionNode)current.def).list.items.get(0));
                debug = vb.debug_name;
                int colon = debug.indexOf(':');
                int slash = debug.indexOf('/');
                if (colon < 0)
                {
                    pkg = "";
                    if (slash < 0)
                        cls = "";
                    else
                        cls = debug.substring(0, slash).intern();
                }
                else
                {
                    pkg = debug.substring(0, colon).intern();
                    if (slash < 0)
                        cls = "";
                    else if (slash < colon)
                    {
                        pkg = "";
                        cls = debug.substring(0, slash).intern();
                    }
                    else
                        cls = debug.substring(colon + 1, slash).intern();
                }
            }
            //Add to list for other classes (they will be in a separate package)
            if (!pkg.equals(packageName))
            {
                if (cls.equals(""))
                    cls = "null";
                List l = (List)otherClasses.get(cls);
                if (l == null)
                    l = new ArrayList();
                l.add(current);
                otherClasses.put(cls, l);
            }
            else  //Add to list for public class
                mainClass.add(current);
        }
        
        if (mainDef)  //there exists a public class definition
            this.put(name, mainClass, inheritance, exclude, cx);
        else    //null classname for package level functions
            this.put(new QName(packageName, "null"), mainClass, inheritance, exclude, cx);
        
        //for classes outside the package but in the same sourcefile (should be private, but we exclude anyway)
        if (otherPackage != null)
        {
            Iterator iter = otherClasses.keySet().iterator();
            while (iter.hasNext())
            {
                //Add other classes under asc generated package name
                String cls = ((String)iter.next()).intern();
                this.put(new QName(otherPackage, cls), (List)otherClasses.get(cls), (HashSet)otherInheritance.get(cls), true, cx);
            }
        }
        
        //This is to ensure that the packageTable contains all the package names (as keys).
        if (!packageTable.containsKey(packageName))
            packageTable.put(packageName, null);
        if (otherPackage != null && !packageTable.containsKey(otherPackage))
            packageTable.put(otherPackage, null);
    }
    
    private void put(QName name, List comments, HashSet inheritance, boolean exclude, Context cx)
    {
        CommentsTable table = new CommentsTable(name.getNamespace(), name.getLocalPart(), inheritance, exclude, cx);
        int temp = comments.size();
        for (int i = 0; i < temp; i++)
        {
            DocCommentNode tempNode = (DocCommentNode)comments.get(i);
            DocComment tempComment = table.addComment(tempNode);
            //keep a reference to the first packageEntry encountered for each package.
            if (packageTable.get(name.getNamespace()) == null && tempComment != null)
                if (tempComment.getType() == DocComment.PACKAGE)
                    packageTable.put(name.getNamespace(), tempComment);
        }
        classTable.put(NameFormatter.toDot(name), table);
    }
    
    public List getAllClassComments(String className, String packageName)
    {
        try
        {
            if (packageName == null)
                packageName = "";
            if (className == null || className.equals(""))
                className = "null";
            String name = NameFormatter.toDot(new QName(packageName, className));
            CommentsTable temp = (CommentsTable)classTable.get(name);
            return new ArrayList(temp.values());
        } catch (NullPointerException e)
        {
            return null;   //if a given class/package do not exist
        }
    }

    public List getAllComments()
    {
        List comments = new ArrayList();
        Iterator iter = classTable.keySet().iterator();
        while (iter.hasNext())
        {
            CommentsTable temp1 = (CommentsTable)classTable.get(iter.next());
            comments.addAll(temp1.values());
        }
        return comments;
    }

    public Map getClasses(String packageName)
    {
        return getCsOrIs(packageName, true, false);
    }

    public Map getClassesAndInterfaces(String packageName)
    {
        return getCsOrIs(packageName, true, true);
    }
    
    /**
     * Helper function for getting Classes/Interfaces/Both.
     * @param packageName
     * @param c
     * @param i
     * @return
     */
    private Map getCsOrIs(String packageName, boolean c, boolean i)
    {
        if (packageName == null)
            packageName = "";
        Map comments = new LinkedHashMap();
        Iterator iter = classTable.keySet().iterator();
        while (iter.hasNext())
        {
            String key = (String)iter.next();
            int dot = key.lastIndexOf(".");
            
            //Case when in empty package. ("null" package signifies top-level functions/variables)
            if (dot < 0 && packageName.equals("") && !key.equals("null"))
            {
                CommentsTable temp1 = (CommentsTable)classTable.get(key);
                if (!temp1.isInterface() && c)
                    comments.put(key, temp1.get(new KeyPair(key, DocComment.CLASS)));
                else if (i)
                    comments.put(key, temp1.get(new KeyPair(key, DocComment.INTERFACE)));
            }
            else if (dot >= 0 && (key.substring(0, dot)).equals(packageName))  //must match packageName
            {
                CommentsTable temp1 = (CommentsTable)classTable.get(key);
                String key2 = key.substring(dot + 1);
                if (!temp1.isInterface() && c)
                    comments.put(key2, temp1.get(new KeyPair(key, DocComment.CLASS)));
                else if (i)
                    comments.put(key2, temp1.get(new KeyPair(key, DocComment.INTERFACE)));
            }
        }
        return comments;
    }
    
    public Map getFields(String className, String packageName)
    {
        return getMany(className, packageName, DocComment.FIELD);
    }

    public DocComment getComment(String name, String className, String packageName, int type)
    {
        try
        {
            if (packageName == null)
                packageName = "";
            if (className == null || className.equals(""))
                className = "null";
            if (type == DocComment.PACKAGE)
                return (DocComment)packageTable.get(packageName);
            else 
            {
                CommentsTable temp = (CommentsTable)classTable.get(NameFormatter.toDot(new QName(packageName, className)));
                if (type == DocComment.CLASS || type == DocComment.INTERFACE)
                    return (DocComment)temp.get(new KeyPair(className, type));
                else if (type <= DocComment.FIELD && type >= DocComment.FUNCTION)
                    return (DocComment)temp.get(new KeyPair(name, type));
                else
                    return null;
            }
        } catch (NullPointerException e)
        {
            return null;
        }
    }
    
    public Map getFunctions(String className, String packageName)
    {
        return getMany(className, packageName, DocComment.FUNCTION);
    }

    public Map getGetMethods(String className, String packageName)
    {
        return getMany(className, packageName, DocComment.FUNCTION_GET);
    }
    
    public Map getSetMethods(String className, String packageName)
    {
        return getMany(className, packageName, DocComment.FUNCTION_GET);
    }
    
    /**
     * Helper function for getting functions/fields.
     * @param className
     * @param packageName
     * @param type
     * @return
     */
    private Map getMany(String className, String packageName, int type)
    {
        if (packageName == null)
            packageName = "";
        if (className == null || className.equals(""))
            className = "null";
        Map comments = new LinkedHashMap();
        CommentsTable temp = (CommentsTable)classTable.get(NameFormatter.toDot(new QName(packageName, className)));
        if (temp == null)
            return null;
        Iterator iter = temp.keySet().iterator();
        while (iter.hasNext())
        {
            KeyPair key = (KeyPair)iter.next();
            if (key.type == type)
                comments.put(key.name, temp.get(key));
        }
        return comments;
    }

    public Map getInterfaces(String packageName)
    {
        return getCsOrIs(packageName, false, true);
    }

    public Map getPackages()
    {
        return new LinkedHashMap(packageTable);
    }
    
    /**
     * A CommentsTable stores CommentEntries in itself by extending
     * TreeMap. The key is a private utility class KeyPair that
     * stores a name and integer type. Extending TreeMap keeps the
     * CommentEntries in the order provided by KeyPair. CommentsTable
     * also assists in finding the correct CommentEntry to inherit
     * documentation from.
     * 
     * 
     * @author klin
     *
     */
    private class CommentsTable extends TreeMap {
        
        private boolean exclude;
        private HashSet inheritance;
        private macromedia.asc.util.Context cx;
        private String ownerClass;
        private String ownerPackage;
        private boolean isInterface;
        
        public CommentsTable(String packageName, String className, HashSet inheritance, boolean exclude, Context cx)
        {
            super();
            this.ownerPackage = packageName;
            this.ownerClass = className;
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
        public DocComment addComment(DocCommentNode comment)
        {
            CommentEntry entry = new CommentEntry(comment, exclude);
            if (entry.key.type == DocComment.INTERFACE)
                isInterface = true;
            //make sure there are no duplicates (happens with metadata present)
            if (!this.containsKey(entry.key))
            {
                this.put(entry.key, entry);
                return entry;
            }
            return null;
        }
        
        public boolean isInterface()
        {
            return isInterface;
        }
        
        /**
         * Finds inherited documentation to comment.
         *
         * @return Returns inherited documentation
         */
        private Object[] findInheritDoc(KeyPair key)
        {
            //placeholder until null case is figured out.
            Object[] inheritDoc = null;
            
            //Search through all parent classes and implemented interfaces
            Iterator iter = inheritance.iterator();
            while (iter.hasNext()){
                QName nextClass = (QName)iter.next();
                CommentsTable t = (CommentsTable)classTable.get(NameFormatter.toDot(nextClass));
                if (t != null)
                {
                    //retrieve inherited Documentation.
                    //Special case for class definition comments
                    if (key.type == DocComment.CLASS)
                        inheritDoc = t.getCommentForInherit(new KeyPair(nextClass.getLocalPart(), DocComment.CLASS));
                    else  
                        inheritDoc = t.getCommentForInherit(key);
                }
                if (inheritDoc != null)
                    break;
            }
            return inheritDoc;
        }
        
        /**
         * Returns an array of the description, paramTags and returnTag
         * of the comment to be inherited.
         */
        public Object[] getCommentForInherit(KeyPair key)
        {
            CommentEntry temp = (CommentEntry)this.get(key);
            if (temp != null)
                return temp.getInheritedDoc();
            else
                return findInheritDoc(key);
        }
        
        /**
         * Each CommentEntry represents one comment associated with a
         * certain definition. When first instantiated, a CommentEntry
         * will take an asc DocCommentNode and retrieve all the information
         * necessary including tags. It also processes any inheritDoc tags
         * by searching through parent classes and interfaces. Each
         * CommentEntry within a certain class has a unique KeyPair
         * that allows for easy retrieval from a CommentsTable. Metadata
         * and their comments are held in a definition's CommentEntry
         * through the List, metadata.
         * 
         * @author klin
         *
         */
        private class CommentEntry implements DocComment{
            
            private boolean exclude;
            
            public KeyPair key;
            private String fullname;
            
            //Shared
            private String description;
            private boolean isFinal;
            private boolean isStatic;
            private boolean isOverride;
            
            //Classes and Interfaces
            private String sourcefile;
            private String namespace;
            private String access;
            private boolean isDynamic;
            
            //Classes
            private String baseClass;
            private String[] interfaces;
            
            //Interfaces
            private String[] baseClasses;

            //Methods
            private String[] paramNames;
            private String[] paramTypes;
            private String[] paramDefaults;
            private String resultType;
            
            //Fields
            private String vartype;
            private boolean isConst;
            private String defaultValue;
            
            //Metadata
            private List metadata;
            private String metadataType;
            private String owner;
            private String type_meta;
            private String event_meta;
            private String kind_meta;
            private String arrayType_meta;
            private String format_meta;
            private String inherit_meta;
            private String enumeration_meta;
            
            
            //Tags
            private List authorTags;
            private String categoryTag;
            private String copyTag;
            private String defaultTag;
            private boolean hasDeprecatedTag;
            private String eventTag;
            private String eventTypeTag;
            private List exampleTags;
            private List exampleTextTags;
            private String excludeInheritedTag;
            private String helpidTag;
            private List importTags;
            private List includeExampleTags;
            private boolean hasInheritTag;
            private String internalTag;
            private String keywordTag;
            private String langversionTag;
            private String migrationTag;
            private List paramTags;
            private String playerversionTag;
            private boolean hasPrivateTag;
            private String productversionTag;
            private String returnTag;
            private boolean hasReviewTag;
            private List seeTags;
            private List throwsTags;
            private String tiptextTag;
            private String toolversionTag;
            private Map customTags;
            
            /**
             * Main Constructor.
             * 
             * @param comment
             * @param exclude
             */
            public CommentEntry(DocCommentNode comment, boolean exclude)
            {
                this.exclude = exclude;
                processComment(comment);
            }
            
            /**
             * Constructor for creating a metadata CommentEntry.
             * 
             * @param debugName
             * @param meta
             * @param isAttributeOfDefinition
             * @param current
             */
            public CommentEntry(String debugName, MetaDataNode meta, boolean isAttributeOfDefinition, MetaDataNode current)
            {
                createMetadataEntry(debugName, meta, isAttributeOfDefinition, current);
            }
            
            private void createMetadataEntry(String debugName, MetaDataNode meta, boolean isAttributeOfDefinition, MetaDataNode current)
            {
                this.key = new KeyPair("IGNORE", METADATA);
                this.metadataType = meta.id;
                this.owner = debugName;

                // write out the first keyless value, if any, as the name attribute. Output all keyValuePairs
                //  as usual.
                boolean has_name = false;
                if (meta.values != null)
                {
                    int l = meta.values.length;
                    for (int i = 0; i < l; i++)
                    {
                        Value v = meta.values[i];
                        if (v != null)
                        {
                            if (v instanceof MetaDataEvaluator.KeylessValue && has_name == false)
                            {
                                MetaDataEvaluator.KeylessValue ov = (MetaDataEvaluator.KeylessValue)v;
                                this.key.name = ov.obj;
                                has_name = true;
                                continue;
                            }
                            if (v instanceof MetaDataEvaluator.KeyValuePair)
                            {
                                MetaDataEvaluator.KeyValuePair kv = (MetaDataEvaluator.KeyValuePair)v;
                                String s = kv.key.intern();
                                if (s.equals("name"))
                                    this.key.name = kv.obj;
                                else if (s.equals("type"))
                                    this.type_meta = kv.obj;
                                else if (s.equals("event"))
                                    this.event_meta = kv.obj;
                                else if (s.equals("kind"))
                                    this.kind_meta = kv.obj;
                                else if (s.equals("arrayType"))
                                    this.arrayType_meta = kv.obj;
                                else if (s.equals("format"))
                                    this.format_meta = kv.obj;
                                else if (s.equals("inherit"))
                                    this.inherit_meta = kv.obj;
                                else if (s.equals("enumeration"))
                                    this.enumeration_meta = kv.obj;
                                continue;
                            }
                        }
                    }
                }
                else if(meta.id != null)
                {
                    // metadata with an id, but no values
                    this.key.name = meta.id;
                }

                // [Event], [Style], and [Effect] are documented as seperate entities, rather than
                //   as elements of other entities.  In that case, we need to write out the asDoc
                //   comment here 
                if (isAttributeOfDefinition == false)
                {
                    if (current.id != null)
                    {
                        // Id, but no values
                        this.processTags(current.id);
                    }
                }
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
            
            private void processPackage(PackageDefinitionNode pd)
            {
                this.key.type = PACKAGE;
                this.key.name = pd.name.id != null ? pd.name.id.pkg_part : "";
                fullname = pd.name.id != null ? pd.name.id.pkg_part + "." + pd.name.id.def_part : "";
            }
            
            private void processClassAndInterface(ClassDefinitionNode cd)
            {
                this.key.name = cd.name.name;
                fullname = cd.debug_name;
                InterfaceDefinitionNode idn = null;
                if (cd instanceof InterfaceDefinitionNode)
                {
                    this.key.type = INTERFACE;
                    idn = (InterfaceDefinitionNode)cd;
                }
                else
                {
                    this.key.type = CLASS;
                }
                
                if (cd.cx.input != null && cd.cx.input.origin.length() != 0)
                {
                    sourcefile = cd.cx.input.origin;
                }
                namespace = cd.cframe.builder.classname.ns.name;
                access = getAccessKindFromNS(cd.cframe.builder.classname.ns);
                
                if (idn != null)
                {
                    if (idn.interfaces != null)
                    {
                        List values = idn.interfaces.values;
                        baseClasses = new String[values.size()];
                        for (int i = 0; i < values.size(); i++)
                        {
                            ReferenceValue rv = (ReferenceValue)values.get(i);
                            Slot s = rv.getSlot(cx, Tokens.GET_TOKEN);
                            baseClasses[i] = (s == null || s.getDebugName().length() == 0) ? rv.name : s.getDebugName();
                        }
                    }
                    else
                    {
                        baseClasses = new String[] {"Object"};
                    }
                }
                else
                {
                    if (cd.baseref != null)
                    {
                        Slot s = cd.baseref.getSlot(cx, Tokens.GET_TOKEN);
                        baseClass = (s == null || s.getDebugName().length() == 0) ? "Object" : s.getDebugName();
                    }
                    else
                    {
                        baseClass = "Object";
                    }

                    if (cd.interfaces != null)
                    {
                        List values = cd.interfaces.values;
                        interfaces = new String[values.size()];
                        for (int i = 0; i < values.size(); i++)
                        {
                            ReferenceValue rv = (ReferenceValue)values.get(i);
                            Slot s = rv.getSlot(cx, Tokens.GET_TOKEN);
                            interfaces[i] = (s == null || s.getDebugName().length() == 0) ? rv.name : s.getDebugName();
                        }
                    }
                }

                AttributeListNode attrs = cd.attrs;
                if (attrs != null)
                {
                    isFinal = attrs.hasFinal ? true : false;
                    isDynamic = attrs.hasDynamic ? true : false;
                }
            }

            
            private void processFunction(FunctionDefinitionNode fd)
            {
                key.type = FUNCTION;
                int check1 = fd.fexpr.debug_name.indexOf("/get");
                int check2 = fd.fexpr.debug_name.indexOf("/set");
                if (check1 == fd.fexpr.debug_name.length()-4)
                    key.type = FUNCTION_GET;
                else if (check2 == fd.fexpr.debug_name.length()-4)
                    key.type = FUNCTION_SET;
                
                key.name = fd.name.identifier.name;
                
                fullname = fd.fexpr.debug_name;
     
                AttributeListNode attrs = fd.attrs;
                if(attrs != null)
                {
                    isStatic = attrs.hasStatic;
                    isFinal = attrs.hasFinal;
                    isOverride = attrs.hasOverride;
                }
                
                ParameterListNode pln = fd.fexpr.signature.parameter;
                if (pln != null)
                {
                    int size = pln.items.size();
                    paramNames = new String[size];
                    paramTypes = new String[size];
                    paramDefaults = new String[size];
                    //param_names
                    ParameterNode pn;
                    for (int i = 0; i < size; i++)
                    {
                        pn = (ParameterNode)pln.items.get(i);
                        //parameter names
                        paramNames[i] = pn.ref != null ? pn.ref.name : "";
                        
                        //parameter types
                        if (pn instanceof RestParameterNode)
                            paramTypes[i] = "restParam";
                        else if (pn.typeref != null)
                        {
                            Slot s = pn.typeref.getSlot(cx, Tokens.GET_TOKEN);
                            paramTypes[i] = (s == null || s.getDebugName().length() == 0) ? pn.typeref.name : s.getDebugName();
                        }
                        
                        //parameter defaults
                        if (pn.init == null)
                            paramDefaults[i] = "undefined";
                        else
                        {
                            if (pn.init instanceof LiteralNumberNode)
                            {
                                paramDefaults[i] = ((LiteralNumberNode)(pn.init)).value;
                            }
                            else if (pn.init instanceof LiteralStringNode)
                            {
                                paramDefaults[i] = DocCommentNode.escapeXml(((LiteralStringNode)(pn.init)).value);
                            }
                            else if (pn.init instanceof LiteralNullNode)
                            {
                                paramDefaults[i] = "null";
                            }
                            else if (pn.init instanceof LiteralBooleanNode)
                            {
                                paramDefaults[i] = (((LiteralBooleanNode)(pn.init)).value) ? "true" : "false";
                            }
                            else
                            {
                                paramDefaults[i] = "unknown";
                            }
                        }
                    }
                }
                
                if (fd.fexpr.signature.result != null)
                {
                    TypeExpressionNode result = (TypeExpressionNode)fd.fexpr.signature.result;
                    if(result.expr != null)
                    {
                        MemberExpressionNode expr = (MemberExpressionNode)result.expr;
                        Slot s = expr.ref.getSlot(cx, Tokens.GET_TOKEN);
                        resultType = (s == null || s.getDebugName().length() == 0) ? expr.ref.name : s.getDebugName();
                    }
                }
                else if( fd.fexpr.signature.void_anno )
                    resultType = "void";
                else
                    resultType = cx.noType().name.toString();
            }
            
            private void processField(VariableDefinitionNode vd)
            {
                VariableBindingNode vb = (VariableBindingNode)(vd.list.items.get(0));
                key.type = FIELD;
                key.name = vb.variable.identifier.name;
                fullname = vb.debug_name;

                if (vb.typeref != null)
                {
                    Slot s = vb.typeref.getSlot(cx, Tokens.GET_TOKEN);
                    vartype = (s == null || s.getDebugName().length() == 0) ? vb.typeref.name : s.getDebugName();
                }

                AttributeListNode attrs = vd.attrs;
                if (attrs != null)
                {
                    isStatic = attrs.hasStatic;
                }
                
                Slot s = vb.ref.getSlot(cx);
                if (s != null)
                {
                    isConst = s.isConst();               
                }

                if (vb.initializer != null)
                {
                    if (vb.initializer instanceof LiteralNumberNode)
                    {
                        defaultValue = ((LiteralNumberNode)(vb.initializer)).value;
                    }
                    else if (vb.initializer instanceof LiteralStringNode)
                    {
                        defaultValue = DocCommentNode.escapeXml(((LiteralStringNode)(vb.initializer)).value);
                    }
                    else if (vb.initializer instanceof LiteralNullNode)
                    {
                        defaultValue = "null";
                    }
                    else if (vb.initializer instanceof LiteralBooleanNode)
                    {
                        defaultValue = (((LiteralBooleanNode)(vb.initializer)).value) ? "true" : "false";
                    }
                    else if (vb.initializer instanceof MemberExpressionNode)
                    {
                        MemberExpressionNode mb = (MemberExpressionNode)(vb.initializer);
                        Slot vs = (mb.ref != null ? mb.ref.getSlot(cx, Tokens.GET_TOKEN) : null);
                        Value v = (vs != null ? vs.getValue() : null);
                        ObjectValue ov = ((v instanceof ObjectValue) ? (ObjectValue)(v) : null);
                        // if constant evaluator has determined this has a value, use it.
                        defaultValue = (ov != null) ? ov.getValue() : "unknown";
                    }
                    else
                    {
                        Slot vs = vb.ref.getSlot(cx, Tokens.GET_TOKEN);
                        Value v = (vs != null ? vs.getValue() : null);
                        ObjectValue ov = ((v instanceof ObjectValue) ? (ObjectValue)(v) : null);
                        // if constant evaluator has determined this has a value, use it.
                        defaultValue = (ov != null) ? ov.getValue() : "unknown";
                    }
                }
            }
            
            private void createMetaDataComment(String debugName, MetaDataNode meta, boolean isAttributeOfDefinition, MetaDataNode current)
            {
                if (metadata == null)
                    metadata = new ArrayList();
                CommentEntry newMetadata = new CommentEntry(debugName, meta, isAttributeOfDefinition, current);
                metadata.add(newMetadata);
            }
            
            private void processMetadata(DocCommentNode comment)
            {
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
                                        createMetaDataComment(fullname, mdi, false, metaDataComment);
                                        x++;
                                    }
                                    else  // emit it even if it doesn't have a comment.
                                    {
                                        createMetaDataComment(fullname, mdi, true, null);
                                    }
                                }
                                else
                                {
                                    createMetaDataComment(fullname, mdi, true, null);
                                }
                            }
                            else if (mdi.id.equals("Bindable") || mdi.id.equals("Deprecated") || mdi.id.equals("Exclude") || mdi.id.equals("DefaultProperty"))
                            {
                                createMetaDataComment(fullname, mdi, true, null);
                            }
                        }
                    }
                }
            }
            
            /**
             * Tries to match tagname to known tag names.
             */
            private boolean matchesAnyTag(String tagName)
            {
                return tagNames.contains(tagName);
            }
            
            /**
             * Parses out all the descriptions and tags. It leaves anything 
             * that's not found as null. processTags() also checks for certain
             * errors and reports them through the logger.
             * 
             * @param id
             */
            private void processTags(String id)
            {
                //Extracting description
                int is = id.indexOf("<description><![CDATA[");
                int ie = id.indexOf("]]></description>");
                description = id.substring(is + "<description><![CDATA[".length(), ie);
                
                //extracting @return
                int index = id.indexOf("]]></return>");
                int endCDATABefore;
                int begin;
                if (index >= 0)
                {
                    endCDATABefore = id.substring(0, index).lastIndexOf("]]>");
                    begin = id.indexOf("<return><![CDATA[", endCDATABefore) + "<return><![CDATA[".length();
                    returnTag = id.substring(begin, index);
                }
                index = id.indexOf("]]></return>", index+12);
                if (index > 0)
                    ThreadLocalToolkit.getLogger().logError("More than one @return found in " + this.fullname + ".");
                
                //extracting @param (multiple)
                index = id.indexOf("]]></param>");
                if (index >= 0)
                    paramTags = new ArrayList();
                while (index >= 0)
                {
                    endCDATABefore = id.substring(0, index).lastIndexOf("]]>");
                    begin = id.indexOf("<param><![CDATA[", endCDATABefore);
                    paramTags.add(id.substring(begin + "<param><![CDATA[".length(), index));
                    index = id.indexOf("]]></param>", index + "]]></param>".length());
                }
                //check for @inheritDoc
                index = id.indexOf("]]></inheritDoc>");
                hasInheritTag = index > 0 ? true : false;
                index = id.indexOf("]]></inheritDoc>", index+16);
                if (index > 0)
                    ThreadLocalToolkit.getLogger().logError("More than one @inheritDoc found in " + this.fullname + ".");
                
                //extracting @author tags
                index = id.indexOf("]]></author>");
                if (index >= 0)
                    authorTags = new ArrayList();
                while (index >= 0)
                {
                    endCDATABefore = id.substring(0, index).lastIndexOf("]]>");
                    begin = id.indexOf("<author><![CDATA[", endCDATABefore);
                    authorTags.add(id.substring(begin + "<author><![CDATA[".length(), index));
                    index = id.indexOf("]]></author>", index + "]]></author>".length());
                }
                
                //extracting @category
                index = id.indexOf("]]></category>");
                if (index > 0)
                {
                    endCDATABefore = id.substring(0, index).lastIndexOf("]]>");
                    begin = id.indexOf("<category><![CDATA[", endCDATABefore) + "<category><![CDATA[".length();
                    categoryTag = id.substring(begin, index);
                }
                //extracting @copy
                index = id.indexOf("]]></copy>");
                if (index > 0)
                {
                    endCDATABefore = id.substring(0, index).lastIndexOf("]]>");
                    begin = id.indexOf("<copy><![CDATA[", endCDATABefore) + "<copy><![CDATA[".length();
                    copyTag = id.substring(begin, index);
                }
                //extracting @default
                index = id.indexOf("]]></default>");
                if (index > 0)
                {
                    endCDATABefore = id.substring(0, index).lastIndexOf("]]>");
                    begin = id.indexOf("<default><![CDATA[", endCDATABefore) + "<default><![CDATA[".length();
                    defaultTag = id.substring(begin, index);
                }
                //check for @deprecated
                hasDeprecatedTag = id.indexOf("]]></deprecated>") > 0 ? true : false;
                //extracting @event
                index = id.indexOf("]]></event>");
                if (index > 0)
                {
                    endCDATABefore = id.substring(0, index).lastIndexOf("]]>");
                    begin = id.indexOf("<event><![CDATA[", endCDATABefore) + "<event><![CDATA[".length();
                    eventTag = id.substring(begin, index);
                }
                //extracting @eventType
                index = id.indexOf("]]></eventType>");
                if (index > 0)
                {
                    endCDATABefore = id.substring(0, index).lastIndexOf("]]>");
                    begin = id.indexOf("<eventType><![CDATA[", endCDATABefore) + "<eventType><![CDATA[".length();
                    eventTypeTag = id.substring(begin, index);
                }
                //extracting @example (multiple)
                index = id.indexOf("]]></example>");
                if (index >= 0)
                    exampleTags = new ArrayList();
                while (index >= 0)
                {
                    endCDATABefore = id.substring(0, index).lastIndexOf("]]>");
                    begin = id.indexOf("<example><![CDATA[", endCDATABefore);
                    exampleTags.add(id.substring(begin + "<example><![CDATA[".length(), index));
                    index = id.indexOf("]]></example>", index + "]]></example>".length());
                }
                //extracting @exampleText (multiple)
                index = id.indexOf("]]></exampleText>");
                if (index >= 0)
                    exampleTextTags = new ArrayList();
                while (index >= 0)
                {
                    endCDATABefore = id.substring(0, index).lastIndexOf("]]>");
                    begin = id.indexOf("<exampleText><![CDATA[", endCDATABefore);
                    exampleTextTags.add(id.substring(begin + "<exampleText><![CDATA[".length(), index));
                    index = id.indexOf("]]></exampleText>", index + "]]></exampleText>".length());
                }
                //extracting @excludeInherited
                index = id.indexOf("]]></excludeInherited>");
                if (index > 0)
                {
                    endCDATABefore = id.substring(0, index).lastIndexOf("]]>");
                    begin = id.indexOf("<excludeInherited><![CDATA[", endCDATABefore) + "<excludeInherited><![CDATA[".length();
                    excludeInheritedTag = id.substring(begin, index);
                }
                //extracting @helpid
                index = id.indexOf("]]></helpid>");
                if (index > 0)
                {
                    endCDATABefore = id.substring(0, index).lastIndexOf("]]>");
                    begin = id.indexOf("<helpid><![CDATA[", endCDATABefore) + "<helpid><![CDATA[".length();
                    helpidTag = id.substring(begin, index);
                }
                //extracting @import (multiple)
                index = id.indexOf("]]></import>");
                if (index >= 0)
                    importTags = new ArrayList();
                while (index >= 0)
                {
                    endCDATABefore = id.substring(0, index).lastIndexOf("]]>");
                    begin = id.indexOf("<import><![CDATA[", endCDATABefore);
                    importTags.add(id.substring(begin + "<import><![CDATA[".length(), index));
                    index = id.indexOf("]]></import>", index + "]]></import>".length());
                }
                //extracting @includeExample (multiple)
                index = id.indexOf("]]></includeExample>");
                if (index >= 0)
                    includeExampleTags = new ArrayList();
                while (index >= 0)
                {
                    endCDATABefore = id.substring(0, index).lastIndexOf("]]>");
                    begin = id.indexOf("<includeExample><![CDATA[", endCDATABefore);
                    includeExampleTags.add(id.substring(begin + "<includeExample><![CDATA[".length(), index));
                    index = id.indexOf("]]></includeExample>", index + "]]></includeExample>".length());
                }
                //extracting @internal
                index = id.indexOf("]]></internal>");
                if (index > 0)
                {
                    endCDATABefore = id.substring(0, index).lastIndexOf("]]>");
                    begin = id.indexOf("<internal><![CDATA[", endCDATABefore) + "<internal><![CDATA[".length();
                    internalTag = id.substring(begin, index);
                }
                //extracting @keyword  (maybe make this a list)
                index = id.indexOf("]]></keyword>");
                if (index > 0)
                {
                    endCDATABefore = id.substring(0, index).lastIndexOf("]]>");
                    begin = id.indexOf("<keyword><![CDATA[", endCDATABefore) + "<keyword><![CDATA[".length();
                    keywordTag = id.substring(begin, index);
                }
                //extracting @langversion
                index = id.indexOf("]]></langversion>");
                if (index > 0)
                {
                    endCDATABefore = id.substring(0, index).lastIndexOf("]]>");
                    begin = id.indexOf("<langversion><![CDATA[", endCDATABefore) + "<langversion><![CDATA[".length();
                    langversionTag = id.substring(begin, index);
                }
                //extracting @migration
                index = id.indexOf("]]></migration>");
                if (index > 0)
                {
                    endCDATABefore = id.substring(0, index).lastIndexOf("]]>");
                    begin = id.indexOf("<migration><![CDATA[", endCDATABefore) + "<migration><![CDATA[".length();
                    migrationTag = id.substring(begin, index);
                }
                //extracting @playerversion
                index = id.indexOf("]]></playerversion>");
                if (index > 0)
                {
                    endCDATABefore = id.substring(0, index).lastIndexOf("]]>");
                    begin = id.indexOf("<playerversion><![CDATA[", endCDATABefore) + "<playerversion><![CDATA[".length();
                    playerversionTag = id.substring(begin, index);
                }
                //check for @private
                hasPrivateTag = id.indexOf("]]></private>") > 0 ? true : false;
                //extracting @productversion
                index = id.indexOf("]]></productversion>");
                if (index > 0)
                {
                    endCDATABefore = id.substring(0, index).lastIndexOf("]]>");
                    begin = id.indexOf("<productversion><![CDATA[", endCDATABefore) + "<productversion><![CDATA[".length();
                    productversionTag = id.substring(begin, index);
                }
                //check for @review
                hasReviewTag = id.indexOf("]]></review>") > 0 ? true : false;
                //extracting @see (multiple)
                index = id.indexOf("]]></see>");
                if (index >= 0)
                    seeTags = new ArrayList();
                while (index >= 0)
                {
                    endCDATABefore = id.substring(0, index).lastIndexOf("]]>");
                    begin = id.indexOf("<see><![CDATA[", endCDATABefore);
                    String see = id.substring(begin + "<see><![CDATA[".length(), index);
                    if (see.indexOf('<') >= 0)
                        ThreadLocalToolkit.getLogger().logError("Do not use html in @see. Offending text: " + see + " Located at " + this.fullname + ".");
                    seeTags.add(see);
                    index = id.indexOf("]]></see>", index + "]]></see>".length());
                }
                //extracting @throws (multiple)
                index = id.indexOf("]]></throws>");
                if (index >= 0)
                    throwsTags = new ArrayList();
                while (index >= 0)
                {
                    endCDATABefore = id.substring(0, index).lastIndexOf("]]>");
                    begin = id.indexOf("<throws><![CDATA[", endCDATABefore);
                    throwsTags.add(id.substring(begin + "<throws><![CDATA[".length(), index));
                    index = id.indexOf("]]></throws>", index + "]]></throws>".length());
                }
                //extracting @tiptext
                index = id.indexOf("]]></tiptext>");
                if (index > 0)
                {
                    endCDATABefore = id.substring(0, index).lastIndexOf("]]>");
                    begin = id.indexOf("<tiptext><![CDATA[", endCDATABefore) + "<tiptext><![CDATA[".length();
                    tiptextTag = id.substring(begin, index);
                }
                //extracting @toolversion
                index = id.indexOf("]]></toolversion>");
                if (index > 0)
                {
                    endCDATABefore = id.substring(0, index).lastIndexOf("]]>");
                    begin = id.indexOf("<toolversion><![CDATA[", endCDATABefore) + "<toolversion><![CDATA[".length();
                    toolversionTag = id.substring(begin, index);
                }
                //extracting @<unknown>
                index = id.indexOf("]]></");
                while (index >= 0)
                {
                    int beginTag = index + "]]></".length();
                    int endTag = id.indexOf(">", beginTag);
                    String tagName = (id.substring(beginTag, endTag)).intern();
                    if (!matchesAnyTag(tagName))
                    {
                        if (customTags == null)
                            customTags = new LinkedHashMap();
                        endCDATABefore = id.substring(0, index).lastIndexOf("]]>");
                        String tag = "<" + tagName + "><![CDATA[";
                        begin = id.indexOf(tag, endCDATABefore) + tag.length();
                        customTags.put(tagName, id.substring(begin, index));
                    }
                    index = id.indexOf("]]></", endTag + 1);
                }
            }
            
            
            /**
             * processComment() extracts the necessary information from the
             * DocCommentNode using its helper methods. The helper methods use
             * the logic derived from the asc parser. It also processes any 
             * inheritDoc tags.
             */
            private void processComment(DocCommentNode comment)
            {
                this.key = new KeyPair("IGNORE", -1);
                
                //Extracts information (name and def type) for identifying a comment
                if (comment.def instanceof PackageDefinitionNode)
                {
                    processPackage((PackageDefinitionNode)comment.def);
                }
                else if (comment.def instanceof ClassDefinitionNode)
                {
                    ClassDefinitionNode cd = (ClassDefinitionNode)comment.def;
                    
                    if (cd.metaData.items.at(0) == comment)
                    {
                        processClassAndInterface(cd);
                    }
                }
                else if (comment.def instanceof FunctionDefinitionNode)
                {
                    processFunction((FunctionDefinitionNode)comment.def);
                }
                else if (comment.def instanceof VariableDefinitionNode)
                {
                    processField((VariableDefinitionNode)comment.def);
                }
                else
                {
                    //unsupported definition
                    this.key.name = "Unsupported";
                }
                
                if (this.key.type == -1)
                    return;
                
                //extracts @ tags.
                if (comment.id != null)
                    processTags(comment.id);
                
                //only process inheritDoc when needed
                if (!exclude && hasInheritTag)
                {
                    processInheritDoc();
                }
                
                processMetadata(comment);
            }

            /**
             * adds description and parameters/return tags if they exist to the current comment
             */
            private void addToComment(Object[] inheritDoc)
            {
                String desc = (String)inheritDoc[0];
                List para = (List)inheritDoc[1];
                String retu = (String)inheritDoc[2];
                
                //add description
                if (desc != null)
                    this.description += desc;
                //add parameters
                if (para != null)
                {
                    if (this.paramTags != null)
                    {
                        int diff = para.size() - this.paramTags.size();
                        if (diff > 0)
                        {
                            int size = this.paramTags.size();
                            for (int i = size; i < size + diff; i++)
                                this.paramTags.add(para.get(i));
                        }
                    }
                    else
                        this.paramTags = para;
                }
                //add return
                if (this.returnTag == null && retu != null)
                    this.returnTag = retu;
            }
            
            /**
             * @return Returns an Object array of a {String, List, String}
             * that represent the description, paramter tags, return tag.
             */
            public Object[] getInheritedDoc()
            {
                //only search for inherited documentation if
                //we did not search for it earlier.
                Object[] inheritDoc;
                if (hasInheritTag && exclude)
                {
                    inheritDoc = findInheritDoc(this.key);
                    if (inheritDoc != null)
                        addToComment(inheritDoc);
                }
                
                inheritDoc = new Object[3];
                inheritDoc[0] = this.getDescription();
                inheritDoc[1] = this.getParamTags();
                inheritDoc[2] = this.getReturnTag();
                return inheritDoc;
            }
            
            /**
             * Processes inheritDoc tag.
             */
            private void processInheritDoc()
            {
                //Retrieve inherited documentation
                Object[] inheritDoc = findInheritDoc(this.key);
                
                //Add it to this CommentEntry
                if (inheritDoc != null)
                    addToComment(inheritDoc);
                else
                    if (Trace.asdoc) System.out.println("Cannot find inherited documentation for: " + this.key.name);
            }
            
            /**
             * Method that returns a map of all the information 
             * derived from parsing the tags. The keys are the
             * tag names.
             */
            public Map getAllTags()
            {
                Map tags = new LinkedHashMap();
                tags.put("author", getAuthorTags());
                tags.put("category", getCategoryTag());
                tags.put("copy", getCopyTag());
                tags.put("default", getDefaultTag());
                tags.put("event", getEventTag());
                tags.put("eventType", getEventTypeTag());
                tags.put("example", getExampleTags());
                tags.put("exampleText", getExampleTextTags());
                tags.put("excludeInherited", getExcludeInheritedTag());
                tags.put("helpid", getHelpidTag());
                tags.put("import", getImportTags());
                tags.put("includeExample", getIncludeExampleTags());
                tags.put("internal", getInternalTag());
                tags.put("keyword", getKeywordTag());
                tags.put("langversion", getLangversionTag());
                tags.put("migration", getMigrationTag());
                tags.put("param", getParamTags());
                tags.put("playerversion", getPlayerversionTag());
                tags.put("productversion", getProductversionTag());
                tags.put("return", getReturnTag());
                tags.put("see", getSeeTags());
                tags.put("throws", getThrowsTags());
                tags.put("tiptext", getTiptextTag());
                tags.put("toolversion", getToolversionTag());
                tags.put("deprecated", Boolean.valueOf(hasDeprecatedTag()));
                tags.put("inheritDoc", Boolean.valueOf(hasInheritTag()));
                tags.put("private", Boolean.valueOf(hasPrivateTag()));
                tags.put("review", Boolean.valueOf(hasReviewTag()));
                tags.put("custom", getCustomTags());
                return tags;
            }
            
            public int getType()
            {
                return this.key.type;
            }
            
            public String getDescription()
            {
                return this.description;
            }
            
            public List getParamTags()
            {
                return this.paramTags;
            }
            
            public String getReturnTag()
            {
                return this.returnTag;
            }

            public String getAccess()
            {
                return this.access;
            }

            public String getArrayType_meta()
            {
                return this.arrayType_meta;
            }

            public List getAuthorTags()
            {
                return this.authorTags;
            }
            
            public String getBaseClass()
            {
                return this.baseClass;
            }
            
            public String[] getBaseclasses()
            {
                return this.baseClasses;
            }

            public String getCategoryTag()
            {
                return this.categoryTag;
            }

            public String getCopyTag()
            {
                return this.copyTag;
            }

            public Map getCustomTags()
            {
                return this.customTags;
            }

            public String getDefaultTag()
            {
                return this.defaultTag;
            }

            public String getDefaultValue()
            {
                return this.defaultValue;
            }

            public String getEnumeration_meta()
            {
                return this.enumeration_meta;
            }

            public String getEvent_meta()
            {
                return this.event_meta;
            }

            public String getEventTag()
            {
                return this.eventTag;
            }

            public String getEventTypeTag()
            {
                return this.eventTypeTag;
            }

            public List getExampleTags()
            {
                return this.exampleTags;
            }

            public List getExampleTextTags()
            {
                return this.exampleTextTags;
            }

            public String getExcludeInheritedTag()
            {
                return this.excludeInheritedTag;
            }

            public String getFormat_meta()
            {
                return this.format_meta;
            }

            public String getInherit_meta()
            {
                return this.inherit_meta;
            }

            public String getHelpidTag()
            {
                return this.helpidTag;
            }

            public List getImportTags()
            {
                return this.importTags;
            }

            public List getIncludeExampleTags()
            {
                return this.includeExampleTags;
            }
            
            public String[] getInterfaces()
            {
                return this.interfaces;
            }

            public String getInternalTag()
            {
                return this.internalTag;
            }

            public String getKeywordTag()
            {
                return this.keywordTag;
            }

            public String getKind_meta()
            {
                return this.kind_meta;
            }

            public String getLangversionTag()
            {
                return this.langversionTag;
            }

            public List getMetadata()
            {
                return this.metadata;
            }

            public String getMetadataType()
            {
                return this.metadataType;
            }

            public String getMigrationTag()
            {
                return this.migrationTag;
            }

            public String getName()
            {
                return this.key.name;
            }
            
            public String getFullname()
            {
                return this.fullname;
            }

            public String getNamespace()
            {
                return this.namespace;
            }

            public String getOwner()
            {
                return this.owner;
            }

            public String getOwnerClass()
            {
                return ownerClass;
            }

            public String getOwnerPackage()
            {
                return ownerPackage;
            }

            public String[] getParamDefaults()
            {
                return this.paramDefaults;
            }

            public String[] getParamNames() 
            {
                return this.paramNames;
            }

            public String[] getParamTypes()
            {
                return this.paramTypes;
            }

            public String getPlayerversionTag()
            {
                return this.playerversionTag;
            }

            public String getProductversionTag() 
            {
                return this.productversionTag;
            }

            public String getResultType()
            {
                return this.resultType;
            }

            public List getSeeTags() 
            {
                return this.seeTags;
            }

            public String getSourceFile()
            {
                return this.sourcefile;
            }

            public List getThrowsTags()
            {
                return this.throwsTags;
            }

            public String getTiptextTag()
            {
                return this.tiptextTag;
            }

            public String getToolversionTag()
            {
                return this.toolversionTag;
            }

            public String getType_meta()
            {
                return this.type_meta;
            }

            public String getVartype()
            {
                return this.vartype;
            }

            public boolean hasDeprecatedTag()
            {
                return this.hasDeprecatedTag;
            }

            public boolean hasInheritTag()
            {
                return this.hasInheritTag;
            }

            public boolean hasPrivateTag()
            {
                return this.hasPrivateTag;
            }

            public boolean hasReviewTag()
            {
                return this.hasReviewTag;
            }

            public boolean isConst()
            {
                return this.isConst;
            }

            public boolean isDynamic()
            {
                return this.isDynamic;
            }

            public boolean isExcluded()
            {
                return this.exclude;
            }

            public boolean isFinal()
            {
                return this.isFinal;
            }

            public boolean isOverride()
            {
                return this.isOverride;
            }

            public boolean isStatic()
            {
                return this.isStatic;
            }
        }
    }
    
    /**
     * Key to retrieve individual CommentEntry's. Composed of the
     * name of the definition associated with the comment
     * and the type of definition. Implements Comparable
     * only for equality (high and low are arbitrary). Suppose we
     * have a field called foo and a class called bar. Comparisons
     * are done with strings in this manner: the field foo would 
     * be "6foo" and the class bar would be "1bar".
     * 
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
}
