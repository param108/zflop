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
import flex2.compiler.util.CompilerMessage.CompilerWarning;
import flex2.compiler.mxml.Configuration;
import flex2.compiler.mxml.dom.CDATANode;
import flex2.compiler.mxml.dom.Node;
import flex2.compiler.mxml.dom.XMLNode;
import flex2.compiler.mxml.lang.TextParser;
import flex2.compiler.mxml.lang.StandardDefs;
import flex2.compiler.mxml.reflect.Type;
import flex2.compiler.mxml.reflect.TypeTable;
import flex2.compiler.mxml.rep.BindingExpression;
import flex2.compiler.mxml.rep.Model;
import flex2.compiler.mxml.rep.MxmlDocument;
import flex2.compiler.mxml.rep.XML;
import flex2.compiler.util.IntegerPool;
import flex2.compiler.util.NameFormatter;
import flex2.compiler.util.QName;
import flex2.compiler.util.XMLStringSerializer;
import flex2.compiler.util.QNameMap;
import org.xml.sax.Attributes;

import java.io.IOException;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.Stack;

/**
 * TODO haven't converted the text value parsing here. CDATANode.inCDATA is being ignored; don't know if there are other issues.
 * @author Clement Wong
 */
class XMLBuilder extends Builder
{
	XMLBuilder(CompilationUnit unit, TypeTable typeTable, Configuration configuration, MxmlDocument document)
	{
		this(unit, typeTable, configuration, document, null);
		allowTopLevelBinding = true;
	}

	XMLBuilder(CompilationUnit unit, TypeTable typeTable, Configuration configuration, MxmlDocument document, Model parent)
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
	XML xml;

	public void analyze(XMLNode node)
	{
		id = (String) node.getAttribute("id");
		boolean e4x = node.isE4X();
		Type t = typeTable.getType(StandardDefs.getXmlBackingClassName(e4x));
        xml = new XML(document, t, parent, e4x, node.beginLine);
        if (id != null)
        {
            xml.setId(id, false);
        }

        //	decide format based on per-node specification
		if (!checkFormat(node))
		{
			xml.setLiteralXML("null");
			return;
		}

		StringWriter writer = new StringWriter();
		XMLStringSerializer xmlStringSerializer = new XMLStringSerializer(writer);

        try
		{
			if (node.getChildCount() == 0)
			{
				writer.write("null");
			}
			else if (node.getChildCount() == 1 && node.getChildAt(0) instanceof CDATANode)
			{
				CDATANode cdata = (CDATANode) node.getChildAt(0);
				if (cdata.image.length() > 0)
				{
					String bindingExprString = TextParser.getBindingExpressionFromString(cdata.image.trim());
					if (bindingExprString == null)
					{
						if (e4x)
						{
							writer.write(TextParser.cleanupBindingEscapes(cdata.image));
						}
						else
						{
							xmlStringSerializer.writeString(TextParser.cleanupBindingEscapes(cdata.image));
						}
					}
					else if (allowTopLevelBinding)
					{
						BindingExpression be = new BindingExpression(bindingExprString, cdata.beginLine, document);
						be.setDestination(xml);
					}
					else
					{
						log(cdata, new Builder.BindingNotAllowed());
					}
				}
			}
			else
			{
				if (e4x)
				{
					processChildren(e4x, node, writer, null, new Stack(), new Stack());
				}
				else
				{
					processChildren(e4x, node, xmlStringSerializer, null, new Stack(), null);
				}
			}
		}
		catch (IOException e)
		{
			logError(node, e.getLocalizedMessage());
		}

		xml.setLiteralXML(writer.toString());
	}

	private boolean checkFormat(XMLNode node)
	{
		if (node.getChildCount() > 1)
		{
			log(node, new OnlyOneRootTag());
			return false;
		}
		else if (node.getChildAt(0) instanceof CDATANode)
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
	private void processNode(boolean e4x,
                             Node node,
	                         Object serializer,
							 String getElementsByLocalName,
                             Stack destinationPropertyStack,
	                         Stack namespaces)
	{
		QNameMap attributeBindings = processAttributes(node);

		if (attributeBindings != null)
		{
			String destinationProperty = createExpression(destinationPropertyStack);

			for (Iterator i = attributeBindings.keySet().iterator(); i.hasNext();)
			{
				flex2.compiler.util.QName attrName = (flex2.compiler.util.QName) i.next();

				String attrExpr, nsUri = null;
				int nsId = 0;
				if (e4x)
				{
					//PrefixMapping pm = (PrefixMapping) namespaces.peek();
					nsUri = attrName.getNamespace();
					if (nsUri.length() > 0)
					{
						nsId = getNamespaceId(nsUri, namespaces);
						attrExpr = getElementsByLocalName + ".@ns" + nsId + "::" + attrName.getLocalPart();
					}
					else
					{
						attrExpr = getElementsByLocalName + ".@" + attrName.getLocalPart();
					}
				}
				else
				{
					attrExpr = getElementsByLocalName + ".attributes[\"" + attrName.getLocalPart() + "\"]";
				}

				BindingExpression be = new BindingExpression((String) attributeBindings.get(attrName),
				                                             node.beginLine, document);
				be.setDestinationXMLAttribute(true);
				be.setDestinationLValue(attrExpr);
				be.setDestinationProperty(destinationProperty + "[" + node.getIndex() + "]");
				be.setDestination(xml);

				xml.setHasBindings(true);

				if (e4x)
				{
					pushNamespaces(be, namespaces);
					if (nsUri.length() > 0)
					{
						be.addNamespace(nsUri, nsId);
					}
				}
			}
		}

		try
		{
			if (e4x)
			{
				node.toStartElement((StringWriter) serializer);
			}
			else
			{
				QName qname = new QName(node.getNamespace(), node.getLocalPart(), node.getPrefix());
				((XMLStringSerializer) serializer).startElement(qname, new AttributesHelper(node));
			}

			if (node.getChildCount() == 1 && node.getChildAt(0) instanceof CDATANode)
			{
				CDATANode cdata = (CDATANode) node.getChildAt(0);
				if (cdata.image.length() > 0)
				{
					String bindingExprString = TextParser.getBindingExpressionFromString(cdata.image);
					//do not extract bindings for nodes in cdata sections 
					if (bindingExprString != null && !cdata.inCDATA)//
					{
						String destinationProperty = createExpression(destinationPropertyStack);

						BindingExpression be = new BindingExpression(bindingExprString, cdata.beginLine, document);
						be.setDestinationLValue(getElementsByLocalName);
						be.setDestinationProperty(destinationProperty + "[" + node.getIndex() + "]");
						be.setDestination(xml);
						be.setDestinationXMLNode(true);

						xml.setHasBindings(true);

						if (e4x)
						{
							be.setDestinationE4X(true);
							pushNamespaces(be, namespaces);
						}
                    }
					else if (e4x)
					{
						//cleanup binding escapes ONLY if node was not in CDATA Section, otherwise leave exactly as is.
						if (!cdata.inCDATA) {
							((StringWriter) serializer).write(TextParser.replaceBindingEscapesForE4X(cdata.image));
						} else {
							((StringWriter) serializer).write("<![CDATA[" + cdata.image + "]]>");
						}
					}
					else
					{
						//cleanup binding escapes ONLY if node was not in CDATA Section, otherwise leave exactly as is.
						String cdataString = "";
						if (!cdata.inCDATA) {
							cdataString = TextParser.cleanupBindingEscapes(cdata.image);
						} else {
							cdataString = cdata.image;
						}
						
						((XMLStringSerializer) serializer).writeString(cdataString);
					}
				}
			}
			else
			{
				processChildren(e4x, node, serializer, getElementsByLocalName, destinationPropertyStack, namespaces);
			}

			if (e4x)
			{
				node.toEndElement((StringWriter) serializer);
			}
			else
			{
				((XMLStringSerializer) serializer).endElement();
			}
		}
		catch (IOException e)
		{
			logError(node, e.getLocalizedMessage());
		}
	}

	private void processChildren(boolean e4x,
                                 Node node,
	                             Object serializer,
								 String getElementsByLocalName,
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
					log(child, new MixedContent(child.image));
				}
				else if (e4x)
				{
					((StringWriter) serializer).write(cdata.image);
				}
				else
				{
					// Whitespace is OK
					try
					{
						((XMLStringSerializer) serializer).writeString(cdata.image);
					}
					catch (IOException e)
					{
						logError(cdata, e.getLocalizedMessage());
					}
				}
			}
			else if (e4x)
			{
				pushNodeNamespace(child, namespaces);
				
				if (getElementsByLocalName != null)
				{
					StringBuffer e4xbuffer = new StringBuffer(getElementsByLocalName);
					String destProp = child.getLocalPart();
					if (child.getNamespace().length() > 0)
					{
						PrefixMapping pm = (PrefixMapping) namespaces.peek();
						destProp = "ns" + pm.ns + "::" + destProp;
					}
					e4xbuffer.append(".").append(destProp).append("[").append(child.getIndex()).append("]");

					destinationProperty.push(destProp);
					processNode(e4x, child, serializer, e4xbuffer.toString(), destinationProperty, namespaces);
					destinationProperty.pop();
				}
				else
				{
					processNode(e4x, child, serializer, xml.getId(), destinationProperty, namespaces);
				}
				
				popNodeNamespace(namespaces);
			}
			else
			{
				String classNamespaceUtil = NameFormatter.toDot(StandardDefs.CLASS_NAMESPACEUTIL);
				document.addImport(classNamespaceUtil, node.beginLine);

				StringBuffer buffer = new StringBuffer(classNamespaceUtil + ".getElementsByLocalName(");
				buffer.append((getElementsByLocalName == null) ? xml.getId() : getElementsByLocalName);
				buffer.append(", \"").append(child.getLocalPart()).append("\")[").append(child.getIndex()).append("]");

				destinationProperty.push(child.getLocalPart());
				processNode(e4x, child, serializer, buffer.toString(), destinationProperty, null);
				destinationProperty.pop();
			}
		}
	}

	private QNameMap processAttributes(Node node) // Map<String, String>
	{
		QNameMap attributeBindings = null;

		for (Iterator i = node.getAttributeNames(); i != null && i.hasNext();)
		{
			flex2.compiler.util.QName qname = (flex2.compiler.util.QName) i.next();
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
	
	// C: Not efficient... flex2.compiler.mxml.Element needs a better data structure to support
	//    SAX-style Attributes.
	class AttributesHelper implements Attributes
	{
		AttributesHelper(Node node)
		{
			namespaces = new String[node.getAttributeCount()];
			localParts = new String[node.getAttributeCount()];
			values = new Object[node.getAttributeCount()];

			Iterator names = node.getAttributeNames();
			for (int i = 0; names != null && names.hasNext(); i++)
			{
				flex2.compiler.util.QName qname = (flex2.compiler.util.QName) names.next();
				namespaces[i] = qname.getNamespace();
				localParts[i] = qname.getLocalPart();
				values[i] = node.getAttribute(qname);
			}
		}

		private String[] namespaces;
		private String[] localParts;
		private Object[] values;

		public int getLength ()
		{
			return values.length;
		}

		public String getURI (int index)
		{
			return namespaces[index];
		}

		public String getLocalName (int index)
		{
			return localParts[index];
		}

		public String getQName (int index)
		{
		    if ((namespaces[index] == null) || (namespaces[index].equals("")))
		    {
		        return localParts[index];
		    }
            else
            {
			    return namespaces[index] + ":" + localParts[index];
            }
		}

		public String getType (int index)
		{
			return "CDATA";
		}

		public String getValue (int index)
		{
			return (String) values[index];
		}

		public int getIndex (String uri, String localName)
		{
			for (int i = 0, count = namespaces.length; i < count; i++)
			{
				if (namespaces[i].equals(uri) && localParts[i].equals(localName))
				{
					return i;
				}
			}

			return -1;
		}

		public int getIndex (String qName)
		{
			for (int i = 0, count = namespaces.length; i < count; i++)
			{
				if (getQName(i).equals(qName))
				{
					return i;
				}
			}

			return -1;
		}

		public String getType (String uri, String localName)
		{
			return "CDATA";
		}

		public String getType (String qName)
		{
			return "CDATA";
		}

		public String getValue (String uri, String localName)
		{
			for (int i = 0, count = namespaces.length; i < count; i++)
			{
				if (namespaces[i].equals(uri) && localParts[i].equals(localName))
				{
					return (String) values[i];
				}
			}

			return null;
		}

		public String getValue (String qName)
		{
			for (int i = 0, count = namespaces.length; i < count; i++)
			{
				if (getQName(i).equals(qName))
				{
					return (String) values[i];
				}
			}

			return null;
		}
	}

    public static class MixedContent extends CompilerWarning
    {
        public String image;

        public MixedContent(String image)
        {
            this.image = image;
        }
    }

	public static class OnlyOneRootTag extends CompilerError
	{
		public OnlyOneRootTag()
		{
			super();
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
