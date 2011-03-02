////////////////////////////////////////////////////////////////////////////////
//
//  ADOBE SYSTEMS INCORPORATED
//  Copyright 2004-2006 Adobe Systems Incorporated
//  All Rights Reserved.
//
//  NOTICE: Adobe permits you to use, modify, and distribute this file
//  in accordance with the terms of the license agreement accompanying it.
//
////////////////////////////////////////////////////////////////////////////////

package flex2.compiler.mxml.builder;

import flex2.compiler.CompilationUnit;
import flex2.compiler.util.CompilerMessage.CompilerError;
import flex2.compiler.mxml.Configuration;
import flex2.compiler.mxml.dom.*;
import flex2.compiler.mxml.lang.BindingHandler;
import flex2.compiler.mxml.lang.TextParser;
import flex2.compiler.mxml.lang.ValueNodeHandler;
import flex2.compiler.mxml.reflect.Type;
import flex2.compiler.mxml.reflect.TypeTable;
import flex2.compiler.mxml.rep.Array;
import flex2.compiler.mxml.rep.BindingExpression;
import flex2.compiler.mxml.rep.Model;
import flex2.compiler.mxml.rep.MxmlDocument;

import java.util.Collection;
import java.util.Iterator;

/**
 * @author Clement Wong
 */
class ArrayBuilder extends Builder
{
	ArrayBuilder(CompilationUnit unit, TypeTable typeTable, Configuration configuration, MxmlDocument document)
	{
		this(unit, typeTable, configuration, document, null, null, true, typeTable.objectType);
	}

	ArrayBuilder(CompilationUnit unit, TypeTable typeTable, Configuration configuration, MxmlDocument document,
				 Model parent, String name, boolean allowBinding, Type elementType)
	{
		super(unit, typeTable, configuration, document);
		this.elementType = elementType;
		this.parent = parent;
		this.name = name;
		this.allowBinding = allowBinding;
	}

	private ElementNodeHandler elementNodeHandler = new ElementNodeHandler();
	private ElementBindingHandler elementBindingHandler = new ElementBindingHandler();

	private Type elementType;
	private Model parent;
	private String name;
	private boolean allowBinding;
	Array array;

	public void analyze(ArrayNode node)
	{
		createArrayModel((String)node.getAttribute("id"), node.beginLine);
		processChildren(node.getChildren());
	}

	/**
	 * TODO should take array element type and use when processing text initializer, etc.
	 */
	public void createArrayModel(String id, int line)
	{
		array = new Array(document, elementType, parent, line);
        array.setParentIndex(name);
		registerModel(id, array, parent == null);
	}

	/**
	 *
	 */
	void createSyntheticArrayModel(int line)
	{
		createArrayModel(null, line);
	}

	/**
	 *
	 */
	void processChildren(Collection nodes)
	{
		CDATANode cdata = getTextContent(nodes, true);
		if (cdata != null)
		{
			processTextInitializer(cdata.image, typeTable.objectType, cdata.inCDATA, cdata.beginLine);
		}
		else
		{
			for (Iterator iter = nodes.iterator(); iter.hasNext(); )
			{
				elementNodeHandler.invoke((Node)iter.next());
			}
		}
	}

	/**
	 *
	 */
	protected class ElementNodeHandler extends ValueNodeHandler
	{
		protected void componentNode(Node node)
		{
			ComponentBuilder builder = new ComponentBuilder(unit, typeTable, configuration, document, array, false, elementBindingHandler);
			node.analyze(builder);
			builder.component.setParentIndex(array.size());
			array.addEntry(builder.component);
		}

		protected void arrayNode(ArrayNode node)
		{
			ArrayBuilder builder = new ArrayBuilder(unit, typeTable, configuration,	document, array, null, allowBinding, typeTable.objectType);
			node.analyze(builder);
			builder.array.setParentIndex(array.size());
			array.addEntry(builder.array);
		}

		protected void primitiveNode(PrimitiveNode node)
		{
			PrimitiveBuilder builder = new PrimitiveBuilder(unit, typeTable, configuration, document, false, elementBindingHandler);
			node.analyze(builder);
			array.addEntry(builder.value);
		}

		protected void xmlNode(XMLNode node)
		{
			//	TODO why not support XML nodes as array elements?
			log(node, new ElementNotSupported(node.image));
		}
        
        protected void xmlListNode(XMLListNode node)
        {
            //  TODO why not support XMLLists nodes as array elements?
            log(node, new ElementNotSupported(node.image));
        }

		protected void modelNode(ModelNode node)
		{
			//	TODO why not support Model nodes as array elements?
			log(node, new ElementNotSupported(node.image));
		}

		protected void inlineComponentNode(InlineComponentNode node)
		{
			InlineComponentBuilder builder = new InlineComponentBuilder(unit, typeTable, configuration, document, false);
			node.analyze(builder);
			array.addEntry(builder.getRValue());
		}

		protected void unknown(Node node)
		{
			log(node, new UnknownNode(node.image));
		}
	}

	/**
	 *
	 */
	public void processTextInitializer(String text, Type arrayElementType, boolean cdata, int line)
	{
		int flags = cdata ? TextParser.FlagInCDATA : 0;
		Object result = textParser.parseValue(text, typeTable.arrayType, arrayElementType, flags, line, typeTable.arrayType.getName());

		if (result != null)
		{
			if (result instanceof BindingExpression)
			{
				if (allowBinding)
				{
					BindingExpression bindingExpression = (BindingExpression)result;
					if (parent != null)
					{
						bindingExpression.setDestination(parent);
					}
					else
					{
						bindingExpression.setDestination(array);
					}

					bindingExpression.setDestinationLValue(name);
					bindingExpression.setDestinationProperty(name);
				}
				else
				{
					log(line, new BindingNotAllowed());
				}
			}
			else
			{
				//	TODO for symmetry's sake, allow <Array>[a,b,c]</Array>. (Used to error.) Can yank.
				assert result instanceof Array;
				array.setEntries(((Array)result).getEntries());
			}
		}
	}

	/**
	 * Note that we don't mind if dest == null. See comments in PrimitiveBuilder
	 */
	protected class ElementBindingHandler implements BindingHandler
	{
		public BindingExpression invoke(BindingExpression bindingExpression, Model dest)
		{
			bindingExpression.setDestination(array);
			bindingExpression.setDestinationLValue(Integer.toString(array.size()));
			bindingExpression.setDestinationProperty(array.size());
			return bindingExpression;
		}
	}

    public static class ElementNotSupported extends CompilerError
    {
        public String image;

        public ElementNotSupported(String image)
        {
            this.image = image;
        }
    }

    public static class UnknownNode extends CompilerError
    {
        public String image;

        public UnknownNode(String image)
        {
            this.image = image;
        }
    }
}
