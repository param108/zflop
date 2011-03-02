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
import flex2.compiler.util.CompilerMessage.CompilerError;
import flex2.compiler.util.NameFormatter;
import flex2.compiler.mxml.Configuration;
import flex2.compiler.mxml.dom.CDATANode;
import flex2.compiler.mxml.dom.ModelNode;
import flex2.compiler.mxml.dom.Node;
import flex2.compiler.mxml.lang.TextParser;
import flex2.compiler.mxml.lang.StandardDefs;
import flex2.compiler.mxml.reflect.Type;
import flex2.compiler.mxml.reflect.TypeTable;
import flex2.compiler.mxml.rep.AnonymousObjectGraph;
import flex2.compiler.mxml.rep.BindingExpression;
import flex2.compiler.mxml.rep.MxmlDocument;

/**
 * TODO haven't converted the text value parsing here. CDATANode.inCDATA is being ignored; don't know if there are other issues.
 * @author Clement Wong
 */
class ModelBuilder extends AnonymousObjectGraphBuilder
{
	ModelBuilder(CompilationUnit unit, TypeTable typeTable, Configuration configuration, MxmlDocument document)
	{
		super(unit, typeTable, configuration, document);
	}

    public void analyze(ModelNode node)
	{
        String classObjectProxy = NameFormatter.toDot(StandardDefs.CLASS_OBJECTPROXY);
        document.addImport(classObjectProxy, node.beginLine);
		Type bindingClass = typeTable.getType(StandardDefs.CLASS_OBJECTPROXY);
		if (bindingClass == null)
		{
			log(node, new ClassNotFound(classObjectProxy));
		}

		graph = new AnonymousObjectGraph(document, bindingClass, node.beginLine);

		registerModel(node, graph, true);

		if (node.getChildCount() == 1 && node.getChildAt(0) instanceof CDATANode)
		{
			CDATANode cdata = (CDATANode) node.getChildAt(0);
			if (cdata.image.length() > 0)
			{
				String bindingExprString = TextParser.getBindingExpressionFromString(cdata.image);
				if (bindingExprString != null)
				{
                    /**
                     * <mx:Model>{binding_expression}</mx:Model>
                     */
                    BindingExpression bindingExpression = new BindingExpression(bindingExprString, cdata.beginLine, document);
                    bindingExpression.setDestination(graph);
                    bindingExpression.setDestinationObjectProxy(true);
				}
				else
				{
                    /**
                     * <mx:Model>some string</mx:Model>
                     */
					log(cdata, new OnlyScalarError((String) node.getAttribute("id")));
				}
			}
		}
		else if (node.getChildCount() == 1)
		{
			/**
			 * <mx:Model>
			 * <com>
			 *     <foo>...</foo>
			 *     <bar>...</bar>
			 *     ...
			 * </com>
			 * </mx:Model>
			 */
			processChildren((Node) node.getChildAt(0), graph);
		}
		else if (node.getChildCount() > 1)
		{
			log(node, new OnlyOneRootTag());
		}
	}

    public static class ClassNotFound extends CompilerError
    {
        public String className;

        public ClassNotFound(String className)
        {
            this.className = className;
        }
    }

    public static class OnlyScalarError extends CompilerError
    {
        public String id;

        public OnlyScalarError(String id)
        {
            this.id = id;
        }
    }
    
	public static class OnlyOneRootTag extends CompilerError
	{
		public OnlyOneRootTag()
		{
			super();
		}
	}
}
