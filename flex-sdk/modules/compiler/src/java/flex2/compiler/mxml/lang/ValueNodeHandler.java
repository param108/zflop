////////////////////////////////////////////////////////////////////////////////
//
//  ADOBE SYSTEMS INCORPORATED
//  Copyright 2005-2006 Adobe Systems Incorporated
//  All Rights Reserved.
//
//  NOTICE: Adobe permits you to use, modify, and distribute this file
//  in accordance with the terms of the license agreement accompanying it.
//
////////////////////////////////////////////////////////////////////////////////

package flex2.compiler.mxml.lang;

import flex2.compiler.mxml.dom.*;

/**
 * The idea of "value node" is that any node which can represent an AS value *anywhere* - property initializers,
 * top-level declarations, whatever - is a value node. There are other node classes that map to AS types but represent
 * more than simple values, i.e. legacy "special" nodes like RemoteObjectNode, etc.
 *
 * This class exists to provide a) a simple way of writing code against all value node types, without repeating a specific
 * and somewhat awkward set of tests on the node's class, and b) an easy way to make sure you've covered all the cases.
 * Of course in some cases it may be more convenient to simply switch on the node class.
 *
 * For typical use-cases, see callers of isValueNode, and subclasses.
 */
public abstract class ValueNodeHandler
{
	protected abstract void componentNode(Node node);
	protected abstract void arrayNode(ArrayNode node);
	protected abstract void primitiveNode(PrimitiveNode node);
	protected abstract void xmlNode(XMLNode node);
    protected abstract void xmlListNode(XMLListNode node);
	protected abstract void modelNode(ModelNode node);
	protected abstract void inlineComponentNode(InlineComponentNode node);
	protected abstract void unknown(Node node);

	public static boolean isValueNode(Node node)
	{
		Class nodeClass = node.getClass();
		return nodeClass == Node.class ||
				nodeClass == ApplicationNode.class ||
				nodeClass == ArrayNode.class ||
				node instanceof PrimitiveNode ||
				nodeClass == XMLNode.class ||
                nodeClass == XMLListNode.class ||
				nodeClass == ModelNode.class ||
				nodeClass == InlineComponentNode.class;
	}

	public void invoke(Node node)
	{
		Class nodeClass = node.getClass();

		if (nodeClass == Node.class ||
			nodeClass == ApplicationNode.class)
		{
			componentNode(node);
		}
		else if (nodeClass == ArrayNode.class)
		{
			arrayNode((ArrayNode)node);
		}
		else if (node instanceof PrimitiveNode)
		{
			primitiveNode((PrimitiveNode)node);
		}
		else if (nodeClass == XMLNode.class)
		{
			xmlNode((XMLNode)node);
		}
        else if (nodeClass == XMLListNode.class)
        {
            xmlListNode((XMLListNode)node);
        }
		else if (nodeClass == ModelNode.class)
		{
			modelNode((ModelNode)node);
		}
		else if (nodeClass == InlineComponentNode.class)
		{
			inlineComponentNode((InlineComponentNode)node);
		}
		else
		{
			assert !isValueNode(node) : "value node class not handled by invoke()";
			unknown(node);
		}
	}
}
