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

package flex2.compiler.mxml.builder;

import flex2.compiler.CompilationUnit;
import flex2.compiler.mxml.Configuration;
import flex2.compiler.mxml.analyzer.WebServiceAnalyzer;
import flex2.compiler.mxml.dom.*;
import flex2.compiler.mxml.reflect.Type;
import flex2.compiler.mxml.reflect.TypeTable;
import flex2.compiler.mxml.rep.Model;
import flex2.compiler.mxml.rep.MxmlDocument;
import flex2.compiler.util.ThreadLocalToolkit;

/**
 *
 */
class WebServiceBuilder extends ComponentBuilder
{
	private static final String OPERATIONS = "operations";
	private static final String WEB_SERVICE_OPERATION = "WebServiceOperation";
	private static final String REQUEST = "request";
	private static final String NAME = "name";

	WebServiceBuilder(CompilationUnit unit, TypeTable typeTable, Configuration configuration, MxmlDocument document)
	{
		super(unit, typeTable, configuration, document, null, true, null);
		this.childNodeHandler = new WebServiceChildNodeHandler(typeTable);
	}

	private Model ops;

	public void analyze(WebServiceNode node)
	{
		new WebServiceAnalyzer(unit, configuration).analyze(node);
		if (ThreadLocalToolkit.errorCount() > 0)
		{
			return;
		}

        //	create WebService VO, register id with document
        Type type = typeTable.getType(node.getNamespace(), node.getLocalPart());

        component = new Model(document, type, node.beginLine);
        registerModel(node, component, true);

		processAttributes(node, type);

		//	create <Object/> VO for WebService.operations
        ops = new Model(document, typeTable.objectType, component, component.getXmlLineNumber());
        ops.setParentIndex(OPERATIONS);
        component.setProperty(OPERATIONS, ops);

		processChildren(node, type);
	}

	/**
	 *
	 */
	protected class WebServiceChildNodeHandler extends ComponentChildNodeHandler
	{
		public WebServiceChildNodeHandler(TypeTable typeTable)
		{
			super(typeTable);
		}

		//	<operation/> children become OperationNode instances, which come through languageNode()
		protected void languageNode()
		{
			if (child instanceof OperationNode)
			{
				addOperation(ops, (OperationNode)child);
			}
			else
			{
				super.languageNode();
			}
		}
	}

	/**
	 * add <operation/> children as members of <operations/>.
	 */
	public void addOperation(Model ops, OperationNode node)
	{
		Type type = typeTable.getType(node.getNamespace(), WEB_SERVICE_OPERATION);

		//	push parent tag VO, child tag handler
		Model ws = component;
		ComponentChildNodeHandler wsChildNodeHandler = childNodeHandler;

		//	install our own
		component = new Model(document, type, ops, node.beginLine);
		childNodeHandler = new RequestChildNodeHandler(typeTable);

		//	process child tag: first, add new VO as a property on the parent WebService
		String name = (String)node.getAttribute(NAME);
        ops.setProperty(name, component);
		component.setParentIndex(name);

		//	process attributes. note that this will use parent's attribute handler, but route to our VO in 'component'
		processAttributes(node, type);

		//	process child nodes. this will use our child tag handler and our VO
		processChildren(node, type);

		//	pop parent tag VO, child tag handler
		childNodeHandler = wsChildNodeHandler;
		component = ws;
	}

	/**
	 *
	 */
	protected class RequestChildNodeHandler extends ComponentChildNodeHandler
	{
		int requestCount = 0;

		public RequestChildNodeHandler(TypeTable typeTable)
		{
			super(typeTable);
		}

		// <request/> child will be *either* a RequestNode, which comes through languageNode(),
		// *or* (if format="literal"|"xml") an XMLNode, which comes through nestedDeclaration()

		protected void languageNode()
		{
			if (child instanceof RequestNode && requestCount == 0)
			{
				requestCount = 1;
				addRequest(component, child);
			}
			else
			{
				super.languageNode();
			}
		}

		protected void nestedDeclaration()
		{
			if (child instanceof XMLNode && requestCount == 0)
			{
				requestCount = 1;
				addRequest(component, child);
			}
			else
			{
				super.languageNode();
			}
		}
	}

	/**
	 *
	 */
	public void addRequest(Model op, Node node)
    {
        Model request;

        if (node instanceof XMLNode)
        {
            XMLBuilder builder = new XMLBuilder(unit, typeTable, configuration, document, op);
            node.analyze(builder);
            request = builder.xml;
        }
        else
        {
            ServiceRequestBuilder builder = new ServiceRequestBuilder(unit, typeTable, configuration, document, REQUEST);
            node.analyze(builder);
            request = builder.graph;

            /*
            Array argNames = new Array(typeTable, op);
            argNames.setXmlLineNumber(node.beginLine);
            op.setProperty("argumentNames", argNames);
            argNames.setParentIndex("argumentNames");

            for (Iterator propNames = request.orderedPropertyNames(); propNames.hasNext();)
            {
                String s = (String)propNames.next();
                argNames.addEntry(s);
            }
            */
        }

        op.setProperty(REQUEST, request);
        request.setParentIndex(REQUEST);
        request.setParent(op);
    }
}
