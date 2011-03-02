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

package flex2.compiler.abc;

import java.util.List;

/**
 * @author Clement Wong
 */
public interface Namespace
{
	String getName();

	String getValue();

	Attributes getAttributes();

	List getMetaData(); // List<MetaData>

	List getMetaData(String name); // List<MetaData>
}

