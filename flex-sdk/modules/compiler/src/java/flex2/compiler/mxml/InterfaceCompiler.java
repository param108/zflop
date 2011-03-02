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

package flex2.compiler.mxml;

import flash.util.StringUtils;
import flex2.compiler.CompilationUnit;
import flex2.compiler.Logger;
import flex2.compiler.Source;
import flex2.compiler.SymbolTable;
import flex2.compiler.as3.SyntaxTreeEvaluator;
import flex2.compiler.as3.reflect.NodeMagic;
import flex2.compiler.as3.binding.ClassInfo;
import flex2.compiler.io.FileUtil;
import flex2.compiler.io.TextFile;
import flex2.compiler.io.VirtualFile;
import flex2.compiler.mxml.analyzer.SyntaxAnalyzer;
import flex2.compiler.mxml.dom.*;
import flex2.compiler.mxml.lang.*;
import flex2.compiler.mxml.reflect.*;
import flex2.compiler.mxml.rep.DocumentInfo;
import flex2.compiler.mxml.rep.DocumentInfo.NameInfo;
import flex2.compiler.mxml.rep.Script;
import flex2.compiler.util.*;

import macromedia.asc.util.ObjectList;

import org.apache.velocity.Template;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.exception.MethodInvocationException;
import org.apache.velocity.exception.ParseErrorException;
import org.apache.velocity.exception.ResourceNotFoundException;

import java.io.*;
import java.util.*;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

/**
 * The tasks of the InterfaceCompiler are:
 *
 * <p>1. Expose the public signature of our generated MXML class at the same point in the compiler workflow as a native
 * AS class would. This means generating "public signature" AS code <strong>immediately</strong> (i.e., for consumption
 * in analyze1..N). This public signature must contain:
 *
 * 	<br>-typed declarations for any MXML children with id attributes;
 * 	<br>-user script code (which might also contain public declarations).
 *
 * <p>2. "Bring in" all types referred to by component tags, so that MXML DOM traversal, which happens in the
 * ImplementationCompiler, has the information it needs to disambiguate child tags.
 *
 * <p>Example: if a &lt;Button/&gt; tag appears in the document, information contained in the definition for
 * mx.controls.Button (assuming that's what &lt;Button/&gt; resolves to) will be necessary for disambiguating properties,
 * styles, nested components, etc., in the associated MXML subtree.
 *
 * <p>The InterfaceCompiler's postprocess() phase uses (local class) DependencyAnalyzer to do this type registration.
 *
 * <p><strong>Note that not all necessary imports are registered here, only types needed to process the MXML DOM.</strong>
 * For instance, the definitions specified by name for Class- and Function-typed properties and styles need to be imported,
 * but they are not necessary for DOM processing. They're registered as imports later, <strong>during</strong> DOM
 * processing, as the properties etc. are actually processed (see e.g. Builder.TextParser's className() handler).
 *
 * <p>Why not register all such types here? Because values are subject to interpretation (e.g. @Embeds, or
 * {bindings}, may appear as rvalues, and have different type implications), and all that sort of interpretation happens
 * during DOM processing (by the Builders). Ordinary type registration happens naturally at that point; think of the
 * registration that happens here as being "early" out of necessity, but as minimal as possible.

 * <p>2a. update: the blanket early-class-import requirement due to style trimming has been removed. However, there are
 * still early-import dependencies in the Builders (other than for DOM-walking), including some that may be unnecessary.
 * One that definitely *is* and will remain necessary is due to generating classname-to-IFactory coercions: the Builder
 * code introspects the definition associated with the classname in order to generate automatic assignments of properties
 * like outerDocument. Others may be brunable, including event type defs and generic imports of the classes assigned to
 * Class-typed properties. TODO: review and narrow the set of import requests generated here to only those that are
 * strictly necessary.
 *
 * <p>At the end of the InterfaceCompiler phases, the workflow switches to ImplementationCompiler for generation of the
 * complete AS code. (As noted above, the additional dependencies outside those needed purely for the document's
 * component tags, are detected and registered during that phase.)
 *
 * <p>TODO enable auto-imports as specified by superclass metadata, e.g. [Event]. This will require sneaking code
 * generation into the beginning of analyze1().
 *
 * @author Clement Wong
 */
class InterfaceCompiler implements flex2.compiler.Compiler
{
	//	path to our velocity template
	private static final String TemplatePath = InterfaceCompiler.class.getPackage().getName().replace('.', '/') + "/gen/InterfaceDef.vm";

	//	context attributes
	private static final String AttrTypeRequests = "TypeRequests";
	private static final String AttrInlineComponentSyntaxTree = "InlineComponentSyntaxTree";
	private static final String APPLICATION_NODE = "ApplicationNode";

	/**
	 *
	 */
	public InterfaceCompiler(final flex2.compiler.mxml.Configuration mxmlConfiguration,
							 final flex2.compiler.as3.Configuration ascConfiguration,
							 NameMappings mappings)
	{
		this.mxmlConfiguration = mxmlConfiguration;
		this.nameMappings = mappings;

		mimeTypes = new String[]{MimeMappings.MXML};
		asc = new flex2.compiler.as3.Compiler(new flex2.compiler.as3.Configuration()
		{
			public boolean debug() { return false; }
            public boolean profile() { return false; }
			public boolean strict() { return ascConfiguration.strict(); }
			public int dialect() { return ascConfiguration.dialect(); }
			public boolean adjustOpDebugLine() { return ascConfiguration.adjustOpDebugLine(); }
			public boolean warnings() { return false; }
			public boolean doc() { return false; }
			public String getEncoding() { return null; }
			public boolean metadataExport() { return false; }
			public boolean warn_array_tostring_changes() { return false; }
			public boolean warn_assignment_within_conditional() { return false; }
			public boolean warn_bad_array_cast() { return false; }
			public boolean warn_bad_bool_assignment() { return false; }
			public boolean warn_bad_date_cast() { return false; }
			public boolean warn_bad_es3_type_method() { return false; }
			public boolean warn_bad_es3_type_prop() { return false; }
			public boolean warn_bad_nan_comparison() { return false; }
			public boolean warn_bad_null_assignment() { return false; }
			public boolean warn_bad_null_comparison() { return false; }
			public boolean warn_bad_undefined_comparison() { return false; }
			public boolean warn_boolean_constructor_with_no_args() { return false; }
			public boolean warn_changes_in_resolve() { return false; }
			public boolean warn_class_is_sealed() { return false; }
			public boolean warn_const_not_initialized() { return false; }
			public boolean warn_constructor_returns_value() { return false; }
			public boolean warn_deprecated_event_handler_error() { return false; }
			public boolean warn_deprecated_function_error() { return false; }
			public boolean warn_deprecated_property_error() { return false; }
			public boolean warn_duplicate_argument_names() { return false; }
			public boolean warn_duplicate_variable_def() { return false; }
			public boolean warn_for_var_in_changes() { return false; }
			public boolean warn_import_hides_class() { return false; }
			public boolean warn_instance_of_changes() { return false; }
			public boolean warn_internal_error() { return false; }
			public boolean warn_level_not_supported() { return false; }
			public boolean warn_missing_namespace_decl() { return false; }
			public boolean warn_negative_uint_literal() { return false; }
			public boolean warn_no_constructor() { return false; }
			public boolean warn_no_explicit_super_call_in_constructor() { return false; }
			public boolean warn_no_type_decl() { return false; }
			public boolean warn_number_from_string_changes() { return false; }
			public boolean warn_scoping_change_in_this() { return false; }
			public boolean warn_slow_text_field_addition() { return false; }
			public boolean warn_unlikely_function_value() { return false; }
			public boolean warn_xml_class_has_changed() { return false; }
            public ObjectList getDefine() { return ascConfiguration.getDefine(); }
		});
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
		String componentName = source.getShortName();
		if (!TextParser.isValidIdentifier(componentName))
		{
			CompilerMessage m = new InvalidComponentName(componentName);
			m.setPath(source.getNameForReporting());
			ThreadLocalToolkit.log(m);
		}

		return source;
	}

	/**
	 * Do basic MXML parse, generate AS containing public signature contributors, and parse that using our ASC instance.
	 * Result will be "outer" compilation unit, with the unit returned by ASC stashed in a context attribute.
	 */
	public CompilationUnit parse1(Source source, SymbolTable symbolTable)
	{
		flex2.compiler.Context context = new flex2.compiler.Context();

		// 1. parse/analyze MXML, or retrieve preparsed DOM
		// 2. add MXML syntax tree to a new CompilationUnit
		ApplicationNode app;
		CompilationUnit unit;

		Object preparsedSyntaxTree = source.getSourceFragment(AttrInlineComponentSyntaxTree);
		if (preparsedSyntaxTree == null)
		{
			if ((app = parseMXML(source)) == null)
			{
				return null;
			}

			unit = source.newCompilationUnit(app, context);

			// do more syntax checking, chase includes, etc.
			app.analyze(new SyntaxAnalyzer(unit, mxmlConfiguration));
			if (ThreadLocalToolkit.errorCount() > 0)
			{
				return null;
			}
		}
		else
		{
			assert preparsedSyntaxTree instanceof ApplicationNode : "bogus preparsed root node passed to InterfaceCompiler";
			app = (ApplicationNode) preparsedSyntaxTree;
			unit = source.newCompilationUnit(app, context);
		}

		unit.getContext().setAttribute(APPLICATION_NODE, app);

		//	start a new DocumentInfo. this will accumulate document state as compilation proceeds
		DocumentInfo docInfo = createDocumentInfo(app, source);
		if (ThreadLocalToolkit.errorCount() > 0)
		{
			return null;
		}

		unit.getContext().setAttribute(Compiler.DOCUMENT_INFO, docInfo);
		unit.topLevelDefinitions.add(new QName(docInfo.getPackageName(), docInfo.getClassName()));
		transferDependencies(docInfo, unit.inheritance, unit.inheritanceHistory);

		return unit;
	}
	
	public void parse2(CompilationUnit unit, SymbolTable symbolTable)
	{
		DocumentInfo docInfo = (DocumentInfo) unit.getContext().getAttribute(Compiler.DOCUMENT_INFO);
		Source source = unit.getSource();

		//	get parsed superclass info - NOTE may be null in case of error
		ClassInfo baseClassInfo = getClassInfo(source, symbolTable, docInfo.getQualifiedSuperClassName());
		if (baseClassInfo == null && docInfo.getQualifiedSuperClassName() != null)
		{
			String qualifiedClassName = NameFormatter.toDot(docInfo.getPackageName(), docInfo.getClassName());
			ThreadLocalToolkit.log(new BaseClassNotFound(qualifiedClassName, docInfo.getQualifiedSuperClassName()), source);
			return;
		}

		//	InterfaceAnalyzer will collect items to be included in generated interface code, and add them to info
		InterfaceAnalyzer analyzer = new InterfaceAnalyzer(unit, docInfo, baseClassInfo);
		ApplicationNode app = (ApplicationNode) unit.getContext().getAttribute(APPLICATION_NODE);
		app.analyze(analyzer);

		if (ThreadLocalToolkit.errorCount() > 0)
		{
			return;
		}

		//	generate AS for the interface (i.e., public signature) of our class. This will include
		//	- superclass, interface and metadata declarations, as specified in the MXML
		//	- public var declarations for id-attributed children of the MXML
		//	- user-supplied script code
		LineNumberMap map = new LineNumberMap(source.getName());
		VirtualFile generated = generateSkeleton(docInfo, analyzer.bogusImports, map, source);
		if (generated == null)
		{
			return;
		}
		map.setNewName(generated.getName());

		//	use ASC to produce new CU for generated interface source. Will be managed by "outer" MXML CU
		CompilationUnit interfaceUnit = compileInterface(generated, source, docInfo, map, symbolTable);
		if (interfaceUnit != null)
		{
			//  transfer includes from the interface unit to the real MXML unit
			unit.getSource().addFileIncludes(interfaceUnit.getSource());

			//	InterfaceUnit, LineNumberMap are used in subsequent phases of InterfaceCompiler
			unit.getContext().setAttribute(Compiler.LINE_NUMBER_MAP, map);
			unit.getContext().setAttribute(Compiler.DELEGATE_UNIT, interfaceUnit);

			Source.transferMetaData(interfaceUnit, unit);
			// Source.transferAssets(interfaceUnit, unit);
			// Source.transferGeneratedSources(interfaceUnit, unit);
		}
		else
		{
			return;
		}

		Source.transferInheritance(unit, interfaceUnit);
		
		Logger original = ThreadLocalToolkit.getLogger();
		LogAdapter adapter = new LogAdapter(original, map);
		adapter.addLineNumberMaps(unit.getSource().getSourceFragmentLineMaps());

		ThreadLocalToolkit.setLogger(adapter);
		asc.parse2(interfaceUnit, symbolTable);
		ThreadLocalToolkit.setLogger(original);
	}

	/**
	 * run asc.analyze1() on unit's private AS unit representing the public signature, shuttling results back to outer unit
	 */
	public void analyze1(CompilationUnit unit, SymbolTable symbolTable)
	{
		CompilationUnit interfaceUnit = (CompilationUnit) unit.getContext().getAttribute(Compiler.DELEGATE_UNIT);

		Logger original = ThreadLocalToolkit.getLogger();
		LineNumberMap map = (LineNumberMap) unit.getContext().getAttribute(Compiler.LINE_NUMBER_MAP);
		LogAdapter adapter = new LogAdapter(original, map);
		adapter.addLineNumberMaps(unit.getSource().getSourceFragmentLineMaps());

		ThreadLocalToolkit.setLogger(adapter);
		asc.analyze1(interfaceUnit, symbolTable);
		ThreadLocalToolkit.setLogger(original);

		Source.transferTypeInfo(interfaceUnit, unit);
		Source.transferNamespaces(interfaceUnit, unit);
	}

	/**
	 * run asc.analyze1() on unit's private AS unit representing the public signature, shuttling results back to outer unit
	 */
	public void analyze2(CompilationUnit unit, SymbolTable symbolTable)
	{
		CompilationUnit interfaceUnit = (CompilationUnit) unit.getContext().getAttribute(Compiler.DELEGATE_UNIT);
		Source.transferDependencies(unit, interfaceUnit);
		
		Logger original = ThreadLocalToolkit.getLogger();
		LineNumberMap map = (LineNumberMap) unit.getContext().getAttribute(Compiler.LINE_NUMBER_MAP);
		LogAdapter adapter = new LogAdapter(original, map);
		adapter.addLineNumberMaps(unit.getSource().getSourceFragmentLineMaps());

		ThreadLocalToolkit.setLogger(adapter);
		asc.analyze2(interfaceUnit, symbolTable);
		ThreadLocalToolkit.setLogger(original);

		Source.transferDependencies(interfaceUnit, unit);
	}

	public void analyze3(CompilationUnit unit, SymbolTable symbolTable)
	{
		CompilationUnit interfaceUnit = (CompilationUnit) unit.getContext().getAttribute(Compiler.DELEGATE_UNIT);

		// C: unit.importDefinitionStatements has no bogus statements
		//    interfaceUnit.importDefinitionStatements may have bogus statements.
		QNameSet importDefinitionStatements = new QNameSet(interfaceUnit.importDefinitionStatements);

		Source.transferDependencies(unit, interfaceUnit);

		// C: But we tell asc that those bogus statements are legitimate.
		//    Don't try this in ImplementationCompiler!
		interfaceUnit.importDefinitionStatements.addAll(importDefinitionStatements);

		Logger original = ThreadLocalToolkit.getLogger();
		LineNumberMap map = (LineNumberMap) unit.getContext().getAttribute(Compiler.LINE_NUMBER_MAP);
		LogAdapter adapter = new LogAdapter(original, map);
		adapter.addLineNumberMaps(unit.getSource().getSourceFragmentLineMaps());

		ThreadLocalToolkit.setLogger(adapter);
		asc.analyze3(interfaceUnit, symbolTable);
		ThreadLocalToolkit.setLogger(original);
	}

	/**
	 * run asc.analyze1() on unit's private AS unit representing the public signature, shuttling results back to outer unit
	 */
	public void analyze4(CompilationUnit unit, SymbolTable symbolTable)
	{
		CompilationUnit interfaceUnit = (CompilationUnit) unit.getContext().getAttribute(Compiler.DELEGATE_UNIT);

		Logger original = ThreadLocalToolkit.getLogger();
		LineNumberMap map = (LineNumberMap) unit.getContext().getAttribute(Compiler.LINE_NUMBER_MAP);
		LogAdapter adapter = new LogAdapter(original, map);
		adapter.addLineNumberMaps(unit.getSource().getSourceFragmentLineMaps());

		ThreadLocalToolkit.setLogger(adapter);
		asc.analyze4(interfaceUnit, symbolTable);
		ThreadLocalToolkit.setLogger(original);

		Source.transferDependencies(interfaceUnit, unit);
		Source.transferLoaderClassBase(interfaceUnit, unit);
		Source.transferGeneratedSources(interfaceUnit, unit);
		Source.transferClassTable(interfaceUnit, unit);
		Source.transferStyles(interfaceUnit, unit);
	}

	public void generate(CompilationUnit unit, SymbolTable symbolTable)
	{
		// Source.transferBytecodes(interfaceUnit, unit);
	}

	/**
	 * Perform stepwise breadth-first traversal of MXML DOM to generate dependency information. At each step, the set
	 * <code>checkNodes</code> contains nodes that are known to represent component (not property) tags, for which the
	 * backing classdefs are known to be available in <code>symbolTable</code>.
	 * <p>
	 * On each such node N, we run the DependencyAnalyzer to collect the set of N's child
	 * component (not property) nodes. These are added to the new <code>checkNodes</code>, and names of their backing
	 * classes are added to the compilation unit's dependencies. As a result of the latter, the classdefs should be
	 * present in symbolTable by the next time postprocess() is called.
	 * <p>
	 * The DependencyAnalyzer will also accumulate information into DocumentInfo for later use, as a side effect of this
	 * traversal.
	 */
	public void postprocess(CompilationUnit unit, SymbolTable symbolTable)
	{
		//	System.out.println("\n*** InterfaceCompiler.postProcess ***\n");

		TypeTable typeTable = (TypeTable) symbolTable.getContext().getAttribute(Compiler.TYPE_TABLE);
		if (typeTable == null)
		{
			typeTable = new TypeTable(symbolTable, nameMappings);
			symbolTable.getContext().setAttribute(Compiler.TYPE_TABLE, typeTable);
		}

		DocumentInfo info = (DocumentInfo)unit.getContext().getAttribute(Compiler.DOCUMENT_INFO);
		Set checkNodes = (Set)unit.getContext().getAttribute(Compiler.CHECK_NODES);
		Set allTypeRequests = (Set)unit.getContext().getAttribute(AttrTypeRequests);

		if (checkNodes == null)
		{
			//	first call to postprocess

			// 	seed checkNodes with root node
			checkNodes = new HashSet();
			checkNodes.add(info.getRootNode());

			//	set up type request record - apparently unit.expressions gets scrubbed of multinames that fail to resolve?
			allTypeRequests = new HashSet();
			unit.getContext().setAttribute(AttrTypeRequests, allTypeRequests);
		}

		if (!checkNodes.isEmpty())
		{
			Set newCheckNodes = new HashSet();
			Set newTypeRequests = new HashSet();
			DependencyAnalyzer analyzer = new DependencyAnalyzer(unit, typeTable, info, newCheckNodes, newTypeRequests, allTypeRequests);
			for (Iterator i = checkNodes.iterator(); i.hasNext(); )
			{
				((Node)i.next()).analyze(analyzer);
			}

			unit.getContext().setAttribute(Compiler.CHECK_NODES, newCheckNodes);
			if (!newTypeRequests.isEmpty())
			{
				//	add new type requests to our memory list
				allTypeRequests.addAll(newTypeRequests);
				unit.expressions.addAll(newTypeRequests);
			}
		}
	}

	private void transferDependencies(DocumentInfo docInfo, MultiNameSet target, MultiNameMap history)
	{
		target.add(NameFormatter.toMultiName(docInfo.getQualifiedSuperClassName()));

		Iterator iterator = docInfo.getInterfaceNames().iterator();
		while (iterator.hasNext())
		{
			NameInfo nameInfo = (NameInfo) iterator.next();
			target.add(NameFormatter.toMultiName(nameInfo.getName()));
		}
	}

	/**
	 * parse MXML source into an ApplicationNode
	 */
	private ApplicationNode parseMXML(Source source)
	{
		ApplicationNode app = null;
		InputStream in = null;
		try
		{
			in = new BufferedInputStream(source.getInputStream());
			flex2.compiler.mxml.dom.Scanner s = new flex2.compiler.mxml.dom.Scanner(in);

			Parser p = new Parser(s);
			Visitor v = new SyntaxTreeBuilder();
			p.setVisitor(v);
			app = (ApplicationNode) p.parseApplication();

			//	check for MXML 1 namespace - check code is here only so it doesn't run on every node.
			//	NOTE: of course, you can place this namespace on *any* node, and use it as a legitimate manifest key.
			//	This error does a special check in the interests of detecting an obvious mistake early.
			if (!checkMxmlNamespace(source, app))
			{
				app = null;
			}
		}
		catch (ParseException ex)
		{
			Token token = ex.currentToken.next;

			//	Strip the unhelpful "was expecting ..." part of JavaCC exception message.
			String msg = ex.getMessage();
			int wasExpecting = msg.indexOf(System.getProperty("line.separator") + "Was expecting");
			if (wasExpecting > 0)
			{
				msg = msg.substring(0, wasExpecting);

				//	experience tells us that msg is now "Encountered \"...\" at line X, column Y."
				//	Here we just want to reverse-engineer this so it can be localized, but we also fault to
				//	printing the unlocalized message if it's not the pattern above.
				//	Also note that we don't need to get the line/column info from the message; it's in token.
				Pattern msgPatt = Pattern.compile("Encountered \"(.*)\" at line.*");
				Matcher m = msgPatt.matcher(msg);
				if (m.matches())
				{
					//	convert to localized version
					msg = new InvalidText(m.group(1)).getLocalizedMessage();
				}
			}

			ThreadLocalToolkit.logError(source.getNameForReporting(), token.beginLine, token.beginColumn, msg);
			app = null;
		}
		catch (Exception ex) // FileNotFoundException, IOException
		{
			String msg = ex.getMessage();
			if (msg == null)
			{
				StringWriter stwriter = new StringWriter();
				PrintWriter pw = new PrintWriter(stwriter);
				ex.printStackTrace(pw);
				msg = stwriter.toString();
			}
			ThreadLocalToolkit.logError(source.getNameForReporting(), msg);
			app = null;
		}
		catch (ScannerError err)
		{
			String msg = err.getReason();
			// We want a different error message here, so we check for the one that we'd like to change.
			// The message is always in English.
			if (msg.equals(flex2.compiler.mxml.dom.Scanner.MarkupNotRecognizedInContent))
			{
				ThreadLocalToolkit.log(new InvalidCharacterOrMarkup(), source);
			}
			else if (msg.equals(flex2.compiler.mxml.dom.Scanner.ReservedPITarget))
			{
				ThreadLocalToolkit.log(new WhitespaceBeforePI(), source);
			}
			else if (msg.equals(flex2.compiler.mxml.dom.Scanner.MarkupNotRecognizedInMisc))
			{
				ThreadLocalToolkit.log(new InvalidMarkupAfterRootElement(), source);
			}
			else
			{
				//	regexp-based msg traps for localization
				Pattern msgPatt;
				Matcher m;

				// C: I'm not sure if the pattern matcher supports double-byte characters. Let's leave
				//    this one alone for now...
				
				//	1. The element type "..." must be terminated by the matching end-tag "</...>"
				msgPatt = Pattern.compile("The element type \"(.*)\" must be terminated by the matching end-tag \"(.*)\".");
				m = msgPatt.matcher(msg);
				if (m.matches())
				{
					msg = new MissingEndTag(m.group(1), m.group(2)).getLocalizedMessage();
				}

				//	additional traps would go here... unless we can localize Xerces, which would be better

				ThreadLocalToolkit.logError(source.getNameForReporting(), err.getLineNumber(), err.getColumnNumber(), msg);
			}

			app = null;
		}
		finally
		{
			if (in != null)
			{
				try
				{
					in.close();
				}
				catch (IOException ex)
				{
				}
			}
		}
		return app;
	}

	/**
	 *
	 */
	private boolean checkMxmlNamespace(Source source, Node node)
	{
		if (node.getNamespace().equals(Parser.MXML_1_NAMESPACE) || node.getNamespace().equals(Parser.MXML_2_NAMESPACE))
		{
			ThreadLocalToolkit.log(new WrongMXMLNamespace(node.getNamespace(), Parser.MXML_ADOBE_NAMESPACE), source);
			return false;
		}
		else
		{
			return true;
		}
	}

	/**
	 * set up a DocumentInfo for the compilation:
	 * <li>- initializes everything immediately derivable from root node and source properties
	 * <li>- populates import names with initial set
	 * <p>InterfaceAnalyzer then adds additional stuff from the DOM, prior to interface codegen
	 */
	private DocumentInfo createDocumentInfo(ApplicationNode app, Source source)
	{
		DocumentInfo info = new DocumentInfo(source.getNameForReporting());

		//	set MXML root
		info.setRootNode(app, app.beginLine);

		//	package/class is derived from source name and location
		info.setClassName(source.getShortName());
		info.setPackageName(source.getRelativePath().replace('/','.'));

		//	superclass is derived from root node name
		String superClassName = nameMappings.resolveClassName(app.getNamespace(), app.getLocalPart());
		if (superClassName != null)
		{
			info.setQualifiedSuperClassName(NameFormatter.toDot(superClassName), app.beginLine);
		}
		else
		{
			ThreadLocalToolkit.log(new AnalyzerAdapter.CouldNotResolveToComponent(app.image), source, app.beginLine);
			return null;
		}

		//	interfaces specified by "implements" attribute.
		//	TODO "implements" is language def, it should be in a list of language constants somewhere
		String interfaceNames = (String) app.getAttribute("implements");
		if (interfaceNames != null)
		{
			StringTokenizer t = new StringTokenizer(interfaceNames, ",");
			while (t.hasMoreTokens())
			{
				info.addInterfaceName(t.nextToken().trim(), app.getLineNumber("implements"));
			}
		}

		//	seed import name set with the unconditional imports present in all generated MXML classes
		info.addImportNames(StandardDefs.implicitImports, app.beginLine);
		if (mxmlConfiguration.getCompilingForAIR())
		{
			info.addImportNames(StandardDefs.airOnlyImplicitImports, app.beginLine);
		}
		info.addImportNames(StandardDefs.standardMxmlImports, app.beginLine);

		return info;
	}

	/**
	 *
	 */
	private static ClassInfo getClassInfo(Source source, SymbolTable symbolTable, String className)
	{
		//	NOTE: make throwaway ASC context, since our real one isn't created until ASC parses the generated code.
		macromedia.asc.util.Context tempContext = new macromedia.asc.util.Context(symbolTable.perCompileData);
		tempContext.setScriptName(source.getName());
		tempContext.setPath(source.getParent());

		tempContext.setEmitter(symbolTable.emitter);
		tempContext.setHandler(new flex2.compiler.as3.Compiler.CompilerHandler());
		symbolTable.perCompileData.handler = tempContext.getHandler();

		return symbolTable.getTypeAnalyzer().analyzeClass(tempContext, new MultiName(NameFormatter.toColon(className)));
	}

	/**
	 * generate skeleton code based on information in DocumentInfo
	 */
	private VirtualFile generateSkeleton(DocumentInfo info, Set bogusImports, LineNumberMap map, Source source)
	{
		//long start = System.currentTimeMillis();
		String path = source.getName();
		Template template = VelocityManager.getTemplate(TemplatePath);
		if (template != null)
		{
			try
			{
				// create a velocity context
				VelocityContext velocityContext = VelocityManager.getCodeGenContext();

				//	SourceCodeBuffer tracks line number change during codegen
				SourceCodeBuffer out = new SourceCodeBuffer((int) source.size());

				//	create SourceCode wrappers for scripts
				Set scriptSet = new LinkedHashSet();
				for (Iterator iter = info.getScripts().iterator(); iter.hasNext(); )
				{
					Script script = (Script) iter.next();
					if (!script.isEmbedded())
					{
						scriptSet.add(new SourceCode(script.getText(), script.getXmlLineNumber(), out, map));
					}
					else
					{
						// use Source.getName() to construct the new VirtualFile name
						String n = source.getName().replace('\\', '/') + ":" + script.getXmlLineNumber() + "," + script.getEndXmlLineNumber();
						VirtualFile f = new TextFile(script.getText(), n, source.getParent(), MimeMappings.AS, source.getLastModified());

						// line number map is for error reporting, so the names must come from error reporting...
						LineNumberMap m = new LineNumberMap(source.getNameForReporting(), n);

						m.put(script.getXmlLineNumber(), 1, (script.getEndXmlLineNumber() - script.getXmlLineNumber()));
						// C: add this so that when unexpected EOF occurs, (last line + 1) maps to the last line
						//    in the original XML Script block.
						m.put(script.getEndXmlLineNumber(), script.getEndXmlLineNumber() - script.getXmlLineNumber() + 1, 1);

						// 'n' must match 'n' in the include directive...
						source.addSourceFragment(n, f, m);

						// 'n' must match 'n' in the addSourceFragment call.
						scriptSet.add(new SourceCode("include \"" + n + "\";", script.getXmlLineNumber(), out, map));
					}
				}

				//	create SourceCode wrappers for metadata entries
				Set metadataSet = new LinkedHashSet();
				for (Iterator iter = info.getMetadata().iterator(); iter.hasNext(); )
				{
					Script script = (Script)iter.next();
					metadataSet.add(new SourceCode(script.getText(), script.getXmlLineNumber(), out, map));
				}

				//	create SourceCode wrappers for variable declarations
				Map varDeclMap = new LinkedHashMap();
				for (Iterator iter = info.getVarDecls().values().iterator(); iter.hasNext(); )
				{
					DocumentInfo.VarDecl varDecl = (DocumentInfo.VarDecl)iter.next();
					varDeclMap.put(varDecl.name, new SourceCode(varDecl.className, varDecl.line, out, map));
				}

				int superClassLineNumber = 1;

				Set importNameSet = new LinkedHashSet();
				for (Iterator i = info.getImportNames().iterator(); i.hasNext();)
				{
					DocumentInfo.NameInfo importName = (DocumentInfo.NameInfo) i.next();
					importNameSet.add(new SourceCode(importName.getName(), importName.getLine(), out, map));

					if (importName.getName().equals(info.getQualifiedSuperClassName()))
					{
						superClassLineNumber = importName.getLine();
					}
				}
				for (Iterator i = bogusImports.iterator(); i.hasNext();)
				{
					String importName = (String) i.next();
					importNameSet.add(new SourceCode(importName, 1, out, map));
				}

				Set interfaceNameSet = new LinkedHashSet();
				for (Iterator i = info.getInterfaceNames().iterator(); i.hasNext();)
				{
					DocumentInfo.NameInfo interfaceName = (DocumentInfo.NameInfo) i.next();
					interfaceNameSet.add(new SourceCode(interfaceName.getName(), interfaceName.getLine(), out, map));
				}

				// register values
				velocityContext.put("imports", importNameSet);
				velocityContext.put("variables", varDeclMap.entrySet());
				velocityContext.put("scripts", scriptSet);
				velocityContext.put("classMetaData", metadataSet);
				velocityContext.put("bindingManagementVariables", FrameworkDefs.bindingManagementVars);

				// C: should really give line number mappings to superclass name and interface names.
				velocityContext.put("superClassName", new SourceCode(info.getQualifiedSuperClassName(), superClassLineNumber, out, map));
				velocityContext.put("interfaceNames", interfaceNameSet);

				velocityContext.put("className", info.getClassName());
				velocityContext.put("packageName", info.getPackageName());

				// run the template!
				//long s2 = System.currentTimeMillis();
				//VelocityManager.parseTime += s2 - start;
				template.merge(velocityContext, out);
				//VelocityManager.mergeTime += System.currentTimeMillis() - s2;

				// Normalize line endings as a temporary work around for bug 149821
				String generated = out.toString().replaceAll("\r\n", "\n");

				String filename = writeGenerated(info, generated);

				return new TextFile(generated, filename, source.getParent(), MimeMappings.AS, source.getLastModified());
			}
			catch (ResourceNotFoundException ex)
			{
				ThreadLocalToolkit.logError(path, FileUtil.getExceptionMessage(ex));
			}
			catch (ParseErrorException ex)
			{
				ThreadLocalToolkit.logError(path, FileUtil.getExceptionMessage(ex));
			}
			catch (MethodInvocationException ex)
			{
				ThreadLocalToolkit.logError(path, FileUtil.getExceptionMessage(ex));
			}
			catch (Exception ex)
			{
				ThreadLocalToolkit.logError(path, FileUtil.getExceptionMessage(ex));
			}
		}

		return null;
	}

	/**
	 * TODO return real-looking filename only if file is actually created
	 */
	private String writeGenerated(DocumentInfo info, String generated) throws IOException
	{
		String filename = Compiler.getGeneratedName(mxmlConfiguration, info.getPackageName(), info.getClassName(),
										   "-interface.as");
		if (mxmlConfiguration.keepGeneratedActionScript())
		{
			new File(filename).getParentFile().mkdirs();
			FileUtil.writeFile(filename, generated);
		}
		return filename;
	}

	/**
	 * compile generated interface code. returns new CU as returned by ASC
	 */
	private CompilationUnit compileInterface(VirtualFile generated, Source origSource, DocumentInfo info, LineNumberMap map, SymbolTable symbolTable)
	{
		Source newSource = new Source(generated, origSource);

		// set the current logger to the one with line number mapping support
		Logger original = ThreadLocalToolkit.getLogger();
		LogAdapter adapter = new LogAdapter(original, map);
		adapter.addLineNumberMaps(origSource.getSourceFragmentLineMaps());
		ThreadLocalToolkit.setLogger(adapter);

		CompilationUnit interfaceUnit = asc.parse1(newSource, symbolTable);

		//	performance: strip non-signature code from parse tree; it's not needed in the Interface pass
		//	Basil: commented out to fix 169038, 169004 - both of these were due to type references that existed
		// 	only in method bodies being stripped out by this optimization. (In this case, the type references were
		//	to classes that had [Style] metadata, which is needed at implementation generation time. The classes
		//	were correctly brought in, but only as a *result* of the generated implementation.)
		//	TODO implement a more elaborate version of removeNonAPIContent, which notices and preserves typerefs
		//	SyntaxTreeEvaluator.removeNonAPIContent(interfaceUnit);

		//	make sure management vars only occur once in class chain
		SyntaxTreeEvaluator.stripRedeclaredManagementVars(interfaceUnit, info.getQName().toString(), symbolTable);

		//	metadata post-processing
		if (interfaceUnit != null && info != null)
		{
			List md = info.getMetadata();
			if (md.size() > 0)
			{
				//	ensure <mx:Metadata> contains only [metadata] annotations
				int[] beginLines = new int[md.size()], endLines = new int[md.size()];
				for (int i = 0, size = md.size(); i < size; i++)
				{
					Script script = (Script) md.get(i);
					beginLines[i] = script.getXmlLineNumber();
					endLines[i] = script.getEndXmlLineNumber();
				}
				NodeMagic.metaDataOnly(interfaceUnit, map, beginLines, endLines);
			}
		}

		if (interfaceUnit != null)
		{
			SyntaxTreeEvaluator.ensureMetaDataHasDefinition(interfaceUnit);
		}

		// reset the current logger to the original one...
		ThreadLocalToolkit.setLogger(original);
		return interfaceUnit;
	}

	/**
	 * Walk MXML DOM, accumulating public signature (interface) info into associated DocumentInfo.
	 * <p><strong>NOTE: </strong>since user script blocks may contain elements of the public signature, they must be
	 * included in the initial "skeleton" codegen. Thus <strong>any implicit imports that are necessary to propertly
	 * compile user script blocks must be added to info.importNames as a result of this traversal.</strong>
	 *
	 * <p>Note: for complete results, analyze() must be initially invoked on an ApplicationNode.
	 */
	private class InterfaceAnalyzer extends AnalyzerAdapter
	{
		private DocumentInfo docInfo;
		private ClassInfo baseClassInfo;
		private int repeaterNum = 0;
		private int inlineComponentCount = 0;
		private Set inlineComponentClassNames = new HashSet();
		private Set bogusImports = new HashSet();

		private InterfaceAnalyzer(CompilationUnit unit, DocumentInfo docInfo, ClassInfo baseClassInfo)
		{
			super(unit, null);
			this.docInfo = docInfo;
			this.baseClassInfo = baseClassInfo;
		}

		//	AnalyzerAdapter impl

		public void analyze(Node node)
		{
			boolean inRepeater = false;
			String className = nameMappings.resolveClassName(node.getNamespace(), node.getLocalPart());
			if (className != null && StandardDefs.isRepeater(className))
			{
				repeaterNum++;
				inRepeater = true;
			}

			registerVariable(node);
			super.analyze(node);

			if (inRepeater)
			{
				repeaterNum--;
			}
		}

		public void analyze(ScriptNode node)
		{
			if (node.getSourceFile() == null)
			{
				CDATANode cdata = (CDATANode) node.getChildAt(0);
				if (cdata != null)
				{
					Script script = new Script(cdata.image, cdata.beginLine, cdata.endLine);
					script.setEmbeddedScript(true);
					docInfo.addScript(script);
				}
			}
			else
			{
				String source = (String) node.getAttribute("source");
				if (source != null)
				{
					Script script = new Script("include \"" + source + "\";", node.beginLine);
					docInfo.addScript(script);
				}
			}
		}

		public void analyze(MetaDataNode node)
		{
			CDATANode cdata = (CDATANode) node.getChildAt(0);
			if (cdata != null)
			{
				Script script = new Script(cdata.image, cdata.beginLine, cdata.endLine);
				docInfo.addMetadata(script);
			}
		}

		public void analyze(ModelNode node)
		{
			registerVariable(node, "id", NameFormatter.toDot(StandardDefs.CLASS_OBJECTPROXY));
		}

		public void analyze(XMLNode node)
		{
			//	auto-import XMLUtil in generated source, if we're using XML tags
			docInfo.addImportName(NameFormatter.toDot(StandardDefs.CLASS_XMLLIST), node.beginLine);

			registerVariable(node, "id", NameFormatter.toDot(StandardDefs.getXmlBackingClassName(node.isE4X())));
		}

		public void analyze(XMLListNode node)
		{
			//	auto-import XMLUtil in generated source, if we're using XML tags
			docInfo.addImportName(NameFormatter.toDot(StandardDefs.CLASS_XMLLIST), node.beginLine);

			registerVariable(node, "id", NameFormatter.toDot(StandardDefs.CLASS_XMLLIST));
		}

		public void analyze(ArrayNode node)
		{
			registerVariable(node, "id", "Array");
			super.analyze(node);
		}

		public void analyze(BindingNode node)
		{
		}

		public void analyze(StringNode node)
		{
			registerVariable(node);
		}

		public void analyze(NumberNode node)
		{
			registerVariable(node);
		}

		public void analyze(IntNode node)
		{
			registerVariable(node);
		}

		public void analyze(UIntNode node)
		{
			registerVariable(node);
		}

		public void analyze(BooleanNode node)
		{
			registerVariable(node);
		}

		public void analyze(WebServiceNode node)
		{
			registerVariable(node);
			super.analyze(node);
		}

		public void analyze(OperationNode node)
		{
			super.analyze(node);
		}

		public void analyze(HTTPServiceNode node)
		{
			registerVariable(node);
		}

		public void analyze(RemoteObjectNode node)
		{
			registerVariable(node);
		}

		 public void analyze(InlineComponentNode node)
		{
			createInlineComponentUnit(node);

			registerVariable(node, "id", StandardDefs.INTERFACE_IFACTORY);

			//	Note: do not traverse contents
		}

		private void registerVariable(Node node)
		{
			registerVariable(node, "id");
		}

		private void registerVariable(Node node, String attrName)
		{
			registerVariable(node, attrName, null);
		}

		/**
		 * Note: if a variable is declared, an import for the declared type will be automatically added to docInfo.
		 * Caller only needs to explicitly add *other* imports that may be needed (e.g. XMLUtil for XMLNodes.)
		 */
		private void registerVariable(Node node, String attrName, String className)
		{
			String id = (String) node.getAttribute(attrName);
			int line = node.getLineNumber(attrName);

			if (id != null)
			{
				if (!TextParser.isValidIdentifier(id))
				{
					log(node, line, new InvalidIdentifier(id));
				}
				else if (docInfo.containsVarDecl(id))
				{
					log(line, new IdentifierUsedMoreThanOnce(id));
				}
				else if (docInfo.getClassName().equals(id))
				{
					log(line, new IdentifierMatchesClassName(id));
				}
				else
				{
					if (className == null)
					{
						className = nameMappings.resolveClassName(node.getNamespace(), node.getLocalPart());
					}

					// We use Array as the type when inside a Repeater.
					if (repeaterNum > 0)
					{
						className = SymbolTable.ARRAY;
					}

					if (className != null)
					{
						if (baseClassInfo == null ||
								!(baseClassInfo.definesVariable(id) ||
										baseClassInfo.definesGetter(id, true) ||
										baseClassInfo.definesSetter(id, true)))
						{
							docInfo.addVarDecl(id, NameFormatter.toDot(className), line);
						}
						else
						{
							// logInfo("base class '" + baseClassInfo.getClassName() + "' defines var/get/set '" + id +"', not declaring");
						}
					}
					else
					{
						log(line, new CouldNotResolveToComponent(node.image));
					}
				}
			}
			else
			{
				// C: This else part tries to add import statements to the *-interface.as files.
				//    It's okay to add them, as long as we trick asc to believe that they're not
				//    bogus imports, even though some of them are in fact bogus. Please see
				//    InterfaceCompiler.analyze3() to find out how we trick asc.
				if (className == null)
				{
					className = nameMappings.resolveClassName(node.getNamespace(), node.getLocalPart());
				}
				if (className != null && className.indexOf('*') == -1)
				{
					bogusImports.add(NameFormatter.toDot(className));
				}
			}
		}

		/**
		 *
		 */
		private void createInlineComponentUnit(InlineComponentNode node)
		{
			Node root = (Node) node.getChildAt(0);

			//	TODO central place for MXML language constants
			String className = (String)node.getAttribute(InlineComponentNode.CLASS_NAME_ATTR);
			if (className != null)
			{
				//	user-specified class name - must be unqualified
				if (!TextParser.isValidIdentifier(className))
				{
					log(node.getLineNumber(InlineComponentNode.CLASS_NAME_ATTR), new ClassNameInvalidActionScriptIdentifier());
					className = null;	//	let compiler proceed with generated classname
				}
				else if (inlineComponentClassNames.contains(className))
				{
					log(node.getLineNumber(InlineComponentNode.CLASS_NAME_ATTR), new ClassNameSpecifiedMoreThanOnce());
					className = null;	//	let compiler proceed with generated classname
				}
				else
				{
					inlineComponentClassNames.add(className);
				}
			}

			if (className == null)
			{
				// Create a unique class name.
				//	TODO add hash to avoid collisions with user-defined classes?
				className = docInfo.getClassName() + "_inlineComponent" + (++inlineComponentCount);
			}

			//	qualify inline component classname with specifying component's package
			QName classQName = new QName(docInfo.getPackageName(), className);

			//	save classname to the inline component node
			//	TODO ideally, we could just convert this in-place to a ClassNode for downstream processing
			//	Good example of why an explicit type descriptor enum is generally more useful than subclassing
			node.setClassQName(classQName);

			// Create a new Source for the node.
			VirtualFile virtualFile = new TextFile("", className, unit.getSource().getName(), unit.getSource().getParent(),
												   MimeMappings.MXML, unit.getSource().getLastModified());
			Source source = new Source(virtualFile, unit.getSource(), false, false);

			// Set the Source's syntax tree to the ApplicationNode
			// equivalent of the grandchild, so that the text
			// representation won't have to be recreated and reparsed.
			ApplicationNode inlineApplicationNode =
				ApplicationNode.inlineApplicationNode(root.getNamespace(), root.getLocalPart(),
						NameFormatter.toDot(docInfo.getPackageName(), docInfo.getClassName()));

			inlineApplicationNode.beginLine = root.beginLine;
			inlineApplicationNode.beginColumn = root.beginColumn;
			inlineApplicationNode.endLine = root.endLine;
			inlineApplicationNode.endColumn = root.endColumn;
			inlineApplicationNode.image = root.image;

			root.copy(inlineApplicationNode);

			source.addSourceFragment(AttrInlineComponentSyntaxTree, inlineApplicationNode, null);

			unit.addGeneratedSource(classQName, source);
		}
	}

	/**
	 * Walk MXML (sub-)DOM, using already-resolved types to go as far into the tree as you can, examining children at
	 * each level. When a node is encountered whose backing class is unresolved, put node and classname on a request queue.
	 * The type will be added to the CU's type dependency set, and you'll be called back on the corresponding node after
	 * type resolution has been attempted.
	 */
	private class DependencyAnalyzer extends AnalyzerAdapter
	{
		private final TypeTable typeTable;
		private final DocumentInfo info;
		private final Set checkNodes;
		private final Set newTypeRequests;
		private final Set allTypeRequests;
		private AttributeDependencyScanner attributeDependencyScanner;
		private ChildNodeDependencyScanner childNodeDependencyScanner;
		private ClassInitializerTextParser classInitializerTextParser;

		private DependencyAnalyzer(CompilationUnit unit, TypeTable typeTable, DocumentInfo info,
								   Set checkNodes, Set newTypeRequests, Set allTypeRequests)
		{
			super(unit, null);
			this.typeTable = typeTable;
			this.info = info;
			this.checkNodes = checkNodes;
			this.newTypeRequests = newTypeRequests;
			this.allTypeRequests = allTypeRequests;
			this.attributeDependencyScanner = new AttributeDependencyScanner();
			this.childNodeDependencyScanner = new ChildNodeDependencyScanner(typeTable);
			this.classInitializerTextParser = new ClassInitializerTextParser();
		}

		//	AnalyzerAdapter impl

		public void analyze(Node node)
		{
			registerDependencies(node);
		}

		public void analyze(CDATANode node) {}

		public void analyze(StyleNode node)
		{
			//	TODO register dependencies arising from e,g, object/inline styles
		}

		public void analyze(ScriptNode node) {}

		public void analyze(MetaDataNode node) {}

		public void analyze(ModelNode node)
		{
			requestType(StandardDefs.CLASS_OBJECTPROXY, node);
		}

		public void analyze(XMLNode node)
		{
			requestType(StandardDefs.CLASS_XMLUTIL, node);
			requestType(StandardDefs.getXmlBackingClassName(node.isE4X()), node);
		}

		public void analyze(XMLListNode node)
		{
			requestType(StandardDefs.CLASS_XMLLIST, node);
		}

		public void analyze(BindingNode node) {}

		public void analyze(StringNode node) {}

		public void analyze(NumberNode node) {}

		public void analyze(IntNode node) {}

		public void analyze(UIntNode node) {}

		public void analyze(BooleanNode node) {}

		public void analyze(WebServiceNode node)
		{
			registerDependencies(node);
		}

		public void analyze(HTTPServiceNode node)
		{
			registerDependencies(node);
		}

		public void analyze(RemoteObjectNode node)
		{
			registerDependencies(node);
		}

		public void analyze(OperationNode node)
		{
			registerDependencies(node, StandardDefs.getConvertedTagName(node));
		}

		public void analyze(RequestNode node) {}

		public void analyze(MethodNode node)
		{
			registerDependencies(node, StandardDefs.getConvertedTagName(node));
		}

		public void analyze(ArgumentsNode node) {}

		 public void analyze(InlineComponentNode node)
		{
			QName classQName = node.getClassQName();
			if (classQName == null)
			{
				log(node, new InlineComponentInternalError());
			}
			else
			{
				newTypeRequests.add(NameFormatter.toMultiName(StandardDefs.CLASS_CLASSFACTORY));
				newTypeRequests.add(NameFormatter.toMultiName(classQName));

				info.addImportName(NameFormatter.toDot(StandardDefs.CLASS_CLASSFACTORY), node.beginLine);
				info.addImportName(NameFormatter.toDot(classQName), node.beginLine);
			}
		}

		/**
		 * register dependencies on a class-backed node and its subtree.
		 */
		private void registerDependencies(Node node)
		{
			registerDependencies(node, node.getLocalPart());
		}

		/**
		 * register dependencies on a class-backed node and its subtree. (this entry point allows overriding node name -
		 * used when mapping 1.x node names to their associated 2.0 class names)
		 * <p>
		 * Per class comments, the logic here accounts for the situation where the compiler may not yet have attempted
		 * to resolve the backing class. In this instance, analysis is deferred (pending node and typename are added to
		 * their respective queues). DependencyAnalyzer will get reinvoked on the node after the compiler has attempted
		 * to resolve the pending types we specify here.
		 */
		private void registerDependencies(Node node, String localPart)
		{
			String className = nameMappings.resolveClassName(node.getNamespace(), localPart);
			if (className == null)
			{
				//	e.g. may be missing from manifest
				ThreadLocalToolkit.log(new CouldNotResolveToComponent(node.image), unit.getSource(), node.beginLine);
				return;
			}

			Type type = requestType(className, node);
			if (type != null)
			{
				//	name resolves to type def - this gives us enough info to scan attributes and children:

				//	scan attributes - see note 2a at top of file
				for (Iterator iter = node.getAttributeNames(); iter.hasNext() && ThreadLocalToolkit.errorCount() == 0; )
				{
					attributeDependencyScanner.invoke(node, type, (QName)iter.next());
				}

				//	scan child nodes
				childNodeDependencyScanner.scanChildNodes(node, type);
			}
		}

		/**
		 *
		 */
		private Type requestType(String className, Node node)
		{
			//	add to import list for later (implementation) codegen
			info.addImportName(NameFormatter.toDot(className), node.beginLine);

			//	request type from typeTable - may or may not have been loaded already
			Type type = typeTable.getType(className);
			if (type == null)
			{
				//	no type available for this name (yet) - need to register a dependency, or error if one
				//	has already been requested unsucessfully
				MultiName multiName = NameFormatter.toMultiName(className);
				if (allTypeRequests.contains(multiName))
				{
					//	type has been requested already, and has failed to resolve
					ThreadLocalToolkit.log(new CouldNotResolveToComponent(node.image), unit.getSource(), node.beginLine);
				}
				else
				{
					//	queue up new entry for dependency registration
					//	System.out.println("@ registering dependency on " + multiName);
					checkNodes.add(node);
					newTypeRequests.add(multiName);
				}
			}

			return type;
		}

		/**
		 * See note 2a at top of file.
		 */
		protected class AttributeDependencyScanner extends AttributeHandler
		{
			protected boolean isSpecial(String namespace, String localPart)
			{
				return false;
			}

			protected void special(String namespace, String localPart) {}

			protected void event(Event event)
			{
				requestEventType(event);
			}

			protected void property(Property property)
			{
				Type type = property.getType();
				if (type.equals(type.getTypeTable().classType) || StandardDefs.isInstanceGenerator(type))
				{
					classInitializerTextParser.parse(text, type);
				}
			}

			protected void effect(Effect effect) {}

			protected void style(Style style) {}

			protected void dynamicProperty(String name) {}

			protected void unknownNamespace(String namespace) {}

			protected void unknown(String name) {}
		}

		/**
		 * See note 2a at top of file.
		 */
		protected class ChildNodeDependencyScanner extends ChildNodeHandler
		{
			protected ChildNodeDependencyScanner(TypeTable typeTable)
			{
				super(typeTable);
			}

			private void scanGrandchildren()
			{
				for (Iterator iter = child.getChildIterator(); iter.hasNext(); )
					((Node)iter.next()).analyze(DependencyAnalyzer.this);
			}

			private void scanChild()
			{
				child.analyze(DependencyAnalyzer.this);
			}

			//	ChildNodeHandler impl

			protected void event(Event event)
			{
				requestEventType(event);
				scanGrandchildren();
			}

			protected void property(Property property)
			{
				Type type = property.getType();
				if (type.equals(type.getTypeTable().classType) || StandardDefs.isInstanceGenerator(type))
				{
					//	TODO process <Class>name</Class>
					CDATANode cdata = getTextContent(child.getChildren(), true);
					if (cdata != null)
					{
						classInitializerTextParser.parse(cdata.image, type);
					}
				}

				scanGrandchildren();
			}

			protected void effect(Effect effect)
			{
				scanGrandchildren();
			}

			protected void style(Style style)
			{
				scanGrandchildren();
			}

			protected void dynamicProperty(String name)
			{
				if (parent instanceof ApplicationNode)
				{
					//	root is never dynamic, but if super is, then nested declarations will trigger a call to this handler.
					//	TODO mxml.lang.DocumentChildNodeHandler
					scanChild();
				}
				else
				{
					scanGrandchildren();
				}
			}

			protected void defaultPropertyElement(boolean locError)
			{
				scanChild();
			}

			protected void nestedDeclaration()
			{
				scanChild();
			}

			protected void textContent()
			{
			}

			protected void languageNode()
			{
				scanChild();
			}
		}

		/**
		 *
		 */
		protected void requestEventType(Event event)
		{
			if (event.getType() == null)
			{
				newTypeRequests.add(NameFormatter.toMultiName(event.getTypeName()));
			}
		}

		/**
		 * here our only purpose is to pick up classnames. Everything else is ignored (including parse errors).
		 * See note 2a at top of file.
		 */
		protected class ClassInitializerTextParser extends TextParser
		{
			ClassInitializerTextParser()
			{
				super(typeTable);
			}

			public void parse(String text, Type type)
			{
				super.parse(text, type, null, 0);
			}

			//	TextParser impl

			public String contextRoot(String text)
			{
				return null;
			}

			public Object embed(String text, Type type)
			{
				if (StandardDefs.isIFactory(type))
				{
					newTypeRequests.add(NameFormatter.toMultiName(StandardDefs.CLASS_CLASSFACTORY));
				}

				return null;
			}

			public Object resource(String text, Type type)
			{
				return null;
			}

			public Object bindingExpression(String converted)
			{
				return null;
			}

			public Object percentage(String pct)
			{
				return null;
			}

			public Object array(Collection entries, Type arrayElementType)
			{
				return null;
			}

			public Object functionText(String text)
			{
				return null;
			}

			public Object className(String name, Type type)
			{
				if (StandardDefs.isIFactory(type))
				{
					newTypeRequests.add(NameFormatter.toMultiName(StandardDefs.CLASS_CLASSFACTORY));
				}

				newTypeRequests.add(NameFormatter.toMultiName(name));
				return null;
			}

			public void error(int err, String text, Type type, Type arrayElementType)
			{
			}
		}
	}

	/**
	 * wrapper class associates (string, source line number) with a destination line number set when
	 * the velocity template invokes toString. items must only be emitted once.
	 */
	private class SourceCode
	{
		private SourceCode(String text, int beginLine, SourceCodeBuffer out, LineNumberMap map)
		{
			this.text = text;
			this.beginLine = beginLine;
			this.out = out;
			this.map = map;

			newBeginLine = 0;
			lineCount = StringUtils.countLines(text);
		}

		private String text;
		private int beginLine, newBeginLine, lineCount;
		private SourceCodeBuffer out;
		private LineNumberMap map;

		public String toString()
		{
			if (newBeginLine == 0)
			{
				newBeginLine = out.getLineNumber();
				map.put(beginLine, newBeginLine, lineCount + 1);
				// System.out.println("beginLine: " + beginLine + " newBeginLine: " + newBeginLine + " lineCount: " + lineCount);
			}
			else
			{
				throw new IllegalStateException("InterfaceGenerator: toString() was called more than once...");
			}
			return text;
		}
	}

	// error messages

	public static class InvalidCharacterOrMarkup extends CompilerMessage.CompilerError
	{
	}

	public static class WhitespaceBeforePI extends CompilerMessage.CompilerError
	{
	}

	public static class WrongMXMLNamespace extends CompilerMessage.CompilerError
	{
		public WrongMXMLNamespace(String namespace1, String namespace2)
		{
			super();
			this.namespace1 = namespace1;
			this.namespace2 = namespace2;
		}

		public final String namespace1, namespace2;
	}

	public static class InvalidIdentifier extends CompilerMessage.CompilerError
	{
		public InvalidIdentifier(String id)
		{
			super();
			this.id = id;
		}

		public final String id;
	}

	public static class IdentifierUsedMoreThanOnce extends CompilerMessage.CompilerError
	{
		public IdentifierUsedMoreThanOnce(String id)
		{
			super();
			this.id = id;
		}

		public final String id;
	}

	public static class IdentifierMatchesClassName extends CompilerMessage.CompilerError
	{
		public String name;
		public IdentifierMatchesClassName (String name) { this.name = name; }
	}

	public static class ClassNameInvalidActionScriptIdentifier extends CompilerMessage.CompilerError
	{
		public ClassNameInvalidActionScriptIdentifier()
		{
			super();
		}
	}

	public static class ClassNameSpecifiedMoreThanOnce extends CompilerMessage.CompilerError
	{
		public ClassNameSpecifiedMoreThanOnce()
		{
			super();
		}
	}

	public static class InlineComponentInternalError extends CompilerMessage.CompilerError
	{
		public InlineComponentInternalError()
		{
			super();
		}
	}

	public static class BaseClassNotFound extends CompilerMessage.CompilerError
	{
		public String className, baseClassName;
		public BaseClassNotFound(String className, String baseClassName)
		{
			super();
			this.className = className;
			this.baseClassName = baseClassName;
		}
	}

	public static class InvalidComponentName extends CompilerMessage.CompilerError
	{
		public String name;
		public InvalidComponentName(String name)
		{
			this.name = name;
		}
	}

	public static class InvalidText extends CompilerMessage.CompilerError
	{
		public final String text;
		public InvalidText(String text) { this.text = text; }
	}

	public static class MissingEndTag extends CompilerMessage.CompilerError
	{
		public final String tag, endTag;
		public MissingEndTag(String tag, String endTag) { this.tag = tag; this.endTag = endTag; }
	}

	public static class InvalidMarkupAfterRootElement extends CompilerMessage.CompilerError {}
}
