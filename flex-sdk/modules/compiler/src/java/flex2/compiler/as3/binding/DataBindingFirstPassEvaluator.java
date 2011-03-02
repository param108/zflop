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

import flash.swf.tools.as3.EvaluatorAdapter;
import flex2.compiler.CompilationUnit;
import flex2.compiler.SymbolTable;
import flex2.compiler.abc.Attributes;
import flex2.compiler.abc.Class;
import flex2.compiler.abc.MetaData;
import flex2.compiler.abc.Method;
import flex2.compiler.abc.Variable;
import flex2.compiler.as3.reflect.NodeMagic;
import flex2.compiler.as3.reflect.TypeTable;
import flex2.compiler.mxml.lang.StandardDefs;
import flex2.compiler.mxml.rep.BindingExpression;
import flex2.compiler.mxml.rep.Model;
import flex2.compiler.util.*;
import macromedia.asc.parser.*;
import macromedia.asc.semantics.ReferenceValue;
import macromedia.asc.semantics.Value;
import macromedia.asc.semantics.VariableSlot;
import macromedia.asc.util.Context;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Stack;

/**
 * This class is a rough equivalent of Flex 1.5's flex.compiler.WatcherVisitor.
 *
 * @author Paul Reilly
 * @author Matt Chotin
 */
public class DataBindingFirstPassEvaluator extends EvaluatorAdapter
{
    private static final String BINDABLE = "Bindable";
    private static final String CHANGE_EVENT = "ChangeEvent";
    private static final String NON_COMMITTING_CHANGE_EVENT = "NonCommittingChangeEvent";
    private static final String EVENT = "event";

    private boolean showBindingWarnings;

    private TypeTable typeTable;
    private Stack srcTypeStack; // Stack<String>
    private Stack argumentListStack; // Stack<ArgumentListNode>
    private Set skipInitSet; // Set<ArgumentListNode>
    private Set resetSet; // Set<ArgumentListNode>
    private MemberExpressionNode xmlMember = null;
    private boolean insideXmlExpression = false;
    private boolean insideBindingExpressionsFunction = false;
    private boolean insideCallExpression = false;
    private boolean insideGetExpression = false;
    private boolean insideSetExpression = false;
    private boolean insideArrayExpression = false;
    private LinkedList watcherList; // LinkeList<Watcher>
    private ClassDefinitionNode currentClassDefinition;
    private BindingExpression currentBindingExpression;
    /**
     * This is used for incrementing the watcher id count.
     */
    private int currentWatcherId = 0;
    private String bindingFunctionName;
    private List bindingExpressions; // List<BindingExpression>
    private Set evaluatedClasses; // Set<String>

    private DataBindingInfo dataBindingInfo;
    private List dataBindingInfoList; // List<DataBindingInfo>
    private boolean makeSecondPass = false;

    public DataBindingFirstPassEvaluator(CompilationUnit unit, TypeTable typeTable, boolean showBindingWarnings)
    {
        this.typeTable = typeTable;
        argumentListStack = new Stack();
        skipInitSet = new HashSet();
        resetSet = new HashSet();
        srcTypeStack = new Stack();
        bindingExpressions = (List) unit.getContext().getAttribute(flex2.compiler.Context.BINDING_EXPRESSIONS);
        evaluatedClasses = new HashSet();
        dataBindingInfoList = new ArrayList();
        this.showBindingWarnings = showBindingWarnings;

        setLocalizationManager(ThreadLocalToolkit.getLocalizationManager());
    }

    private boolean addBindables(Watcher watcher, List bindables)
    {
        boolean addedBindable = false;

        if (bindables != null)
        {
            Iterator bindablesIterator = bindables.iterator();

            while ( bindablesIterator.hasNext() )
            {
                MetaData metaData = (MetaData) bindablesIterator.next();
                String event = getEventName(metaData);
                if (event != null)
                {
                    watcher.addChangeEvent(event);
                }
                else
                {
                    watcher.addChangeEvent(StandardDefs.MDPARAM_PROPERTY_CHANGE);
                }
                addedBindable = true;
            }
        }

        return addedBindable;
    }

    private void checkForStaticProperty(Attributes attributes, Watcher watcher, String srcTypeName)
    {
        if ((attributes != null) && attributes.hasStatic() && (watcher instanceof PropertyWatcher))
        {
            ((PropertyWatcher) watcher).setStaticProperty(true);
            watcher.setClassName(srcTypeName);
        }
    }

    /**
     * TODO this exactly replicates BindableFirstPassEvaluator.getEventName()'s logic on MetaDataNode. Find a way to factor
     */
    private String getEventName(MetaData metaData)
    {
        String eventName = metaData.getValue(EVENT);
        if (eventName != null)
        {
            //    [Bindable( ... event="<eventname>" ... )]
            return eventName;
        }
        else if (metaData.count() == 1)
        {
            // 1. Currently, ASC builds MetaDataNodes in such a way that [Foo] and [Foo("Foo")] both result in
            // node.count() == 1 and node.getValue(0).equals("Foo"). Soooooo, best not to have an event named Bindable!
            String param = metaData.getValue(0);
            if (!param.equals(metaData.getID()))
            {
                //    [Bindable("<eventname>")]
                return param;
            }
        }
        return null;
    }

    private boolean addChangeEvents(Watcher watcher, List changeEvents)
    {
        boolean addedChangeEvent = false;

        if (changeEvents != null)
        {
            Iterator changeEventIterator = changeEvents.iterator();

            while ( changeEventIterator.hasNext() )
            {
                MetaData metaData = (MetaData) changeEventIterator.next();
                String event = metaData.getValue(0);
                if (event != null)
                {
                    watcher.addChangeEvent(event);
                    addedChangeEvent = true;
                }
            }
        }

        return addedChangeEvent;
    }

    private boolean addNonCommittingChangeEvents(Watcher watcher, List changeEvents)
    {
        boolean addedChangeEvent = false;

        if (changeEvents != null)
        {
            Iterator changeEventIterator = changeEvents.iterator();

            while ( changeEventIterator.hasNext() )
            {
                MetaData metaData = (MetaData) changeEventIterator.next();
                String event = metaData.getValue(0);
                if (event != null)
                {
                    watcher.addChangeEvent(event, false);
                    addedChangeEvent = true;
                }
            }
        }

        return addedChangeEvent;
    }

    public Value evaluate(Context context, ArgumentListNode node)
    {
        if (insideBindingExpressionsFunction && insideSetExpression)
        {
            for (int i = 0, size = node.items.size(); i < size; i++)
            {
                Node argument = (Node) node.items.get(i);

                LinkedList tempWatcherList = watcherList;

                argumentListStack.push(node);

                if (!skipInitSet.remove(node))
                {
                    watcherList = new LinkedList();
                }

                argument.evaluate(context, this);

                if (resetSet.remove(node))
                {
                    watcherList = tempWatcherList;
                }

                argumentListStack.pop();
            }
        }

        return null;
    }

    public Value evaluate(Context context, BinaryExpressionNode node)
    {
        if (node.lhs != null)
        {
            node.lhs.evaluate(context, this);
        }

        if (insideBindingExpressionsFunction && insideSetExpression)
        {
            watcherList = new LinkedList();
        }

        if (node.rhs != null)
        {
            node.rhs.evaluate(context, this);
        }

        if (insideBindingExpressionsFunction && insideSetExpression)
        {
            watcherList = new LinkedList();
        }

        return null;
    }

    public Value evaluate(Context context, CallExpressionNode node)
    {
        if (insideBindingExpressionsFunction && insideSetExpression)
        {
            if ((!node.is_new) && (node.expr != null))
            {
                argumentListStack.push(node.args);

                if (node.expr instanceof IdentifierNode)
                {
                    IdentifierNode identifier = (IdentifierNode) node.expr;

                    // If expr.name is the same as the ref.type.name.name, then this seems to
                    // be a cast.  There is no need to setup a watcher for a cast, so skip
                    // evaluating expr.
                    if ((identifier.ref != null) &&
                        (identifier.ref.getType(context) != null) &&
                        !identifier.name.equals(identifier.ref.getType(context).getName().name))
                    {
                        insideCallExpression = true;
                        node.expr.evaluate(context, this);
                        insideCallExpression = false;
                        resetSet.add(node.args);
                    }
                }
                else
                {
                    assert false : "Unexpected CallExpressionNode.expr type: " + node.expr.getClass().getName();
                }

                if (node.args != null)
                {
                    srcTypeStack.push( srcTypeStack.firstElement() );

                    node.args.evaluate(context, this);

                    srcTypeStack.pop();
                }

                argumentListStack.pop();
            }
        }

        return null;
    }

    public Value evaluate(Context context, ClassDefinitionNode node)
    {
        if (!evaluatedClasses.contains(node))
        {
            currentClassDefinition = node;
            String className = node.name.name;
            String convertedClassName = "_" + className.replace('.', '_').replace(':', '_');
            bindingFunctionName = convertedClassName + "_bindingExprs";

            String fullyQualifiedClassName = node.cframe.name.toString();
            srcTypeStack.push( TypeTable.convertName(fullyQualifiedClassName) );
            if (node.fexprs != null)
            {
                for (int i = 0, size = node.fexprs.size(); i < size; i++)
                {
                    FunctionCommonNode functionCommon = (FunctionCommonNode) node.fexprs.get(i);
                    functionCommon.evaluate(context, this);
                }
            }
            srcTypeStack.pop();

            if (dataBindingInfo != null)
            {
                dataBindingInfo.setBindingExpressions(bindingExpressions);
                dataBindingInfo.setClassName(fullyQualifiedClassName);
                dataBindingInfoList.add(dataBindingInfo);
                dataBindingInfo = null;
            }

            evaluatedClasses.add(node);
            currentClassDefinition = null;
        }

        return null;
    }

    public Value evaluate(Context context, ConditionalExpressionNode node)
    {
        if (node.condition != null)
        {
            node.condition.evaluate(context, this);
        }

        if (insideBindingExpressionsFunction && insideSetExpression)
        {
            watcherList = new LinkedList();
        }

        if (node.thenexpr != null)
        {
            node.thenexpr.evaluate(context, this);
        }

        if (insideBindingExpressionsFunction && insideSetExpression)
        {
            watcherList = new LinkedList();
        }

        if (node.elseexpr != null)
        {
            node.elseexpr.evaluate(context, this);
        }

        if (insideBindingExpressionsFunction && insideSetExpression)
        {
            watcherList = new LinkedList();
        }

        return null;
    }

    public Value evaluate(Context context, FunctionCommonNode functionCommon)
    {
        if ((functionCommon.identifier != null) &&
            functionCommon.identifier.name.equals(bindingFunctionName))
        {
            insideBindingExpressionsFunction = true;
            dataBindingInfo = new DataBindingInfo( NodeMagic.getImports(currentClassDefinition.imported_names) );
            Iterator iterator = functionCommon.body.items.iterator();

            while ( iterator.hasNext() )
            {
                Object item = iterator.next();
                if ((item instanceof MetaDataNode) && !(item instanceof DocCommentNode))
                {
                    MetaDataNode metaDataNode = (MetaDataNode) item;
                    int bindingId = Integer.parseInt( metaDataNode.getValue("id") );
                    currentBindingExpression = (BindingExpression) bindingExpressions.get(bindingId);

                    if ( iterator.hasNext() )
                    {
                        Object definition = iterator.next();
                        if (definition instanceof ExpressionStatementNode)
                        {
                            ExpressionStatementNode expressionStatement = (ExpressionStatementNode) definition;
                            watcherList = new LinkedList();
                            evaluate(context, expressionStatement);
                        }
                    }
                }
            }

            insideBindingExpressionsFunction = false;
            makeSecondPass = true;
        }
        return null;
    }

    public Value evaluate(Context context, GetExpressionNode node)
    {
        if (node.expr != null)
        {
            insideGetExpression = true;

            if (node.expr instanceof ArgumentListNode)
            {
                if (!insideXmlExpression && showBindingWarnings)
                {
                    context.localizedWarning2(node.pos(), new UnableToDetectSquareBracketChanges());
                }

                argumentListStack.push(node.expr);
                watchExpressionArray();
                argumentListStack.pop();
                resetSet.add(node.expr);
            }

            node.expr.evaluate(context, this);
            insideGetExpression = false;
        }
        return null;
    }

    public Value evaluate(Context context, IdentifierNode node)
    {
        if (insideBindingExpressionsFunction && insideSetExpression)
        {
            watchExpression(context, node, new MultiName(SymbolTable.VISIBILITY_NAMESPACES, node.name));
        }

        return null;
    }

    public Value evaluate(Context context, InvokeNode node)
    {
        if (insideBindingExpressionsFunction && insideSetExpression && insideXmlExpression)
        {
            // We get here when the data binding destination is something like:
            //
            //   xdata.(@id=='123456').@timestamp
            //
            // where xdata is an E4X expression.  The IdentifierNode for "id" is in
            // node.args and we want the XMLWatcher for "id" to be added as a child of the
            // PropertyWatcher for "xdata", so we signal to evaluate(Context,
            // ArgumentListNode) to skip initializing the watcherList.
            skipInitSet.add(node.args);
        }

        super.evaluate(context, node);

        return null;
    }

    public Value evaluate(Context context, LiteralNumberNode node)
    {
        if (insideBindingExpressionsFunction && insideSetExpression && insideGetExpression)
        {
            watchExpressionArray();
        }

        return null;
    }

    public Value evaluate(Context context, WithStatementNode node)
    {
        if (node.expr != null)
        {
            node.expr.evaluate(context, this);
        }

        LinkedList savedWatcherList = watcherList;
        boolean normalWarningMode = showBindingWarnings;
        if (insideBindingExpressionsFunction && insideSetExpression && !insideArrayExpression)
        {
            if (xmlMember != null)
            {
                Watcher watcher = watchExpressionStringAsXML(xmlMember.ref.name);
                if (watcher != null)
                {
                    String name = xmlMember.ref.name;
                    MultiName multiName = new MultiName(SymbolTable.VISIBILITY_NAMESPACES, name);
                    findEvents(context, name, multiName, xmlMember.pos(), watcher);
                }
                xmlMember = null;
            }
            if (insideXmlExpression)
            {
                watcherList = new LinkedList();
                showBindingWarnings = false;     // inside an e4x selector is the freakin' wild west..
            }
        }

        if (node.statement != null)
        {
            node.statement.evaluate(context, this);
        }
        if (insideBindingExpressionsFunction && insideSetExpression && !insideArrayExpression && insideXmlExpression)
        {
            watcherList = savedWatcherList;
            showBindingWarnings = normalWarningMode;
        }

        return null;
    }

    public Value evaluate(Context context, MemberExpressionNode node)
    {
        int pushed = 0;
        boolean oldArrayExpression = insideArrayExpression;
        insideArrayExpression = node.isIndexedMemberExpression();

        if (node.base != null)
        {
            node.base.evaluate(context, this);

            if (insideBindingExpressionsFunction && insideSetExpression && !insideArrayExpression)
            {
                ReferenceValue ref = null;

                if (node.base instanceof CallExpressionNode)
                {
                    CallExpressionNode base = (CallExpressionNode) node.base;
                    ref = base.ref;
                }
                else if (node.base instanceof MemberExpressionNode)
                {
                    MemberExpressionNode base = (MemberExpressionNode) node.base;
                    ref = base.ref;
                }
                else if (node.base instanceof GetExpressionNode)
                {
                    GetExpressionNode base = (GetExpressionNode) node.base;
                    ref = base.ref;
                }

                if ((ref != null) && (ref.slot != null))
                {
                    if ((ref.slot.getObjectValue() != null) && (!ref.slot.getObjectValue().toString().equals("")))
                    {
                        srcTypeStack.push( TypeTable.convertName( ref.slot.getObjectValue().toString() ) );
                    }
                    else if ((ref.getType(context) != null) && (!ref.getType(context).getName().toString().equals("")))
                    {
                        srcTypeStack.push( TypeTable.convertName( ref.getType(context).getName().toString() ) );
                    }
                    else if ((ref.slot.getType() != null) && (!ref.slot.getType().getName().toString().equals("")))
                    {
                        srcTypeStack.push( TypeTable.convertName( ref.slot.getType().getName().toString() ) );
                    }
                    else
                    {
                        srcTypeStack.push(null);
                    }
                }
                else if (node.base instanceof ThisExpressionNode)
                {
                    srcTypeStack.push( srcTypeStack.firstElement() );
                }
                else
                {
                    srcTypeStack.push(null);
                }

                pushed++;
            }
        }

        // Figure out if this member expression is a static reference.
        // If it is, then don't bother evaluating any further, because
        // we can't watch statics for changes.
        boolean staticReference = false;

        if (insideBindingExpressionsFunction && insideSetExpression)
        {
            ReferenceValue ref = node.ref;
            if (ref != null)
            {
                if (isStaticReference(node.selector, ref))
                {
                    staticReference = true;
                    srcTypeStack.push( TypeTable.convertName( ref.slot.getObjectValue().toString() ) );
                    pushed++;
                }

                if (ref.getType(context).getName().toString().equals("XML"))
                {
                    xmlMember = node;
                    insideXmlExpression = true;
                }
            }
        }

        if ((node.selector != null) && !staticReference)
        {
            node.selector.evaluate(context, this);
        }

        insideArrayExpression = oldArrayExpression;

        for (int i = 0; i < pushed; i++)
        {
            srcTypeStack.pop();
        }

        return null;
    }

    public Value evaluate(Context context, MetaDataNode node)
    {
        return null;
    }

    public Value evaluate(Context context, QualifiedIdentifierNode node)
    {
        if (insideBindingExpressionsFunction && insideSetExpression)
        {
            watchExpression(context, node, NodeMagic.getQName(node));
        }

        return null;
    }

    public Value evaluate(Context context, SetExpressionNode node)
    {
        if (node.expr != null)
        {
            node.expr.evaluate(context, this);
        }

        if (node.args != null)
        {
            insideSetExpression = true;
            node.args.evaluate(context, this);
            insideSetExpression = false;
            insideXmlExpression = false;
            xmlMember = null;
        }

        return null;
    }

    private void findEvents(Context context, IdentifierNode node, String name, MultiName multiName, Watcher watcher)
    {
        findEvents(context, name, multiName, node.pos(), watcher);
    }

    private void findEvents(Context context, String name, MultiName multiName, int pos, Watcher watcher)
    {
        String srcTypeName = null;

        if (watcher.isPartOfAnonObjectGraph())
        {
            srcTypeName = StandardDefs.CLASS_OBJECTPROXY;
        }
        else if (! srcTypeStack.empty())
        {
            srcTypeName = (String) srcTypeStack.peek();
        }

        Watcher parentWatcher = watcher.getParent();

        if (srcTypeName != null)
        {
            Class watchedClass = typeTable.getClass(srcTypeName);

            if (watchedClass != null)
            {
                if ( watchedClass.isSubclassOf(StandardDefs.CLASS_OBJECTPROXY) )
                {
                    watcher.setPartOfAnonObjectGraph(true);
                }
                else if (watchedClass.isSubclassOf("XMLList"))
                {
                    if ((parentWatcher == null) || !(parentWatcher instanceof XMLWatcher) && showBindingWarnings)
                    {
                        context.localizedWarning2(pos, new UnableToDetectXMLListChanges(name));
                    }
                }

                List metaData = watchedClass.getMetaData(BINDABLE, true);
                boolean foundEvents = addBindables(watcher, metaData);
                boolean foundSource = false;

                Variable variable = watchedClass.getVariable(multiName.getNamespace(), multiName.getLocalPart(), true);

                if (variable != null)
                {
                    metaData = variable.getMetaData(BINDABLE);
                    foundEvents = addBindables(watcher, metaData) || foundEvents;
                    metaData = variable.getMetaData(CHANGE_EVENT);
                    foundEvents = addChangeEvents(watcher, metaData) || foundEvents;
                    metaData = variable.getMetaData(NON_COMMITTING_CHANGE_EVENT);
                    foundEvents = addNonCommittingChangeEvents(watcher, metaData) || foundEvents;

                    Attributes attributes = variable.getAttributes();

                    // Object has a public static const variable names "length", which is
                    // some legacy compatibility crap left over from EMCA script 262, so
                    // we ignore it.
                    if ((attributes != null) && attributes.hasConst() &&
                        !(multiName.getLocalPart().equals("length") &&
                          variable.getDeclaringClass().getName().equals(SymbolTable.OBJECT)))
                    {
                        // We didn't really find any events, but we want
                        // to follow the same code path below as if we did.
                        foundEvents = true;
                        //    TODO will this ever be something besides a PropertyWatcher?
                        if (watcher instanceof PropertyWatcher)
                        {
                            ((PropertyWatcher) watcher).suppress();
                        }
                    }

                    // See comment above.
                    if ((attributes != null) &&
                        !(multiName.getLocalPart().equals("length") &&
                          variable.getDeclaringClass().getName().equals(SymbolTable.OBJECT)))
                    {
                        checkForStaticProperty(attributes, watcher, srcTypeName);
                    }

                    foundSource = true;
                }

                if (!foundEvents)
                {
                    Method getter = watchedClass.getGetter(multiName.getNamespace(), multiName.getLocalPart(), true);

                    if (getter != null)
                    {
                        metaData = getter.getMetaData(BINDABLE);
                        foundEvents = addBindables(watcher, metaData);
                        metaData = getter.getMetaData(CHANGE_EVENT);
                        foundEvents = addChangeEvents(watcher, metaData) || foundEvents;
                        metaData = getter.getMetaData(NON_COMMITTING_CHANGE_EVENT);
                        foundEvents = addNonCommittingChangeEvents( watcher, metaData) || foundEvents;

                        Attributes attributes = getter.getAttributes();
                        checkForStaticProperty(attributes, watcher, srcTypeName);

                        foundSource = true;
                    }

                    Method setter = watchedClass.getSetter(multiName.getNamespace(), multiName.getLocalPart(), true);

                    if (setter != null)
                    {
                        metaData = setter.getMetaData(BINDABLE);
                        foundEvents = addBindables(watcher, metaData) || foundEvents;
                        metaData = setter.getMetaData(CHANGE_EVENT);
                        foundEvents = addChangeEvents(watcher, metaData) || foundEvents;
                        metaData = setter.getMetaData(NON_COMMITTING_CHANGE_EVENT);
                        foundEvents = addNonCommittingChangeEvents(watcher, metaData) || foundEvents;

                        Attributes attributes = setter.getAttributes();
                        checkForStaticProperty(attributes, watcher, srcTypeName);

                        foundSource = true;
                    }
                    else
                    {
                        if (getter != null)
                        {
                            //    getters without setters are de facto const, use same bypass as above for const vars
                            foundEvents = true;
                        }
                    }
                }

                if (!foundSource)
                {
                    Method function = watchedClass.getMethod(multiName.getNamespace(), multiName.getLocalPart(), true);

                    if (function != null)
                    {
                        metaData = function.getMetaData(BINDABLE);
                        foundEvents = addBindables( watcher, metaData) || foundEvents;
                        metaData = function.getMetaData(CHANGE_EVENT);
                        foundEvents = addChangeEvents(watcher, metaData) || foundEvents;
                        metaData = function.getMetaData(NON_COMMITTING_CHANGE_EVENT);
                        foundEvents = addNonCommittingChangeEvents(watcher, metaData) || foundEvents;
                        foundSource = true;

                        if (!foundEvents && !insideCallExpression)
                        {
                            foundEvents = true;
                            //    TODO will this ever be something besides a PropertyWatcher?
                            if (watcher instanceof PropertyWatcher)
                            {
                                ((PropertyWatcher)watcher).suppress();
                            }
                        }
                    }
                }

                if ((!foundSource) && watchedClass.isSubclassOf(StandardDefs.CLASS_ABSTRACTSERVICE))
                {
                    watcher.setOperation(true);
                }
                else if (!foundEvents &&
                         !(watcher instanceof FunctionReturnWatcher) &&
                         !(watcher instanceof XMLWatcher) &&
                         !watcher.isOperation())
                {
                    /***
                     * NOTE: when we've failed to find change events for properties of untyped or Object-typed parents, we go
                     * ahead and generate code to create a runtime PropertyWatcher with no change events specified. The lack
                     * of change events tells the runtime PW to introspect RTTI to discover change events associated with the
                     * actual type of the actual value being assigned to the property.
                     * OTOH for strongly-typed properties, we still require change events to be reachable at compile time.
                     */
                    if (!(watchedClass.getName().equals(SymbolTable.OBJECT) || watchedClass.getName().equals(SymbolTable.NOTYPE)))
                    {
                        //    TODO do we still want this to be configurable?
                        if (showBindingWarnings)
                        {
                            context.localizedWarning2(pos, new UnableToDetectChanges(name));
                        }

                        //    TODO will this ever be something besides a PropertyWatcher?
                        if (watcher instanceof PropertyWatcher)
                        {
                            ((PropertyWatcher)watcher).suppress();
                        }
                    }
                }
            }
        }
        else if ((parentWatcher != null) && parentWatcher.isOperation())
        {
            watcher.addChangeEvent("resultForBinding");
        }
    }

    public List getDataBindingInfoList()
    {
        return dataBindingInfoList;
    }

    private boolean isStaticReference(SelectorNode selector, ReferenceValue referenceValue)
    {
        // Note: With a static variable reference, the selector will be a GetExpression
        // and the slot will be a VariableSlot.  With a static function call, the selector
        // will be a CallExpression and the slot will be a MethodSlot.  With a function
        // call that returns an instance of type Class, the selector will be a
        // CallExpression and the slot will be a MethodSlot.  With a cast/type conversion,
        // the selector will be a CallExpression and the slot will be a VariableSlot.
        return (((selector == null) || (selector instanceof GetExpressionNode)) && 
                (referenceValue != null) &&
                (referenceValue.slot != null) &&
                (referenceValue.slot instanceof VariableSlot) &&
                (referenceValue.slot.getType() != null) &&
                (referenceValue.slot.getType().getName().ns.toString().equals("")) &&
                (referenceValue.slot.getType().getName().name.equals("Class")) &&
                (referenceValue.slot.getObjectValue() != null));
    }

    public boolean makeSecondPass()
    {
        return makeSecondPass;
    }

    private Watcher watchIdentifier(String name)
    {
        Watcher watcher = null;

        int size = srcTypeStack.size();

        // Skip "Top Level" constants
        if (insideGetExpression &&
            (! ((size == 1) &&
                (name.equals("Infinity") ||
                 name.equals("-Infinity") ||
                 name.equals("NaN") ||
                 name.equals("undefined")))))
        {
            String src = (String) srcTypeStack.peek();

            if ((!watcherList.isEmpty() && (watcherList.getLast() instanceof XMLWatcher)) ||
                ((src != null) && ((src.equals("XML") || src.equals("XMLList")))))
            {
                watcher = watchExpressionStringAsXML(name);
                xmlMember = null;
            }
            else
            {
                watcher = watchExpressionStringAsProperty(name);
            }
        }
        // Skip "Top Level" functions
        else if (insideCallExpression &&
                 (! ((size == 1) &&
                     (name.equals("Array") ||
                      name.equals("Boolean") ||
                      name.equals("decodeURI") ||
                      name.equals("decodeURIComponent") ||
                      name.equals("encodeURI") ||
                      name.equals("encodeURIComponent") ||
                      name.equals("escape") ||
                      name.equals("int") ||
                      name.equals("isFinite") ||
                      name.equals("isNaN") ||
                      name.equals("isXMLName") ||
                      name.equals("Number") ||
                      name.equals("Object") ||
                      name.equals("parseFloat") ||
                      name.equals("parseInt") ||
                      name.equals("trace") ||
                      name.equals("uint") ||
                      name.equals("unescape") ||
                      name.equals("XML") ||
                      name.equals("XMLList")))))
        {
            watcher = watchExpressionStringAsFunction(name);
        }

        return watcher;
    }

    private void watchExpression(Context context, IdentifierNode identifier, MultiName multiName)
    {
        String name = multiName.getLocalPart();

        Watcher watcher = watchIdentifier(name);

        if ((watcher != null) && !(watcher instanceof RepeaterDataProviderWatcher))
        {
            findEvents(context, identifier, name, multiName, watcher);
        }
    }

    private void watchExpression(Context context, QualifiedIdentifierNode qualifiedIdentifier, QName qName)
    {
        String name = qName.getNamespace() + "::" + qName.getLocalPart();

        Watcher watcher = watchIdentifier(name);

        if ((watcher != null) && !(watcher instanceof RepeaterDataProviderWatcher))
        {
            MultiName multiName = new MultiName(new String[] {qName.getNamespace()}, qName.getLocalPart());
            findEvents(context, qualifiedIdentifier, name, multiName, watcher);
        }
    }

    private void watchExpressionArray()
    {
        ArrayElementWatcher watcher = new ArrayElementWatcher(currentWatcherId++,
                                                              currentBindingExpression,
                                                              (ArgumentListNode) argumentListStack.peek());

        if (!watcherList.isEmpty())
        {
            Watcher parentWatcher = (Watcher) watcherList.getLast();
            watcher.setParentWatcher(parentWatcher);
            parentWatcher.addChild(watcher);

            if (parentWatcher.isPartOfAnonObjectGraph())
            {
                watcher.setPartOfAnonObjectGraph(true);
            }
        }

        watcherList.addLast(watcher);
    }

    private FunctionReturnWatcher watchExpressionStringAsFunction(String value)
    {
        FunctionReturnWatcher watcher = new FunctionReturnWatcher(currentWatcherId++,
                                                                  currentBindingExpression,
                                                                  value,
                                                                  (ArgumentListNode) argumentListStack.peek());

        if (!watcherList.isEmpty())
        {
            Watcher parentWatcher = (Watcher) watcherList.getLast();

            if (parentWatcher instanceof RepeaterDataProviderWatcher)
            {
                RepeaterItemWatcher repeaterItemWatcher = (RepeaterItemWatcher) parentWatcher.getChild(Watcher.REPEATER_ITEM);

                if (repeaterItemWatcher == null)
                {
                    repeaterItemWatcher = new RepeaterItemWatcher(currentWatcherId++);
                    parentWatcher.addChild(repeaterItemWatcher);
                }

                repeaterItemWatcher.addChild(watcher);
                watcherList.addLast(repeaterItemWatcher);
            }
            else
            {
                watcher.setParentWatcher(parentWatcher);
                parentWatcher.addChild(watcher);
            }
        }
        else
        {
            //we want to get unique FunctionReturnWatchers in there
            dataBindingInfo.getRootWatchers().put(value + watcher.getId(), watcher);
        }

        String src = (String) srcTypeStack.peek();

        // If the top of srcTypeStack is not the document's class and
        // the watcherList is empty, then we need to set the new
        // watcher's className.
        if ((srcTypeStack.size() > 1) &&
            (src != null) &&
            (srcTypeStack.firstElement() != src) &&
            watcherList.isEmpty())
        {
            watcher.setClassName(src);
        }

        watcherList.addLast(watcher);

        return watcher;
    }

    private XMLWatcher watchExpressionStringAsXML(String value)
    {
        XMLWatcher watcher;

        if (watcherList.isEmpty())
        {
            Map rootWatchers = dataBindingInfo.getRootWatchers();

            if (rootWatchers.containsKey(value))
            {
                // See bug 159393 for a test case that gets here.
                return null;
            }
            else
            {
                watcher = new XMLWatcher(currentWatcherId++, value);
                rootWatchers.put(value, watcher);
            }
        }
        else
        {
            Watcher parentWatcher = (Watcher) watcherList.getLast();

            if (parentWatcher instanceof RepeaterDataProviderWatcher)
            {
                RepeaterItemWatcher repeaterItemWatcher = (RepeaterItemWatcher) parentWatcher.getChild(Watcher.REPEATER_ITEM);

                if (repeaterItemWatcher == null)
                {
                    repeaterItemWatcher = new RepeaterItemWatcher(currentWatcherId++);
                    parentWatcher.addChild(repeaterItemWatcher);
                }

                watcher = new XMLWatcher(currentWatcherId++, value);
                repeaterItemWatcher.addChild(watcher);
                watcherList.addLast(repeaterItemWatcher);
            }
            else
            {
                Watcher child = parentWatcher.getChild(value);

                if (child instanceof XMLWatcher)
                {
                    watcher = (XMLWatcher) child;
                }
                else
                {
                    watcher = new XMLWatcher(currentWatcherId++, value);
                    parentWatcher.addChild(watcher);
                }
            }
        }

        String src = (String) srcTypeStack.peek();

        // If the top of srcTypeStack is not the document's class and
        // the watcherList is empty, then we need to set the new
        // watcher's className.
        if ((srcTypeStack.size() > 1) &&
            (src != null) &&
            (srcTypeStack.firstElement() != src) &&
            watcherList.isEmpty())
        {
            watcher.setClassName(src);
        }

        watcherList.addLast(watcher);
        watcher.addBindingExpression(currentBindingExpression);

        return watcher;
    }

    private PropertyWatcher watchExpressionStringAsProperty(String value)
    {
        PropertyWatcher watcher = null;

        if (watcherList.isEmpty())
        {
            watcher = watchRootProperty(value);
        }
        else
        {
            Watcher parentWatcher = (Watcher) watcherList.getLast();

            if (parentWatcher instanceof RepeaterDataProviderWatcher)
            {
                RepeaterItemWatcher repeaterItemWatcher = (RepeaterItemWatcher) parentWatcher.getChild(Watcher.REPEATER_ITEM);
                if (repeaterItemWatcher == null)
                {
                    repeaterItemWatcher = new RepeaterItemWatcher(currentWatcherId++);
                    parentWatcher.addChild(repeaterItemWatcher);
                }
                else
                {
                    watcher = repeaterItemWatcher.getChild(value);
                }

                if (watcher == null)
                {
                    watcher = new PropertyWatcher(currentWatcherId++, value);
                }

                repeaterItemWatcher.addChild(watcher);
                watcherList.addLast(repeaterItemWatcher);
            }
            else
            {
                watcher = parentWatcher.getChild(value);

                if (watcher == null)
                {
                    if (currentBindingExpression.isRepeatable() &&
                        (parentWatcher instanceof PropertyWatcher) &&
                        currentBindingExpression.getRepeaterLevel(((PropertyWatcher) parentWatcher).getProperty()) >= 0)
                    {
                        if (value.equals(Watcher.CURRENT_ITEM) || value.equals(Watcher.CURRENT_INDEX))
                        {
                            watcher = parentWatcher.getChild(Watcher.DATA_PROVIDER);
                            if (watcher == null)
                            {
                                watcher = new RepeaterDataProviderWatcher(currentWatcherId++);
                                parentWatcher.addChild(watcher);
                            }
                        }
                        else
                        {
                            watcher = new PropertyWatcher(currentWatcherId++, value);
                            parentWatcher.addChild(watcher);
                        }
                    }
                    else
                    {
                        watcher = new PropertyWatcher(currentWatcherId++, value);
                        if (parentWatcher.isPartOfAnonObjectGraph())
                        {
                            watcher.setPartOfAnonObjectGraph(true);
                        }
                        else if (!parentWatcher.getShouldWriteChildren())
                        {
                            watcher.setShouldWriteChildren(false);
                        }
                        parentWatcher.addChild(watcher);
                    }
                }
            }
        }

        watcherList.addLast(watcher);
        watcher.addBindingExpression(currentBindingExpression);

        return watcher;
    }

    private PropertyWatcher watchRootProperty(String propertyName)
    {
        Map rootWatchers = dataBindingInfo.getRootWatchers();
        String key = propertyName;
        String srcType = (String) srcTypeStack.peek();
        String className = null;

        // If the top of srcTypeStack is not the document's class,
        // then we need to set the new watcher's className.
        if ((srcTypeStack.size() > 1) &&
            (srcType != null) &&
            (srcTypeStack.firstElement() != srcType))
        {
            className = srcType;
            key = className + "." + propertyName;
        }
        
        PropertyWatcher result = (PropertyWatcher) rootWatchers.get(key);

        if (result == null)
        {
            Model destination = currentBindingExpression.getDestination();

            if ((destination != null) && (destination.getRepeaterLevel() > 1))
            {
                result = new RepeaterComponentWatcher(currentWatcherId++, propertyName, destination.getRepeaterLevel());
            }
            else
            {
                result = new PropertyWatcher(currentWatcherId++, propertyName);
            }

            if (className != null)
            {
                result.setClassName(className);
            }

            rootWatchers.put(key, result);
        }

        return result;
    }

    /**
     * CompilerMessages
     */
    public class UnableToDetectChanges extends CompilerMessage.CompilerWarning
    {
        public String name;

        public UnableToDetectChanges(String name)
        {
            this.name = name;
        }
    }

    public class UnableToDetectXMLListChanges extends CompilerMessage.CompilerWarning
    {
        public String name;

        public UnableToDetectXMLListChanges(String name)
        {
            this.name = name;
        }
    }

    public class UnableToDetectSquareBracketChanges extends CompilerMessage.CompilerWarning
    {
    }
}
