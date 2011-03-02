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

package flex2.compiler.mxml.builder;

import flex2.compiler.CompilationUnit;
import flex2.compiler.mxml.Configuration;
import flex2.compiler.mxml.dom.ArgumentsNode;
import flex2.compiler.mxml.dom.CDATANode;
import flex2.compiler.mxml.dom.Node;
import flex2.compiler.mxml.dom.RequestNode;
import flex2.compiler.mxml.lang.TextParser;
import flex2.compiler.mxml.reflect.TypeTable;
import flex2.compiler.mxml.rep.AnonymousObjectGraph;
import flex2.compiler.mxml.rep.BindingExpression;
import flex2.compiler.mxml.rep.MxmlDocument;

/**
 * TODO haven't converted the text value parsing here. CDATANode.inCDATA is being ignored; don't know if there are other issues.
 */
public class ServiceRequestBuilder extends AnonymousObjectGraphBuilder
{
    private String requestName;
    public ServiceRequestBuilder(CompilationUnit unit, TypeTable typeTable, Configuration configuration, MxmlDocument document, String name)
    {
        super(unit, typeTable, configuration, document);
        requestName = name;
    }

    public void analyze(ArgumentsNode node)
    {
        processRequest(node);
    }

    public void analyze(RequestNode node)
    {
        processRequest(node);
    }

    public void processRequest(Node node)
	{
		graph = new AnonymousObjectGraph(document, typeTable.objectType, node.beginLine);

		if (node.getChildCount() == 1 && node.getChildAt(0) instanceof CDATANode)
		{
			/**
			 * <requestName>{binding_expression}</requestName>
			 */
			CDATANode cdata = (CDATANode) node.getChildAt(0);
			if (cdata.image.length() > 0)
			{
				String bindingExprString = TextParser.getBindingExpressionFromString(cdata.image);
				if (bindingExprString != null)
				{
					BindingExpression bexpr = new BindingExpression(bindingExprString, cdata.beginLine, document);
					bexpr.setDestination(graph);
				}
				else
				{
					log(cdata, new ModelBuilder.OnlyScalarError(requestName));
				}
			}
		}
		else
		{
			/**
			 * <requestName>
			 *     <foo>...</foo>
			 *     <bar>...</bar>
			 *     ...
			 * </requestName>
			 */
			processChildren(node, graph);
		}
	}
}
