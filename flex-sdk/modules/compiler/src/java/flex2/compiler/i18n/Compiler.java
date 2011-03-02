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

package flex2.compiler.i18n;                                                   

import flex2.compiler.CompilationUnit;
import flex2.compiler.Context;
import flex2.compiler.Source;
import flex2.compiler.SymbolTable;
import flex2.compiler.Transcoder;
import flex2.compiler.as3.EmbedExtension;
import flex2.compiler.as3.Extension;
import flex2.compiler.common.CompilerConfiguration;
import flex2.compiler.common.MxmlConfiguration;
import flex2.compiler.io.LocalFile;
import flex2.compiler.io.TextFile;
import flex2.compiler.mxml.rep.AtEmbed;
import flex2.compiler.util.MimeMappings;
import flex2.compiler.util.ThreadLocalToolkit;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Set;
import java.util.Map;
import java.io.File;
import java.io.IOException;

import macromedia.asc.util.ObjectList;

import flash.util.FileUtils;
import flash.util.StringJoiner;
import flash.util.StringUtils;

/**
 * Transforms translation files (usually property files) into classes that extend ResourceBundle
 *
 * @author Clement Wong
 * @author Brian Deitte
 */
public class Compiler implements flex2.compiler.Compiler
{
    public Compiler(final CompilerConfiguration compilerConfig, Transcoder[] transcoders)
    {
    	configuration = compilerConfig;
        asc = new flex2.compiler.as3.Compiler(new flex2.compiler.as3.Configuration()
        {
            public boolean debug() { return false; }
            public boolean profile() { return false; }
            public boolean strict() { return true; }
	        public int dialect() { return compilerConfig.dialect(); }
	        public boolean adjustOpDebugLine() { return compilerConfig.adjustOpDebugLine(); }
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
            public ObjectList getDefine() { return null; }
        });

        generatedDir = (compilerConfig.keepGeneratedActionScript() ? compilerConfig.getGeneratedDirectory() : null);
        addCompilerExtension(new EmbedExtension(transcoders, generatedDir, compilerConfig.showDeprecationWarnings()));
        format = I18nUtils.getTranslationFormat(compilerConfig);
        locales = compilerConfig.getLocales();
    }

    private flex2.compiler.as3.Compiler asc;
    private TranslationFormat format;
    public String generatedDir;
    private String[] locales;
    private CompilerConfiguration configuration;

	public void addCompilerExtension(Extension ext)
	{
		asc.addCompilerExtension(ext);
	}

    public boolean isSupported(String mimeType)
    {
        return format.isSupported(mimeType);
    }

    public String[] getSupportedMimeTypes()
    {
        return format.getSupportedMimeTypes();
    }

    public Source preprocess(Source source)
    {
        return source;
    }

    public CompilationUnit parse1(Source source, SymbolTable symbolTable)
    {
        TranslationInfo[] translations = new TranslationInfo[locales == null ? 0 : locales.length];
        
        for (int i = 0, len = translations.length; i < len; i++)
        {
            try
            {
                translations[i] = format.getTranslationSet(configuration, symbolTable, source, locales[i]);
            }
            catch(TranslationException te)
            {
                ThreadLocalToolkit.logError(te.getMessage());
            }
        }
        
        if (ThreadLocalToolkit.errorCount() > 0)
        {
        	return null;
        }

        Source transSource = transform(source, translations);

        if (ThreadLocalToolkit.errorCount() > 0)
        {
            return null;
        }

        CompilationUnit ascUnit = asc.parse1(transSource, symbolTable);

        if (ThreadLocalToolkit.errorCount() > 0)
        {
            return null;
        }

        flex2.compiler.Context context = new flex2.compiler.Context();
        context.setAttribute("ascUnit", ascUnit);
        context.setAttribute(Context.L10N_ARCHIVE_FILES, new HashMap());

        CompilationUnit unit = source.newCompilationUnit(null, context);

        // when building a SWC, we want to locate all the asset sources and ask compc to put them in the SWC.
        for (int i = 0, len = translations.length; i < len; i++)
        {
        	if (translations[i] != null)
        	{
        		TranslationInfo info = translations[i];
        		Set atEmbeds = info.getEmeds();
                if (atEmbeds != null && configuration.archiveClassesAndAssets())
                {
                	Map archiveFiles = (Map) context.getAttribute(Context.L10N_ARCHIVE_FILES);
                	for (Iterator j = atEmbeds.iterator(); j.hasNext(); )
                	{
                		AtEmbed e = (AtEmbed) j.next();
                		String src = (String) e.getAttributes().get(Transcoder.SOURCE);
                		String original = (String) e.getAttributes().get(Transcoder.ORIGINAL);
                		if (src != null)
                		{
                			if (source.getRelativePath().length() == 0)
                			{
                				archiveFiles.put("locale/" + locales[i] + "/" + original, new LocalFile(new File(src)));
                			}
                			else
                			{
                				archiveFiles.put("locale/" + locales[i] + "/" + source.getRelativePath() + "/" + original, new LocalFile(new File(src)));
                			}
                		}
                	}
                }
        	}
        }

	    Source.transferMetaData(ascUnit, unit);
	    Source.transferGeneratedSources(ascUnit, unit);
	    Source.transferDefinitions(ascUnit, unit);
	    Source.transferInheritance(ascUnit, unit);

	    return unit;
    }
    
    public void parse2(CompilationUnit unit, SymbolTable symbolTable)
    {
	    CompilationUnit ascUnit = (CompilationUnit) unit.getContext().getAttribute("ascUnit");
	    Source.transferInheritance(unit, ascUnit);

	    asc.parse2(ascUnit, symbolTable);    	

		Source.transferAssets(ascUnit, unit);
	    Source.transferGeneratedSources(ascUnit, unit);
    }

    /**
     * Transforms foo.properties into a Source text file
	 * with the following autogenerated code
	 * for a resource bundle class:
     * 
	 * package 
	 * {
	 * import mx.resources.ResourceBundle;
	 * 
	 * [ExcludeClass]
	 * 
	 * public class foo_properties extends ResourceBundle
	 * {
	 *     public function foo_properties()
	 *     {
	 *         super("en_US", "foo");
	 *     }
	 * 
	 *     override protected function getContent():Object
	 *     {
	 *         var content:Object =
	 *         {
	 *             "key1": "value1",
	 *             "key2": "value2"
	 *         };
 	 *         return content;
	 *     }
 	 * }
 	 * 
	 * }
     */
    private String transform(Source source, TranslationInfo translations,
    						 String packageName, String className, String locale)
    {
        String lineSep = System.getProperty("line.separator");
	    String bundleName = I18nUtils.bundleNameFromClassName(className);

        String[] codePieces =
        {
        	codegenImports(translations.getClassReferences()),
        	"[ExcludeClass]", lineSep, lineSep,
        	"public class ", className, " extends ResourceBundle", lineSep,
			"{", lineSep,
			codegenAtEmbeds(translations.getEmeds()),
			"    public function ", className, "()",  lineSep,
            "    {", lineSep,
			codegenSuper(locale, bundleName), lineSep,
			"    }", lineSep, lineSep,
			"    override protected function getContent():Object", lineSep,
		    "    {", lineSep,
			"        var content:Object =", lineSep,
			"        {", lineSep,
			codegenContent(translations.getTranslationSet()),
			"        };", lineSep,
			"        return content;",  lineSep,
            "    }", lineSep,
            "}", lineSep, lineSep, 
        };
        
        return StringJoiner.join(codePieces, null);
    }

    private Source transform(Source source, TranslationInfo[] translations)
    {
        String name = source.getName();
        String relativePath = source.getRelativePath();
        String shortName = source.getShortName();
        String lineSep = System.getProperty("line.separator");
        String packageName = relativePath.replace('/', '.');	    

    	StringBuffer code = new StringBuffer();    	
    	
    	code.append("package ");
    	code.append(packageName);
    	code.append(lineSep);
    	code.append("{");
    	code.append(lineSep);
    	code.append(lineSep);
    	
    	for (int i = 0, len = translations == null ? 0 : translations.length; i < len; i++)
    	{
            // A Source instance for a .properties file knows which locale it is for
            // and produces a class name like "en_US$core_properties".
            String className = locales[i] + "$" + shortName + I18nUtils.CLASS_SUFFIX;
    		code.append(transform(source, translations[i], packageName, className, locales[i]));
    		
    		code.append(lineSep);
    		code.append(lineSep);
    	}
		
    	code.append("}");
    	code.append(lineSep);

	    if (generatedDir != null)
	    {
		    try
		    {
			    FileUtils.writeClassToFile(generatedDir, packageName, shortName + I18nUtils.CLASS_SUFFIX + ".as", code.toString());
		    }
		    catch(IOException ioe)
		    {
			    ThreadLocalToolkit.logError(ioe.toString());
		    }
	    }
	    
	    return new Source(new TextFile(code.toString(), name, source.getParent(),
	    							   MimeMappings.AS, source.getLastModified()), source);
    }

    private String codegenImports(Set imports)
    {
		String lineSep = System.getProperty("line.separator");
		StringJoiner.ItemStringer itemStringer = new StringJoiner.ItemStringer()
    	{
    		public String itemToString(Object obj)
    		{
    			return "import " + (String)obj + ";";
    		}
    	};
    	return StringJoiner.join(imports, lineSep, itemStringer) + lineSep + lineSep;
    }
    
    private String codegenAtEmbeds(Set atEmbeds)
    {
    	if (configuration.archiveClassesAndAssets()) return "";
    	
		String lineSep = System.getProperty("line.separator");
		StringJoiner.ItemStringer itemStringer = new StringJoiner.ItemStringer()
    	{
    		public String itemToString(Object obj)
    		{
    			AtEmbed atEmbed = (AtEmbed)obj;
    			return codegenEmbedMetadata(atEmbed) +
    				   codegenEmbedVar(atEmbed);
    		}
    	};
		return StringJoiner.join(atEmbeds, lineSep, itemStringer) + lineSep;
    }
    
    private String codegenEmbedMetadata(AtEmbed atEmbed)
    {
		String lineSep = System.getProperty("line.separator");
    	StringJoiner.ItemStringer itemStringer = new StringJoiner.MapEntryItemWithEquals();
    	return "    [Embed(" +
    		   StringJoiner.join(atEmbed.getAttributes().entrySet(), ", ", itemStringer) +
    		   ")]" + lineSep;
    }
    
    private String codegenEmbedVar(AtEmbed atEmbed)
    {
		String lineSep = System.getProperty("line.separator");
    	return "    private static var " + atEmbed.getPropName() + ":" + atEmbed.getType() + ";" + lineSep;
    }
    
	private String codegenSuper(String locale, String bundleName)
	{
		// -resource-hack=true is only for compiler performance testing.
		// It allows the Flex 3 compiler to compile the Flex 2 framework
		// and Flex 2 apps.
		// The problem is that the ResourceBundle constructor changed
		// between Flex 2 (where it took no arguments) and Flex 3
		// (where it takes an optional locale and bundleName).
		// Setting -resource-hack=true makes the constructors of the
		// autogenerated bundle classes call super() like they used to,
		// rather than calling super(locale, bundleName).

		return configuration.getResourceHack() ?
			   "		 super();" :
			   "		 super(\"" + locale + "\", \"" + bundleName + "\");";
	}
    
	private String codegenContent(Set resources)
	{
		String lineSep = System.getProperty("line.separator");
		
		int version = configuration.getCompatibilityVersion();
		final boolean flex2Compatible = version < MxmlConfiguration.VERSION_3_0;
	   	
		StringJoiner.ItemStringer itemStringer = new StringJoiner.ItemStringer()
    	{
    		public String itemToString(Object obj)
    		{
    		    Map.Entry entry = (Map.Entry)obj;
    		    
    		    String key = (String)entry.getKey();
     		    if (!flex2Compatible)
     		    {
     		    	key = escape(key);
     		    }
     		    
    		    Object value = entry.getValue();
    		    if (value instanceof ClassReference)
    			{
    				value = value.toString();
    			}
    		    else if (value instanceof AtEmbed)
    		    {
    		    	value = (configuration.archiveClassesAndAssets()) ? "\"\"" : ((AtEmbed)value).getPropName();
    		    }
    			else
    			{
         		    if (!flex2Compatible)
         		    {
         		    	value = escape((String)value);
         		    }
    				value = "\"" + value + "\"";
    			}
        		return "            \"" + key + "\": " + value;
    		}
    	};
    	return StringJoiner.join(resources, "," + lineSep, itemStringer) + lineSep;
	}
	
	/*
	 * s may contain chars like double-quote, carriage-return, and newline
	 * which need to be written out as \", \r, and \n inside an AS String literal.
	 */
	private String escape(String s)
	{
		s = StringUtils.formatString(s);
		int n = s.length();
		return s.substring(1, n - 1);
	}

	public void analyze1(CompilationUnit unit, SymbolTable symbolTable)
	{
	    CompilationUnit ascUnit = (CompilationUnit) unit.getContext().getAttribute("ascUnit");
	    asc.analyze1(ascUnit, symbolTable);

	    Source.transferTypeInfo(ascUnit, unit);
	    Source.transferNamespaces(ascUnit, unit);
	}

    public void analyze2(CompilationUnit unit, SymbolTable symbolTable)
    {
        CompilationUnit ascUnit = (CompilationUnit) unit.getContext().getAttribute("ascUnit");
        Source.transferDependencies(unit, ascUnit);
        asc.analyze2(ascUnit, symbolTable);
        Source.transferDependencies(ascUnit, unit);
    }

    public void analyze3(CompilationUnit unit, SymbolTable symbolTable)
    {
        CompilationUnit ascUnit = (CompilationUnit) unit.getContext().getAttribute("ascUnit");
        Source.transferDependencies(unit, ascUnit);
        asc.analyze3(ascUnit, symbolTable);
    }

    public void analyze4(CompilationUnit unit, SymbolTable symbolTable)
    {
        CompilationUnit ascUnit = (CompilationUnit) unit.getContext().getAttribute("ascUnit");
        asc.analyze4(ascUnit, symbolTable);
        Source.transferExpressions(ascUnit, unit);
	    // Source.transferLoaderClassBase(ascUnit, unit);
	    // Source.transferGeneratedSources(ascUnit, unit);
	    // Source.transferClassTable(ascUnit, unit);
	    // Source.transferStyles(ascUnit, unit);
    }

    public void generate(CompilationUnit unit, SymbolTable symbolTable)
    {
        CompilationUnit ascUnit = (CompilationUnit) unit.getContext().getAttribute("ascUnit");
        asc.generate(ascUnit, symbolTable);

        unit.bytes.clear();
        unit.bytes.addAll(ascUnit.bytes);
    }

    public void postprocess(CompilationUnit unit, SymbolTable symbolTable)
    {
    }
}
