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

package flex2.compiler.mxml;

import flex2.compiler.mxml.rep.AtResource;
import flex2.compiler.util.LinkedQNameMap;
import flex2.compiler.util.QName;

import java.util.*;
import java.io.StringWriter;

/**
 * @author Clement Wong
 */
public class Element extends Token
{
	public Element(String uri, String localPart, int size)
	{
		this.uri = uri;
		this.localPart = localPart;

		if (size > 0)
		{
			attributes = new LinkedQNameMap(size); // QNameMap<QName, Object>
		}

		prefixMappings = null;
	}

	private String uri;
	private String localPart;

	private LinkedQNameMap attributes; // Map<QName, Object>
	private Map prefixMappings;

	private List children; // List<Token>

	public void addPrefixMapping(String uri, String prefix)
	{
		if (prefixMappings == null)
		{
			prefixMappings = new HashMap(8);
		}
		prefixMappings.put(uri, prefix);
	}

	public void addAttribute(String uri, String localName, Object value, int line)
	{
		if (attributes == null)
		{
			attributes = new LinkedQNameMap(); // QNameMap<QName, Object>
		}
		attributes.put(uri, localName, new Value(value, line));
	}

	public Object getAttribute(String localName)
	{
		return getAttribute("", localName);
	}

	public Object getAttribute(String uri, String localName)
	{
		if (attributes == null)
		{
			return null;
		}
		else
		{
			Value v = (Value) attributes.get(uri, localName);
			if (v != null)
			{
				return v.value;
			}
			else
			{
				return null;
			}
		}
	}

	public Object getAttribute(QName qname)
	{
		if (attributes == null)
		{
			return null;
		}
		else
		{
			Value v = (Value) attributes.get(qname);
			if (v != null)
			{
				return v.value;
			}
			else
			{
				return null;
			}
		}
	}

	public int getLineNumber(String localName)
	{
		return getLineNumber("", localName);
	}

	public int getLineNumber(String uri, String localName)
	{
		if (attributes == null)
		{
			return beginLine;
		}
		else
		{
			Value v = (Value) attributes.get(uri, localName);
			if (v != null)
			{
				return v.line;
			}
			else
			{
				return beginLine;
			}
		}
	}

	public int getLineNumber(QName qname)
	{
		if (attributes == null)
		{
			return beginLine;
		}
		else
		{
			Value v = (Value) attributes.get(qname);
			if (v != null)
			{
				return v.line;
			}
			else
			{
				return beginLine;
			}
		}
	}

	public Iterator getAttributeNames() // Iterator<QName>
	{
		return (attributes == null) ? Collections.EMPTY_SET.iterator() : attributes.keySet().iterator();
	}

	public int getAttributeCount()
	{
		return (attributes == null) ? 0 : attributes.size();
	}

	public String getNamespace()
	{
		return uri;
	}

	public String getLocalPart()
	{
		return localPart;
	}

	public String getPrefix()
	{
		return (prefixMappings == null) ? null : (String) prefixMappings.get(uri);
	}

	public void addChildren(List children) // List<Token>
	{
		if (this.children == null)
		{
			this.children = children;
		}
		else
		{
			this.children.addAll(children);
		}
	}

	public void addChild(Token child)
	{
		if (child != null)
		{
			if (children == null)
			{
				children = new ArrayList(); // ArrayList<Token>
			}
			children.add(child);
		}
	}

    public void copy(Element element)
    {
        element.uri = uri;
        element.localPart = localPart;
	    element.prefixMappings = prefixMappings;
        element.attributes = attributes;
        element.children = children;
    }

	public Token getChildAt(int index)
	{
		return (Token) (children == null ? null : children.get(index));
	}

	public int getChildCount()
	{
		return (children == null) ? 0 : children.size();
	}

	public Collection getChildren()
	{
		return children == null ? Collections.EMPTY_LIST : Collections.unmodifiableCollection(children);
	}

	public final Iterator getChildIterator()
	{
		return getChildren().iterator();
	}

	private class Value
	{
		private Value(Object value, int line)
		{
			this.value = value;
			this.line = line;
		}

		private Object value;
		private int line;
	}

    public void removeAttribute(QName qname)
    {
		attributes.remove(qname);
    }

	public String getPrefix(String uri)
	{
		return (prefixMappings == null) ? null : (String) prefixMappings.get(uri);
	}

	public void toStartElement(StringWriter w)
	{
		String p = null;
		w.write('<');
		if ((p = getPrefix(uri)) != null && p.length() > 0)
		{
			w.write(p);
			w.write(':');
		}
		w.write(localPart);

		for (Iterator i = getAttributeNames(); i.hasNext();)
		{
			QName qName = (QName) i.next();
			w.write(' ');
			if ((p = getPrefix(qName.getNamespace())) != null && p.length() > 0)
			{
				w.write(p);
				w.write(':');
			}
			w.write(qName.getLocalPart());
            
            final Object attr = getAttribute(qName);
            // handle @Resource specially
            if(attr instanceof AtResource)
            {
                // e4x expression, so braces instead of double-quotes
                w.write("={");
                w.write(((AtResource)attr).getValueExpression());
                w.write("}");
            }
            else
            {
                // string expression
                w.write("=\"");
                w.write(getAttribute(qName).toString());
                w.write("\"");
            }
		}

		for (Iterator k = prefixMappings == null ? null : prefixMappings.keySet().iterator(); k != null && k.hasNext();)
		{
			String ns = (String) k.next();
			String px = getPrefix(ns);
			if (px != null)
			{
				w.write(" xmlns");
				if (px.length() > 0)
				{
					w.write(':');
					w.write(px);
				}
				w.write("=\"");
				w.write(ns);
				w.write("\"");
			}
		}

		w.write('>');
	}

	public void toEndElement(StringWriter w)
	{
		String p = null;
		w.write("</");
		if ((p = getPrefix(uri)) != null && p.length() > 0)
		{
			w.write(p);
			w.write(':');
		}
		w.write(localPart);
		w.write('>');
	}
}
