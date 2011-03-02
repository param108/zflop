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

package flex2.compiler;

/**
 * @author Clement Wong
 */
public interface Compiler
{
	/**
	 * If this compiler can process the specified file, return true.
	 */
	boolean isSupported(String mimeType);

	/**
	 * Return supported mime types.
	 */
	String[] getSupportedMimeTypes();

	/**
	 * Pre-process source file.
	 */
	Source preprocess(Source source);

	/**
	 * Parse... The implementation must:
	 *
	 * 1. create a compilation unit
	 * 2. put the Source object and the syntax tree in the compilation unit
	 * 3. register unit.includes, unit.dependencies, unit.topLevelDefinitions and unit.metadata
	 */
	CompilationUnit parse1(Source source, SymbolTable symbolTable);
	void parse2(CompilationUnit unit, SymbolTable symbolTable);

	/**
	 * Analyze... The implementation must:
	 *
	 * 1. register type info to SymbolTable
	 */
	void analyze1(CompilationUnit unit, SymbolTable symbolTable);
	void analyze2(CompilationUnit unit, SymbolTable symbolTable);
	void analyze3(CompilationUnit unit, SymbolTable symbolTable);
	void analyze4(CompilationUnit unit, SymbolTable symbolTable);

	/**
	 * Code Generate
	 */
	void generate(CompilationUnit unit, SymbolTable symbolTable);

	/**
	 * Postprocess... could be invoked multiple times...
	 */
	void postprocess(CompilationUnit unit, SymbolTable symbolTable);
}
