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
import flex2.compiler.mxml.analyzer.HTTPServiceAnalyzer;
import flex2.compiler.mxml.dom.HTTPServiceNode;
import flex2.compiler.mxml.dom.RequestNode;
import flex2.compiler.mxml.reflect.Type;
import flex2.compiler.mxml.reflect.TypeTable;
import flex2.compiler.mxml.rep.Model;
import flex2.compiler.mxml.rep.MxmlDocument;
import flex2.compiler.util.ThreadLocalToolkit;

/**
 *
 */
class HTTPServiceBuilder extends ComponentBuilder
{
	private static final String REQUEST = "request";

	private Type type;

	HTTPServiceBuilder(CompilationUnit unit, TypeTable typeTable, Configuration configuration, MxmlDocument document)
	{
		super(unit, typeTable, configuration, document, null, true, null);
		this.childNodeHandler = new HTTPServiceChildNodeHandler(typeTable);
	}


	public void analyze(HTTPServiceNode node)
	{
		new HTTPServiceAnalyzer(unit, configuration).analyze(node);
		if (ThreadLocalToolkit.errorCount() > 0)
		{
			return;
		}

        type = typeTable.getType(node.getNamespace(), node.getLocalPart());

		component = new Model(document, type, node.beginLine);
        registerModel(node, component, true);

		processAttributes(node, type);
		processChildren(node, type);
	}

	/**
	 *
	 */
	protected class HTTPServiceChildNodeHandler extends ComponentChildNodeHandler
	{
		int requestCount = 0;

		public HTTPServiceChildNodeHandler(TypeTable typeTable)
		{
			super(typeTable);
		}

		// <request/> children become RequestNode instances, which come through languageNode()
		// Note: multiple <request/>s result 'not allowed here' error on the second one, which is good enough.	
		protected void languageNode()
		{
			if (child instanceof RequestNode && requestCount == 0)
			{
				requestCount = 1;
				addRequest((RequestNode)child);
			}
			else
			{
				super.languageNode();
			}
		}
	}

	private void addRequest(RequestNode child)
	{
		ServiceRequestBuilder builder = new ServiceRequestBuilder(unit, typeTable, configuration, document, REQUEST);
		child.analyze(builder);
		Model request = builder.graph;
		component.setProperty(REQUEST, request);
		request.setParentIndex(REQUEST);
		request.setParent(component);
	}
}
