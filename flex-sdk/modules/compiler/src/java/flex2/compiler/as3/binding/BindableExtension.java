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

package flex2.compiler.as3.binding;

import flex2.compiler.CompilationUnit;
import flex2.compiler.as3.genext.GenerativeExtension;
import flex2.compiler.as3.genext.GenerativeFirstPassEvaluator;
import flex2.compiler.as3.reflect.TypeTable;
import flex2.compiler.mxml.lang.StandardDefs;
import flex2.compiler.util.MultiName;
import macromedia.asc.parser.Evaluator;

/**
 * @author Paul Reilly
 */
public final class BindableExtension extends GenerativeExtension
{
	/**
	 *
	 */
	public BindableExtension(String generatedOutputDirectory)
	{
		super(generatedOutputDirectory);
	}

	/**
	 * Add the MultiNames for the definitions that the BindableSecondPassEvaluator
	 * requires.
	 */
    protected void addInheritance(CompilationUnit unit)
    {
        unit.inheritance.add(new MultiName(StandardDefs.PACKAGE_FLASH_EVENTS, IEVENT_DISPATCHER));
    }

	/**
	 *
	 */
	protected GenerativeFirstPassEvaluator getFirstPassEvaluator(CompilationUnit unit,
																 TypeTable typeTable)
	{
		return new BindableFirstPassEvaluator(typeTable, unit.metadata);
	}

	/**
	 *
	 */
	protected String getFirstPassEvaluatorKey()
    {
        return "BindableFirstPassEvaluator";
    }

	/**
	 *
	 */
	protected Evaluator getSecondPassEvaluator(CompilationUnit unit,
											   TypeAnalyzer typeAnalyzer,
											   GenerativeFirstPassEvaluator firstPassEvaluator)
	{
		return new BindableSecondPassEvaluator(unit, firstPassEvaluator.getClassMap(),
											   typeAnalyzer, generatedOutputDirectory);
	}
}
