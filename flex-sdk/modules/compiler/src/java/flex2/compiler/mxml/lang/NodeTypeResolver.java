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

import flex2.compiler.mxml.reflect.Type;
import flex2.compiler.mxml.reflect.TypeTable;
import flex2.compiler.mxml.dom.*;

/**
 * Encapsulates knowledge of how value nodes map to backing classes. Used in determining lvalue/rvalue type compatibility.
 */
public class NodeTypeResolver extends ValueNodeHandler
{
	final TypeTable typeTable;
	Type type;

	public NodeTypeResolver(TypeTable typeTable)
	{
		this.typeTable = typeTable;
	}

	public Type resolveType(Node node)
	{
		invoke(node);
		return type;
	}

    //	ValueNodeHandler impl

	protected void componentNode(Node node)
	{
		type = typeTable.getType(node.getNamespace(), node.getLocalPart());
	}

	protected void arrayNode(ArrayNode node)
	{
		type = typeTable.arrayType;
	}

	protected void primitiveNode(PrimitiveNode node)
	{
		Class nodeClass = node.getClass();
		type = nodeClass == BooleanNode.class ? typeTable.booleanType :
				nodeClass == NumberNode.class ? typeTable.numberType :
                nodeClass == IntNode.class ? typeTable.intType :
                nodeClass == UIntNode.class ? typeTable.uintType :
                nodeClass == StringNode.class ? typeTable.stringType :
				nodeClass == ClassNode.class ? typeTable.classType :
				nodeClass == FunctionNode.class ? typeTable.functionType :
				null;
		assert type != null : "unknown subclass of PrimitiveNode";
	}

	protected void xmlNode(XMLNode node)
	{
		if (((XMLNode)node).isE4X())
		{
			type = typeTable.xmlType;
		}
		else
		{
			type = typeTable.getType(StandardDefs.CLASS_XMLNODE);
			assert type != null : "MXML core type " + StandardDefs.CLASS_XMLNODE + " not loaded";
		}
	}
    
    protected void xmlListNode(XMLListNode node)
    {
        type = typeTable.xmlListType;
    }

	protected void modelNode(ModelNode node)
	{
		// Note that here we return objectType, even though this often wind up getting
		// typed to ObjectProxy.  This may well be a problem when it comes to array
		// coercion.  TODO confirm/deny that Array is assignable to ObjectProxy
		type = typeTable.objectType;
	}

	protected void inlineComponentNode(InlineComponentNode node)
	{
		type = typeTable.getType(StandardDefs.INTERFACE_IFACTORY);
		assert type != null : "MXML core type " + StandardDefs.INTERFACE_IFACTORY + " not loaded";
	}

	protected void unknown(Node node)
	{
		type = null;
	}
}
