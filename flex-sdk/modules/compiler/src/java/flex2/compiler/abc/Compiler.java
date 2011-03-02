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

package flex2.compiler.abc;

import flash.util.FileUtils;
import flex2.compiler.CompilationUnit;
import flex2.compiler.Source;
import flex2.compiler.SymbolTable;
import flex2.compiler.as3.Extension;
import flex2.compiler.as3.SyntaxTreeEvaluator;
import flex2.compiler.as3.reflect.TypeTable;
import flex2.compiler.css.StyleConflictException;
import flex2.compiler.util.*;
import macromedia.abc.AbcParser;
import macromedia.asc.embedding.avmplus.GlobalBuilder;
import macromedia.asc.parser.ProgramNode;
import macromedia.asc.semantics.*;

import java.io.IOException;
import java.util.*;

/**
 * @author Clement Wong
 */
public class Compiler implements flex2.compiler.Compiler
{
	static
	{
		TypeValue.init();
		ObjectValue.init();
	}

	static final String AttrTypeTable = flex2.compiler.as3.reflect.TypeTable.class.getName();

	public Compiler(flex2.compiler.as3.Configuration configuration)
	{
		mimeTypes = new String[]{MimeMappings.ABC};
		compilerExtensions = new ArrayList(); // ArrayList<Extension>
		this.configuration = configuration;
	}

	private String[] mimeTypes;
	private List compilerExtensions; // List<Extension>
	private flex2.compiler.as3.Configuration configuration;
	        
	public boolean isSupported(String mimeType)
	{
		return mimeTypes[0].equals(mimeType);
	}

	public String[] getSupportedMimeTypes()
	{
		return mimeTypes;
	}

	public void addCompilerExtension(Extension ext)
	{
		compilerExtensions.add(ext);
	}

	public Source preprocess(Source source)
	{
		return source;
	}

	public CompilationUnit parse1(Source source, SymbolTable symbolTable)
	{
		CompilationUnit unit = source.getCompilationUnit();

		if (unit != null && unit.hasTypeInfo)
		{
			copyBytecodes(source, unit);
			return unit;
		}

		if ((unit != null) && (unit.getSyntaxTree() != null))
		{
			return unit;
		}

		final String path = source.getName();
		ProgramNode node = null;

		flex2.compiler.Context context = new flex2.compiler.Context();

		macromedia.asc.util.Context cx = new macromedia.asc.util.Context(symbolTable.perCompileData);

		cx.setScriptName(source.getName());
		cx.setPath(source.getParent());

		cx.setEmitter(symbolTable.emitter);
		cx.setHandler(new flex2.compiler.as3.Compiler.CompilerHandler()
		{
			public void error2(String filename, int ln, int col, Object msg, String source)
			{
				filename = (filename == null || filename.length() == 0) ? path : filename;
				ThreadLocalToolkit.log((CompilerMessage) msg, filename);
			}

			public void warning2(String filename, int ln, int col, Object msg, String source)
			{
				filename = (filename == null || filename.length() == 0) ? path : filename;
				ThreadLocalToolkit.log((CompilerMessage) msg, filename);
			}

			public void error(String filename, int ln, int col, String msg, String source, int errorCode)
			{
				filename = (filename == null || filename.length() == 0) ? path : filename;
				if (errorCode != -1)
				{
					ThreadLocalToolkit.logError(filename, msg, errorCode);
				}
				else
				{
					ThreadLocalToolkit.logError(filename, msg);
				}
			}

			public void warning(String filename, int ln, int col, String msg, String source, int errorCode)
			{
				filename = (filename == null || filename.length() == 0) ? path : filename;
				if (errorCode != -1)
				{
					ThreadLocalToolkit.logWarning(filename, msg, errorCode);
				}
				else
				{
					ThreadLocalToolkit.logWarning(filename, msg);
				}
			}

			public void error(String filename, int ln, int col, String msg, String source)
			{
				filename = (filename == null || filename.length() == 0) ? path : filename;
				ThreadLocalToolkit.logError(filename, msg);
			}

			public void warning(String filename, int ln, int col, String msg, String source)
			{
				filename = (filename == null || filename.length() == 0) ? path : filename;
				ThreadLocalToolkit.logWarning(filename, msg);
			}

			public FileInclude findFileInclude(String parentPath, String filespec)
			{
				return null;
			}
		});
		symbolTable.perCompileData.handler = cx.getHandler();

		context.setAttribute("cx", cx);

		byte[] abc = null;
		try
		{
			abc = source.toByteArray();

			if (abc == null)
			{
				abc = FileUtils.toByteArray(source.getInputStream());
			}

			if (abc == null || abc.length == 0)
			{
				ThreadLocalToolkit.log(new NoBytecodeIsAvailable(), source);
			}
			else
			{
				AbcParser parser = new AbcParser(cx, abc);
				node = parser.parseAbc();

				if (node == null && ThreadLocalToolkit.errorCount() == 0)
				{
					ThreadLocalToolkit.log(new BytecodeDecodingFailed(), source);
				}

				flex2.compiler.as3.Compiler.cleanNodeFactory(cx.getNodeFactory());
			}
		}
		catch (IOException ex)
		{
			ThreadLocalToolkit.logError(source.getNameForReporting(), ex.getLocalizedMessage());
		}

		if (ThreadLocalToolkit.errorCount() > 0)
		{
			return null;
		}

		unit.setSyntaxTree(node);
		unit.getContext().setAttributes(context);
		unit.bytes.set(abc, abc.length);

		SyntaxTreeEvaluator treeEvaluator = new SyntaxTreeEvaluator(unit);
		treeEvaluator.setLocalizationManager(ThreadLocalToolkit.getLocalizationManager());
		node.evaluate(cx, treeEvaluator);

		for (int i = 0, length = compilerExtensions.size(); i < length; i++)
		{
			((Extension) compilerExtensions.get(i)).parse1(unit, null);

			if (ThreadLocalToolkit.errorCount() > 0)
			{
				return null;
			}
		}

		return unit;
	}
	
	public void parse2(CompilationUnit unit, SymbolTable symbolTable)
	{
		if (unit.hasTypeInfo)
		{
			return;
		}

		for (int i = 0, length = compilerExtensions.size(); i < length; i++)
		{
			((Extension) compilerExtensions.get(i)).parse2(unit, null);

			if (ThreadLocalToolkit.errorCount() > 0)
			{
				return;
			}
		}
	}

	public void analyze1(CompilationUnit unit, SymbolTable symbolTable)
	{
		if (unit.hasTypeInfo)
		{
			return;
		}

		ProgramNode node = (ProgramNode) unit.getSyntaxTree();
		if (node.state != ProgramNode.Inheritance)
		{
			return;
		}

		flex2.compiler.Context context = unit.getContext();
		macromedia.asc.util.Context cx = (macromedia.asc.util.Context) context.getAttribute("cx");
		symbolTable.perCompileData.handler = cx.getHandler();

		ObjectValue global = new ObjectValue(cx, new GlobalBuilder(), null);
		cx.pushScope(global); // first scope is always considered the global scope.

		// run FlowAnalyzer
		FlowGraphEmitter flowem = new FlowGraphEmitter(cx, unit.getSource().getName(), false);
		FlowAnalyzer flower = new FlowAnalyzer(flowem);
		context.setAttribute("FlowAnalyzer", flower);

		// 1. ProgramNode.state == Inheritance
		node.evaluate(cx, flower);
		cx.popScope();

		if (ThreadLocalToolkit.errorCount() > 0)
		{
			return;
		}

		unit.typeInfo = node.frame;

		for (int i = 0, length = compilerExtensions.size(); i < length; i++)
		{
			((Extension) compilerExtensions.get(i)).analyze1(unit, null);

			if (ThreadLocalToolkit.errorCount() > 0)
			{
				return;
			}
		}
	}

	public void analyze2(CompilationUnit unit, SymbolTable symbolTable)
	{
		if (unit.hasTypeInfo)
		{
			return;
		}

		ProgramNode node = (ProgramNode) unit.getSyntaxTree();
		if (node.state != ProgramNode.Else)
		{
			return;
		}
		flex2.compiler.Context context = unit.getContext();
		macromedia.asc.util.Context cx = (macromedia.asc.util.Context) context.getAttribute("cx");
		symbolTable.perCompileData.handler = cx.getHandler();

		FlowAnalyzer flower = (FlowAnalyzer) context.getAttribute("FlowAnalyzer");
		context.setAttribute("processed", new HashSet(15));

		inheritSlots(unit, unit.inheritance, symbolTable);
		inheritSlots(unit, unit.namespaces, symbolTable);

		cx.pushScope(node.frame);
		// 2. ProgramNode.state == Else
		node.evaluate(cx, flower);
		cx.popScope();

		if (ThreadLocalToolkit.errorCount() > 0)
		{
			return;
		}

		for (int i = 0, length = compilerExtensions.size(); i < length; i++)
		{
			((Extension) compilerExtensions.get(i)).analyze2(unit, null);

			if (ThreadLocalToolkit.errorCount() > 0)
			{
				return;
			}
		}
	}

	public void analyze3(CompilationUnit unit, SymbolTable symbolTable)
	{
		if (unit.hasTypeInfo)
		{
			return;
		}

		ProgramNode node = (ProgramNode) unit.getSyntaxTree();

		flex2.compiler.Context context = unit.getContext();
		macromedia.asc.util.Context cx = (macromedia.asc.util.Context) context.getAttribute("cx");
		symbolTable.perCompileData.handler = cx.getHandler();

		inheritSlots(unit, unit.types, symbolTable);
		inheritSlots(unit, unit.namespaces, symbolTable);

		// run ConstantEvaluator
		cx.pushScope(node.frame);
		ConstantEvaluator analyzer = new ConstantEvaluator(cx);
		analyzer.PreprocessDefinitionTypeInfo(cx, node);
		cx.popScope();
		context.setAttribute("ConstantEvaluator", analyzer);

		if (ThreadLocalToolkit.errorCount() > 0)
		{
		    return;
		}

		for (int i = 0, length = compilerExtensions.size(); i < length; i++)
		{
			((Extension) compilerExtensions.get(i)).analyze3(unit, null);

			if (ThreadLocalToolkit.errorCount() > 0)
			{
				return;
			}
		}
	}

	public void analyze4(CompilationUnit unit, SymbolTable symbolTable)
	{
		TypeTable typeTable = null;
		if (symbolTable != null)
		{
			typeTable = (TypeTable) symbolTable.getContext().getAttribute(AttrTypeTable);
			if (typeTable == null)
			{
				typeTable = new TypeTable(symbolTable);
				symbolTable.getContext().setAttribute(AttrTypeTable, typeTable);
			}
		}

		if (unit.hasTypeInfo)
		{
			Map classMap = unit.classTable;
			for (Iterator i = classMap.keySet().iterator(); i.hasNext();)
			{
				String className = (String) i.next();
				flex2.compiler.abc.Class c = (flex2.compiler.abc.Class) classMap.get(className);
				c.setTypeTable(typeTable);
				symbolTable.registerClass(className, c);
			}

			try
			{
				symbolTable.registerStyles(unit.styles);
			}
			catch (StyleConflictException e)
			{
				// C: assume that StyleConflictException is going to be internationalized...
				ThreadLocalToolkit.logError(unit.getSource().getNameForReporting(), e.getLocalizedMessage());
			}

			flex2.compiler.as3.Compiler.evaluateLoaderClassBase(unit, typeTable);
			return;
		}

		ProgramNode node = (ProgramNode) unit.getSyntaxTree();

		flex2.compiler.Context context = unit.getContext();
		macromedia.asc.util.Context cx = (macromedia.asc.util.Context) context.getAttribute("cx");
		symbolTable.perCompileData.handler = cx.getHandler();

		// run ConstantEvaluator
		cx.pushScope(node.frame);
		ConstantEvaluator analyzer = (ConstantEvaluator) context.removeAttribute("ConstantEvaluator");
		node.evaluate(cx, analyzer);
		cx.popScope();

		if (ThreadLocalToolkit.errorCount() > 0)
		{
			return;
		}

		if (symbolTable != null)
		{
			Map classMap = typeTable.createClasses(node.clsdefs, unit.topLevelDefinitions);
			for (Iterator i = classMap.keySet().iterator(); i.hasNext();)
			{
				String className = (String) i.next();
				flex2.compiler.abc.Class c = (flex2.compiler.abc.Class) classMap.get(className);
				symbolTable.registerClass(className, c);
				unit.classTable.put(className, c);
			}

			try
			{
				symbolTable.registerStyles(unit.styles);
			}
			catch (StyleConflictException e)
			{
				// C: assume that StyleConflictException is going to be internationalized...
				ThreadLocalToolkit.logError(unit.getSource().getNameForReporting(), e.getLocalizedMessage());
			}

			flex2.compiler.as3.Compiler.evaluateLoaderClassBase(unit, typeTable);
		}

		for (int i = 0, length = compilerExtensions.size(); i < length; i++)
		{
			((Extension) compilerExtensions.get(i)).analyze4(unit, typeTable);

			if (ThreadLocalToolkit.errorCount() > 0)
			{
				return;
			}
		}
	}

	public void generate(CompilationUnit unit, SymbolTable symbolTable)
	{
		if (unit.hasTypeInfo)
		{
			return;
		}

		for (int i = 0, length = compilerExtensions.size(); i < length; i++)
		{
			((Extension) compilerExtensions.get(i)).generate(unit, null);

			if (ThreadLocalToolkit.errorCount() > 0)
			{
				return;
			}
		}

		macromedia.asc.util.Context cx = (macromedia.asc.util.Context) unit.getContext().removeAttribute("cx");
		flex2.compiler.as3.Compiler.cleanSlots((ObjectValue) unit.typeInfo, cx, unit.topLevelDefinitions);
		cx.setHandler(null);
	}

	public void postprocess(CompilationUnit unit, SymbolTable symbolTable)
	{
	}

	private void inheritSlots(CompilationUnit unit, MultiNameSet types, SymbolTable symbolTable)
	{
		flex2.compiler.Context context = unit.getContext();
		ProgramNode node = (ProgramNode) unit.getSyntaxTree();
		
		macromedia.asc.util.Context cx = (macromedia.asc.util.Context) context.getAttribute("cx");
		FlowAnalyzer flower = (FlowAnalyzer) context.getAttribute("FlowAnalyzer");
		Set processed = (Set) context.getAttribute("processed");

		for (Iterator i = types.iterator(); i.hasNext(); )
		{
			Object name = i.next();

			if (name instanceof flex2.compiler.util.QName)
			{
				flex2.compiler.util.QName qName = (flex2.compiler.util.QName) name;

				Source s = symbolTable.findSourceByQName(qName);
				CompilationUnit u = s.getCompilationUnit();
				if (unit == u)
				{
					continue;
				}
				
				ObjectValue frame = (ObjectValue) u.typeInfo;
				if (frame != null && !processed.contains(s.getName()))
				{
					//ThreadLocalToolkit.logDebug("import: " + s.getName() + " --> " + target);
					flower.inheritContextSlots(frame, node.frame, node.frame.builder, cx);
					processed.add(s.getName());
				}
			}
		}
	}

	public static void copyBytecodes(Source source, CompilationUnit unit)
	{
		try
		{
			byte[] abc = source.toByteArray();

			if (abc == null)
			{
				abc = FileUtils.toByteArray(source.getInputStream());
			}

			if (abc == null || abc.length == 0)
			{
				ThreadLocalToolkit.log(new NoBytecodeIsAvailable(), source);
			}
			else
			{
			    unit.bytes.set(abc, abc.length);
			}
		}
		catch (IOException ex)
		{
			ThreadLocalToolkit.logError(source.getNameForReporting(), ex.getLocalizedMessage());
		}
	}

	// error messages

	public static class NoBytecodeIsAvailable extends CompilerMessage.CompilerError
	{
		public NoBytecodeIsAvailable()
		{
			super();
		}
	}

	public static class BytecodeDecodingFailed extends CompilerMessage.CompilerError
	{
		public BytecodeDecodingFailed()
		{
			super();
		}
	}
}
