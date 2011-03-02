////////////////////////////////////////////////////////////////////////////////
//
//  ADOBE SYSTEMS INCORPORATED
//  Copyright 2006-2007 Adobe Systems Incorporated
//  All Rights Reserved.
//
//  NOTICE: Adobe permits you to use, modify, and distribute this file
//  in accordance with the terms of the license agreement accompanying it.
//
////////////////////////////////////////////////////////////////////////////////

package flex2.compiler.mxml.builder;

import java.io.StringWriter;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.Stack;

import flex2.compiler.CompilationUnit;
import flex2.compiler.util.CompilerMessage.CompilerError;
import flex2.compiler.mxml.Configuration;
import flex2.compiler.mxml.dom.CDATANode;
import flex2.compiler.mxml.dom.Node;
import flex2.compiler.mxml.dom.XMLListNode;
import flex2.compiler.mxml.lang.TextParser;
import flex2.compiler.mxml.reflect.TypeTable;
import flex2.compiler.mxml.rep.AtResource;
import flex2.compiler.mxml.rep.BindingExpression;
import flex2.compiler.mxml.rep.Model;
import flex2.compiler.mxml.rep.MxmlDocument;
import flex2.compiler.mxml.rep.XMLList;
import flex2.compiler.util.IntegerPool;
import flex2.compiler.util.QName;
import flex2.compiler.util.QNameMap;

public class XMLListBuilder extends Builder
{

    XMLListBuilder(CompilationUnit unit, TypeTable typeTable, Configuration configuration, MxmlDocument document)
    {
        this(unit, typeTable, configuration, document, null);
        allowTopLevelBinding = true;
    }

    XMLListBuilder(CompilationUnit unit, TypeTable typeTable, Configuration configuration, MxmlDocument document, Model parent)
    {
        super(unit, typeTable, configuration, document);
        this.parent = parent;
        allowTopLevelBinding = false;
		temp = new HashSet();
    }

	private String id;
	private Set temp;
	private Model parent;
    private boolean allowTopLevelBinding;
    XMLList xmlList;

    public void analyze(XMLListNode node)
    {
	    id = (String) node.getAttribute("id");
        xmlList = new XMLList(document, typeTable.xmlListType, parent, node.beginLine);
        if (id != null)
        {
            xmlList.setId(id, false);
        }

        if (!checkFormat(node))
        {
            xmlList.setLiteralXML("null");
	        return;
        }

	    StringWriter writer = new StringWriter();

	    if (node.getChildCount() == 1 && node.getChildAt(0) instanceof CDATANode)
	    {
            //TODO test?
		    CDATANode cdata = (CDATANode) node.getChildAt(0);
		    if (cdata.image.length() > 0)
		    {
			    String bindingExprString = TextParser.getBindingExpressionFromString(cdata.image.trim());
			    if (bindingExprString == null)
			    {
				    writer.write(TextParser.cleanupBindingEscapes(cdata.image));
			    }
			    else if (allowTopLevelBinding)
			    {
				    BindingExpression be = new BindingExpression(bindingExprString, cdata.beginLine, document);
				    be.setDestination(xmlList);
			    }
			    else
			    {
				    log(cdata, new Builder.BindingNotAllowed());
			    }
		    }
	    }
	    else
	    {
		    processChildren(node, writer, null, new Stack(), new Stack());
	    }

	    xmlList.setLiteralXML(writer.toString());
    }

    private boolean checkFormat(XMLListNode node)
    {
        if (node.getChildAt(0) instanceof CDATANode)
        {
            log(node, new RequireXMLContent());
            return false;
        }
        else
        {
            return true;
        }
    }

	private void pushNodeNamespace(Node node, Stack namespaces)
	{
		temp.clear();
		String uri = node.getNamespace();

		for (int i = 0, size = namespaces.size(); i < size; i++)
		{
			PrefixMapping pm = (PrefixMapping) namespaces.get(i);
			temp.add(pm);
			if (pm.uri.equals(uri))
			{
				namespaces.push(pm);
				return;
			}
		}

		namespaces.push(new PrefixMapping(temp.size() + 1, uri));

	}

	private int getNamespaceId(String nsUri, Stack namespaces)
	{
		temp.clear();
		for (int i = 0, size = namespaces.size(); i < size; i++)
		{
			PrefixMapping pm = (PrefixMapping) namespaces.get(i);
			temp.add(pm);
			if (pm.uri.equals(nsUri))
			{
				return pm.ns;
			}
		}

		return temp.size() + 1;
	}

	private void popNodeNamespace(Stack namespaces)
	{
		namespaces.pop();
	}

	private void pushNamespaces(BindingExpression be, Stack namespaces)
	{
		temp.clear();
		for (int i = 0, count = namespaces.size(); i < count; i++)
		{
			PrefixMapping pm = (PrefixMapping) namespaces.get(i);
			temp.add(pm);
		}
		for (Iterator j = temp.iterator(); j.hasNext();)
		{
			PrefixMapping pm = (PrefixMapping) j.next();
			be.addNamespace(pm.uri, pm.ns);
		}
	}

    /**
     *
     */
    private void processNode(Node node,
                             StringWriter serializer,
                             String e4xElementsByLocalName,
                             Stack destinationPropertyStack,
                             Stack namespaces)
    {
	    QNameMap attributeBindings = processBindingAttributes(node);
        processResourceAttributes(node);
        
	    if (attributeBindings != null)
	    {
		    //[Matt] I think this value is wrong but I can't see where it's used so we'll leave it for now.
		    String destinationProperty = createExpression(destinationPropertyStack);

		    for (Iterator i = attributeBindings.keySet().iterator(); i.hasNext();)
		    {
			    QName attrName = (QName) i.next();

				// PrefixMapping pm = (PrefixMapping) namespaces.peek();
			    String nsUri = attrName.getNamespace();
				String attrExpr;
				int nsId = 0;

				if (nsUri.length() > 0)
				{
					nsId = getNamespaceId(nsUri, namespaces);
					attrExpr = e4xElementsByLocalName + ".@ns" + nsId + "::" + attrName.getLocalPart();
				}
				else
				{
					attrExpr = e4xElementsByLocalName + ".@" + attrName.getLocalPart();
				}

			    BindingExpression be = new BindingExpression((String) attributeBindings.get(attrName),
			                                                 node.beginLine, document);
			    be.setDestinationXMLAttribute(true);
			    be.setDestinationLValue(attrExpr);
			    be.setDestinationProperty(destinationProperty + "[" + node.getIndex() + "]");
			    be.setDestination(xmlList);

				xmlList.setHasBindings(true);

				pushNamespaces(be, namespaces);
				if (nsUri.length() > 0)
				{
					be.addNamespace(nsUri, nsId);
				}
		    }
	    }

	    node.toStartElement(serializer);

	    if (node.getChildCount() == 1 && node.getChildAt(0) instanceof CDATANode)
	    {
		    CDATANode cdata = (CDATANode) node.getChildAt(0);
		    if (cdata.image.length() > 0)
		    {
			    String bindingExprString = TextParser.getBindingExpressionFromString(cdata.image);
			    if (bindingExprString != null)
			    {
				    //[Matt] I think this value is wrong but I can't see where it's used so we'll leave it for now.
				    String destinationProperty = createExpression(destinationPropertyStack);

				    BindingExpression be = new BindingExpression(bindingExprString, cdata.beginLine, document);
				    be.setDestinationLValue(e4xElementsByLocalName);
				    be.setDestinationProperty(destinationProperty + "[" + node.getIndex() + "]");
				    be.setDestination(xmlList);
				    be.setDestinationXMLNode(true);
				    be.setDestinationE4X(true);

					xmlList.setHasBindings(true);

					pushNamespaces(be, namespaces);
			    }
			    else
			    {
				    serializer.write(TextParser.cleanupBindingEscapes(cdata.image));
			    }
		    }
	    }
	    else
	    {
		    processChildren(node, serializer, e4xElementsByLocalName, destinationPropertyStack, namespaces);
	    }

	    node.toEndElement(serializer);
    }

	private void processChildren(Node node,
                                 StringWriter serializer,
                                 String e4xElementsByLocalName,
                                 Stack destinationProperty,
                                 Stack namespaces)
    {
        assignIndices(node);

        for (int i = 0, count = node.getChildCount(); i < count; i++)
        {
            Node child = (Node) node.getChildAt(i);
            if (child instanceof CDATANode)
            {
                CDATANode cdata = (CDATANode) child;
                if (cdata.image.trim().length() > 0)
                {
                    // C: ignore CDATANode if other XML elements exist...
                    log(child, new XMLBuilder.MixedContent(child.image));
                }
                else
                {
                    // Whitespace is OK
                    serializer.write(cdata.image);
                }
            }
            else
            {
				pushNodeNamespace(child, namespaces);

                if (e4xElementsByLocalName != null)
                {
	                StringBuffer e4xbuffer = new StringBuffer(e4xElementsByLocalName);
					String destProp = child.getLocalPart();
					if (child.getNamespace().length() > 0)
					{
						PrefixMapping pm = (PrefixMapping) namespaces.peek();
						destProp = "ns" + pm.ns + "::" + destProp;
					}
	                e4xbuffer.append(".").append(destProp).append("[").append(child.getIndex()).append("]");

	                destinationProperty.push(destProp);
	                processNode(child, serializer, e4xbuffer.toString(), destinationProperty, namespaces);
	                destinationProperty.pop();
                }
                else
                {
	                StringBuffer e4xbuffer = new StringBuffer(xmlList.getId());
                    e4xbuffer.append("[").append(i).append("]");

	                destinationProperty.push(child.getLocalPart());
	                processNode(child, serializer, e4xbuffer.toString(), destinationProperty, namespaces);
	                destinationProperty.pop();
                }

				popNodeNamespace(namespaces);
            }
        }
    }

    /**
     * Collects/processes Binding attributes from the node, and then removes them from the node.
     */
    private QNameMap processBindingAttributes(Node node) // Map<String, String>
    {
        QNameMap attributeBindings = null;

        for (Iterator i = node.getAttributeNames(); i != null && i.hasNext();)
        {
            QName qname = (QName) i.next();
            String value = (String) node.getAttribute(qname);
            String bindingExprString = TextParser.getBindingExpressionFromString(value);

            if (bindingExprString != null)
            {
                if (attributeBindings == null)
                {
                    attributeBindings = new QNameMap();
                }
                // C: only localPart as the key?
                attributeBindings.put(qname, bindingExprString);
	            i.remove();
            }
        }

        return attributeBindings;
    }
    
    /**
     * Processes @Resource attributes from the node.
     */
    // TODO need to do all sorts of error testing
    //         * e.g. invalid atFunctions
    //         * invalid Resource arguments
    //         * @Resource in CDATA (is this allowed?) (like databinding in CDATA?)
    private void processResourceAttributes(Node node) // Map<String, String>
    {
        final QNameMap attributeResources = new QNameMap();
        
        for (Iterator i = node.getAttributeNames(); i != null && i.hasNext();)
        {
            final QName qname = (QName)i.next();
            final String text = (String) node.getAttribute(qname);

            final String atFunction = TextParser.getAtFunctionName(text);
            if ("Resource".equals(atFunction))
            {
                //TODO I am assuming that this should always be a string type because this is
                //     XML, though @Resources allow things like Embed. I'm right?
                //TODO test an Embed and see what happens?
                final AtResource atResource
                    = (AtResource)textParser.resource(text, typeTable.stringType);
                
                if (atResource != null)
                {                
                    // C: only localPart as the key?
                    attributeResources.put(qname, atResource);
                    
                    // we don't remove these here since we don't want to reorder the attributes
                    // we also don't call addAttribute() to update the map to avoid a potential
                    // ConcurrentModificationException on the iterator
                    //i.remove();
                }
            }
            else if (atFunction != null)
            {
                // if we found an invalid @Function, throw an error
                textParser.desc = atFunction;
                textParser.line = node.beginLine;
                textParser.error(TextParser.ErrUnrecognizedAtFunction, null, null, null);
            }
        }
        
        // now update the definitions in the attribute map
        for(Iterator iter = attributeResources.keySet().iterator(); iter.hasNext();)
        {
            final QName qname = (QName) iter.next();
            final AtResource atResource = (AtResource)attributeResources.get(qname);
            
            // attributes are in a LinkedHashMap, so this just updates the existing mapping
            // with the qname -> AtResource. When Element.toStartElement() is emitting the
            // attribute's value, it will notice the special case of an AtResource object
            // (instead of String) and emit an E4X expression with braces rather than a
            // String with double-quotes. 
            node.addAttribute(qname.getNamespace(), qname.getLocalPart(), atResource, node.beginLine);
        }
    }
    
    // C: The implementation of this method depends on the implementation of app model's
    //    NamespaceUtil.getElementsByLocalName()...
    private void assignIndices(Node parent)
    {
        Map counts = new HashMap();

        Integer zero = IntegerPool.getNumber(0);

        for (int i = 0, count = parent.getChildCount(); i < count; i++)
        {
            Node child = (Node) parent.getChildAt(i);
            if (child instanceof CDATANode)
            {
                continue;
            }

            if (!counts.containsKey(child.image))
            {
                counts.put(child.image, zero);
                child.setIndex(0);
            }
            else
            {
                int num = ((Integer) counts.get(child.image)).intValue() + 1;
                counts.put(child.image, IntegerPool.getNumber(num));
                child.setIndex(num);
            }
        }
    }

    private String createExpression(Stack stack)
    {
        StringBuffer buffer = new StringBuffer();

        for (int i = 0, count = stack.size(); i < count; i++)
        {
            buffer.append(stack.get(i));
            if (i < count - 1)
            {
                buffer.append(".");
            }
        }

        return buffer.toString();
    }

	class PrefixMapping
	{
		PrefixMapping(int ns, String uri)
		{
			this.ns = ns;
			this.uri = uri;
		}

		int ns;
		String uri;

		public boolean equals(Object obj)
		{
			return uri.equals(obj);
		}

		public int hashCode()
		{
			return uri.hashCode();
		}
	}

    public static class RequireXMLContent extends CompilerError
    {
        public RequireXMLContent()
        {
            super();
        }
    }
}
