////////////////////////////////////////////////////////////////////////////////
//
//  ADOBE SYSTEMS INCORPORATED
//  Copyright 2005 Adobe Systems Incorporated
//  All Rights Reserved.
//
//  NOTICE: Adobe permits you to use, modify, and distribute this file
//  in accordance with the terms of the license agreement accompanying it.
//
////////////////////////////////////////////////////////////////////////////////

package flex2.compiler.mxml.lang;

import flex2.compiler.mxml.reflect.Type;
import flex2.compiler.mxml.dom.Node;
import flex2.compiler.util.QName;

/**
 * Attribute-specific wrapper around DeclarationHandler
 */
public abstract class AttributeHandler extends DeclarationHandler
{
	protected Node node;
	protected Type type;
	protected String text;
	protected int line;

	/**
	 * attribute fails to resolve due to unknown namespace 
	 */
	protected abstract void unknownNamespace(String namespace);

	/**
	 *
	 */
	public void invoke(Node node, Type type, QName qname)
	{
		//	String msg = "AttributeHandler[" + node.image + "/" + node.beginLine + ":" + type.getName() + "].invoke('" + qname + "'): ";

		this.text = (String)node.getAttribute(qname);
		this.line = node.getLineNumber(qname);

		String namespace = qname.getNamespace();
		String localPart = qname.getLocalPart();

		if (isSpecial(namespace, localPart))
		{
			//	System.out.println(msg + "special()");
			special(namespace, localPart);;
		}
		else if (namespace.length() != 0)
		{
			//	MXML 2.0 doesn't allow non-default namespaces on attributes at all.
			//	System.out.println(msg + "unknownNamespace()");
			unknownNamespace(namespace);
		}
		else
		{
			//	System.out.println(msg + "super.invoke()");
			super.invoke(type, localPart);
		}
	}

	/**
	 *
	 */
	protected abstract boolean isSpecial(String namespace, String localPart);

	/**
	 *
	 */
	protected abstract void special(String namespace, String localPart);
}
