////////////////////////////////////////////////////////////////////////////////
//
//  ADOBE SYSTEMS INCORPORATED
//  Copyright 2004-2006 Adobe Systems Incorporated
//  All Rights Reserved.
//
//  NOTICE: Adobe permits you to use, modify, and distribute this file
//  in accordance with the terms of the license agreement accompanying it.
//
////////////////////////////////////////////////////////////////////////////////

package flex2.compiler.mxml;

import flash.util.FileUtils;
import flex2.compiler.CompilationUnit;
import flex2.compiler.Source;
import flex2.compiler.SymbolTable;
import flex2.compiler.Transcoder;
import flex2.compiler.as3.Extension;
import flex2.compiler.util.NameMappings;

import java.io.File;

/**
 * Wrapper for mxml interface and implementation compilers. The logic here relies on the flex2.compiler.API workflow,
 * in which CompilationUnits that don't produce bytecode are "reset". Here, we use the reset to transition from interface
 * to implementation compilation.
 */
public class Compiler implements flex2.compiler.Compiler
{
	//	ATTR_STATE is used to indicate progress through double-pass compilation process.
	private static final String ATTR_STATE = "MxmlState";

	//	values of ATTR_STATE - see later in class def for state mgmt logic
	private static final int
			STATE_INTERFACE_PARSED = 0,
			STATE_INTERFACE_GENERATED = 1,
			STATE_IMPLEMENTATION_PARSED = 2,
			STATE_IMPLEMENTATION_GENERATED = 3;

	//	MXML document state
	static final String DOCUMENT_INFO = "DocumentInfo";
	//	type table wrapper around symbol table, held to avoid recreation overhead
	static final String TYPE_TABLE = flex2.compiler.mxml.reflect.TypeTable.class.getName();
	//	line number map - maps regions of generated code back to original MXML. TODO fold into document info
	static final String LINE_NUMBER_MAP = "LineNumberMap";
	//	each subcompiler uses a delegate compilation unit for generated code
	static final String DELEGATE_UNIT = "DelegateUnit";
	//	context attribute used to maintain state during InterfaceCompiler.postprocess(). Checked in ImplementationCompiler.parse(), see comments there.
	static final String CHECK_NODES = "CheckNodes";
	
	//	subcompilers
	private InterfaceCompiler intfc;
	private ImplementationCompiler implc;

	public Compiler(flex2.compiler.mxml.Configuration mxmlConfiguration,
					flex2.compiler.as3.Configuration ascConfiguration,
					NameMappings mappings, Transcoder[] transcoders)
	{
		intfc = new InterfaceCompiler(mxmlConfiguration, ascConfiguration, mappings);
		implc = new ImplementationCompiler(mxmlConfiguration, ascConfiguration, mappings, transcoders);
	}

	public boolean isSupported(String mimeType) { return implc.isSupported(mimeType); }

	public String[] getSupportedMimeTypes() { return implc.getSupportedMimeTypes(); }

	public void addInterfaceCompilerExtension(Extension ext)
	{
		intfc.getASCompiler().addCompilerExtension(ext);
	}

	public void addImplementationCompilerExtension(Extension ext)
	{
		implc.getASCompiler().addCompilerExtension(ext);
	}

	public Source preprocess(Source source)
	{
		if (source.getCompilationUnit() == null)
			return intfc.preprocess(source);
		else
			return implc.preprocess(source);
	}

	public CompilationUnit parse1(Source source, SymbolTable symbolTable)
	{
		CompilationUnit unit = source.getCompilationUnit();

		if (unit == null)
		{
			// System.out.println(source.getName() + ": intfc parse");

			//	first time through: begin the InterfaceCompiler pass
			unit = intfc.parse1(source, symbolTable);
			if (unit != null)
			{
				setState(unit, STATE_INTERFACE_PARSED);
			}
		}
		else
		{
			//	We're here (in parse() with unit non-null) for one of two reasons: a) this is the bona fide start of the
			// 	second pass, in which case we invoke ImplementationCompiler.parse(); b) we're actually still at the
			// 	beginning of the first pass, but parse() has already been called on this source due to TypeAnalyzer's
			// 	eager superclass parsing. In the latter case, we need to continue the InterfaceCompiler pass.
			
			if (getState(unit) == STATE_INTERFACE_GENERATED)
			{
				// System.out.println(source.getName() + ": implc parse (state == " + getState(unit) + ")");
				unit = implc.parse1(source, symbolTable);
				if (unit != null)
				{
					advanceState(unit);
				}
			}
			else
			{
				//	no-op
				// System.out.println(source.getName() + ": noop (state == " + getState(unit) + ")");
			}
		}

		return unit;
	}

	public void parse2(CompilationUnit unit, SymbolTable symbolTable)
	{
		getSubCompiler(unit).parse2(unit, symbolTable);
	}

	public void analyze1(CompilationUnit unit, SymbolTable symbolTable)
	{
		// System.out.println(unit.getSource().getName() + ": analyze1 (state == " + getState(unit) + ")");
		getSubCompiler(unit).analyze1(unit, symbolTable);
	}

	public void analyze2(CompilationUnit unit, SymbolTable symbolTable)
	{
		// System.out.println(unit.getSource().getName() + ": analyze2 (state == " + getState(unit) + ")");
		getSubCompiler(unit).analyze2(unit, symbolTable);
	}

	public void analyze3(CompilationUnit unit, SymbolTable symbolTable)
	{
		// System.out.println(unit.getSource().getName() + ": analyze3 (state == " + getState(unit) + ")");
		getSubCompiler(unit).analyze3(unit, symbolTable);
	}

	public void analyze4(CompilationUnit unit, SymbolTable symbolTable)
	{
		// System.out.println(unit.getSource().getName() + ": analyze4 (state == " + getState(unit) + ")");
		getSubCompiler(unit).analyze4(unit, symbolTable);
	}

	public void generate(CompilationUnit unit, SymbolTable symbolTable)
	{
		// System.out.println(unit.getSource().getName() + ": generate (state == " + getState(unit) + ")");
		getSubCompiler(unit).generate(unit, symbolTable);
		advanceState(unit);
	}

	public void postprocess(CompilationUnit unit, SymbolTable symbolTable)
	{
		// System.out.println(unit.getSource().getName() + ": postprocess (state == " + getState(unit) + ")");
		getSubCompiler(unit).postprocess(unit, symbolTable);
	}

	/**
	 * state mgmt
	 */

	private int getState(CompilationUnit unit)
	{
		assert unit.getContext().getAttribute(ATTR_STATE) != null : "unit lacks " + ATTR_STATE + " attribute";
		return ((Integer)unit.getContext().getAttribute(ATTR_STATE)).intValue();
	}

	private void setState(CompilationUnit unit, int state)
	{
		unit.getContext().setAttribute(ATTR_STATE, new Integer(state));
	}

	private void advanceState(CompilationUnit unit)
	{
		int state = getState(unit);
		// System.out.println(unit.getSource().getName() + ": advancing from " + state + " to " + (state + 1));
		assert state < STATE_IMPLEMENTATION_GENERATED : "advanceState called with state == " + state;
		setState(unit, state + 1);
	}

	/**
	 * pick subcompiler based on unit state
	 */
	private flex2.compiler.Compiler getSubCompiler(CompilationUnit unit)
	{
		return getState(unit) < STATE_IMPLEMENTATION_PARSED ? (flex2.compiler.Compiler)intfc : implc;
	}

	/**
	 * utilities used by both subcompilers
	 */
	
	static String getGeneratedName(Configuration mxmlConfiguration,
								   String packageName, String className, String suffix)
	{
		String dir = mxmlConfiguration.getGeneratedDirectory();
		if ((packageName != null) && (packageName.length() > 0))
		{
			dir = FileUtils.addPathComponents(dir, packageName.replace('.', File.separatorChar), File.separatorChar);
		}

		return FileUtils.addPathComponents(dir, className + suffix, File.separatorChar);
	}
}
