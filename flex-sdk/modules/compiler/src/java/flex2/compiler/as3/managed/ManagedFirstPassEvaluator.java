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

package flex2.compiler.as3.managed;

import flex.messaging.config.ServicesDependencies;
import flex2.compiler.as3.genext.GenerativeFirstPassEvaluator;
import flex2.compiler.as3.reflect.NodeMagic;
import flex2.compiler.as3.reflect.TypeTable;
import flex2.compiler.mxml.lang.StandardDefs;
import flex2.compiler.util.CompilerMessage;
import flex2.compiler.util.NameFormatter;
import flex2.compiler.util.QName;
import macromedia.asc.parser.*;
import macromedia.asc.semantics.Value;
import macromedia.asc.util.Context;

import java.util.*;

/**
 * @author Paul Reilly
 */
public class ManagedFirstPassEvaluator extends GenerativeFirstPassEvaluator
{
	private final Set metaData;

	private ManagedClassInfo currentInfo;	//	current class info, as we traverse subtree
    
	private Map managedClasses; 	// Map<String, ManagedInfo> or null

    private Map managedProperties;  // Map<defnode, ManagedClassInfo> used to set prop-level modes and detect wayward [Managed] tags

    private ServicesDependencies servicesDependencies;

	/**
	 *
	 */
	public ManagedFirstPassEvaluator(TypeTable typeTable, Set metaData)
	{
		this(typeTable, metaData, null);
	}

    public ManagedFirstPassEvaluator(TypeTable typeTable, Set metaData, ServicesDependencies servicesDependencies)
    {
        super(typeTable);
        this.metaData = metaData;
        this.servicesDependencies = servicesDependencies;
    }

	/**
	 *
	 */
	public Map getClassMap()
    {
        return managedClasses != null ? managedClasses : Collections.EMPTY_MAP;
    }

	/**
	 *
	 */
    public boolean makeSecondPass()
    {
        return managedClasses != null;
    }

	/**
	 * Note: the standard depth-first traversal is *not* performed. We do everything here,
     * to force a predictable evaluation order (all annotated classes, then all annotated members).
	 */
	public Value evaluate(Context context, ProgramNode programNode)
	{
        //  class metadata first...
        for (Iterator iter = metaData.iterator(); iter.hasNext(); )
		{
            MetaDataNode metaDataNode = (MetaDataNode)iter.next();
			if (StandardDefs.MD_MANAGED.equals(metaDataNode.id))
			{
                if (metaDataNode.def instanceof ClassDefinitionNode)
				{
                    ClassDefinitionNode classDef = (ClassDefinitionNode)metaDataNode.def;

                    currentInfo = new ManagedClassInfo(context,
                            typeTable.getSymbolTable(),
                            NodeMagic.getClassName(classDef));

                    String destination = metaDataNode.getValue(StandardDefs.MDPARAM_DESTINATION);
                    registerLazyAssociations(destination, metaDataNode, classDef, context);

                    evaluate(context, classDef);

                    currentInfo = null;
                }
            }
		}

        //  ...then properties
        for (Iterator iter = metaData.iterator(); iter.hasNext(); )
		{
            MetaDataNode metaDataNode = (MetaDataNode)iter.next();
            if (StandardDefs.MD_MANAGED.equals(metaDataNode.id))
            {
                if (metaDataNode.def instanceof ClassDefinitionNode)
                {
                    //  skip
                }
                else if (metaDataNode.def instanceof VariableDefinitionNode)
                {
                    VariableDefinitionNode varNode = (VariableDefinitionNode)metaDataNode.def;
                    ManagedClassInfo classInfo = getClassOfManagedMember(varNode);

                    if (classInfo != null)
                    {
                        setPropertyMode(context, metaDataNode, classInfo,
                            new QName(NodeMagic.getUserNamespace(varNode), NodeMagic.getVariableName(varNode)));
                    }
                    else
                    {
                        context.localizedWarning2(metaDataNode.pos(), new ManagedOnNonClassError());
                    }
                }
                else if (metaDataNode.def instanceof FunctionDefinitionNode)
                {
                    FunctionDefinitionNode node = (FunctionDefinitionNode)metaDataNode.def;
                    ManagedClassInfo classInfo = getClassOfManagedMember(node);

                    if (classInfo != null)
                    {
                        setPropertyMode(context, metaDataNode, classInfo,
                            new QName(NodeMagic.getUserNamespace(node), NodeMagic.getFunctionName(node)));
                    }
                    else
                    {
                        context.localizedWarning2(metaDataNode.pos(), new ManagedOnNonClassError());
                    }
                }
                else
                {
                    context.localizedWarning2(metaDataNode.pos(), new ManagedOnNonClassError());
                }
            }
        }

        //  now prune manual mode properties
        if (managedClasses != null)
        {
             for (Iterator iter = managedClasses.values().iterator(); iter.hasNext(); )
             {
                 ManagedClassInfo info = (ManagedClassInfo)iter.next();
                 Map accessors = info.getAccessors();

                 if (accessors != null)
                 {
                     for (Iterator propIter = accessors.entrySet().iterator(); propIter.hasNext(); )
                     {
                         Map.Entry entry = (Map.Entry)propIter.next();
                         QName propQName = (QName)entry.getKey();
                         int mode = info.getPropertyMode(propQName);

                         if (mode == ManagedClassInfo.MODE_MANUAL)
                         {
                             propIter.remove();
                         }
                     }
                 }
             }
        }

        return null;
    }

    /**
     * Note that only [Managed] classdef nodes and their descendants are visited, via metadata loop in
     * evaluate(ProgramNode)
     */
    public Value evaluate(Context context, ClassDefinitionNode node)
    {
        registerManagedClass(currentInfo);

        addManagedImports(context, node);

        if (node.statements != null)
        {
            if (node.instanceinits != null)
            {
                //	visit instance variable initializers
                Iterator iterator = node.instanceinits.iterator();

                while (iterator.hasNext())
                {
                    Node instanceinit = (Node) iterator.next();
                    instanceinit.evaluate(context, this);
                }
            }

            //	visit all statements within the classdef
            node.statements.evaluate(context, this);
        }

        return null;
    }

	/**
	 * Note that only function defs in [Managed] classes are visited
	 */
	public Value evaluate(Context context, FunctionDefinitionNode node)
	{
		boolean isGetter = NodeMagic.functionIsGetter(node);
		if ((isGetter || NodeMagic.functionIsSetter(node))  && canManage(node))
		{
			assert currentInfo != null : "currentInfo == null";

            currentInfo.addAccessorFunction(node, false, isGetter);

            registerManagedProperty(node, currentInfo);
        }

		return null;
	}

	/**
	 * Note that only variable defs in [Managed] classes are visited
	 */
	public Value evaluate(Context context, VariableDefinitionNode node)
	{
		if (canManage(node))
		{
			assert currentInfo != null : "currentInfo == null";

			currentInfo.addAccessorVariable(node, false);

            registerManagedProperty(node, currentInfo);
		}

		return null;
	}

	/**
	 * TODO see paul's note about hasAttribute(CONST) in flex2.as3.reflect.Variable - need to change this?
	 */
	private static boolean canManage(DefinitionNode def)
	{
		return def.attrs != null &&
				def.attrs.hasAttribute(NodeMagic.PUBLIC) &&
				!def.attrs.hasAttribute(NodeMagic.STATIC) &&
				!def.attrs.hasAttribute(NodeMagic.CONST);
	}

    /**
     * FDS DataService destinations require configuration to describe properties that 
     * represent associations between one or more destinations. The developer can also 
     * configure how the data manager should handle these associations. If an association
     * is marked as a "lazy" association, the data manager should not send the values of the
     * associated instances as they are handled by the other destination. This helps reduce
     * the amount of information that needs to be transmitted when a change is made to
     * a particular destination. During [Managed] code-gen this evaluator will add [Transient]
     * metadata to any property that is a lazy association to stop that property from being
     * serialized.
     * 
     * @param destination
     * @param node
     * @param context
     */
    private void registerLazyAssociations(String destination, MetaDataNode node, ClassDefinitionNode classNode, Context context)
    {
        // Check destination="xyz" was specified for [Managed] metadata
        if (destination != null)
        {
            // Check --services was specified.
            if (servicesDependencies == null)
            {
                context.localizedWarning2(node.pos(), new LazyAssociationsRequireServicesConfiguration(classNode.name.name));
            }
            else
            {
                Set lazyAssociations = servicesDependencies.getLazyAssociations(destination);
                currentInfo.setTransientProperties(lazyAssociations);
            }
        }
    }

	/**
	 *
	 */
	private void registerManagedClass(ManagedClassInfo info)
	{
		if (managedClasses == null)
		{
			managedClasses = new LinkedHashMap();
		}

		managedClasses.put(info.getClassName(), info);
	}

    /**
     *
     */
    private void registerManagedProperty(DefinitionNode node, ManagedClassInfo classInfo)
    {
        if (managedProperties == null)
        {
            managedProperties = new HashMap();
        }

        managedProperties.put(node, classInfo);
    }

    /**
     *
     */
    private ManagedClassInfo getClassOfManagedMember(DefinitionNode node)
    {
        return managedProperties == null ? null : (ManagedClassInfo)managedProperties.get(node);
    }

    /**
	 *
	 */
	private void addManagedImports(Context context, ClassDefinitionNode node)
	{
		NodeMagic.addImport(context, node, NameFormatter.toDot(StandardDefs.CLASS_EVENT));
		NodeMagic.addImport(context, node, NameFormatter.toDot(StandardDefs.CLASS_EVENTDISPATCHER));
		NodeMagic.addImport(context, node, NameFormatter.toDot(StandardDefs.CLASS_PROPERTYCHANGEEVENT));
		NodeMagic.addImport(context, node, NameFormatter.toDot(StandardDefs.CLASS_MANAGED));
		NodeMagic.addImport(context, node, NameFormatter.toDot(StandardDefs.CLASS_UIDUTIL));
		NodeMagic.addImport(context, node, NameFormatter.toDot(StandardDefs.INTERFACE_IMANAGED));
		NodeMagic.addImport(context, node, NameFormatter.toDot(StandardDefs.NAMESPACE_MX_INTERNAL));
	}

    /**
     *
     */
    private void setPropertyMode(Context context, MetaDataNode node, ManagedClassInfo classInfo, QName propertyQName)
    {
        int mode = modeFromString(context, node);

        //  Note that we record all (valid) modes at this stage, for conflict detection.
        //  Defaults etc. will be pruned later.
        if (mode != ManagedClassInfo.MODE_INVALID)
        {
            if (classInfo.hasExplicitMode(propertyQName) && classInfo.getPropertyMode(propertyQName) != mode)
            {
                context.localizedError2(node.pos(), new ManagedModeConflictError());
            }
            else
            {
                classInfo.setPropertyMode(propertyQName, mode);
            }
        }
    }

    /**
     *
     */
    private int modeFromString(Context context, MetaDataNode node)
    {
        String mode = node.getValue(StandardDefs.MDPARAM_MODE);

        if (mode == null || mode.equals(StandardDefs.MDPARAM_MANAGED_MODE_HIERARCHICAL))
        {
            return ManagedClassInfo.MODE_HIER;
        }
        else if (mode.equals(StandardDefs.MDPARAM_MANAGED_MODE_ASSOCIATION))
        {
            return ManagedClassInfo.MODE_ASSOC;
        }
        else if (mode.equals(StandardDefs.MDPARAM_MANAGED_MODE_MANUAL))
        {
            return ManagedClassInfo.MODE_MANUAL;
        }
        else
        {
            context.localizedError2(node.pos(), new InvalidManagedModeError(mode));
            return ManagedClassInfo.MODE_INVALID;
        }
    }

    /**
	 * CompilerMessages
	 */
	public static class ManagedOnNonClassError extends CompilerMessage.CompilerError {}

    public static class InvalidManagedModeError extends CompilerMessage.CompilerError
    {
        public String mode;
        public InvalidManagedModeError(String mode) { this.mode = mode; }
    }

    public static class ManagedModeConflictError extends CompilerMessage.CompilerError {}

    public static class LazyAssociationsRequireServicesConfiguration extends CompilerMessage.CompilerWarning
    {
        public String className;
        public LazyAssociationsRequireServicesConfiguration(String className) { this.className = className; }
    }
}
