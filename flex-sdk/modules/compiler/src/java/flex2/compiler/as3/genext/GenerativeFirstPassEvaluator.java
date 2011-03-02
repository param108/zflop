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

package flex2.compiler.as3.genext;

import flex2.compiler.as3.reflect.TypeTable;
import flex2.compiler.util.ThreadLocalToolkit;
import flash.swf.tools.as3.EvaluatorAdapter;

import java.util.Map;

/**
 * common superclass for Bindable and Managed first pass evaluators.
 */
public abstract class GenerativeFirstPassEvaluator extends EvaluatorAdapter
{
	protected final TypeTable typeTable;

	public GenerativeFirstPassEvaluator(TypeTable typeTable)
	{
		this.typeTable = typeTable;
		setLocalizationManager(ThreadLocalToolkit.getLocalizationManager());
	}

	public abstract boolean makeSecondPass();

	public abstract Map getClassMap();

}
