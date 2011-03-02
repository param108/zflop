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

package flex2.compiler.mxml.rep;

/**
 * Simple structure for holding a variable declaration to generate.
 * This is much more lightweight than decl.PropertyDeclaration, which represents things induced by the MXML document.
 * These are for e.g. behind-the-scenes management variables, etc., e.g. see FrameworkDefs.documentManagementVariables
 *
 * TODO break out quals, if they need to be introspected
 */
public class VariableDeclaration
{
	private String namespace, name, type, initializer;

	public VariableDeclaration(String namespace, String name, String type, String initializer)
	{
		this.namespace = namespace;
		this.name = name;
		this.type = type;
        this.initializer = initializer;
	}

	public final String getNamespace() { return namespace; }
	public final String getName() { return name; }
	public final String getType() { return type; }
    public final String getInitializer() { return initializer; }
}
