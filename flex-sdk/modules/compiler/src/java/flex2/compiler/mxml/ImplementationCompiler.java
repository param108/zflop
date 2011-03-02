////////////////////////////////////////////////////////////////////////////////
//
//  ADOBE SYSTEMS INCORPORATED
//  Copyright 2004-2007 Adobe Systems Incorporated
//  All Rights Reserved.
//
//  NOTICE: Adobe permits you to use, modify, and distribute this file
//  in accordance with the terms of the license agreement accompanying it.
//
////////////////////////////////////////////////////////////////////////////////

package flex2.compiler.mxml;

import flex2.compiler.*;
import flex2.compiler.as3.EmbedExtension;
import flex2.compiler.as3.SignatureExtension;
import flex2.compiler.as3.StyleExtension;
import flex2.compiler.as3.binding.BindableExtension;
import flex2.compiler.as3.binding.DataBindingExtension;
import flex2.compiler.as3.managed.ManagedExtensionError;
import flex2.compiler.common.CompilerConfiguration;
import flex2.compiler.io.FileUtil;
import flex2.compiler.io.TextFile;
import flex2.compiler.io.VirtualFile;
import flex2.compiler.mxml.builder.ApplicationBuilder;
import flex2.compiler.mxml.dom.AnalyzerAdapter;
import flex2.compiler.mxml.dom.ApplicationNode;
import flex2.compiler.mxml.dom.Node;
import flex2.compiler.mxml.gen.VelocityUtil;
import flex2.compiler.mxml.reflect.TypeTable;
import flex2.compiler.mxml.rep.DocumentInfo;
import flex2.compiler.mxml.rep.MxmlDocument;
import flex2.compiler.util.*;

import org.apache.velocity.Template;
import org.apache.velocity.VelocityContext;

import java.io.IOException;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @author Clement Wong
 */
class ImplementationCompiler implements flex2.compiler.Compiler
{
	private static final String DOC_KEY = "doc";
	private static final String CLASSDEF_TEMPLATE_PATH = "flex2/compiler/mxml/gen/";
	private static final String CLASSDEF_TEMPLATE = CLASSDEF_TEMPLATE_PATH + "ClassDef.vm";
	private static final String CLASSDEF_LIB = CLASSDEF_TEMPLATE_PATH + "ClassDefLib.vm";

	public ImplementationCompiler(flex2.compiler.mxml.Configuration mxmlConfiguration,
	                              flex2.compiler.as3.Configuration ascConfiguration,
	                              NameMappings mappings, Transcoder[] transcoders)
	{
		this.mxmlConfiguration = mxmlConfiguration;
		this.nameMappings = mappings;

		mimeTypes = new String[]{MimeMappings.MXML};
        
		// set up ASC and extensions -- mostly mirrors flex2.tools.API.getCompilers()
		asc = new flex2.compiler.as3.Compiler(ascConfiguration);
        
        // signature generation should occur before other extensions can touch the syntax tree.
        if ((ascConfiguration instanceof CompilerConfiguration)
                // currently, both configs reference same object, and are CompilerConfigurations
                && !((CompilerConfiguration)ascConfiguration).getDisableIncrementalOptimizations())
        {
            // SignatureExtension was already initialized in flex2.tools.API.getCompilers()
            asc.addCompilerExtension(SignatureExtension.getInstance());
        }
        String gendir = (mxmlConfiguration.keepGeneratedActionScript()? mxmlConfiguration.getGeneratedDirectory() : null);
		asc.addCompilerExtension(new EmbedExtension(transcoders, gendir, mxmlConfiguration.showDeprecationWarnings()));
		asc.addCompilerExtension(new StyleExtension());
		asc.addCompilerExtension(new BindableExtension(gendir));
		asc.addCompilerExtension(new DataBindingExtension(gendir, mxmlConfiguration.showBindingWarnings()));
		asc.addCompilerExtension(new ManagedExtensionError());
        // asc.addCompilerExtension(new flex2.compiler.util.TraceExtension());
    }

	private flex2.compiler.mxml.Configuration mxmlConfiguration;
	private NameMappings nameMappings;
	private String[] mimeTypes;
	private flex2.compiler.as3.Compiler asc;

	flex2.compiler.as3.Compiler getASCompiler()
	{
		return asc;
	}
	
	public boolean isSupported(String mimeType)
	{
		return mimeTypes[0].equals(mimeType);
	}

	public String[] getSupportedMimeTypes()
	{
		return mimeTypes;
	}

	public Source preprocess(Source source)
	{
		return source;
	}

	/**
	 * Traverse the MXML DOM, building an MxmlDocument object. Then use that object to generate AS3 source code.
	 * Then parse that source. 
	 * <p>Note that we're guaranteed to have all the types we need to walk the DOM, due to InterfaceCompiler's
	 * previous traversal which registered the necessary types as dependencies. However, it's still our responsibility
	 * to e.g. generate imports for Classes that are used in the generated program, and so on.
	 */
	public CompilationUnit parse1(Source source, SymbolTable symbolTable)
	{
		// use TypeTable to do the encapsulation - SymbolTable can be too low-level for MXML...
		TypeTable typeTable = (TypeTable) symbolTable.getContext().getAttribute(Compiler.TYPE_TABLE);
		if (typeTable == null)
		{
			typeTable = new TypeTable(symbolTable, nameMappings);
			symbolTable.getContext().setAttribute(Compiler.TYPE_TABLE, typeTable);
		}

		CompilationUnit unit = source.getCompilationUnit();

		/**
		 * Note: because of the way the Compiler framework works, if it's ever the case that <strong>every type request
		 * made by a given iteration of InterfaceCompiler.postprocess() fails to resolve, then postprocess() will not
		 * be reinvoked.<strong> Hence the following.
		 */
		if (hasUnresolvedNodes(unit))
		{
			return null;
		}

		ApplicationNode app = (ApplicationNode)unit.getSyntaxTree();
		assert app != null;

		DocumentInfo info = (DocumentInfo)unit.getContext().removeAttribute(Compiler.DOCUMENT_INFO);
		assert info != null;

		//	build MxmlDocument from MXML DOM
		MxmlDocument document = new MxmlDocument(unit, typeTable, info, mxmlConfiguration);
		ApplicationBuilder builder = new ApplicationBuilder(unit, typeTable, mxmlConfiguration, document);
		app.analyze(builder);

		// generate AS3 code
		VirtualFile genFile = generateImplementation(document);
		// obtain the line number map...
		DualModeLineNumberMap lineMap = document.getLineNumberMap();
		// C: null out MxmlDocument after -generated.as code generation
		document.getStylesContainer().setMxmlDocument(null);
		document = null;
		// C: MXML DOM no longer needed
		unit.setSyntaxTree(null);

		Source genSource;

		if (genFile != null && ThreadLocalToolkit.errorCount() == 0)
		{
			genSource = new Source(genFile, unit.getSource());
			// C: I don't think this is necessary...
			genSource.addFileIncludes(unit.getSource());
		}
		else
		{
			return null;
		}

		// use LogAdapter to do filtering, e.g. -generated.as -> .mxml, as line -> mxml line, etc...
		Logger original = ThreadLocalToolkit.getLogger();
		Logger adapter = new LogAdapter(original, lineMap);
		ThreadLocalToolkit.setLogger(adapter);

		// 6. invoke asc
		CompilationUnit ascUnit = asc.parse1(genSource, symbolTable);

		if (ThreadLocalToolkit.errorCount() > 0)
		{
			ThreadLocalToolkit.setLogger(original);
			return null;
		}

		ThreadLocalToolkit.setLogger(original);

		unit.getContext().setAttribute(Compiler.DELEGATE_UNIT, ascUnit);
		unit.getContext().setAttribute(Compiler.LINE_NUMBER_MAP, lineMap);

		// set this so asc can use the line number map to re-map line numbers for debug-mode movies.
		ascUnit.getContext().setAttribute(Compiler.LINE_NUMBER_MAP, lineMap);
		List bindingExpressions = (List) unit.getContext().getAttribute(Context.BINDING_EXPRESSIONS);
		ascUnit.getContext().setAttribute(Context.BINDING_EXPRESSIONS, bindingExpressions);

		unit.getSource().addFileIncludes(ascUnit.getSource());

		Source.transferMetaData(ascUnit, unit);
		Source.transferGeneratedSources(ascUnit, unit);
		Source.transferDefinitions(ascUnit, unit);
		Source.transferInheritance(ascUnit, unit);

		// 7. return CompilationUnit
		return unit;
	}

	public void parse2(CompilationUnit unit, SymbolTable symbolTable)
	{
		CompilationUnit ascUnit = (CompilationUnit) unit.getContext().getAttribute(Compiler.DELEGATE_UNIT);
		Source.transferInheritance(unit, ascUnit);

		Logger original = ThreadLocalToolkit.getLogger();
		LineNumberMap map = (LineNumberMap) unit.getContext().getAttribute(Compiler.LINE_NUMBER_MAP);
		Logger adapter = new LogAdapter(original, map);

		ThreadLocalToolkit.setLogger(adapter);
		asc.parse2(ascUnit, symbolTable);
		ThreadLocalToolkit.setLogger(original);

		Source.transferAssets(ascUnit, unit);
		Source.transferGeneratedSources(ascUnit, unit);
	}

	public void analyze1(CompilationUnit unit, SymbolTable symbolTable)
	{
		CompilationUnit ascUnit = (CompilationUnit) unit.getContext().getAttribute(Compiler.DELEGATE_UNIT);

		Logger original = ThreadLocalToolkit.getLogger();
		LineNumberMap map = (LineNumberMap) unit.getContext().getAttribute(Compiler.LINE_NUMBER_MAP);
		Logger adapter = new LogAdapter(original, map);

		ThreadLocalToolkit.setLogger(adapter);
		asc.analyze1(ascUnit, symbolTable);
		ThreadLocalToolkit.setLogger(original);

		Source.transferTypeInfo(ascUnit, unit);
		Source.transferNamespaces(ascUnit, unit);
	}

	public void analyze2(CompilationUnit unit, SymbolTable symbolTable)
	{
		CompilationUnit ascUnit = (CompilationUnit) unit.getContext().getAttribute(Compiler.DELEGATE_UNIT);
		Source.transferDependencies(unit, ascUnit);

		Logger original = ThreadLocalToolkit.getLogger();
		LineNumberMap map = (LineNumberMap) unit.getContext().getAttribute(Compiler.LINE_NUMBER_MAP);
		Logger adapter = new LogAdapter(original, map);

		ThreadLocalToolkit.setLogger(adapter);
		asc.analyze2(ascUnit, symbolTable);
		ThreadLocalToolkit.setLogger(original);

		Source.transferDependencies(ascUnit, unit);
	}

	public void analyze3(CompilationUnit unit, SymbolTable symbolTable)
	{
		CompilationUnit ascUnit = (CompilationUnit) unit.getContext().getAttribute(Compiler.DELEGATE_UNIT);
		Source.transferDependencies(unit, ascUnit);

		Logger original = ThreadLocalToolkit.getLogger();
		LineNumberMap map = (LineNumberMap) unit.getContext().getAttribute(Compiler.LINE_NUMBER_MAP);
		Logger adapter = new LogAdapter(original, map);

		ThreadLocalToolkit.setLogger(adapter);
		asc.analyze3(ascUnit, symbolTable);
		ThreadLocalToolkit.setLogger(original);
	}

	public void analyze4(CompilationUnit unit, SymbolTable symbolTable)
	{
		CompilationUnit ascUnit = (CompilationUnit) unit.getContext().getAttribute(Compiler.DELEGATE_UNIT);

		Logger original = ThreadLocalToolkit.getLogger();
		LineNumberMap map = (LineNumberMap) unit.getContext().getAttribute(Compiler.LINE_NUMBER_MAP);
		LogAdapter adapter = new LogAdapter(original, map);
		adapter.setRenamedVariableMap( (Map) ascUnit.getContext().getAttribute(Context.RENAMED_VARIABLE_MAP) );

		ThreadLocalToolkit.setLogger(adapter);
		asc.analyze4(ascUnit, symbolTable);

		if (ThreadLocalToolkit.errorCount() > 0)
		{
			ThreadLocalToolkit.setLogger(original);
			return;
		}

		ThreadLocalToolkit.setLogger(original);

		Source.transferExpressions(ascUnit, unit);
		Source.transferMetaData(ascUnit, unit);
		Source.transferLoaderClassBase(ascUnit, unit);
		Source.transferClassTable(ascUnit, unit);
		Source.transferStyles(ascUnit, unit);
	}

	public void generate(CompilationUnit unit, SymbolTable symbolTable)
	{
		CompilationUnit ascUnit = (CompilationUnit) unit.getContext().getAttribute(Compiler.DELEGATE_UNIT);

		Logger original = ThreadLocalToolkit.getLogger();

        DualModeLineNumberMap map = (DualModeLineNumberMap) unit.getContext().getAttribute(Compiler.LINE_NUMBER_MAP);
        map.flushTemp();    //  flush all compile-error-only line number mappings

        Logger adapter = new LogAdapter(original, map);

		ThreadLocalToolkit.setLogger(adapter);
		asc.generate(ascUnit, symbolTable);

		if (ThreadLocalToolkit.errorCount() > 0)
		{
			ThreadLocalToolkit.setLogger(original);
			return;
		}

		ThreadLocalToolkit.setLogger(original);

		Source.transferGeneratedSources(ascUnit, unit);
		Source.transferBytecodes(ascUnit, unit);
	}

	public void postprocess(CompilationUnit unit, SymbolTable symbolTable)
	{
        // This method is never called, because generate() always produces bytecode, which
        // causes API.postprocess() to skip calling the flex2.compiler.mxml.Compiler's
        // postprocess() method.
	}

	/**
	 *
	 */
	private boolean hasUnresolvedNodes(CompilationUnit unit)
	{
		Set checkNodes = (Set)unit.getContext().removeAttribute(Compiler.CHECK_NODES);
		if (checkNodes != null && !checkNodes.isEmpty())
		{
			for (Iterator iter = checkNodes.iterator(); iter.hasNext(); )
			{
				Node node = (Node)iter.next();
				ThreadLocalToolkit.log(new AnalyzerAdapter.CouldNotResolveToComponent(node.image), unit.getSource());
			}
		}
        return ThreadLocalToolkit.errorCount() > 0;
	}

	/**
	 *
	 */
	private final VirtualFile generateImplementation(MxmlDocument doc)
	{
		//	load template
		Template template = VelocityManager.getTemplate(CLASSDEF_TEMPLATE, CLASSDEF_LIB);
		if (template == null)
		{
			ThreadLocalToolkit.log(new UnableToLoadTemplate(CLASSDEF_TEMPLATE));
			return null;
		}

		//	evaluate template against document
		String genFileName = Compiler.getGeneratedName(mxmlConfiguration, doc.getPackageName(), doc.getClassName(),
		                                                        "-generated.as");

		Source source = doc.getCompilationUnit().getSource();

		// C: I would like to guesstimate this number based on MXML component size...
		SourceCodeBuffer out = new SourceCodeBuffer((int) (source.size() * 4));
		try
		{
            DualModeLineNumberMap lineMap = new DualModeLineNumberMap(source.getNameForReporting(), genFileName);
            doc.setLineNumberMap(lineMap);

            VelocityUtil util = new VelocityUtil(CLASSDEF_TEMPLATE_PATH, mxmlConfiguration.debug(), out, lineMap);
			VelocityContext vc = VelocityManager.getCodeGenContext(util);
			vc.put(DOC_KEY, doc);

            template.merge(vc, out);
		}
		catch (Exception e)
		{
			ThreadLocalToolkit.log(new CodeGenerationException(doc.getSourcePath(), e.getLocalizedMessage()));
			return null;
		}

		//	(flush and) return result
		if (out.getBuffer() != null)
		{
			String code = out.toString();

			if (mxmlConfiguration.keepGeneratedActionScript())
			{
				try
				{
					FileUtil.writeFile(genFileName, code);
				}
				catch (IOException e)
				{
					ThreadLocalToolkit.log(new VelocityException.UnableToWriteGeneratedFile(genFileName, e.getLocalizedMessage()));
				}
			}

			// -generated.as should use the originating source timestamp
			return new TextFile(code, genFileName, doc.getCompilationUnit().getSource().getParent(),
			                    MimeMappings.AS, doc.getCompilationUnit().getSource().getLastModified());
		}
		else
		{
			return null;
		}
	}

    // error messages

	public static class UnableToLoadTemplate extends CompilerMessage.CompilerError
	{
		public UnableToLoadTemplate(String template)
		{
			this.template = template;
			noPath();
		}

		public String template;
	}

	public static class CodeGenerationException extends CompilerMessage.CompilerError
	{
		public CodeGenerationException(String template, String message)
		{
			super();
			this.template = template;
			this.message = message;
		}

		public final String template, message;
	}
}
