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

package flex2.compiler.abc;

import java.util.Iterator;

/**
 * @author Clement Wong
 */
public interface Attributes
{
	boolean hasIntrinsic();

	boolean hasStatic();

	boolean hasFinal();

	boolean hasVirtual();

	boolean hasOverride();

	boolean hasDynamic();

	boolean hasNative();

	boolean hasPrivate();

	boolean hasPublic();

	boolean hasProtected();

	boolean hasInternal();

	boolean hasConst();

	boolean hasFalse();

	boolean hasPrototype();

	boolean hasNamespace(String nsValue);

	Iterator getNamespaces(); // Iterator<String>
}
