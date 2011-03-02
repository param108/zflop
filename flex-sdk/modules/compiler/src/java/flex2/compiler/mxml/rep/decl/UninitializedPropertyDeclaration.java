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

package flex2.compiler.mxml.rep.decl;

import flex2.compiler.util.NameFormatter;

/**
 *
 */
public class UninitializedPropertyDeclaration implements PropertyDeclaration
{
	private final String name;
	private final String typeName;
	private final int lineRef;
	private final boolean inspectable;
	private final boolean topLevel;
	private final boolean idIsAutogenerated;

	public UninitializedPropertyDeclaration(String name, String typeName, int lineRef, boolean inspectable, boolean topLevel, boolean idIsAutogenerated)
	{
		this.name = name;
		this.typeName = typeName;
		this.lineRef = lineRef;
		this.inspectable = inspectable;
		this.topLevel = topLevel;
		this.idIsAutogenerated = idIsAutogenerated;
	}

	public int getLineRef() { return lineRef; }
	public String getName() { return name; }
	public String getTypeExpr() { return NameFormatter.toDot(typeName); }
	public boolean getInspectable() { return inspectable; }
	public boolean getTopLevel() { return topLevel; }
	public boolean getIdIsAutogenerated() { return idIsAutogenerated; }
}
