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

package flex2.compiler.as3;

import flex2.compiler.CompilationUnit;
import flex2.compiler.Source;
import flex2.compiler.SymbolTable;
import flex2.compiler.as3.reflect.TypeTable;
import flex2.compiler.as3.reflect.NodeMagic;
import flex2.compiler.css.StyleConflictException;
import flex2.compiler.io.VirtualFile;
import flex2.compiler.util.*;
import flex2.compiler.util.QName;
import macromedia.asc.embedding.LintEvaluator;
import macromedia.asc.embedding.WarningConstants;
import macromedia.asc.embedding.avmplus.GlobalBuilder;
import macromedia.asc.embedding.avmplus.RuntimeConstants;
import macromedia.asc.parser.*;
import macromedia.asc.semantics.*;
import macromedia.asc.util.Context;
import macromedia.asc.util.ContextStatics;
import macromedia.asc.util.ObjectList;
import macromedia.asc.util.Slots;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.util.*;

/**
 * @author Clement Wong
 */
public class Compiler implements flex2.compiler.Compiler
{
	static
	{
		// C: Is static method call really a good idea?
		TypeValue.init();
		ObjectValue.init();
	}

	static final String AttrTypeTable = flex2.compiler.as3.reflect.TypeTable.class.getName();

	public static class CompilerHandler extends macromedia.asc.embedding.CompilerHandler
	{
		public CompilerHandler()
		{
			super();
		}

		CompilerHandler(Source s)
		{
			this.s = s;
		}

		private Source s;

		public void error2(String filename, int ln, int col, Object msg, String source)
		{
			ThreadLocalToolkit.log((CompilerMessage) msg, filename, ln, col, source);
		}

		public void warning2(String filename, int ln, int col, Object msg, String source)
		{
			ThreadLocalToolkit.log((CompilerMessage) msg, filename, ln, col, source);
		}

		public void error(String filename, int ln, int col, String msg, String source, int errorCode)
		{
			if (errorCode != -1)
			{
				ThreadLocalToolkit.logError(filename, ln, col, msg, source, errorCode);
			}
			else
			{
				ThreadLocalToolkit.logError(filename, ln, col, msg, source);
			}
		}

		public void warning(String filename, int ln, int col, String msg, String source, int errorCode)
		{
            msg = mapRenamedVariables(msg);

			if (errorCode != -1)
			{
				ThreadLocalToolkit.logWarning(filename, ln, col, msg, source, errorCode);
			}
			else
			{
				ThreadLocalToolkit.logWarning(filename, ln, col, msg, source);
			}
		}

		public void error(String filename, int ln, int col, String msg, String source)
		{
			ThreadLocalToolkit.logError(filename, ln, col, msg, source);
		}

		public void warning(String filename, int ln, int col, String msg, String source)
		{
			ThreadLocalToolkit.logWarning(filename, ln, col, msg, source);
		}

		public FileInclude findFileInclude(String parentPath, String filespec)
		{
			Object obj = s == null ? null : s.getSourceFragment(filespec);

			if (obj instanceof VirtualFile)
			{
				VirtualFile f = (VirtualFile) obj;
				if (f.getParent().equals(parentPath))
				{
					FileInclude incl = new FileInclude();
					try
					{
						if (f.isTextBased())
						{
							incl.text = f.toString();
						}
						else
						{
							incl.in = f.getInputStream();
						}
						incl.parentPath = parentPath;
						// If asc ever reports a problem, it will use the name for reporting...
						incl.fixed_filespec = f.getNameForReporting();
						return incl;
					}
					catch (IOException ex)
					{
						return null;
					}
				}
			}

			return null;
		}

        // flex2.compiler.mxml.LogAdapter has some similar logic.  Ideally it should be
        // consolidated and move into a shared Logger adapter/filter.
        private String mapRenamedVariables(String msg)
        {
            flex2.compiler.Context context = s.getCompilationUnit().getContext();
            Map renamedVariableMap = (Map) context.getAttribute(flex2.compiler.Context.RENAMED_VARIABLE_MAP);

            if (renamedVariableMap != null)
            {
                Iterator iterator = renamedVariableMap.entrySet().iterator();

                while ( iterator.hasNext() )
                {
                    Map.Entry entry = (Map.Entry) iterator.next();
                    String newVariableName = (String) entry.getKey();
                    String oldVariableName = (String) entry.getValue();
                    msg = msg.replaceAll("'" + newVariableName + "'", "'" + oldVariableName + "'");
                }
            }

            return msg;
        }
	}

	public Compiler(flex2.compiler.as3.Configuration configuration)
	{
		mimeTypes = new String[]{MimeMappings.AS};
		compilerExtensions = new ArrayList(); // ArrayList<Extension>
		this.configuration = configuration;

		processCoachSettings();
	}

    private HashMap warnMap = null;
	private String[] mimeTypes;
	private List compilerExtensions; // List<Extension>
	private flex2.compiler.as3.Configuration configuration;
    private boolean coachWarningsAsErrors = false;

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

		if ((unit != null) && (unit.getSyntaxTree() != null))
		{
			return unit;
		}

		String path = source.getName();
		BufferedInputStream in = null;
		ProgramNode node = null;

		flex2.compiler.Context context = new flex2.compiler.Context();

		macromedia.asc.util.Context cx = new macromedia.asc.util.Context(symbolTable.perCompileData);
		cx.setScriptName(source.getName());
		cx.setPath(source.getParent());

		cx.setEmitter(symbolTable.emitter);
		cx.setHandler(new CompilerHandler(source));
		symbolTable.perCompileData.handler = cx.getHandler();

		context.setAttribute("cx", cx);
        
		// conditional compilation: add config settings from the compiler configuration
        // this must be done BEFORE parsing
        final ObjectList arr = configuration.getDefine();
        if (arr != null)
        {
            cx.config_vars.addAll(arr);
        }

		if (source.isTextBased())
		{
			Parser parser = null;
			if (configuration.doc())
			{
				parser = new Parser(cx, source.getInputText(), path, true, false);
			}
			else
			{
				parser = new Parser(cx, source.getInputText(), path);
			}
			node = parser.parseProgram();

			source.close();

			cleanNodeFactory(cx.getNodeFactory());
		}
		else
		{
			try
			{
				in = new BufferedInputStream(source.getInputStream());
				Parser parser = null;
				if (configuration.doc())
				{
					if (configuration.getEncoding() == null)
					{
						parser = new Parser(cx, in, path, true, false);
					}
					else
					{
						parser = new Parser(cx, in, path, configuration.getEncoding(), true, false);
					}
				}
				else
				{
					if (configuration.getEncoding() == null)
					{
						parser = new Parser(cx, in, path);
					}
					else
					{
						parser = new Parser(cx, in, path, configuration.getEncoding());
					}
				}
				node = parser.parseProgram();

				cleanNodeFactory(cx.getNodeFactory());
			}
			catch (IOException ex)
			{
				ThreadLocalToolkit.logError(source.getNameForReporting(), ex.getLocalizedMessage());
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
		}

		if (ThreadLocalToolkit.errorCount() > 0)
		{
			return null;
		}
        
		// conditional compilation: run the AS Configurator over the syntax tree
		// (this must be done before transferDefinitions(), as early as possible after parsing)
		node.evaluate(cx, new ConfigurationEvaluator());

        if (ThreadLocalToolkit.errorCount() > 0)
        {
            return null;
        }
        
		unit = source.newCompilationUnit(node, context);

		SyntaxTreeEvaluator treeEvaluator = new SyntaxTreeEvaluator(unit);
		treeEvaluator.setLocalizationManager(ThreadLocalToolkit.getLocalizationManager());
		node.evaluate(cx, treeEvaluator);

		if (ThreadLocalToolkit.errorCount() > 0)
		{
			return null;
		}

		int size = (node.statements != null) ? node.statements.items.size() : 0;
		List definitions = new ArrayList((source.isSourcePathOwner() || source.isSourceListOwner() ||
				                          source.isResourceBundlePathOwner()) ? 1 : size);
		boolean inPackage = false;

		for (int i = 0; i < size; i++)
		{
			Node n = (Node) node.statements.items.get(i);
			if (n instanceof PackageDefinitionNode)
			{
				inPackage = !inPackage;
			}
			else if (n.isDefinition() && inPackage)
			{
				definitions.add(n);
			}
		}
		// context.setAttribute("definitions", definitions);
		transferDefinitions(unit.topLevelDefinitions, definitions);

		InheritanceEvaluator inheritanceEvaluator = new InheritanceEvaluator();
		node.evaluate(cx, inheritanceEvaluator);
		unit.inheritance.addAll( inheritanceEvaluator.getInheritance() );	
	
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

		for (int i = 0, length = compilerExtensions.size(); i < length; i++)
		{
			((Extension) compilerExtensions.get(i)).parse1(unit, typeTable);

			if (ThreadLocalToolkit.errorCount() > 0)
			{
				return null;
			}
		}

		return unit;
	}

	public void parse2(CompilationUnit unit, SymbolTable symbolTable)
	{
		TypeTable typeTable = (TypeTable) symbolTable.getContext().getAttribute(AttrTypeTable);

		for (int i = 0, length = compilerExtensions.size(); i < length; i++)
		{
			((Extension) compilerExtensions.get(i)).parse2(unit, typeTable);

			if (ThreadLocalToolkit.errorCount() > 0)
			{
				return;
			}
		}
	}

	public void analyze1(CompilationUnit unit, SymbolTable symbolTable)
	{
		TypeTable typeTable = (symbolTable != null) ? (TypeTable) symbolTable.getContext().getAttribute(AttrTypeTable) : null;

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

		// transferDependencies(node.fa_unresolved, unit.inheritance, unit.inheritanceHistory);
		transferDependencies(node.ns_unresolved, unit.namespaces, unit.namespaceHistory);

		// transferDefinitions2(unit.topLevelDefinitions, context);

		unit.typeInfo = node.frame;

        for (int i = 0, length = compilerExtensions.size(); i < length; i++)
		{
			((Extension) compilerExtensions.get(i)).analyze1(unit, typeTable);

			if (ThreadLocalToolkit.errorCount() > 0)
			{
				return;
			}
		}
	}
	
	public void analyze2(CompilationUnit unit, SymbolTable symbolTable)
	{
		TypeTable typeTable = (symbolTable != null) ? (TypeTable) symbolTable.getContext().getAttribute(AttrTypeTable) : null;

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

		transferDependencies(node.ce_unresolved, unit.types, unit.typeHistory);
		transferDependencies(node.body_unresolved, unit.types, unit.typeHistory);
		transferDependencies(node.ns_unresolved, unit.namespaces, unit.namespaceHistory);
	    transferDependencies(node.rt_unresolved, unit.expressions, unit.expressionHistory);

		// only verify import statements when strict is turned on.
		if (configuration != null && configuration.strict())
		{
			transferImportPackages(node.package_unresolved, unit.importPackageStatements);
			transferImportDefinitions(node.import_def_unresolved, unit.importDefinitionStatements);
		}

		for (int i = 0, length = compilerExtensions.size(); i < length; i++)
		{
			((Extension) compilerExtensions.get(i)).analyze2(unit, typeTable);

			if (ThreadLocalToolkit.errorCount() > 0)
			{
				return;
			}
		}
	}

	public void analyze3(CompilationUnit unit, SymbolTable symbolTable)
	{
		ProgramNode node = (ProgramNode) unit.getSyntaxTree();
		if (node.state == ProgramNode.Inheritance || node.state == ProgramNode.Else)
		{
			return;
		}

		flex2.compiler.Context context = unit.getContext();
		macromedia.asc.util.Context cx = (macromedia.asc.util.Context) context.getAttribute("cx");
		symbolTable.perCompileData.handler = cx.getHandler();

		inheritSlots(unit, unit.types, symbolTable);
		inheritSlots(unit, unit.namespaces, symbolTable);
		// C: If --coach is turned on, do inheritSlots for unit.expressions here...
        if (configuration != null && (configuration.strict() || configuration.warnings()))
        {
		    inheritSlots(unit, unit.expressions, symbolTable);
        }

		if (configuration != null && configuration.strict())
		{
			verifyImportPackages(unit.importPackageStatements, context);
			verifyImportDefinitions(unit.importDefinitionStatements, context);
		}

		if (true /*configuration.metadataExport()*/ && ! unit.getSource().isInternal())
        {
            cx.pushScope(node.frame);
	        // C: for SWC generation, use debug(). this makes MetaDataEvaluator generate go-to-definition metadata.
	        //    it's okay because compc doesn't use PostLink
	        //    for debug-mode movies, MetaDataEvaluator will generate go-to-definition metadata.
	        //    But PostLink will take them out.
            macromedia.asc.parser.MetaDataEvaluator printer = new macromedia.asc.parser.MetaDataEvaluator(configuration.debug());
            node.evaluate(cx,printer);

	        if (configuration.doc() && unit.getSource().isDebuggable())
	        {
		        StringBuffer out = new StringBuffer();
		        out.append("<asdoc>").append("\n");

		        ObjectList comments = printer.doccomments;
		        int numComments = comments.size();
		        for(int x = 0; x < numComments; x++)
		        {
			        ((DocCommentNode) comments.get(x)).emit(cx,out);
		        }
		        out.append("\n").append("</asdoc>").append("\n");
	        }

	        cx.popScope();
        }

        if (ThreadLocalToolkit.errorCount() > 0)
        {
            return;
        }

        // run ConstantEvaluator
		cx.pushScope(node.frame);
		ConstantEvaluator analyzer = new ConstantEvaluator(cx);
		context.setAttribute("ConstantEvaluator", analyzer);
		analyzer.PreprocessDefinitionTypeInfo(cx, node);
		cx.popScope();

		if (ThreadLocalToolkit.errorCount() > 0)
		{
		    return;
		}

		TypeTable typeTable = (symbolTable != null) ? (TypeTable) symbolTable.getContext().getAttribute(AttrTypeTable) : null;

		for (int i = 0, length = compilerExtensions.size(); i < length; i++)
		{
			((Extension) compilerExtensions.get(i)).analyze3(unit, typeTable);

			if (ThreadLocalToolkit.errorCount() > 0)
			{
				return;
			}
		}
	}

	public void analyze4(CompilationUnit unit, SymbolTable symbolTable)
	{
		ProgramNode node = (ProgramNode) unit.getSyntaxTree();
		if (node.state == ProgramNode.Inheritance || node.state == ProgramNode.Else)
		{
			return;
		}

		TypeTable typeTable = (symbolTable != null) ? (TypeTable) symbolTable.getContext().getAttribute(AttrTypeTable) : null;

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

		// run -strict and -coach
		if (configuration != null && configuration.warnings())
		{
			cx.pushScope(node.frame);

			LintEvaluator lint = new LintEvaluator(cx, unit.getSource().getName(), warnMap);
			node.evaluate(cx, lint);
			cx.popScope();
            lint.simpleLogWarnings(cx, coachWarningsAsErrors);
            // if we want to go back to the verbose style of warnings, uncomment the line below and
            // comment out the line above
            //lint.logWarnings(context.cx);

			lint.clear();
		}

		if (ThreadLocalToolkit.errorCount() > 0)
		{
			return;
		}

		// last step: collect class definitions, add them to the symbol table and the CompilationUnit
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

			evaluateLoaderClassBase(unit, typeTable);
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
		TypeTable typeTable = (symbolTable != null) ? (TypeTable) symbolTable.getContext().getAttribute(AttrTypeTable) : null;

		flex2.compiler.Context context = unit.getContext();
		macromedia.asc.util.Context cx = (macromedia.asc.util.Context) context.getAttribute("cx");
		symbolTable.perCompileData.handler = cx.getHandler();

		ProgramNode node = (ProgramNode) unit.getSyntaxTree();

		LineNumberMap map = (LineNumberMap) context.getAttribute("LineNumberMap");
		Emitter emitter = new BytecodeEmitter(cx, unit.getSource(),
		                                      configuration != null && configuration.debug(),
		                                      (configuration != null && configuration.adjustOpDebugLine()) ? map : null);

		cx.pushScope(node.frame);
		CodeGenerator generator = new CodeGenerator(emitter);
		if (RuntimeConstants.SWF)
		{
			generator.push_args_right_to_left(true);
		}
		node.evaluate(cx, generator);
		cx.popScope();

		if (ThreadLocalToolkit.errorCount() > 0)
		{
			return;
		}

		emitter.emit(unit.bytes);

		for (int i = 0, length = compilerExtensions.size(); i < length; i++)
		{
			((Extension) compilerExtensions.get(i)).generate(unit, typeTable);

			if (ThreadLocalToolkit.errorCount() > 0)
			{
				return;
			}
		}

		cleanSlots((ObjectValue) unit.typeInfo, cx, unit.topLevelDefinitions);
		unit.getContext().removeAttribute("cx");
		cx.setHandler(null);
	}

	public static void cleanSlots(ObjectValue ov, Context cx, QNameList definitions)
	{
        if (ov != null)
        {
            // clear this map because it points to AST nodes.
            ov.getDeferredClassMap().clear();
            
            // clean slots in the ObjectValue
            final Slots ovSlots = ov.slots;
            if (ovSlots != null)
            {
                for (int i = 0, length = ovSlots.size(); i < length; i++)
                {
                    final Slot slot = (Slot) ovSlots.get(i);
                    
                    // the following block should be relatively in sync with ContextStatics.cleanSlot()
                    if (slot != null)
                    {
                        // destroy references to the AST, but keep metadata
                        final List mdnList = slot.getMetadata();
                        if (mdnList != null)
                        {
                            for (int j = 0, size = mdnList.size(); j < size; j++)
                            {
                                final MetaDataNode mdn = (MetaDataNode) mdnList.get(j);
                                mdn.def  = null;
                                mdn.data = null;
                            }
                        }
                        
                        slot.setImplNode(null);
                    }
                }
            }
        }

        // for each QName definition, clean each slot in TypeValue slot and its prototype
        if (cx != null && definitions != null)
        {
    		for (int i = 0, size = definitions.size(); i < size; i++)
    		{
    			final TypeValue value = cx.userDefined(((QName) definitions.get(i)).toString());
    			if (value != null)
    			{
                    final Slots valueSlots = value.slots;
                    if (valueSlots != null)
                    {
                        for (int j = 0, length = valueSlots.size(); j < length; j++)
                        {
                            ContextStatics.cleanSlot((Slot) valueSlots.get(j));
                        }
                    }
                    
                    final ObjectValue proto = value.prototype;
                    if (proto != null)
                    {
                        final Slots protoSlots = proto.slots;
                        if (protoSlots != null)
                        {
                            for (int j = 0, length = protoSlots.size(); j < length; j++)
                            {
                                ContextStatics.cleanSlot((Slot) protoSlots.get(j));
                            }
                        }
                    }
    			}
            }
		}
	}

	public static void cleanNodeFactory(NodeFactory nodeFactory)
	{
		nodeFactory.pkg_defs.clear();
		nodeFactory.pkg_names.clear();
		nodeFactory.compound_names.clear();
		nodeFactory.current_package = null;
		nodeFactory.dxns = null;
		nodeFactory.use_stmts = null;
	}

	public void postprocess(CompilationUnit unit, SymbolTable symbolTable)
	{
        // This method is never called, because generate() always produces bytecode, which
        // causes API.postprocess() to skip calling the flex2.compiler.mxml.Compiler's
        // postprocess() method.
	}

	private boolean isNotPrivate(AttributeListNode attrs)
	{
        for (int i = 0, size = attrs == null ? 0 : attrs.items.size(); i < size; i++)
        {
            Node n = (Node) attrs.items.get(i);
            if (n != null && n.hasAttribute("private"))
            {
            	return false;
            }
        }
        
        return true;
	}
	
	private String getPackageDefinition(PackageDefinitionNode pkgdef)
	{
		StringBuffer packageName = new StringBuffer();
		
		if (pkgdef != null)
		{
			List list = pkgdef.name.id.list;
			for (int i = 0, size = list == null ? 0 : list.size(); i < size; i++)
			{
				IdentifierNode node = (IdentifierNode) list.get(i);
				packageName.append(node.name);
				if (i < size - 1)
				{
					packageName.append(".");
				}
			}
		}
		
		return packageName.toString();
	}
	
	private QName getClassDefinition(ClassDefinitionNode def)
	{
		return new QName(getPackageDefinition(def.pkgdef), def.name.name);
	}

	private QName getNamespaceDefinition(NamespaceDefinitionNode def)
	{
		return new QName(getPackageDefinition(def.pkgdef), def.name.name);
	}

	private QName getFunctionDefinition(FunctionDefinitionNode def)
	{
		return new QName(getPackageDefinition(def.pkgdef), def.name.identifier.name);
	}

	private QName getVariableBinding(PackageDefinitionNode pkgdef, VariableBindingNode def)
	{
		return new QName(getPackageDefinition(pkgdef), def.variable.identifier.name);
	}

	private void transferDefinitions(Collection topLevelDefinitions, List definitions)
	{
		for (int i = 0, size = definitions.size(); i < size; i++)
		{
			Node n = (Node) definitions.get(i);
			if (n instanceof ClassDefinitionNode)
			{
				ClassDefinitionNode def = (ClassDefinitionNode) n;
				if (isNotPrivate(def.attrs))
				{
					topLevelDefinitions.add(getClassDefinition(def));
				}
			}
			else if (n instanceof NamespaceDefinitionNode)
			{
				NamespaceDefinitionNode def = (NamespaceDefinitionNode) n;
                
                // CNDNs are for conditional compilation, and only on the syntax tree for
                // ASC error handling -- they are effectively hidden to us
				if (isNotPrivate(def.attrs) && !(n instanceof ConfigNamespaceDefinitionNode))
				{
					topLevelDefinitions.add(getNamespaceDefinition(def));
				}
			}
			else if (n instanceof FunctionDefinitionNode)
			{
				FunctionDefinitionNode def = (FunctionDefinitionNode) n;
				if (isNotPrivate(def.attrs))
				{
					topLevelDefinitions.add(getFunctionDefinition(def));
				}
			}
			else if (n instanceof VariableDefinitionNode)
			{
				VariableDefinitionNode def = (VariableDefinitionNode) n;
				if (isNotPrivate(def.attrs))
				{
					for (int j = 0, length = def.list == null ? 0 : def.list.size(); j < length; j++)
					{
						VariableBindingNode binding = (VariableBindingNode) def.list.items.get(j);
						topLevelDefinitions.add(getVariableBinding(def.pkgdef, binding));
					}
				}
			}
		}		
	}

	private void transferDefinitions2(Collection topLevelDefinitions, flex2.compiler.Context context)
	{
		List definitions = (List) context.removeAttribute("definitions");
		for (int i = 0, size = definitions.size(); i < size; i++)
		{
			Node n = (Node) definitions.get(i);
			if (n instanceof ClassDefinitionNode)
			{
				ClassDefinitionNode def = (ClassDefinitionNode) n;
				if (def.attrs == null || !def.attrs.hasPrivate)
				{
					macromedia.asc.semantics.QName qName = def.cframe.builder.classname;
					topLevelDefinitions.add(new flex2.compiler.util.QName(qName.ns.name, qName.name));
				}
			}
			else if (n instanceof NamespaceDefinitionNode)
			{
				NamespaceDefinitionNode def = (NamespaceDefinitionNode) n;
				if ((def.attrs == null || !def.attrs.hasPrivate) && !(n instanceof ConfigNamespaceDefinitionNode))
				{
					macromedia.asc.semantics.QName qName = def.qualifiedname;
					topLevelDefinitions.add(new flex2.compiler.util.QName(qName.ns.name, qName.name));
				}
			}
			else if (n instanceof FunctionDefinitionNode)
			{
				FunctionDefinitionNode def = (FunctionDefinitionNode) n;
				if (def.attrs == null || !def.attrs.hasPrivate)
				{
					ReferenceValue ref = def.ref;
					String ns = ref.namespaces.size() == 0 ? "" : ((ObjectValue) ref.namespaces.get(0)).name;
					topLevelDefinitions.add(new flex2.compiler.util.QName(ns, ref.name));
				}
			}
			else if (n instanceof VariableDefinitionNode)
			{
				VariableDefinitionNode def = (VariableDefinitionNode) n;
				if (def.attrs == null || !def.attrs.hasPrivate)
				{
					for (int j = 0, length = def.list == null ? 0 : def.list.size(); j < length; j++)
					{
						VariableBindingNode binding = (VariableBindingNode) def.list.items.get(j);
						ReferenceValue ref = binding.ref;
						String ns = ref.namespaces.size() == 0 ? "" : ((ObjectValue) ref.namespaces.get(0)).name;
						topLevelDefinitions.add(new flex2.compiler.util.QName(ns, ref.name));
					}
				}
			}
			else if (n instanceof ImportDirectiveNode || n instanceof IncludeDirectiveNode || n instanceof UseDirectiveNode)
			{
			}
		}
	}

	// C: as long as the compiler instance is not shared among multiple concurrent requests, this is okay.
	private final List nsList = new ArrayList();

	private void transferDependencies(Set unresolved, MultiNameSet target, MultiNameMap history)
	{
		for (Iterator i = unresolved.iterator(); i.hasNext();)
		{
			ReferenceValue ref = (ReferenceValue) i.next();

			nsList.clear();
			for (int k = 0, length = ref.namespaces.size(); k < length; k++)
			{
				NamespaceValue nsValue = (NamespaceValue) ref.namespaces.get(k);
				String ns = nsValue.name;
				int nsKind = nsValue.getNamespaceKind();
				// C: skip NS_PRIVATE, NS_PROTECTED and NS_STATICPROTECTED. The inheritance relationships should
				//    take care of PROTECTED...
				if ((nsKind == Context.NS_PUBLIC || nsKind == Context.NS_INTERNAL) && !nsList.contains(ns))
				{
					nsList.add(ns);
				}
			}
			String[] namespaceURI = new String[nsList.size()];
			nsList.toArray(namespaceURI);

			if (!history.containsKey(namespaceURI, ref.name))
			{
				target.add(namespaceURI, ref.name);
			}
		}

		unresolved.clear();
	}

	private void transferImportPackages(Set unresolved, Set target)
	{
		for (Iterator i = unresolved.iterator(); i.hasNext();)
		{
			ReferenceValue ref = (ReferenceValue) i.next();
			target.add(ref.name);
		}

		unresolved.clear();
	}

	private void transferImportDefinitions(Set unresolved, QNameSet target)
	{
		for (Iterator i = unresolved.iterator(); i.hasNext();)
		{
			ReferenceValue ref = (ReferenceValue) i.next();

			for (int k = 0, length = ref.namespaces.size(); k < length; k++)
			{
				NamespaceValue nsValue = (NamespaceValue) ref.namespaces.get(k);
				String ns = nsValue.name;
				int nsKind = nsValue.getNamespaceKind();
				// C: skip NS_PRIVATE, NS_PROTECTED and NS_STATICPROTECTED. The inheritance relationships should
				//    take care of PROTECTED...
				if (nsKind == Context.NS_PUBLIC || nsKind == Context.NS_INTERNAL)
				{
					target.add(ns, ref.name);
				}

				break;
			}
		}

		unresolved.clear();
	}

	private void verifyImportPackages(Set imports, flex2.compiler.Context context)
	{
		macromedia.asc.util.Context cx = (macromedia.asc.util.Context) context.getAttribute("cx");

		for (Iterator i = imports.iterator(); i.hasNext(); )
		{
			ObjectValue ns = cx.getNamespace((String) i.next());
			if (ns != null)
			{
				ns.setPackage(true);
			}
		}
	}

	private void verifyImportDefinitions(QNameSet imports, flex2.compiler.Context context)
	{
		macromedia.asc.util.Context cx = (macromedia.asc.util.Context) context.getAttribute("cx");

		// imports contains only definitions that are available... it doesn't mean that they are linked in.
		for (Iterator i = imports.iterator(); i.hasNext(); )
		{
			QName qName = (QName) i.next();
			// verify import statements
			cx.addValidImport(qName.toString());
		}
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

	public static void evaluateLoaderClassBase(CompilationUnit unit, TypeTable typeTable)
	{
		for (Iterator it = unit.topLevelDefinitions.iterator(); it.hasNext();)
		{
		    QName qName = (QName) it.next();

		    flex2.compiler.abc.Class c = typeTable.getClass( qName.toString() );
		    if (c == null)
		        continue;
		    getParentLoader(unit, typeTable, c);
		}
	}

	private static void getParentLoader(CompilationUnit u, TypeTable typeTable, flex2.compiler.abc.Class c)
	{
	    flex2.compiler.abc.Class sc = typeTable.getClass( c.getSuperTypeName() );
	    if (sc == null)
	        return;

	    List inherited = sc.getMetaData( "Frame", true );
	    String inheritedLoaderClass = null;
	    for (Iterator it = inherited.iterator(); it.hasNext();)
	    {
	        flex2.compiler.abc.MetaData md = (flex2.compiler.abc.MetaData) it.next();

	        String lc = md.getValue( "factoryClass" );
	        if (lc != null)
	        {
	            inheritedLoaderClass = NodeMagic.normalizeClassName( lc );
	            break;
	        }
	    }

		/* C: not doing anything to evaluate u.loaderClassBase...
	    List local = c.getMetaData( "Frame", false );
	    String localLoaderClass = null;
	    for (Iterator it = local.iterator(); it.hasNext();)
	    {
	        flex2.compiler.abc.MetaData md = (flex2.compiler.abc.MetaData) it.next();

	        String lc = md.getValue( "factoryClass" );
	        if (lc != null)
	        {
	            localLoaderClass = normalizeClassName( lc );
	            break;
	        }
	    }
	    */

	    u.loaderClassBase = inheritedLoaderClass;

		/* C: not doing anything to evaluate u.loaderClassBase...
	    if (inheritedLoaderClass != null)
	        System.err.println(c.getName() + " local loader = " + localLoaderClass + " inherited loader = " + inheritedLoaderClass );
	    */
	}

	private void processCoachSettings()
	{
		if (warnMap == null)
		{
			warnMap = LintEvaluator.getWarningDefaults();

			warnMap.put(IntegerPool.getNumber(WarningConstants.kWarning_ArrayToStringChanges), configuration.warn_array_tostring_changes() ? Boolean.TRUE : Boolean.FALSE);
			warnMap.put(IntegerPool.getNumber(WarningConstants.kWarning_AssignmentWithinConditional), configuration.warn_assignment_within_conditional() ? Boolean.TRUE : Boolean.FALSE);
			warnMap.put(IntegerPool.getNumber(WarningConstants.kWarning_BadArrayCast), configuration.warn_bad_array_cast() ? Boolean.TRUE : Boolean.FALSE);
			warnMap.put(IntegerPool.getNumber(WarningConstants.kWarning_BadBoolAssignment), configuration.warn_bad_bool_assignment() ? Boolean.TRUE : Boolean.FALSE);
			warnMap.put(IntegerPool.getNumber(WarningConstants.kWarning_BadDateCast), configuration.warn_bad_date_cast() ? Boolean.TRUE : Boolean.FALSE);
			warnMap.put(IntegerPool.getNumber(WarningConstants.kWarning_BadES3TypeMethod), configuration.warn_bad_es3_type_method() ? Boolean.TRUE : Boolean.FALSE);
			warnMap.put(IntegerPool.getNumber(WarningConstants.kWarning_BadES3TypeProp), configuration.warn_bad_es3_type_prop() ? Boolean.TRUE : Boolean.FALSE);
			warnMap.put(IntegerPool.getNumber(WarningConstants.kWarning_BadNaNComparision), configuration.warn_bad_nan_comparison() ? Boolean.TRUE : Boolean.FALSE);
			warnMap.put(IntegerPool.getNumber(WarningConstants.kWarning_BadNullAssignment), configuration.warn_bad_null_assignment() ? Boolean.TRUE : Boolean.FALSE);
			warnMap.put(IntegerPool.getNumber(WarningConstants.kWarning_BadNullComparision), configuration.warn_bad_null_comparison() ? Boolean.TRUE : Boolean.FALSE);
			warnMap.put(IntegerPool.getNumber(WarningConstants.kWarning_BadUndefinedComparision), configuration.warn_bad_undefined_comparison() ? Boolean.TRUE : Boolean.FALSE);
			warnMap.put(IntegerPool.getNumber(WarningConstants.kWarning_BooleanConstructorWithNoArgs), configuration.warn_boolean_constructor_with_no_args() ? Boolean.TRUE : Boolean.FALSE);
			warnMap.put(IntegerPool.getNumber(WarningConstants.kWarning_ChangesInResolve), configuration.warn_changes_in_resolve() ? Boolean.TRUE : Boolean.FALSE);
			warnMap.put(IntegerPool.getNumber(WarningConstants.kWarning_ClassIsSealed), configuration.warn_class_is_sealed() ? Boolean.TRUE : Boolean.FALSE);
			warnMap.put(IntegerPool.getNumber(WarningConstants.kWarning_ConstNotInitialized), configuration.warn_const_not_initialized() ? Boolean.TRUE : Boolean.FALSE);
			warnMap.put(IntegerPool.getNumber(WarningConstants.kWarning_ConstructorReturnsValue), configuration.warn_constructor_returns_value() ? Boolean.TRUE : Boolean.FALSE);
			warnMap.put(IntegerPool.getNumber(WarningConstants.kWarning_DepricatedEventHandlerError), configuration.warn_deprecated_event_handler_error() ? Boolean.TRUE : Boolean.FALSE);
			warnMap.put(IntegerPool.getNumber(WarningConstants.kWarning_DepricatedFunctionError), configuration.warn_deprecated_function_error() ? Boolean.TRUE : Boolean.FALSE);
			warnMap.put(IntegerPool.getNumber(WarningConstants.kWarning_DepricatedPropertyError), configuration.warn_deprecated_property_error() ? Boolean.TRUE : Boolean.FALSE);
			warnMap.put(IntegerPool.getNumber(WarningConstants.kWarning_DuplicateArgumentNames), configuration.warn_duplicate_argument_names() ? Boolean.TRUE : Boolean.FALSE);
			warnMap.put(IntegerPool.getNumber(WarningConstants.kWarning_DuplicateVariableDef), configuration.warn_duplicate_variable_def() ? Boolean.TRUE : Boolean.FALSE);
			warnMap.put(IntegerPool.getNumber(WarningConstants.kWarning_ForVarInChanges), configuration.warn_for_var_in_changes() ? Boolean.TRUE : Boolean.FALSE);
			warnMap.put(IntegerPool.getNumber(WarningConstants.kWarning_ImportHidesClass), configuration.warn_import_hides_class() ? Boolean.TRUE : Boolean.FALSE);
			warnMap.put(IntegerPool.getNumber(WarningConstants.kWarning_InstanceOfChanges), configuration.warn_instance_of_changes() ? Boolean.TRUE : Boolean.FALSE);
			warnMap.put(IntegerPool.getNumber(WarningConstants.kWarning_InternalError), configuration.warn_internal_error() ? Boolean.TRUE : Boolean.FALSE);
			warnMap.put(IntegerPool.getNumber(WarningConstants.kWarning_LevelNotSupported), configuration.warn_level_not_supported() ? Boolean.TRUE : Boolean.FALSE);
			warnMap.put(IntegerPool.getNumber(WarningConstants.kWarning_MissingNamespaceDecl), configuration.warn_missing_namespace_decl() ? Boolean.TRUE : Boolean.FALSE);
			warnMap.put(IntegerPool.getNumber(WarningConstants.kWarning_NegativeUintLiteral), configuration.warn_negative_uint_literal() ? Boolean.TRUE : Boolean.FALSE);
			warnMap.put(IntegerPool.getNumber(WarningConstants.kWarning_NoConstructor), configuration.warn_no_constructor() ? Boolean.TRUE : Boolean.FALSE);
			warnMap.put(IntegerPool.getNumber(WarningConstants.kWarning_NoExplicitSuperCallInConstructor), configuration.warn_no_explicit_super_call_in_constructor() ? Boolean.TRUE : Boolean.FALSE);
			warnMap.put(IntegerPool.getNumber(WarningConstants.kWarning_NoTypeDecl), configuration.warn_no_type_decl() ? Boolean.TRUE : Boolean.FALSE);
			warnMap.put(IntegerPool.getNumber(WarningConstants.kWarning_NumberFromStringChanges), configuration.warn_number_from_string_changes() ? Boolean.TRUE : Boolean.FALSE);
			warnMap.put(IntegerPool.getNumber(WarningConstants.kWarning_ScopingChangeInThis), configuration.warn_scoping_change_in_this() ? Boolean.TRUE : Boolean.FALSE);
			warnMap.put(IntegerPool.getNumber(WarningConstants.kWarning_SlowTextFieldAddition), configuration.warn_slow_text_field_addition() ? Boolean.TRUE : Boolean.FALSE);
			warnMap.put(IntegerPool.getNumber(WarningConstants.kWarning_UnlikelyFunctionValue), configuration.warn_unlikely_function_value() ? Boolean.TRUE : Boolean.FALSE);
			warnMap.put(IntegerPool.getNumber(WarningConstants.kWarning_XML_ClassHasChanged), configuration.warn_xml_class_has_changed() ? Boolean.TRUE : Boolean.FALSE);
		}
	}
}

