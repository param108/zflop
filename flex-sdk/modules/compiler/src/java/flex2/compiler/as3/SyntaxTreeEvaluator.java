////////////////////////////////////////////////////////////////////////////////
//
//  ADOBE SYSTEMS INCORPORATED
//  Copyright 2004-2007 Adobe Systems Incorporated
//  All Rights Reserved.
//
//  NOTICE: Adobe permits you to use, modify, and distribute this file
//  in accordance with the terms of the license agreement accompanying it.
//
////////////////////////////////////////////////////////////////////////////////

package flex2.compiler.as3;

import flash.swf.tools.as3.EvaluatorAdapter;
import flex2.compiler.CompilationUnit;
import flex2.compiler.SymbolTable;
import flex2.compiler.as3.binding.ClassInfo;
import flex2.compiler.as3.binding.TypeAnalyzer;
import flex2.compiler.as3.reflect.MetaData;
import flex2.compiler.as3.reflect.NodeMagic;
import flex2.compiler.io.FileUtil;
import flex2.compiler.mxml.lang.FrameworkDefs;
import flex2.compiler.mxml.lang.StandardDefs;
import flex2.compiler.mxml.rep.VariableDeclaration;
import flex2.compiler.util.CompilerMessage;
import flex2.compiler.util.NameFormatter;
import flex2.compiler.util.ThreadLocalToolkit;
import macromedia.asc.parser.*;
import macromedia.asc.semantics.Value;
import macromedia.asc.util.Context;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * TODO validate metadata syntax here
 * TODO names to constants
 * @author Clement Wong
 */
public class SyntaxTreeEvaluator extends EvaluatorAdapter
{
    public SyntaxTreeEvaluator(CompilationUnit unit)
    {
        this.unit = unit;
    }

    private CompilationUnit unit;

	//	traversal state
    private String currentClassName = "";
    private ClassDefinitionNode currentClassNode = null;
//    private ProgramNode currentProgramNode = null;
    private PackageDefinitionNode lastPackageNode = null;
    private boolean seenConstructor = false;
    private Map functionMap;


    public Value evaluate(Context cx, PackageDefinitionNode node)
    {
        if (lastPackageNode == node)
            lastPackageNode = null;
        else
            lastPackageNode = null;
        return super.evaluate(cx, node);
    }


    /**
     * init current classname and seen-constructor state, for multiple-constructor test
     */
    public Value evaluate(Context cx, ClassDefinitionNode node)
    {
        ClassDefinitionNode prev = currentClassNode;
        try
        {
            assert currentClassName.equals("") : "nested classdef in SyntaxTreeEvaluator (outer='" + currentClassName + "')";
            currentClassName = NodeMagic.getUnqualifiedClassName(node);
            currentClassNode = node;
            seenConstructor = false;
            functionMap = new HashMap();

            super.evaluate(cx, node);
        }
        finally
        {
            currentClassNode = prev;
            currentClassName = "";
            seenConstructor = false;
            functionMap = null;
        }

        return null;
    }

    private FunctionDefinitionNode cur_func = null;
    /**
     * check for presence of multiple constructor definitions. Happens if constructor has been defined in <Script/>
     * CAUTION: this logic depends on the ordering of generated and <Script/> code in the InterfaceDef.vm -
     * script is assumed to *follow* generated code (or at least, the generated constructor). If that ordering
     * changes, <strong>this must be modified accordingly (save early location, etc.)</strong>.
     */
    public Value evaluate(Context cx, FunctionDefinitionNode node)
    {
        FunctionDefinitionNode old = cur_func;
        cur_func = node;
        super.evaluate(cx, node);
        cur_func = old;

        if (NodeMagic.getFunctionName(node).equals(currentClassName))
        {
            if (seenConstructor)
            {
                cx.localizedError2(
                        cx.input.origin,
                        node.pos(),
                        new MultipleConstructorDefs());

            }
            else
            {
                seenConstructor = true;
            }
        }
        else
        {
            String functionName = NodeMagic.getFunctionName(node);
            if ((functionMap != null) && (functionName != null))
            {
                switch (node.name.kind)
                {
                case Tokens.GET_TOKEN:
                    FunctionDefinitionNode setter = (FunctionDefinitionNode) functionMap.get(functionName);
                    if (setter != null)
                    {
                        processAccessorMetaData(cx, node, setter);
                    }
                    else
                    {
                        functionMap.put(functionName, node);
                    }
                    break;
                case Tokens.SET_TOKEN:
                    FunctionDefinitionNode getter = (FunctionDefinitionNode) functionMap.get(functionName);
                    if (getter != null)
                    {
                        processAccessorMetaData(cx, getter, node);
                    }
                    else
                    {
                        functionMap.put(functionName, node);
                    }
                    break;
                default:
                    break;
                }
            }
        }

        return null;
    }

    public Value evaluate(Context cx, IncludeDirectiveNode node)
    {
        super.evaluate(cx, node);

        unit.getSource().addFileInclude(FileUtil.getCanonicalPath(node.filespec.value));

        return null;
    }

    public Value evaluate(Context cx, MetaDataNode node)
    {
        if (node.data != null)
        {
            macromedia.asc.parser.MetaDataEvaluator mde = new macromedia.asc.parser.MetaDataEvaluator();
            node.evaluate(cx, mde);
        }

        unit.metadata.add(node);

        if (NodeMagic.isClassDefinition(node))
        {
            if (StandardDefs.MD_RESOURCEBUNDLE.equals(node.id))
            {
                processResourceBundle(cx, node, true);
            }
            else if (StandardDefs.MD_SWF.equals(node.id))
            {
                unit.swfMetaData = new MetaData(node);
            }
            else if (StandardDefs.MD_FRAME.equals(node.id))
            {
                processFrameMetaData(node);
            }
            else if (StandardDefs.MD_ACCESSIBILITYCLASS.equals(node.id))
            {
                // C: if "implementation" is missing, report an error here!
                unit.addAccessibilityClass(new MetaData(node));
            }
            else if (StandardDefs.MD_REMOTECLASS.equals(node.id))
            {
                processRemoteClassMetaData(cx, node);
            }
            else if (StandardDefs.MD_REQUIRESLICENSE.equals(node.id))
            {
                processLicenseMetaData(cx, node);
            }
            else if (StandardDefs.MD_MIXIN.equals(node.id))
            {
                unit.mixins.add( NodeMagic.retrieveClassName( node ) );
            }
            else if (StandardDefs.MD_ICONFILE.equals(node.id))
            {
                processIconFileMetaData(cx, node);
            }
            else if (StandardDefs.MD_EVENT.equals(node.id))
            {
                processEventMetaData(node);
            }
            else if (StandardDefs.MD_EFFECT.equals(node.id))
            {
                processEffectTriggerMetaData(node);
            }
        }
        else
        {
            if (StandardDefs.MD_RESOURCEBUNDLE.equals(node.id))
            {
                processResourceBundle(cx, node, false);
            }
            else if (StandardDefs.MD_ARRAYELEMENTTYPE.equals(node.id))
            {
                processArrayElementTypeMetaData(cx, node);
            }
            else if (StandardDefs.MD_INSTANCETYPE.equals(node.id))
            {
                processInstanceTypeMetaData(cx, node);
            }
            else if (StandardDefs.MD_PERCENTPROXY.equals(node.id))
            {
                processPercentProxyMetaData(cx, node);
            }
        }

        return null;
    }

    private boolean isFlexMetaData(String metaData)
    {
        boolean result = false;

        if (metaData.equals(StandardDefs.MD_ACCESSIBILITYCLASS) ||
            metaData.equals(StandardDefs.MD_ARRAYELEMENTTYPE) ||
            metaData.equals(StandardDefs.MD_BINDABLE) ||
            metaData.equals(StandardDefs.MD_CHANGEEVENT) ||
            metaData.equals(StandardDefs.MD_COLLAPSEWHITESPACE) ||
            metaData.equals(StandardDefs.MD_DEFAULTPROPERTY) ||
            metaData.equals(StandardDefs.MD_DEPRECATED) ||
            metaData.equals(StandardDefs.MD_EFFECT) ||
            metaData.equals(StandardDefs.MD_EMBED) ||
            metaData.equals(StandardDefs.MD_EVENT) ||
            metaData.equals(StandardDefs.MD_FRAME) ||
            metaData.equals(StandardDefs.MD_ICONFILE) ||
            metaData.equals(StandardDefs.MD_INSPECTABLE) ||
            metaData.equals(StandardDefs.MD_INSTANCETYPE) ||
            metaData.equals(StandardDefs.MD_MANAGED) ||
            metaData.equals(StandardDefs.MD_MIXIN) ||
            metaData.equals(StandardDefs.MD_NONCOMMITTINGCHANGEEVENT) ||
            metaData.equals(StandardDefs.MD_PERCENTPROXY) ||
            metaData.equals(StandardDefs.MD_REQUIRESLICENSE) ||
            metaData.equals(StandardDefs.MD_REMOTECLASS) ||
            metaData.equals(StandardDefs.MD_RESOURCEBUNDLE) ||
            metaData.equals(StandardDefs.MD_STYLE) ||
            metaData.equals(StandardDefs.MD_SWF) ||
            metaData.equals(StandardDefs.MD_TRANSIENT))
        {
            result = true;
        }

        return result;
    }

    private void processAccessorMetaData(Context cx, FunctionDefinitionNode getter,
                                         FunctionDefinitionNode setter)
    {
        List getterMetaDataList = NodeMagic.getMetaData(getter);
        List setterMetaDataList = NodeMagic.getMetaData(setter);

        Iterator getterMetaDataIterator = getterMetaDataList.iterator();

        while ( getterMetaDataIterator.hasNext() )
        {
            MetaDataNode getterMetaDataNode = (MetaDataNode) getterMetaDataIterator.next();
            if (! (getterMetaDataNode instanceof DocCommentNode))
            {
                Iterator setterMetaDataIterator = setterMetaDataList.iterator();
                while ( setterMetaDataIterator.hasNext() )
                {
					//	NOTE: MetaDataNode gives no way of iterating over name/value param pairs,
					//	so we have no way to determine whether param lists are or aren't equal.
					//	Since there are many legal cases of multiple metadata names (e.g. ChangeEvent, etc.)
					//	all we can really test, in the absence of a way of getting at the complete
					// 	param list, is 0-arg metadata.
                    MetaDataNode setterMetaDataNode = (MetaDataNode) setterMetaDataIterator.next();
                    if (! (setterMetaDataNode instanceof DocCommentNode))
                    {
                        if (getterMetaDataNode.id.equals(setterMetaDataNode.id) &&
                                isFlexMetaData(setterMetaDataNode.id) &&
                                getterMetaDataNode.count() == 0 && setterMetaDataNode.count() == 0)
                        {
                            String functionName = NodeMagic.getFunctionName(setter);
                            // Change this to an error once mx/rpc/soap/mxml/WebService removes
                            // the duplicate Deprecated metadata on serviceName.
                            cx.localizedWarning2(setterMetaDataNode.pos(),
                                    new DuplicateMetaData(currentClassName,
                                            functionName,
                                            setterMetaDataNode.id));
                        }
                }
            }
            }
        }
    }

    /**
     *
     */
    private void processInstanceTypeMetaData(Context cx, MetaDataNode node)
    {
        if (node.count() == 1)
        {
            unit.expressions.add(NameFormatter.toMultiName(node.getValue(0)));
        }
        else
        {
            cx.localizedError2(cx.input.origin, node.pos(), new InstanceTypeMustHaveType());
        }
    }

    private void processResourceBundle(Context context, MetaDataNode node, boolean onClass)
    {
        if (node.count() ==  0)
        {
            context.localizedError2(node.pos(), new RBEmptyMetadata());
        }
        else
        {
            final String name = node.getValue(0);
            // add the ResourceBundle name to the CU list so we can add it to the SWF later
            unit.resourceBundleHistory.add(name);

            if (onClass)
            {
                // e.g. [ResourceBundle(...)] public clas Foo ...
                // nothing else to do
            }
            else
            {
                // e.g. [ResourceBundle(...)] var foo
                Node def = node.def;
                if (def instanceof VariableDefinitionNode)
                {
                    if( cur_func != null )
                    {
                        // A local variable likely will not get turned into a trait, so hoist the metadata to the
                        // enclosing function so that it still appears in the abc
                        cur_func.addMetaDataNode(node);
                    }
                    VariableDefinitionNode varDefinition = (VariableDefinitionNode) def;
    
                    // We look to see if the variable is directly assigned a value.  If not, then
                    // we construct the nodes to assign it to "ResourceBundle.getResourceBundle(name)".
                    // We don't complain if the variable is already assigned a value, as this will
                    // happen if the metadata is coming from a SWC (or the user could just be doing
                    // something crazy).
                    if ((varDefinition.list != null) &&
                        (varDefinition.list.items != null) &&
                        (varDefinition.list.items.size() > 0))
                    {
                        Object item = varDefinition.list.items.get(0);
                        if (item instanceof VariableBindingNode)
                        {
                            VariableBindingNode variableBinding = (VariableBindingNode) item;
                            if (variableBinding.initializer == null)
                            {
                                String type = NodeMagic.lookupType( variableBinding );
                                // FIXME: only checking for ResourceBundle here, not mx.resources.ResourceBundle.
                                // Not sure how to check for the full name, since lookupType() returns the short name
                                // and getVariableTypeName() only returns exactly what is in the code, without
                                // regards to imports
                                // Jono: I don't think it's possible at this time (?), we'd have to
                                // be able to resolve short names with imports, might be too early?
                                if (type == null || ! type.equals("ResourceBundle"))
                                {
                                    context.localizedError2(context.input.origin, node.pos(), new NotResourceBundleType());
                                }
    
                                IdentifierNode idNode = new IdentifierNode("ResourceBundle", 0);
                                GetExpressionNode getNode = new GetExpressionNode(idNode);
                                getNode.setPosition(0);
                                MemberExpressionNode memberNode = new MemberExpressionNode(null, getNode, 0);
    
                                LiteralStringNode litNode = new LiteralStringNode(name);
                                litNode.setPosition(0);
                                ArgumentListNode argNode = new ArgumentListNode(litNode, 0);
                                MemberExpressionNode ad = new MemberExpressionNode(new MemberExpressionNode( null, new GetExpressionNode( new IdentifierNode("ApplicationDomain", 0) ), 0),
                                                                                   new GetExpressionNode( new IdentifierNode( "currentDomain", 0 )), 0);
                                argNode.items.push_back( ad );
                                IdentifierNode idNode3 = new IdentifierNode("getResourceBundle", 0);
                                CallExpressionNode callNode = new CallExpressionNode(idNode3, argNode);
    
                                variableBinding.initializer = new MemberExpressionNode(memberNode, callNode, 0);
                                NodeMagic.addImport( context, currentClassNode, NameFormatter.toDot( StandardDefs.CLASS_APPLICATIONDOMAIN ));
                            }
                        }
                    }
                }
                // if it's on a function, assume this is the metadata that we hoisted up
                else if (! (def instanceof FunctionDefinitionNode))
                {
                    // we need ResourceBundle metadata to be on a variable
                    context.localizedError2(node.pos(), new RBOnlyOnVars());
                }
            }
        }
    }

    /**
     *
     */
    private void processArrayElementTypeMetaData(Context cx, MetaDataNode node)
    {
        if (node.count() == 1)
        {
            unit.expressions.add(NameFormatter.toMultiName(node.getValue(0)));
        }
        else
        {
            cx.localizedError2(cx.input.origin, node.pos(), new ArrayElementTypeMustHaveType());
        }
    }

    /**
     *
     */
    private void processPercentProxyMetaData(Context cx, MetaDataNode node)
    {
        if (node.count() != 1)
        {
            cx.localizedError2(cx.input.origin, node.pos(), new PercentProxyMustHaveProperty());
        }
    }

    /**
     *
     */
    private void processEventMetaData(MetaDataNode node)
    {
        String typeName = node.getValue("type");
        if (typeName != null)
        {
            unit.expressions.add(NameFormatter.toMultiName(typeName));
        }
    }

    /**
     *
     */
    private void processIconFileMetaData(Context cx, MetaDataNode node)
    {
        if (unit.iconFile == null)
        {
            unit.iconFile = new MetaData(node);
        }
        else
        {
            String icon = unit.iconFile.getValue(0);
            String val = node.getValue(0);
            String sourceName = unit.getSource().getNameForReporting();
            if (icon != null && !icon.equals(val))
            {
                cx.localizedError2(cx.input.origin, node.pos(), new DuplicateIconFileMetadata(icon, val, sourceName));
            }
            else if (icon == null)
            {
                unit.iconFile = new MetaData(node);
            }
        }
    }

    /**
     *
     */
    private void processLicenseMetaData(Context cx, MetaDataNode node)
    {
        String className = NodeMagic.retrieveClassName( node );
        String packageName = (className.indexOf( ':' ) == -1)? null : className.substring( 0, className.indexOf( ':' ) );
        String id = node.getValue( "id" );
        if (id == null)
        {
            if (packageName != null)
                id = packageName;
            else
                id = className;
        }
        String handler = node.getValue( "handler" );
        if (handler == null)
        {
            if (packageName != null)
                handler = packageName + ".LicenseHandler";
            else
                handler = id + "LicenseHandler";
        }
        unit.licensedClassReqs.put( id, NodeMagic.normalizeClassName( handler ) );
    }

    /**
     *
     */
    private void processRemoteClassMetaData(Context cx, MetaDataNode node)
    {
        String className = NodeMagic.retrieveClassName( node );
        String alias = node.getValue( "alias" );
        if (alias == null)
        {
			alias = ">" + className;	// Magic, apparently.  See bug 159983.
        }
        unit.remoteClassAliases.put( className, alias );
    }

    /**
     *
     */
    private void processFrameMetaData(MetaDataNode node)
    {
        // Note: internal form of these values is package:classname
        if (node.getValue( "factoryClass" ) != null)
        {
            unit.loaderClass = NodeMagic.normalizeClassName( node.getValue( "factoryClass" ) );
        }
        if (node.getValue( "extraClass" ) != null )
        {
            unit.extraClasses.add( NodeMagic.normalizeClassName( node.getValue( "extraClass" ) ) );
        }
    }

    private void processEffectTriggerMetaData(MetaDataNode node)
    {
        String triggerName = node.getValue( "name" );
        if (triggerName == null)
        {
            triggerName = node.getValue(0);
        }

        String event = node.getValue( "event" );

        if (event == null)
        {
            event = "";
        }

        unit.effectTriggers.put(triggerName, event);
    }

    /**
     * do this after parsing. the purpose is to remove method bodies and rhs of var so as to speed up compilation.
     */
    public static void removeNonAPIContent(CompilationUnit ascUnit)
    {
		//	NOTE: only attempt what follows if everything is a-ok
        if (ThreadLocalToolkit.errorCount() > 0)
        {
            return;
        }

        ClassDefinitionNode clsdef = getFirstClassDefinition((ProgramNode) ascUnit.getSyntaxTree());
        assert clsdef != null : "could not find a class definition node...";

        flex2.compiler.Context context = ascUnit.getContext();
        macromedia.asc.util.Context cx = (macromedia.asc.util.Context) context.getAttribute("cx");

        for (int i = 0, size = (clsdef != null && clsdef.statements != null && clsdef.statements.items != null) ? clsdef.statements.items.size() : 0; i < size; i++)
        {
            Object node = clsdef.statements.items.get(i);

            if (node instanceof FunctionDefinitionNode)
            {
                FunctionDefinitionNode function = (FunctionDefinitionNode) node;

                if (function.fexpr != null && function.fexpr.body != null &&
                    function.fexpr.body.items != null)
                {
                    // C: assume that the last node is always the synthetic ReturnStatementNode.
                    NodeFactory nodeFactory = cx.getNodeFactory();
                    function.fexpr.body = nodeFactory.statementList(null, (macromedia.asc.parser.Node) function.fexpr.body.items.removeLast());

                    if (function.fexpr.signature != null && function.fexpr.signature.result != null)
                    {
                        function.fexpr.body.items.add(0, nodeFactory.list(null, nodeFactory.returnStatement(nodeFactory.literalNull())));
                    }
                }
            }
            else if (node instanceof VariableDefinitionNode)
            {
                VariableDefinitionNode var = (VariableDefinitionNode) node;

                for (int k = 0, len = var.list.items.size(); k < len; k++)
                {
                    VariableBindingNode binding = (VariableBindingNode) var.list.items.get(k);
                    binding.initializer = null;
                }
            }
        }
    }

    /**
     * return the first ClassDefinitionNode
     */
    private static ClassDefinitionNode getFirstClassDefinition(ProgramNode program)
    {
        for (int i = 0, size = (program != null && program.statements != null && program.statements.items != null) ? program.statements.items.size() : 0; i < size; i++)
        {
            Object node = program.statements.items.get(i);

            if (node instanceof ClassDefinitionNode)
            {
                return (ClassDefinitionNode) node;
            }
        }

        return null;
    }

    public static void ensureMetaDataHasDefinition(CompilationUnit compilationUnit)
    {
        ProgramNode programNode = (ProgramNode) compilationUnit.getSyntaxTree();
        flex2.compiler.Context context = compilationUnit.getContext();
        macromedia.asc.util.Context cx = (macromedia.asc.util.Context) context.getAttribute("cx");
        MetaDataEvaluator metaDataEvaluator = new MetaDataEvaluator();
        programNode.evaluate(cx, metaDataEvaluator);
    }

    /**
     * Make sure binding variables only occur once in any
     * generated class's superclass chain.  This is accomplished by assuming they are not
     * in the superclass chain.  See InterfaceDef.vm.  This is the most common case.  The
     * rare case will be that they are already are defined.  In this case, we go into the
     * syntax tree and pluck out the unnecessary variables.
     */
    public static void stripRedeclaredManagementVars(CompilationUnit ascUnit, String className, SymbolTable symbolTable)
    {
		//	NOTE: only attempt what follows if everything is a-ok
        if (ThreadLocalToolkit.errorCount() > 0)
        {
            return;
        }

        ProgramNode program = (ProgramNode) ascUnit.getSyntaxTree();
        ClassDefinitionNode classDefinitionNode = getFirstClassDefinition(program);
        assert classDefinitionNode != null : "could not find a class definition node...";

        flex2.compiler.Context context = ascUnit.getContext();
        macromedia.asc.util.Context cx = (macromedia.asc.util.Context) context.getAttribute("cx");

        TypeAnalyzer typeAnalyzer = symbolTable.getTypeAnalyzer();

        program.evaluate(cx, typeAnalyzer);

        ClassInfo classInfo = typeAnalyzer.getClassInfo(className);

        ClassInfo baseClassInfo = classInfo.getBaseClassInfo();

        if (baseClassInfo != null && !FrameworkDefs.bindingManagementVars.isEmpty())
        {
            // NOTE: take the presence of first var to imply the presence of the entire set
            if (baseClassInfo.definesVariable(((VariableDeclaration)FrameworkDefs.bindingManagementVars.get(0)).getName()))
            {
                removeVariables(classDefinitionNode, FrameworkDefs.bindingManagementVars);
            }
        }
    }

    /**
     * remove definitions of any variables whose names are found in the passed VariableDeclaration array.
     * Note: ignores type and qualifiers. Only names have to match.
     */
    private static void removeVariables(ClassDefinitionNode classDefinition, List variableDeclarations)
    {
        Iterator iterator = classDefinition.statements.items.iterator();

        while ( iterator.hasNext() )
        {
            Object node = iterator.next();

            if (node instanceof VariableDefinitionNode)
            {
                VariableDefinitionNode variableDefinition = (VariableDefinitionNode) node;

                if ((variableDefinition.list != null) &&
                    (variableDefinition.list.items != null) &&
                    (variableDefinition.list.items.size() == 1))
                {
                    Object variableNode = variableDefinition.list.items.get(0);

                    if (variableNode instanceof VariableBindingNode)
                    {
                        VariableBindingNode variableBindingNode = (VariableBindingNode) variableNode;

                        String name = variableBindingNode.variable.identifier.name;

                        for (Iterator varDeclIter = variableDeclarations.iterator(); varDeclIter.hasNext(); )
                        {
                            if (name.equals(((VariableDeclaration)varDeclIter.next()).getName()))
                            {
                                iterator.remove();
                            }
                        }
                    }
                }
            }
        }
    }

    // error messages

    public static class DuplicateIconFileMetadata extends CompilerMessage.CompilerError
    {
        public DuplicateIconFileMetadata(String icon, String val, String sourceName)
        {
            super();
            this.icon = icon;
            this.val = val;
            this.sourceName = sourceName;
        }

        public final String icon;
        public final String val;
        public final String sourceName;
    }

    public static class DuplicateMetaData extends CompilerMessage.CompilerWarning
    {
        public String declaringClass;
        public String setter;
        public String metaData;

        public DuplicateMetaData(String declaringClass, String setter, String metaData)
        {
            this.declaringClass = declaringClass;
            this.setter = setter;
            this.metaData = metaData;
        }
    }

    public static class NotResourceBundleType extends CompilerMessage.CompilerError {}

    public static class RemoteClassRequiresAlias extends CompilerMessage.CompilerError {}

    public static class ArrayElementTypeMustHaveType extends CompilerMessage.CompilerError {}

    public static class InstanceTypeMustHaveType extends CompilerMessage.CompilerError {}

    public static class PercentProxyMustHaveProperty extends CompilerMessage.CompilerError {}

    public static class MultipleConstructorDefs extends CompilerMessage.CompilerError {}

    public static class RBOnlyOnVars extends CompilerMessage.CompilerError {}

    public static class RBEmptyMetadata extends CompilerMessage.CompilerError {}
}