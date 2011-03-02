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
import flex2.compiler.util.CompilerMessage.CompilerError;
import flex2.compiler.util.CompilerMessage.CompilerWarning;
import flex2.compiler.SymbolTable;
import flex2.compiler.mxml.Configuration;
import flex2.compiler.mxml.dom.CDATANode;
import flex2.compiler.mxml.dom.Node;
import flex2.compiler.mxml.reflect.TypeTable;
import flex2.compiler.mxml.reflect.Type;
import flex2.compiler.mxml.rep.*;
import flex2.compiler.util.IntegerPool;
import flex2.compiler.util.NameFormatter;
import flex2.compiler.util.QName;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * TODO port to new setup
 * TODO the primitive-with-properties thing doesn't work in AVM+. Blocked in codegen but must error here also.
 */
public class AnonymousObjectGraphBuilder extends Builder
{
    AnonymousObjectGraph graph;

    AnonymousObjectGraphBuilder(CompilationUnit unit, TypeTable typeTable, Configuration configuration, MxmlDocument document)
    {
        super(unit, typeTable, configuration, document);
    }

    private Object processNode(Node node)
    {
        if (node.getChildCount() == 1 && node.getChildAt(0) instanceof CDATANode)
        {
            CDATANode cdata = (CDATANode)node.getChildAt(0);
            if (cdata.image.length() > 0)
            {
				Object value = textParser.parseValue(cdata.image, typeTable.objectType, 0,
						cdata.beginLine, NameFormatter.toDot(graph.getType().getName()));

				if (value instanceof BindingExpression)
				{
                    if (hasAttributeInitializers(node))
                    {
                        log(cdata, new HasAttributeInitializers());
                        return null;
                    }
                    else
                    {
                        return value;
                    }
                }
                else
                {
                    boolean isPrimitive = (value instanceof String) || (value instanceof Number) || (value instanceof Boolean);
                    if (node.getAttributeCount() > 0 && isPrimitive)
                    {
                        //turn it into a Primitive with properties
                        Primitive p = new Primitive(document, getPrimitiveType(typeTable, value), value, cdata.beginLine);
                        for (Iterator i = node.getAttributeNames(); i.hasNext();)
                        {
                            QName propName = (QName)i.next();
                            p.setProperty(propName.getLocalPart(), node.getAttribute(propName), node.getLineNumber(propName));
                        }
                        value = p;
                    }
                    return value;
                }
            }
            else
            {
                // do nothing if the cdata node has nothing...
                return null;
            }
        }
        else
        {
            Model model = new Model(document, graph.getType(), node.beginLine);
            model.setId(node.getLocalPart(), false);
            model.setIsAnonymous(true);

            processAttributes(node, model);
            processChildren(node, model);

            return model;
        }
    }

    protected void processChildren(Node node, Model parent)
    {
        Map arrays = createArrays(parent, countChildren(node));

        for (int i = 0, count = node.getChildCount(); i < count; i++)
        {
            Node child = (Node)node.getChildAt(i);
            if (child instanceof CDATANode)
            {
                // C: ignore CDATANode if other XML elements exist...
                log(child, new IgnoringCDATA(child.image));
                continue;
            }

            String namespaceURI = child.getNamespace();
            String localPart = child.getLocalPart();

            // C: move this check to Grammar.jj or SyntaxAnalyzer
            if (SymbolTable.OBJECT.equals(localPart) && namespaceURI.length() == 0)
            {
                log(child, new ObjectTag());
                continue;
            }

            Object value = processNode(child);

            if (value == null)
            {
                //	continue;
            }
            else if (arrays.containsKey(localPart))
            {
                Array arrayVal = (Array)arrays.get(localPart);
                if (value instanceof BindingExpression)
                {
                    BindingExpression bexpr = (BindingExpression)value;
                    bexpr.setDestination(arrayVal);
                    bexpr.setDestinationProperty(arrayVal.size());
                    bexpr.setDestinationLValue(Integer.toString(arrayVal.size()));
                }
                else if (value instanceof Model)
                {
                    Model valueModel = (Model)value;
                    valueModel.setParent(arrayVal);
                    valueModel.setParentIndex(arrayVal.size());
                }

                arrayVal.addEntry(value, child.beginLine);
            }
            else
            {
                if (value instanceof BindingExpression)
                {
                    BindingExpression bexpr = (BindingExpression)value;
                    bexpr.setDestination(parent);
                    bexpr.setDestinationProperty(localPart);
                    bexpr.setDestinationLValue(localPart);
                }
                else if (value instanceof Model)
                {
                    Model valueModel = (Model)value;
                    valueModel.setParent(parent);
                }

                parent.setProperty(localPart, value, child.beginLine);
            }
        }

        for (int i = 0, count = node.getChildCount(); i < count; i++)
        {
            Node child = (Node)node.getChildAt(i);
            if (child instanceof CDATANode)
            {
                continue;
            }

            String namespaceURI = child.getNamespace();
            String localPart = child.getLocalPart();
            if (SymbolTable.OBJECT.equals(localPart) && namespaceURI.length() == 0)
            {
                continue;
            }

            if (arrays.containsKey(localPart))
            {
                Array arrayVal = (Array)arrays.get(localPart);
                parent.setProperty(localPart, arrayVal);
            }
        }
    }

    private void processAttributes(Node node, Model model)
    {
        for (Iterator i = node.getAttributeNames(); i != null && i.hasNext();)
        {
            QName qname = (QName)i.next();
			String text = (String)node.getAttribute(qname);
            String localPart = qname.getLocalPart();
			int line = node.getLineNumber(qname);

			processDynamicPropertyText(localPart, text, Builder.TextOrigin.FROM_ATTRIBUTE, line, model);
        }
    }

    private Map countChildren(Node node)
    {
        Map counts = new HashMap();

        for (Iterator i = node.getAttributeNames(); i != null && i.hasNext();)
        {
            QName qname = (QName)i.next();

            String namespaceURI = qname.getNamespace();
            String localPart = qname.getLocalPart();
            if (SymbolTable.OBJECT.equals(localPart) && namespaceURI.length() == 0)
            {
                continue;
            }

            if (!counts.containsKey(localPart))
            {
                counts.put(localPart, IntegerPool.getNumber(1));
            }
            else
            {
                int count = ((Integer)counts.get(localPart)).intValue() + 1;
                counts.put(localPart, IntegerPool.getNumber(count));
            }
        }

        for (int i = 0, count = node.getChildCount(); i < count; i++)
        {
            Node child = (Node)node.getChildAt(i);
            if (child instanceof CDATANode)
            {
                continue;
            }

            String namespaceURI = child.getNamespace();
            String localPart = child.getLocalPart();

            if (SymbolTable.OBJECT.equals(localPart) && namespaceURI.length() == 0)
            {
                continue;
            }

            if (!counts.containsKey(localPart))
            {
                counts.put(localPart, IntegerPool.getNumber(1));
            }
            else
            {
                int num = ((Integer)counts.get(localPart)).intValue() + 1;
                counts.put(localPart, IntegerPool.getNumber(num));
            }
        }

        return counts;
    }

    private Map createArrays(Model parent, Map counts)
    {
        Map arrays = new HashMap();

        for (Iterator i = counts.keySet().iterator(); i.hasNext();)
        {
            String localPart = (String)i.next();

            if (((Integer)counts.get(localPart)).intValue() > 1)
            {
                Array a = new Array(document, typeTable.objectType, parent, parent.getXmlLineNumber());
                a.setId(localPart, false);
                a.setIsAnonymous(true);

                // prepopulate with any properties definied as attributes
                if (parent.hasProperty(localPart))
                {
					a.addEntry(parent.getProperty(localPart), parent.getXmlLineNumber());
                }

                arrays.put(localPart, a);
            }
        }

        return arrays;
    }

	/**
	 * map some java types into AS
	 * TODO should go away once we use Primitive universally
	 */
	private static Type getPrimitiveType(TypeTable typeTable, Object o)
	{
		return (o instanceof Boolean) ? typeTable.booleanType :
				(o instanceof Number) ? typeTable.numberType :
                typeTable.stringType;
	}

	public static class HasAttributeInitializers extends CompilerError
	{
	}

    public static class IgnoringCDATA extends CompilerWarning
    {
        public String image;

        public IgnoringCDATA(String image)
        {
            this.image = image;
        }
    }

    public static class ObjectTag extends CompilerError
    {
    }
}
