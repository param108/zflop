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

package flex2.compiler.css;

import flex2.compiler.CompilationUnit;
import flex2.compiler.ResourceContainer;
import flex2.compiler.Source;
import flex2.compiler.Transcoder;
import flex2.compiler.common.PathResolver;
import flex2.compiler.io.FileUtil;
import flex2.compiler.io.TextFile;
import flex2.compiler.io.VirtualFile;
import flex2.compiler.mxml.Configuration;
import flex2.compiler.mxml.SourceCodeBuffer;
import flex2.compiler.mxml.gen.VelocityUtil;
import flex2.compiler.mxml.rep.AtEmbed;
import flex2.compiler.mxml.rep.MxmlDocument;
import flex2.compiler.swc.SwcFile;
import flex2.compiler.util.CompilerMessage.CompilerWarning;
import flex2.compiler.util.CompilerMessage;
import flex2.compiler.util.MimeMappings;
import flex2.compiler.util.ThreadLocalToolkit;
import flex2.compiler.util.VelocityException;
import flex2.compiler.util.VelocityManager;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;
import java.util.Map;
import java.util.Set;
import flash.css.FontFaceRule;
import flash.css.Rule;
import flash.css.RuleList;
import flash.css.StyleDeclaration;
import flash.css.StyleRule;
import flash.css.StyleSheet;
import flash.fonts.FontManager;
import flash.util.Trace;
import macromedia.asc.util.ContextStatics;

import org.apache.batik.css.parser.AbstractSelector;
import org.apache.velocity.Template;
import org.apache.velocity.VelocityContext;
import org.w3c.css.sac.AttributeCondition;
import org.w3c.css.sac.Condition;
import org.w3c.css.sac.ConditionalSelector;
import org.w3c.css.sac.ElementSelector;
import org.w3c.css.sac.Selector;
import org.w3c.css.sac.SelectorList;

public class StylesContainer
{
	private static final String TEMPLATE_PATH = "flex2/compiler/css/";
	private static final String ATEMBEDS_KEY = "atEmbeds";
	private static final String FONTFACERULES_TEMPLATE = TEMPLATE_PATH + "FontFaceRules.vm";
	private static final String STYLEDEF_KEY = "styleDef";
	private static final String STYLEDEF_TEMPLATE = TEMPLATE_PATH + "StyleDef.vm";

    private static final String _FONTFACERULES = "_FontFaceRules";

    private MxmlDocument mxmlDocument;
    private Configuration configuration;
    private CompilationUnit compilationUnit;
    private ContextStatics perCompileData;
    private Map selectors = new HashMap();
    private Set localStyleTypeNames = new HashSet();
    private Map atEmbeds;
    private List fontFaceRules = new ArrayList();
    private List implicitIncludes = new ArrayList(); // List<VirtualFile>

    public StylesContainer(Configuration configuration,
                           CompilationUnit compilationUnit,
                           ContextStatics perCompileData)
    {
        this.configuration = configuration;
        this.compilationUnit = compilationUnit;
        this.perCompileData = perCompileData;

        atEmbeds = new HashMap();

        loadDefaultStyles();
    }

    public StylesContainer(CompilationUnit compilationUnit,
                           ContextStatics perCompileData,
                           Configuration configuration)
    {
        this.configuration = configuration;
        this.compilationUnit = compilationUnit;
        this.perCompileData = perCompileData;

        compilationUnit.setStylesContainer(this);
    }

	public void setMxmlDocument(MxmlDocument doc)
	{
		mxmlDocument = doc;
	}

	/**
	 *
	 */
	private boolean addAtEmbed(AtEmbed atEmbed)
	{
        if (mxmlDocument != null)
        {
            mxmlDocument.addAtEmbed(atEmbed);
        }
        else if (!atEmbeds.containsKey(atEmbed.getPropName()))
        {
            atEmbeds.put(atEmbed.getPropName(), atEmbed);
        }

        return true;
	}

    /**
     *
     */
    public void addFontFaceRule(FontFaceRule rule)
    {
        assert rule != null;

        String family = rule.getFamily();
        boolean bold = rule.isBold();
        boolean italic = rule.isItalic();

        if (getFontFaceRule(family, bold, italic) == null)
        {
            fontFaceRules.add(rule);

            //    add embed for font
            String propName = "_embed__font_" + family + "_" + (bold? "bold":"medium") + "_" + (italic? "italic":"normal");
            Map embedParams = rule.getEmbedParams();
            StyleDeclaration styleDeclaration = rule.getStyle();
            String path = styleDeclaration.getPath();

            if (path.indexOf('\\') > -1)
            {
                embedParams.put( Transcoder.FILE, path.replace('\\', '/') );
                embedParams.put( Transcoder.PATHSEP, "true" );
            }
            else
            {
                embedParams.put( Transcoder.FILE, path );
            }

            embedParams.put( Transcoder.LINE, Integer.toString(styleDeclaration.getLineNumber()) );
            AtEmbed atEmbed = AtEmbed.create(propName, rule.getStyle().getLineNumber(), embedParams, false);
            addAtEmbed(atEmbed);
        }
        else if (Trace.font)
        {
            Trace.trace("Font face already existed for " + family + " bold? " + bold + " italic? " + italic);
        }
    }

    /**
     *
     */
    private void addStyleClassSelector(String name, Rule rule, int lineNumber)
    {
        StyleDef styleDef;

        if (selectors.containsKey(name))
        {
            styleDef = (StyleDef) selectors.get(name);
        }
        else
        {
            styleDef = new StyleDef(name, false, mxmlDocument, compilationUnit.getSource(),
                                    lineNumber, perCompileData);
            selectors.put(name, styleDef);
        }

        styleDef.addRule(rule);

        if (mxmlDocument != null)
        {
            Iterator iterator = styleDef.getImports().iterator();

            while ( iterator.hasNext() )
            {
                Import importObject = (Import) iterator.next();
                mxmlDocument.addImport(importObject.getValue(), importObject.getLineNumber());
            }
        }
    }

    /**
     *
     */
    private void addStyleTypeSelector(String name, Rule rule, boolean local, int lineNumber)
    {
        StyleDef styleDef;

        if (local)
        {
            localStyleTypeNames.add(name);
        }

        if (selectors.containsKey(name))
        {
            styleDef = (StyleDef) selectors.get(name);
        }
        else
        {
            styleDef = new StyleDef(name, true, mxmlDocument, compilationUnit.getSource(),
                                    lineNumber, perCompileData);
            selectors.put(name, styleDef);
        }

        styleDef.addRule(rule);

        if (mxmlDocument != null)
        {
            Iterator iterator = styleDef.getImports().iterator();

            while ( iterator.hasNext() )
            {
                Import importObject = (Import) iterator.next();
                mxmlDocument.addImport(importObject.getValue(), importObject.getLineNumber());
            }
        }
    }

    public void checkForUnusedTypeSelectors(Set defNames)
    {
        Iterator iterator = selectors.entrySet().iterator();

        Set unqualifiedDefNames = new HashSet();

        Iterator defNameIterator = defNames.iterator();

        while ( defNameIterator.hasNext() )
        {
            String defName = (String) defNameIterator.next();

            unqualifiedDefNames.add( defName.replaceFirst(".*:", "") );
        }

        while ( iterator.hasNext() )
        {
            Entry entry = (Entry) iterator.next();
            String styleName = (String) entry.getKey();
            StyleDef styleDef = (StyleDef) entry.getValue();
            String typeName = StyleDef.dehyphenize(styleName);

            if (styleDef.isTypeSelector() &&
                localStyleTypeNames.contains(styleName) &&
                !unqualifiedDefNames.contains(typeName) &&
                !styleName.equals("global"))
            {
            	if (configuration.showUnusedTypeSelectorWarnings())
            	{
            		ThreadLocalToolkit.log(new UnusedTypeSelector(compilationUnit.getSource().getName(),
                                                                  styleDef.getLineNumber(),
            													  styleName));
            	}
            }
        }
    }

    /**
     *
     */
    private Source createSource(String fileName, SourceCodeBuffer sourceCodeBuffer)
    {
        Source result = null;

		if (sourceCodeBuffer.getBuffer() != null)
		{
            String sourceCode = sourceCodeBuffer.toString();

			if (configuration.keepGeneratedActionScript())
			{
				try
				{
					FileUtil.writeFile(fileName, sourceCode);
				}
				catch (IOException e)
				{
					ThreadLocalToolkit.log(new VelocityException.UnableToWriteGeneratedFile(fileName, e.getMessage()));
				}
			}

			VirtualFile genFile = new TextFile(sourceCode, fileName, null, MimeMappings.AS, Long.MAX_VALUE);
			String shortName = fileName.substring(0, fileName.lastIndexOf('.'));

            result = new Source(genFile, "", shortName, null, false, false, false);
            result.setPathResolver(compilationUnit.getSource().getPathResolver());

            Iterator iterator = implicitIncludes.iterator();

            while ( iterator.hasNext() )
            {
                VirtualFile virtualFile = (VirtualFile) iterator.next();
                result.addFileInclude(virtualFile);
            }
		}

        return result;
    }

    /**
     *
     */
    public void extractStyles(StyleSheet styleSheet, boolean local) throws Exception
    {
        RuleList sheetRules = styleSheet.getCssRules();

        if (sheetRules != null)
        {
            //    aggregate rules by selector

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

                                addStyleClassSelector(name, rule, lineNumber);
                            }
                            else
                            {
                                ConditionTypeNotSupported conditionTypeNotSupported =
                                    new ConditionTypeNotSupported(compilationUnit.getSource().getName(),
                                                                  lineNumber,
                                                                  condition.toString());
                                ThreadLocalToolkit.log(conditionTypeNotSupported);
                            }
                        }
                        else if (selector.getSelectorType() == Selector.SAC_ELEMENT_NODE_SELECTOR)
                        {
                            //    pick up type selectors only from root (application)
                            if (compilationUnit.isRoot())
                            {
                                String name = ((ElementSelector) selector).getLocalName();

                                // Batik seems to generate an empty element
                                // selector when @charset, so filter those out.
                                if (name != null)
                                {
                                    addStyleTypeSelector(name, rule, local, lineNumber);
                                }
                            }
                            else
                            {
                                // [preilly] This restriction should be removed once the
                                // app model supports encapsulation of CSS styles.
                                ComponentTypeSelectorsNotSupported componentTypeSelectorsNotSupported =
                                    new ComponentTypeSelectorsNotSupported(compilationUnit.getSource().getName(),
                                                                           lineNumber,
                                                                           selector.toString());
                                ThreadLocalToolkit.log(componentTypeSelectorsNotSupported);
                            }
                        }
                        else
                        {
                            SelectorTypeNotSupported selectorTypeNotSupported =
                                new SelectorTypeNotSupported(compilationUnit.getSource().getName(),
                                                             lineNumber,
                                                             selector.toString());
                            ThreadLocalToolkit.log(selectorTypeNotSupported);
                        }
                    }
                }
                else if (rule instanceof FontFaceRule)
                {
                    addFontFaceRule((FontFaceRule)rule);
                }
            }
        }
    }

	private String generateFontFaceRuleSourceName()
	{
		String genFileName;
		String genDir = configuration.getGeneratedDirectory();
	    if (genDir != null)
	    {
		    genFileName = genDir + File.separatorChar + "_FontFaceRules.as";
	    }
	    else
	    {
		    genFileName = "_FontFaceRules.as";
	    }
		return genFileName;
	}

	/**
	 *
	 */
    private Source generateFontFaceRules(ResourceContainer resources)
    {
	    String genFileName = generateFontFaceRuleSourceName();
	    Source styleSource = resources.findSource(genFileName);
	    if (styleSource != null)
	    {
            if (styleSource.getCompilationUnit() == null) 
            {
                // if no compilationUnit, then we need to generate source so we can recompile.
                styleSource = null;
            }
            else 
            {
                // C: it is safe to return because this method deals with per-app styles, like defaults.css and themes.
                //    ResourceContainer will not have anything if any of the theme files is touched.
                return styleSource;
            }
	    }

		Template template;

        try
		{
            template = VelocityManager.getTemplate(FONTFACERULES_TEMPLATE);
        }
        catch (Exception exception)
        {
			ThreadLocalToolkit.log(new VelocityException.TemplateNotFound(FONTFACERULES_TEMPLATE));
			return null;
		}

		SourceCodeBuffer out = new SourceCodeBuffer();

		try
		{
			VelocityUtil util = new VelocityUtil(TEMPLATE_PATH, configuration.debug(), out, null);
			VelocityContext vc = VelocityManager.getCodeGenContext(util);
            vc.put(ATEMBEDS_KEY, atEmbeds);
			template.merge(vc, out);
		}
		catch (Exception e)
		{
			ThreadLocalToolkit.log(new VelocityException.GenerateException(compilationUnit.getSource().getRelativePath(),
                                                                           e.getLocalizedMessage()));
			return null;
		}

	    return resources.addResource(createSource(genFileName, out));
    }

	private String generateStyleSourceName(StyleDef styleDef)
	{
		String genFileName;
		String genDir = configuration.getGeneratedDirectory();
	    if (genDir != null)
	    {
		    genFileName = genDir + File.separatorChar + "_" + styleDef.getName() + "Style.as";
	    }
	    else
	    {
		    genFileName = "_" + styleDef.getName() + "Style.as";
	    }

		return genFileName;
	}

	/**
	 *
	 */
    private Source generateStyleSource(StyleDef styleDef, ResourceContainer resources)
    {
	    String genFileName = generateStyleSourceName(styleDef);
	    Source styleSource = resources.findSource(genFileName);
	    
	    if (styleSource != null)
	    {
	    	if (styleSource.getCompilationUnit() == null) 
	    	{
	    		// if no compilationUnit, then we need to generate source so we can recompile.
	    		styleSource = null;
	    	}
	    	else 
	    	{
	    		// C: it is safe to return because this method deals with per-app styles, like defaults.css and themes.
	    		//    ResourceContainer will not have anything if any of the theme files is touched.
	    		return styleSource;
	    	}
	    }

		//	load template
		Template template;

        try
		{
            template = VelocityManager.getTemplate(STYLEDEF_TEMPLATE);
        }
        catch (Exception exception)
        {
			ThreadLocalToolkit.log(new VelocityException.TemplateNotFound(STYLEDEF_TEMPLATE));
			return null;
		}

		SourceCodeBuffer out = new SourceCodeBuffer();

		try
		{
			VelocityUtil util = new VelocityUtil(TEMPLATE_PATH, configuration.debug(), out, null);
			VelocityContext vc = VelocityManager.getCodeGenContext(util);
			vc.put(STYLEDEF_KEY, styleDef);
			template.merge(vc, out);
		}
		catch (Exception e)
		{
			ThreadLocalToolkit.log(new VelocityException.GenerateException(compilationUnit.getSource().getRelativePath(),
                                                                           e.getLocalizedMessage()));
			return null;
		}

	    return resources.addResource(createSource(genFileName, out));
    }

	public FontFaceRule getFontFaceRule(String family, boolean bold, boolean italic)
	{
		//	TODO still necessary?
		if ((family != null) && family.startsWith( "\""))
		{
			family = family.substring( 1 );
			if (family.endsWith( "\""))
			{
				family = family.substring( 0, family.length() - 1 );
			}
		}

		return FontFaceRule.getRule(fontFaceRules, family, bold, italic);
	}

	public List getFontFaceRules()
	{
		return fontFaceRules;
	}

    /**
     *
     */
    MxmlDocument getMxmlDocument()
    {
        return mxmlDocument;
    }

    /**
     *
     */
    public Iterator getStyleDefs()
    {
        return selectors.values().iterator();
    }

	public boolean hasFontFamily(String family)
	{
		//	TODO still necessary?
		if (family.startsWith( "\""))       // why why why why??
		{
			family = family.substring( 1 );
			if (family.endsWith( "\""))
			{
				family = family.substring( 0, family.length() - 1 );
			}
		}

		return FontFaceRule.hasFamily(fontFaceRules, family);
	}

    private VirtualFile resolveDefaultsCssFile()
    {
        VirtualFile defaultsCSSFile = configuration.getDefaultsCssUrl();

        if (defaultsCSSFile == null)
        {
            PathResolver resolver = ThreadLocalToolkit.getPathResolver();

            String version = configuration.getCompatibilityVersionString();

            if (version != null)
            {
                defaultsCSSFile = resolver.resolve("defaults-" + version + ".css");
            }

            if (defaultsCSSFile == null)
            {
                defaultsCSSFile = resolver.resolve("defaults.css");
            }
        }

        return defaultsCSSFile;
    }

    /**
     *
     */
    private void loadDefaultStyles()
    {
        VirtualFile defaultsCSSFile = resolveDefaultsCssFile();

        // Load the per SWC default styles first
        for (Iterator it = configuration.getDefaultsCssFiles().iterator(); it.hasNext();)
        {
            VirtualFile swcDefaultsCssFile = (VirtualFile) it.next();

            // Make sure that we resolve things relative to the SWC.
            ThreadLocalToolkit.getPathResolver().addSinglePathResolver(0, swcDefaultsCssFile);
            processStyleSheet(swcDefaultsCssFile);
            ThreadLocalToolkit.getPathResolver().removeSinglePathResolver(swcDefaultsCssFile);
        }

        // Load the default styles next, so they can override the SWC defaults
        if (defaultsCSSFile != null)
        {
            // Only load the defaults if it's not a SwcFile.  If it's
            // a SwcFile, it should have already been loaded.
            if (!(defaultsCSSFile instanceof SwcFile))
            {
                processStyleSheet(defaultsCSSFile);
            }
        }
        else
        {
            ThreadLocalToolkit.log(new DefaultCSSFileNotFound());
        }

        // Load the theme styles next, so they can override the defaults
        for (Iterator it = configuration.getThemeCssFiles().iterator(); it.hasNext();)
        {
            VirtualFile themeCssFile = (VirtualFile) it.next();

            // Make sure that we resolve things in the theme relative
            // to the theme SWC first.
            ThreadLocalToolkit.getPathResolver().addSinglePathResolver(0, themeCssFile);
            processStyleSheet(themeCssFile);
            ThreadLocalToolkit.getPathResolver().removeSinglePathResolver(themeCssFile);
        }
    }

	public List processDependencies(Set defNames, ResourceContainer resources)
	{
		List extraSources = new ArrayList();

        if (!fontFaceRules.isEmpty())
        {
	        // C: mixins in the generated FlexInit class are referred to by "names". that's why extraClasses
	        //    is necessary.
            compilationUnit.extraClasses.add(_FONTFACERULES);
            compilationUnit.mixins.add(_FONTFACERULES);

	        extraSources.add(generateFontFaceRules(resources));
        }

        Set unqualifiedDefNames = new HashSet();

        Iterator defNameIterator = defNames.iterator();

        while ( defNameIterator.hasNext() )
        {
            String defName = (String) defNameIterator.next();

            unqualifiedDefNames.add( defName.replaceFirst(".*:", "") );
        }

        Iterator iterator = selectors.entrySet().iterator();

        while ( iterator.hasNext() )
        {
            Entry entry = (Entry) iterator.next();
            String styleName = (String) entry.getKey();
            StyleDef styleDef = (StyleDef) entry.getValue();
            String typeName = StyleDef.dehyphenize(styleName);

            if (!styleDef.isTypeSelector() ||
                (unqualifiedDefNames.contains(typeName) ||
                 configuration.keepAllTypeSelectors()) ||
                styleName.equals("global"))
            {
                String className = "_" + typeName + "Style";
	            // C: mixins in the generated FlexInit class are referred to by "names". that's why extraClasses
	            //    is necessary.
                compilationUnit.extraClasses.add(className);
                compilationUnit.mixins.add(className);

	            extraSources.add(generateStyleSource(styleDef, resources));
            }
        }

		return extraSources;
    }

    private void processStyleSheet(VirtualFile cssFile)
    {
        implicitIncludes.add(cssFile);

        try
        {
            FontManager fontManager = configuration.getFontsConfiguration().getTopLevelManager();
            StyleSheet styleSheet = new StyleSheet();
            styleSheet.checkDeprecation(configuration.showDeprecationWarnings());
            styleSheet.parse(cssFile.getName(), cssFile.getInputStream(),
                             ThreadLocalToolkit.getLogger(), fontManager);
            
            extractStyles(styleSheet, false);
        }
        catch (Exception exception)
        {
            CompilerMessage m = new ParseError(exception.getLocalizedMessage());
            m.setPath(cssFile.getName());
            ThreadLocalToolkit.log(m);
        }
    }

    public static class DefaultCSSFileNotFound extends CompilerWarning
    {
        public DefaultCSSFileNotFound()
        {
        }
    }

    public static class UnusedTypeSelector extends CompilerWarning
    {
        public String styleName;

        public UnusedTypeSelector(String path, int line, String styleName)
        {
            this.path = path;
            this.line = line;
            this.styleName = styleName;
        }
    }

    public static class ComponentTypeSelectorsNotSupported extends CompilerWarning
    {
        public String selector;

        public ComponentTypeSelectorsNotSupported(String path, int line, String selector)
        {
            this.path = path;
            this.line = line;
            this.selector = selector;
        }
    }
}
