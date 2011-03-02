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

package flex2.compiler.as3.reflect;

import macromedia.asc.parser.AttributeListNode;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

/**
 * @author Clement Wong
 */
public final class Attributes implements flex2.compiler.abc.Attributes
{
	Attributes(AttributeListNode attrs)
	{
		int size = attrs.namespace_ids.size();
		if (size != 0)
		{
			namespaces = new HashSet();
			for (int j = 0; j < size; j++)
			{
				String ns_id = (String) attrs.namespace_ids.get(j);
				namespaces.add(ns_id);
			}
		}

		hasIntrinsic = attrs.hasIntrinsic;
		hasStatic = attrs.hasStatic;
		hasFinal = attrs.hasFinal;
		hasVirtual = attrs.hasVirtual;
		hasOverride = attrs.hasOverride;
		hasDynamic = attrs.hasDynamic;
		hasNative = attrs.hasNative;
		hasPrivate = attrs.hasPrivate;
		hasProtected = attrs.hasProtected;
		hasPublic = attrs.hasPublic;
		hasInternal = attrs.hasInternal;
		hasConst = attrs.hasConst;
		hasFalse = attrs.hasFalse;
		hasPrototype = attrs.hasPrototype;
	}

	private boolean hasIntrinsic;
	private boolean hasStatic;
	private boolean hasFinal;
	private boolean hasVirtual;
	private boolean hasOverride;
	private boolean hasDynamic;
	private boolean hasNative;
	private boolean hasPrivate;
	private boolean hasPublic;
	private boolean hasProtected;
	private boolean hasInternal;
	private boolean hasConst;
	private boolean hasFalse;
	private boolean hasPrototype;

	private Set namespaces; // Set<String>

	public boolean hasIntrinsic()
	{
		return hasIntrinsic;
	}

	public boolean hasStatic()
	{
		return hasStatic;
	}

	public boolean hasFinal()
	{
		return hasFinal;
	}

	public boolean hasVirtual()
	{
		return hasVirtual;
	}

	public boolean hasOverride()
	{
		return hasOverride;
	}

	public boolean hasDynamic()
	{
		return hasDynamic;
	}

	public boolean hasNative()
	{
		return hasNative;
	}

	public boolean hasPrivate()
	{
		return hasPrivate;
	}

	public boolean hasPublic()
	{
		return hasPublic;
	}

	public boolean hasProtected()
	{
		return hasProtected;
	}

	public boolean hasInternal()
	{
		return hasInternal;
	}

	public boolean hasConst()
	{
		return hasConst;
	}

	public boolean hasFalse()
	{
		return hasFalse;
	}

	public boolean hasPrototype()
	{
		return hasPrototype;
	}

	public boolean hasNamespace(String nsValue)
	{
		return namespaces != null && namespaces.contains(nsValue);
	}

	public Iterator getNamespaces() // Iterator<String>
	{
		return namespaces == null ? null : namespaces.iterator();
	}

	void setHasConst()
	{
		this.hasConst = true;
	}
}
