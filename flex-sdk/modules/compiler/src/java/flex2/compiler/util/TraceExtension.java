////////////////////////////////////////////////////////////////////////////////
//
//  ADOBE SYSTEMS INCORPORATED
//  Copyright 2006 Adobe Systems Incorporated
//  All Rights Reserved.
//
//  NOTICE: Adobe permits you to use, modify, and distribute this file
//  in accordance with the terms of the license agreement accompanying it.
//
////////////////////////////////////////////////////////////////////////////////

package flex2.compiler.util;

import flex2.compiler.CompilationUnit;
import flex2.compiler.as3.Extension;
import flex2.compiler.as3.reflect.TypeTable;

/**
 * @author Clement Wong
 */
public class TraceExtension implements Extension
{
	public void parse1(CompilationUnit unit, TypeTable typeTable)
	{
		ThreadLocalToolkit.logInfo("parse1: " + unit.getSource().getName());
	}

    public void parse2(CompilationUnit unit, TypeTable typeTable)
    {
		ThreadLocalToolkit.logInfo("parse2: " + unit.getSource().getName());
    }

	public void analyze1(CompilationUnit unit, TypeTable typeTable)
	{
		ThreadLocalToolkit.logInfo("analyze1: " + unit.getSource().getName());		
	}

	public void analyze2(CompilationUnit unit, TypeTable typeTable)
	{
		ThreadLocalToolkit.logInfo("analyze2: " + unit.getSource().getName());		
	}

	public void analyze3(CompilationUnit unit, TypeTable typeTable)
	{
		ThreadLocalToolkit.logInfo("analyze3: " + unit.getSource().getName());		
	}

	public void analyze4(CompilationUnit unit, TypeTable typeTable)
	{
		ThreadLocalToolkit.logInfo("analyze4: " + unit.getSource().getName());		
	}

	public void generate(CompilationUnit unit, TypeTable typeTable)
	{
		ThreadLocalToolkit.logInfo("generate: " + unit.getSource().getName());		
	}
}
