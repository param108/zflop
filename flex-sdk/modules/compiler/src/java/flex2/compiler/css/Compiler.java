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

package flex2.compiler.css;

import flash.css.FontFaceRule;
import flash.css.Rule;
import flash.css.RuleList;
import flash.css.StyleRule;
import flash.css.StyleSheet;
import flash.fonts.FontManager;
import flex2.compiler.CompilationUnit;
import flex2.compiler.Context;
import flex2.compiler.Logger;
import flex2.compiler.Source;
import flex2.compiler.SymbolTable;
import flex2.compiler.Transcoder;
import flex2.compiler.as3.EmbedExtension;
import flex2.compiler.common.CompilerConfiguration;
import flex2.compiler.io.FileUtil;
import flex2.compiler.io.LocalFile;
import flex2.compiler.io.TextFile;
import flex2.compiler.io.VirtualFile;
import flex2.compiler.mxml.LogAdapter;
import flex2.compiler.mxml.SourceCodeBuffer;
import flex2.compiler.mxml.gen.VelocityUtil;
import flex2.compiler.mxml.lang.StandardDefs;
import flex2.compiler.mxml.lang.TextParser;
import flex2.compiler.mxml.rep.AtEmbed;
import flex2.compiler.util.CompilerMessage.CompilerError;
import flex2.compiler.util.DualModeLineNumberMap;
import flex2.compiler.util.LineNumberMap;
import flex2.compiler.util.MimeMappings;
import flex2.compiler.util.ThreadLocalToolkit;
import flex2.compiler.util.VelocityException.GenerateException;
import flex2.compiler.util.VelocityException.TemplateNotFound;
import flex2.compiler.util.VelocityException.UnableToWriteGeneratedFile;
import flex2.compiler.util.VelocityManager;
import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.apache.batik.css.parser.AbstractSelector;
import org.apache.velocity.Template;
import org.apache.velocity.VelocityContext;
import org.w3c.css.sac.AttributeCondition;
import org.w3c.css.sac.Condition;
import org.w3c.css.sac.ConditionalSelector;
import org.w3c.css.sac.ElementSelector;
import org.w3c.css.sac.Selector;
import org.w3c.css.sac.SelectorList;

public class Compiler implements flex2.compiler.Compiler
{
    private static final String TEMPLATE_PATH = "flex2/compiler/css/";
    private static final String STYLE_MODULE_KEY = "styleModule";
    private static final String STYLE_MODULE_TEMPLATE = TEMPLATE_PATH + "StyleModule.vm";
    private static final String STYLE_LIBRARY_TEMPLATE = TEMPLATE_PATH + "StyleLibrary.vm";
    private static final String DELEGATE_UNIT = "DelegateUnit";
    private static final String LINE_NUMBER_MAP = "LineNumberMap";

    private String[] mimeTypes;
    private CompilerConfiguration configuration;
    private flex2.compiler.as3.Compiler asc;

    public Compiler(CompilerConfiguration configuration, Transcoder[] transcoders)
    {
        this.configuration = configuration;
        mimeTypes = new String[]{MimeMappings.CSS};
        asc = new flex2.compiler.as3.Compiler(configuration);
        String gendir = (configuration.keepGeneratedActionScript()? configuration.getGeneratedDirectory() : null);
        asc.addCompilerExtension(new EmbedExtension(transcoders, gendir, configuration.showDeprecationWarnings()));
    }

    /**
     * If this compiler can process the specified file, return true.
     */
    public boolean isSupported(String mimeType)
    {
        return mimeTypes[0].equals(mimeType);
    }

    private String generateStyleName(Source source)
    {
        String result = source.getName();

        int lastSeparator = result.lastIndexOf(File.separator);

        if (lastSeparator != -1)
        {
            result = result.substring(lastSeparator + 1);

            int extension = result.indexOf(".css");

            if (extension != -1)
            {
                result = result.substring(0, extension);
            }
        }

        return result;
    }

    /**
     * Return supported mime types.
     */
    public String[] getSupportedMimeTypes()
    {
        return mimeTypes;
    }

    /**
     * Pre-process source file.
     */
    public Source preprocess(Source source)
    {
        // The defs (mx.core:FlexModuleFactory, mx.core:mx_internal,
        // mx.core:IFlexModule, and mx.core:IFlexModuleFactory) are
        // used in frame 1, so they can't be externed.  ModuleBase is
        // used by the generated source, so it can't be externed
        // either.  The generated source depends on the rest of these
        // defs, but they should already be included in the loading
        // SWF, so we explicitly extern them.  The list was created by
        // examining a link report from a simple CSS compilation.
        // Although, this process is manual, it was alot easier than
        // writing a custom linker.  One alternative was to extern all
        // of framework.swc, but this prevents customers from using
        // skin assets from framework.swc in their runtime CSS
        // modules.
    	if (!configuration.archiveClassesAndAssets())
    	{
        configuration.addExtern(StandardDefs.CLASS_CSSSTYLEDECLARATION);
        configuration.addExtern(StandardDefs.CLASS_DOWNLOADPROGRESSBAR);
        configuration.addExtern(StandardDefs.CLASS_FLEXEVENT);
        configuration.addExtern(StandardDefs.CLASS_FLEXSPRITE);
        configuration.addExtern(StandardDefs.CLASS_LOADERCONFIG);
        configuration.addExtern(StandardDefs.CLASS_MODULEEVENT);
        configuration.addExtern(StandardDefs.CLASS_MODULEMANAGER);
        configuration.addExtern(StandardDefs.CLASS_PRELOADER);
        configuration.addExtern(StandardDefs.CLASS_STYLEEVENT);
		// Don't extern StyleManager.  It is needed in case the module
		// is loaded in a bootstrap topology
        // configuration.addExtern(StandardDefs.CLASS_STYLEMANAGER);
        configuration.addExtern(StandardDefs.CLASS_SYSTEMCHILDRENLIST);
        configuration.addExtern(StandardDefs.CLASS_SYSTEMMANAGER);
        configuration.addExtern(StandardDefs.CLASS_SYSTEMRAWCHILDRENLIST);
        configuration.addExtern(StandardDefs.INTERFACE_ICHILDLIST);
        configuration.addExtern(StandardDefs.INTERFACE_IFLEXDISPLAYOBJECT);
        configuration.addExtern(StandardDefs.INTERFACE_IFOCUSMANAGERCONTAINER);
        configuration.addExtern(StandardDefs.INTERFACE_IINVALIDATING);
        configuration.addExtern(StandardDefs.INTERFACE_ILAYOUTMANAGERCLIENT);
        configuration.addExtern(StandardDefs.INTERFACE_IMODULEINFO);
        configuration.addExtern(StandardDefs.INTERFACE_IRAWCHILDRENCONTAINER);
        configuration.addExtern(StandardDefs.INTERFACE_ISIMPLESTYLECLIENT);
        configuration.addExtern(StandardDefs.INTERFACE_ISTYLECLIENT);
        configuration.addExtern(StandardDefs.INTERFACE_ISYSTEMMANAGER);
        configuration.addExtern(StandardDefs.INTERFACE_IUICOMPONENT);
    	}
    	
        String componentName = source.getShortName();
        if (!TextParser.isValidIdentifier(componentName))
        {
            InvalidComponentName invalidComponentName = new InvalidComponentName(componentName);
            invalidComponentName.setPath(source.getNameForReporting());
            ThreadLocalToolkit.log(invalidComponentName);
        }

        return source;
    }

    /**
     * Parse... The implementation must:
     *
     * 1. create a compilation unit
     * 2. put the Source object and the syntax tree in the compilation unit
     * 3. register unit.includes, unit.dependencies, unit.topLevelDefinitions and unit.metadata
     */
    public CompilationUnit parse1(Source source, SymbolTable symbolTable)
    {
        FontManager fontManager = configuration.getFontsConfiguration().getTopLevelManager();

        StyleSheet styleSheet = new StyleSheet();
        styleSheet.checkDeprecation(configuration.showDeprecationWarnings());

        try
        {
            styleSheet.parse(source.getName(),
                             source.getInputStream(),
                             ThreadLocalToolkit.getLogger(),
                             fontManager);
        }
        catch (Exception exception)
        {
            exception.printStackTrace();
            ParseError parseError = new ParseError(exception.getLocalizedMessage());
            parseError.setPath(source.getName());
            ThreadLocalToolkit.log(parseError);
            return null;
        }

        if (styleSheet.errorsExist())
        {
            // Error
            ThreadLocalToolkit.getLogger().log(new StyleSheetParseError(source.getName()));
        }

        String styleName = generateStyleName(source);

        StyleModule styleModule = new StyleModule(styleName, source, symbolTable.perCompileData);

        extractStyles(styleSheet, styleModule);

        Context context = new Context();

        CompilationUnit cssCompilationUnit = source.newCompilationUnit(null, context);

        VirtualFile generatedFile = generateSourceCodeFile(cssCompilationUnit, styleModule);

        Source generatedSource = new Source(generatedFile, source);

        // when building a SWC, we want to locate all the asset sources and ask compc to put them in the SWC.
        Collection atEmbeds = styleModule.getAtEmbeds();
        if (atEmbeds != null && configuration.archiveClassesAndAssets())
        {
        	Map archiveFiles = new HashMap();
        	for (Iterator i = atEmbeds.iterator(); i.hasNext(); )
        	{
        		AtEmbed e = (AtEmbed) i.next();
        		String src = (String) e.getAttributes().get(Transcoder.SOURCE);
        		String original = (String) e.getAttributes().get(Transcoder.ORIGINAL);
        		if (src != null)
        		{
        			archiveFiles.put(original, new LocalFile(new File(src)));
        		}
        	}
        	if (archiveFiles.size() > 0)
        	{
        		context.setAttribute(Context.CSS_ARCHIVE_FILES, archiveFiles);
        	}
        }
        
        // Use LogAdapter to do filtering, e.g. -generated.as -> .css, as line -> css
        // line, etc...
        Logger original = ThreadLocalToolkit.getLogger();
        LineNumberMap lineNumberMap = styleModule.getLineNumberMap();
        Logger adapter = new LogAdapter(original, lineNumberMap);
        ThreadLocalToolkit.setLogger(adapter);

        CompilationUnit ascCompilationUnit = asc.parse1(generatedSource, symbolTable);

        if (ascCompilationUnit != null)
        {
            // transfer includes from the ASC unit to the CSS unit
            cssCompilationUnit.getSource().addFileIncludes(ascCompilationUnit.getSource());
            context.setAttribute(Compiler.DELEGATE_UNIT, ascCompilationUnit);
            context.setAttribute(Compiler.LINE_NUMBER_MAP, lineNumberMap);
            Source.transferMetaData(ascCompilationUnit, cssCompilationUnit);
            Source.transferGeneratedSources(ascCompilationUnit, cssCompilationUnit);
            Source.transferDefinitions(ascCompilationUnit, cssCompilationUnit);
            Source.transferInheritance(ascCompilationUnit, cssCompilationUnit);
        }
        else
        {
            cssCompilationUnit = null;
        }

        return cssCompilationUnit;
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

    /**
     * Analyze... The implementation must:
     *
     * 1. register type info to SymbolTable
     */
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

        ThreadLocalToolkit.setLogger(adapter);
        asc.analyze4(ascUnit, symbolTable);

        if (ThreadLocalToolkit.errorCount() > 0)
        {
            return;
        }

        ThreadLocalToolkit.setLogger(original);

        Source.transferExpressions(ascUnit, unit);
        Source.transferMetaData(ascUnit, unit);
        Source.transferLoaderClassBase(ascUnit, unit);
        Source.transferClassTable(ascUnit, unit);
        Source.transferStyles(ascUnit, unit);
    }

    /**
     *
     */
    public void extractStyles(StyleSheet styleSheet, StyleModule styleModule) //throws Exception
    {
        RuleList sheetRules = styleSheet.getCssRules();

        if (sheetRules != null)
        {
            // aggregate rules by selector
            Iterator ruleIterator = sheetRules.iterator();

            while (ruleIterator.hasNext())
            {
                Rule rule = (Rule) ruleIterator.next();

                if (rule instanceof StyleRule)
                {
                    // for each selector in this rule
                    SelectorList selectors = ((StyleRule) rule).getSelectorList();
                    int nSelectors = selectors.getLength();
                    for (int i = 0; i < nSelectors; i++)
                    {
                        Selector selector = selectors.item(i);
                        int lineNumber = rule.getStyle().getLineNumber();

                        if (selector instanceof AbstractSelector)
                        {
                            lineNumber = ((AbstractSelector) selector).getLineNumber();
                        }

                        if (selector.getSelectorType() == Selector.SAC_CONDITIONAL_SELECTOR)
                        {
                            Condition condition = ((ConditionalSelector) selector).getCondition();

                            if (condition.getConditionType() == Condition.SAC_CLASS_CONDITION)
                            {
                                String name = ((AttributeCondition) condition).getValue();
                                assert name != null : "parsed CSS class selector name is null";

                                styleModule.addSelector(name, false, rule, lineNumber);
                            }
                            else
                            {
                                ConditionTypeNotSupported conditionTypeNotSupported =
                                    new ConditionTypeNotSupported(styleModule.getSource().getName(),
                                                                  lineNumber,
                                                                  condition.toString());
                                ThreadLocalToolkit.log(conditionTypeNotSupported);
                            }
                        }
                        else if (selector.getSelectorType() == Selector.SAC_ELEMENT_NODE_SELECTOR)
                        {
                            String name = ((ElementSelector) selector).getLocalName();

                            // Batik seems to generate an empty element
                            // selector when @charset, so filter those out.
                            if (name != null)
                            {
                                styleModule.addSelector(name, true, rule, lineNumber);
                            }
                        }
                        else
                        {
                            SelectorTypeNotSupported selectorTypeNotSupported =
                                new SelectorTypeNotSupported(styleModule.getSource().getName(),
                                                             lineNumber,
                                                             selector.toString());
                            ThreadLocalToolkit.log(selectorTypeNotSupported);
                        }
                    }
                }
                else if (rule instanceof FontFaceRule)
                {
                    styleModule.addFontFaceRule((FontFaceRule)rule);
                }
            }
        }
    }

    /**
     * Code Generate
     */
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
            return;
        }

        Source.transferGeneratedSources(ascUnit, unit);
        Source.transferBytecodes(ascUnit, unit);
    }

    private VirtualFile generateSourceCodeFile(CompilationUnit compilationUnit, StyleModule styleModule)
    {
        Template template;
        String templateName = !configuration.archiveClassesAndAssets() ? STYLE_MODULE_TEMPLATE : STYLE_LIBRARY_TEMPLATE;

        try
        {
            template = VelocityManager.getTemplate(templateName);
        }
        catch (Exception exception)
        {
            ThreadLocalToolkit.log(new TemplateNotFound(templateName));
            return null;
        }

        SourceCodeBuffer sourceCodeBuffer = new SourceCodeBuffer();

        String genFileName = (configuration.getGeneratedDirectory() +
                              File.separatorChar +
                              styleModule.getName() +
                              "-generated.as");

        Source source = compilationUnit.getSource();

        DualModeLineNumberMap lineNumberMap = new DualModeLineNumberMap(source.getNameForReporting(), genFileName);
        styleModule.setLineNumberMap(lineNumberMap);

        try
        {
            VelocityUtil velocityUtil = new VelocityUtil(TEMPLATE_PATH, configuration.debug(),
                                                         sourceCodeBuffer, lineNumberMap);
            VelocityContext velocityContext = VelocityManager.getCodeGenContext(velocityUtil);
            velocityContext.put(STYLE_MODULE_KEY, styleModule);
            template.merge(velocityContext, sourceCodeBuffer);
        }
        catch (Exception e)
        {
            ThreadLocalToolkit.log(new GenerateException(styleModule.getName(), e.getLocalizedMessage()));
            return null;
        }

        String sourceCode = sourceCodeBuffer.toString();

        if (configuration.keepGeneratedActionScript())
        {
            try
            {
                FileUtil.writeFile(genFileName, sourceCode);
            }
            catch (IOException e)
            {
                ThreadLocalToolkit.log(new UnableToWriteGeneratedFile(genFileName, e.getLocalizedMessage()));
            }
        }

        return new TextFile(sourceCode, genFileName, null, MimeMappings.AS, Long.MAX_VALUE);
    }

    /**
     * Postprocess... could be invoked multiple times...
     */
    public void postprocess(CompilationUnit unit, SymbolTable symbolTable)
    {
    }

    public static class InvalidComponentName extends CompilerError
    {
        public final String name;

        public InvalidComponentName(String name)
        {
            this.name = name;
        }
    }

    public static class StyleSheetParseError extends CompilerError
    {
        public final String stylePath;

        public StyleSheetParseError(String stylePath)
        {
            this.stylePath = stylePath;
        }
    }
}
