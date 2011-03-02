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

package flex2.compiler.as3.binding;

import flex2.compiler.util.CompilerMessage;
import flex2.compiler.as3.genext.GenerativeFirstPassEvaluator;
import flex2.compiler.as3.reflect.NodeMagic;
import flex2.compiler.as3.reflect.TypeTable;
import flex2.compiler.mxml.lang.StandardDefs;
import flex2.compiler.mxml.lang.TextParser;
import flex2.compiler.util.NameFormatter;
import flex2.compiler.util.QName;
import macromedia.asc.parser.*;
import macromedia.asc.semantics.Value;
import macromedia.asc.util.Context;

import java.util.*;

/**
 *
 */
public class BindableFirstPassEvaluator extends GenerativeFirstPassEvaluator
{
    private static final int BINDABLE_NONE = 0, BINDABLE_USER = 1, BINDABLE_CODEGEN_PROP = 2, BINDABLE_CODEGEN_CLASS = 3;

    private final Set metaData;

	private Set bindableClasses;
    private Set managedClasses;
    private Set evaluatedClasses;
    private Map classMap; // Map<String, BindableInfo>

    //  visitation state
    private ClassDefinitionNode currentClassNode = null;
    private BindableInfo bindableInfo;
    private Map visitedProps;

    private boolean inFunction = false;

    public BindableFirstPassEvaluator(TypeTable typeTable, Set metaData)
	{
		super(typeTable);
		this.metaData = metaData;
		classMap = new LinkedHashMap();
		evaluatedClasses = new HashSet();
	}

	/**
	 * Note: we do the classdef MetaDataNode preprocessing here, because it turns out the ProgramNode's statement list
	 * is <strong>out of order</strong>, so we often see a class's MetaDataNode only *after* seeing the class it
	 * annotates.
	 */
	public Value evaluate(Context context, ProgramNode programNode)
	{
        //  first, note any [Managed] classes - need to have these in hand for check below
        for (Iterator iter = metaData.iterator(); iter.hasNext(); )
        {
            MetaDataNode metaDataNode = (MetaDataNode)iter.next();
            if (StandardDefs.MD_MANAGED.equals(metaDataNode.id))
            {
                if (metaDataNode.def instanceof ClassDefinitionNode)
                {
                    registerManagedClass(metaDataNode.def);
                }
            }
        }

        //  now [Bindable] classes - check, then register
        for (Iterator iter = metaData.iterator(); iter.hasNext(); )
		{
			MetaDataNode metaDataNode = (MetaDataNode)iter.next();
			if (StandardDefs.MD_BINDABLE.equals(metaDataNode.id))
			{
				if (metaDataNode.def instanceof ClassDefinitionNode)
				{
					if (isManagedClass(metaDataNode.def))
					{
						context.localizedWarning2(metaDataNode.pos(), new ClassBindableUnnecessaryOnManagedClass());
					}
					else if (getEventName(metaDataNode, context) == null)
					{
						registerBindableClass((ClassDefinitionNode)metaDataNode.def);
					}
				}
			}
		}

        //  now do the standard visit
        return super.evaluate(context, programNode);
	}

	/**
	 * Check property-level [Bindable] annotations here
	 */
	public Value evaluate(Context context, MetaDataNode metaDataNode)
	{
		if (StandardDefs.MD_BINDABLE.equals(metaDataNode.id))
		{
			if (metaDataNode.def instanceof ClassDefinitionNode)
			{
				//	class-level [Bindable] done already, see evaluate(ProgramNode)
			}
			else if (metaDataNode.def instanceof FunctionDefinitionNode)
			{
				//	function [Bindable]
                FunctionDefinitionNode node = (FunctionDefinitionNode)metaDataNode.def;
                if (getEventName(metaDataNode, context) == null)
                {
                    if (inManagedClass())
                    {
                        context.localizedWarning2(node.pos(), new PropertyBindableUnnecessaryOnManagedClass());
                    }
                    else if (inBindableClass())
                    {
                        context.localizedWarning2(node.pos(), new PropertyBindableUnnecessaryOnBindableClass());
                    }
                    else
                    {
                        boolean isGetter = NodeMagic.functionIsGetter(node);
                        if (isGetter || NodeMagic.functionIsSetter(node))
                        {
                            if (checkBindableGetterSetter(context, node, false, false))
                            {
                                registerBindableGetterSetter(context, node, true, isGetter);
                            }
                        }
                        else
                        {
                            context.localizedWarning2(node.pos(), new BindableFunctionRequiresEventName());
                        }
                    }
                }
                else
                {
                    if (NodeMagic.functionIsGetter(node) || NodeMagic.functionIsSetter(node))
                    {
                        String name = NodeMagic.getFunctionName(node);

                        if (getVisitedGetterSetterBindType(name) == BINDABLE_CODEGEN_CLASS)
                        {
                            //  if class is bindable, we may have previously registered the other member of the getter/setter
                            //  pair as requiring codegen. If so, unregister it. (Note OTOH that both may be specified at
                            //  the property level.)
                            QName qname = new QName(NodeMagic.getUserNamespace(node), name);
                            unregisterBindableAccessor(qname);
                        }

                        registerVisitedGetterSetter(name, BINDABLE_USER);
                    }
                }
			}
			else if (metaDataNode.def instanceof VariableDefinitionNode)
			{
				//	var [Bindable]
				VariableDefinitionNode node = (VariableDefinitionNode)metaDataNode.def;

				if (inFunction)
				{
					context.localizedError2(node.pos(), new BindableNotAllowedInsideFunctionDefinition());
				}
				else
				{
					if (getEventName(metaDataNode, context) == null)
					{
                        if (inManagedClass())
                        {
                            context.localizedWarning2(node.pos(), new PropertyBindableUnnecessaryOnManagedClass());
                        }
						else if (inBindableClass())
						{
                            context.localizedWarning2(node.pos(), new PropertyBindableUnnecessaryOnBindableClass());
						}
						else if (checkBindableVariable(context, node, false, false))
						{
							registerBindableVariable(context, node, true);
						}
					}
				}
			}
			else
			{
                //  something unsupported marked [Bindable]
                context.localizedError2(metaDataNode.pos(), new BindableNotAllowedHere());
			}
		}

		return null;
	}

	/**
	 *
	 */
	public Value evaluate(Context context, ClassDefinitionNode node)
	{
		if (!evaluatedClasses.contains(node))
		{
			evaluatedClasses.add(node);

            try
            {
				setCurrentClass(context, node);

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

				if (bindableInfo != null)
				{
					bindableInfo.setClassName(NodeMagic.getUnqualifiedClassName(node));
					classMap.put(NodeMagic.getClassName(node), bindableInfo);

					NodeMagic.addImport(context, node, NameFormatter.toDot(StandardDefs.CLASS_EVENT));
					NodeMagic.addImport(context, node, NameFormatter.toDot(StandardDefs.CLASS_EVENTDISPATCHER));
					NodeMagic.addImport(context, node, NameFormatter.toDot(StandardDefs.INTERFACE_IEVENTDISPATCHER));
					NodeMagic.addImport(context, node, NameFormatter.toDot(StandardDefs.CLASS_PROPERTYCHANGEEVENT));
					NodeMagic.addImport(context, node, NameFormatter.toDot(StandardDefs.CLASS_BINDINGMANAGER));
					NodeMagic.addImport(context, node, NameFormatter.toDot(StandardDefs.INTERFACE_IPROPERTYCHANGENOTIFIER));
					NodeMagic.addImport(context, node, NameFormatter.toDot(StandardDefs.CLASS_OBJECTPROXY));
					NodeMagic.addImport(context, node, NameFormatter.toDot(StandardDefs.CLASS_UIDUTIL));
				}
            }
            finally
            {
                setCurrentClass(context, null);
            }
        }

		return null;
    }

    /**
     * 1. In [Bindable] classes, sweep getter/setters.
     * 2. Set in-function sentinel and visit function subtree.
     */
    public Value evaluate(Context context, FunctionDefinitionNode node)
    {
        String name = NodeMagic.getFunctionName(node);

        if (inBindableClass())
        {
            boolean isGetter = NodeMagic.functionIsGetter(node);

            if (isGetter || NodeMagic.functionIsSetter(node))
            {
                //	pick up getter/setter in [Bindable] class, unless user-specified [Bindable(...)] has already been
                // seen on 'other side'
                if (getVisitedGetterSetterBindType(name) != BINDABLE_USER)
                {
                    registerBindableGetterSetter(context, node, false, isGetter);
                }
            }
        }

        inFunction = true;
        super.evaluate(context, node);
        inFunction = false;
        return null;
    }

    /**
	 * Here we visit every VariableDefinitionNode in the program tree. If we decide to add Bindability to the variable,
	 * we add an entry to bindableInfo and set makeSecondPass true. Eligible variables are: a) in classes marked
     * [Bindable]; b) not function locals; c) not overriden by explicit [Bindable(..)] metadata; d) ok according to
     * checkBindableVariable().
	 */
	public Value evaluate(Context context, VariableDefinitionNode node)
	{
		QName qname = new QName(NodeMagic.getUserNamespace(node), NodeMagic.getVariableName(node));
		if (inBindableClass() &&
            !inFunction &&
            !isBindableAccessor(qname) &&
            checkBindableVariable(context, node, true, true))
		{
			registerBindableVariable(context, node, false);
		}

		return null;
	}

	/**
	 *
	 */
	private boolean checkBindableVariable(Context context, VariableDefinitionNode def, boolean quiet, boolean publicOnly)
	{
		//	variable outside class?
		if (!inClass())
        {
			if (!quiet)
			{
				context.localizedError2(def.pos(), new BindableNotAllowedOnGlobalOrPackageVariables());
			}
			return false;
        }

		//	const variable?
		if ((def.list != null) &&
        	(def.list.items != null) &&
            (def.list.items.size() > 0))
        {
            Object item = def.list.items.get(0);

            if (item instanceof VariableBindingNode)
            {
                VariableBindingNode variableBinding = (VariableBindingNode) item;

                if (variableBinding.kind == Tokens.CONST_TOKEN)
                {
                    if (!quiet)
                    {
                        context.localizedError2(def.pos(), new BindableNotAllowedOnConstMemberVariables());
                    }
                    return false;
                }
            }
        }

		//	non-public variable && publicsOnly?
		if (publicOnly && (def.attrs == null || !def.attrs.hasAttribute(NodeMagic.PUBLIC)))
		{
			if (!quiet)
			{
				context.localizedError2(def.pos(), new BindableNotAllowedHereOnNonPublicMemberVariables());
			}
			return false;
		}

		return true;
	}

	/**
	 *
	 */
	private boolean checkBindableGetterSetter(Context context, FunctionDefinitionNode def, boolean quiet, boolean publicOnly)
	{
		//	Note: getter/setters outside a class is nonsense code, but we're seeing the parse tree before any semantic analysis
		if (!inClass())
		{
			if (!quiet)
			{
				context.localizedError2(def.pos(), new BindableNotAllowedOnGlobalOrPackageFunctions());
			}
			return false;
		}

		//	Note: static getter/setters is nonsense code, but we're seeing the parse tree before any semantic analysis
		if (def.attrs != null && def.attrs.hasAttribute(NodeMagic.STATIC))
		{
			if (!quiet)
			{
				context.localizedError2(def.pos(), new BindableNotAllowedOnStaticFunctions());
			}
			return false;
		}

		//	non-public getter/setter && publicsOnly?
		if (publicOnly && (def.attrs == null || !def.attrs.hasAttribute(NodeMagic.PUBLIC)))
		{
			if (!quiet)
			{
				context.localizedError2(def.pos(), new BindableNotAllowedHereOnNonPublicFunctions());
			}
			return false;
		}

		return true;
	}

    /**
     *
     */
    private void setCurrentClass(Context context, ClassDefinitionNode node)
    {
        currentClassNode = node;
        visitedProps = null;

		if (isBindableClass(currentClassNode))
		{
			//	Creating bindableInfo unconditionally on [Bindable] classes ensures that they
			//	acquire bindability infrastructure, even if they lack properties.
			bindableInfo = new BindableInfo(context, typeTable.getSymbolTable());
		}
		else
		{
			bindableInfo = null;
		}
	}

    /**
     *
     */
    private boolean inClass()
    {
        return currentClassNode != null;
    }

    /**
     *
     */
    private boolean inManagedClass()
    {
        return inClass() && isManagedClass(currentClassNode);
    }

    /**
     *
     */
    private boolean inBindableClass()
    {
        return inClass() && isBindableClass(currentClassNode);
    }

	/**
	 *
	 */
	private void registerBindableClass(ClassDefinitionNode def)
	{
        (bindableClasses != null ? bindableClasses : (bindableClasses = new HashSet())).add(def);
	}

	/**
	 *
	 */
	private boolean isBindableClass(DefinitionNode def)
	{
		return bindableClasses != null && bindableClasses.contains(def);
	}

    /**
     * register a bindable variable, due either to bindable on class, or directly on the variable
     */
    private void registerBindableVariable(Context context,
                                          VariableDefinitionNode node,
                                          boolean propLevel)
    {
        if (bindableInfo == null)
        {
            bindableInfo = new BindableInfo(context, typeTable.getSymbolTable());
        }

        if ((node.attrs != null) && node.attrs.hasAttribute(NodeMagic.STATIC))
        {
            bindableInfo.setRequiresStaticEventDispatcher(true);
        }

        bindableInfo.addAccessorVariable(node, propLevel);
    }

    /**
     * register a bindable getter or setter, due either to [Bindable] on class, or directly on the function.
     * Note that code will not actually be generated unless both a getter and setter are found. If a getter with no
     * setter is marked bindable, a warning is issued (later).
     */
    private void registerBindableGetterSetter(Context context,
                                              FunctionDefinitionNode node,
                                              boolean propLevel,
                                              boolean isGetter)
    {
        if (bindableInfo == null)
        {
            bindableInfo = new BindableInfo(context, typeTable.getSymbolTable());
        }

        bindableInfo.addAccessorFunction(node, propLevel, isGetter);

        registerVisitedGetterSetter(NodeMagic.getFunctionName(node), propLevel ? BINDABLE_CODEGEN_PROP : BINDABLE_CODEGEN_CLASS);
    }

    /**
     *
     */
    private void unregisterBindableAccessor(QName qname)
    {
        if (bindableInfo != null)
        {
            bindableInfo.removeAccessor(qname);
        }
    }

    /**
	 *
	 */
	private boolean isBindableAccessor(QName qname)
	{
		return bindableInfo != null && bindableInfo.hasAccessor(qname);
	}

    /**
     *
     */
    private void registerManagedClass(DefinitionNode def)
    {
        (managedClasses != null ? managedClasses : (managedClasses = new HashSet())).add(def);
    }

    /**
     *
     */
    private boolean isManagedClass(DefinitionNode def)
    {
        return managedClasses != null && managedClasses.contains(def);
    }

	/**
	 *
	 */
	private void registerVisitedGetterSetter(String name, int bindType)
	{
        (visitedProps != null ? visitedProps : (visitedProps = new HashMap())).put(name, new Integer(bindType));
	}

	/**
	 *
	 */
	private int getVisitedGetterSetterBindType(String name)
	{
        return visitedProps == null || !visitedProps.containsKey(name) ? BINDABLE_NONE
                : ((Integer)visitedProps.get(name)).intValue();
	}

	/**
	 *
	 */
	public Map getClassMap()
	{
		return classMap != null ? classMap : Collections.EMPTY_MAP;
	}

	/**
	 *
	 */
    public boolean makeSecondPass()
    {
        return classMap != null && classMap.size() > 0;
    }

    /**
	 * Extract event name from metadata node. Note: <strong>we assume that checkBindableArgs has already been called</strong>.
	 */
	private static String getEventName(MetaDataNode node, Context context)
	{
		//	[Bindable( ... event="<eventname>" ... )]
		String eventName = node.getValue(StandardDefs.MDPARAM_BINDABLE_EVENT);
		if (eventName == null && node.count() == 1)
		{
			//	[Bindable("<eventname>")]
			eventName = node.getValue(0);
		}

		if (eventName != null && ! TextParser.isValidIdentifier(eventName))
		{
			context.localizedError2(node.pos(), new EventNameNotValid());
		}
		return eventName;
	}

    /**
     * CompilerMessages
     */
    public class ClassBindableUnnecessaryOnManagedClass extends CompilerMessage.CompilerWarning {}
    public class PropertyBindableUnnecessaryOnManagedClass extends CompilerMessage.CompilerWarning {}
    public class PropertyBindableUnnecessaryOnBindableClass extends CompilerMessage.CompilerWarning {}
    public class BindableFunctionRequiresEventName extends CompilerMessage.CompilerWarning {}

    public class BindableNotAllowedInsideFunctionDefinition extends CompilerMessage.CompilerError {}
    public class BindableNotAllowedHere extends CompilerMessage.CompilerError {}
    public class BindableNotAllowedOnGlobalOrPackageVariables extends CompilerMessage.CompilerError {}
    public class BindableNotAllowedOnConstMemberVariables extends CompilerMessage.CompilerError {}
    public class BindableNotAllowedOnStaticMemberVariables extends CompilerMessage.CompilerError {}
    public class BindableNotAllowedHereOnNonPublicMemberVariables extends CompilerMessage.CompilerError {}
    public class BindableNotAllowedOnGlobalOrPackageFunctions extends CompilerMessage.CompilerError {}
    public class BindableNotAllowedOnStaticFunctions extends CompilerMessage.CompilerError {}
    public class BindableNotAllowedHereOnNonPublicFunctions extends CompilerMessage.CompilerError {}

	public static class EventNameNotValid extends CompilerMessage.CompilerError {}

}
