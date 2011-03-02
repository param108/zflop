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

package flex2.compiler.common;

import flex2.compiler.config.AdvancedConfigurationInfo;
import flex2.compiler.config.ConfigurationInfo;
import flex2.compiler.config.ConfigurationValue;
import flex2.compiler.io.VirtualFile;
import flash.fonts.CachedFontManager;
import flash.fonts.FontManager;
import flash.fonts.JREFontManager;

import java.util.List;
import java.util.Properties;
import java.util.Map;
import java.util.HashMap;

/**
 * This class handles the &lt;fonts&gt; configuration
 * section of flex-config.xml. We are not expecting Flex
 * developers to set this information on the command
 * line (though technically they could); instead we'd prefer
 * them to refer the compiler to the Flex configuration file.
 *
 * @author Kyle Quevillon
 * @author Peter Farland
 */
public class FontsConfiguration
{
 	private CompilerConfiguration compilerConfig;

	public void setCompilerConfiguration(CompilerConfiguration compilerConfig)
	{
		this.compilerConfig = compilerConfig;
	}
	
    private ConfigurationPathResolver configResolver;

    public void setConfigPathResolver( ConfigurationPathResolver resolver )
    {
        this.configResolver = resolver;
    }

    private FontManager topLevelManager;

    /**
     * Must be called <i>after</i> configuration is committed.
     *
     * @return the last of potentially several FontManagers in the manager list
     */
    public FontManager getTopLevelManager()
    {
        if (topLevelManager == null)
        {
            Map map = new HashMap();
            map.put(CachedFontManager.MAX_CACHED_FONTS_KEY, max_cached_fonts);
            map.put(CachedFontManager.MAX_GLYPHS_PER_FACE_KEY, max_glyphs_per_face);
            map.put(CachedFontManager.COMPATIBILITY_VERSION, compilerConfig.getCompatibilityVersionString());
            if (localFontsSnapshot != null)
		        map.put(JREFontManager.LOCAL_FONTS_SNAPSHOT, localFontsSnapshot.getName());
            topLevelManager = FontManager.create(managers, map);
            if (topLevelManager != null)
                topLevelManager.setLanguageRange(languages);
        }

        return topLevelManager;
    }

    public void setTopLevelManager(FontManager manager)
    {
        topLevelManager = manager;
    }

    //
    // 'compiler.fonts.flash-type' option
    //

	private boolean flashType = true;

	public boolean getFlashType()
	{
		return flashType;
	}

	public void cfgFlashType(ConfigurationValue cv, boolean val)
	{
	    this.flashType = val;
	}

	public static ConfigurationInfo getFlashTypeInfo()
	{
	    return new ConfigurationInfo()
	    {
	        public boolean isDeprecated()
	        {
	        	return true;
	        }
	        
	        public String getDeprecatedReplacement()
	        {
	        	return "compiler.fonts.advanced-anti-aliasing";
	        }
	        
	        public String getDeprecatedSince()
	        {
	        	//C: Don't change this to VersionInfo.getFlexVersion().
	        	return "3.0";
	        }
	    };
	}

	public void cfgAdvancedAntiAliasing(ConfigurationValue cv, boolean val)
	{
	    cfgFlashType(cv, val);
	}

    //
    // 'compiler.fonts.languages.language-range' option
    //

    private Languages languages = new Languages();

    public Languages getLanguagesConfiguration()
    {
        return languages;
    }

    public static class Languages extends Properties
    {
        public void cfgLanguageRange(ConfigurationValue cv, String lang, String range)
        {
            setProperty(lang, range);
        }

        public static ConfigurationInfo getLanguageRangeInfo()
        {
            return new ConfigurationInfo(new String[]{"lang", "range"})
            {
                public boolean allowMultiple()
                {
                    return true;
                }

                public boolean isAdvanced()
                {
                    return true;
                }
            };
        }
    }

    //
    // 'compiler.fonts.local-fonts-snapshot' option
    //

    private VirtualFile localFontsSnapshot = null;
    
    public VirtualFile getLocalFontsSnapshot()
    {
    	return localFontsSnapshot;
    }

    public void cfgLocalFontsSnapshot( ConfigurationValue cv, String localFontsSnapshotPath )
            throws flex2.compiler.config.ConfigurationException
    {
        localFontsSnapshot =
            ConfigurationPathResolver.getVirtualFile( localFontsSnapshotPath, configResolver, cv );
    }
    public static ConfigurationInfo getLocalFontsSnapshotInfo()
    {
        return new AdvancedConfigurationInfo();
    }
    
    //
    // 'compiler.fonts.managers' option
    //
    
    private List managers;

    public List getManagers()
    {
        return managers;
    }

    public void cfgManagers(ConfigurationValue cv, List l)
    {
        managers = l;
    }

    public static ConfigurationInfo getManagersInfo()
    {
        return new ConfigurationInfo(-1, "manager-class")
        {
            public boolean isAdvanced()
            {
                return true;
            }
        };
    }

    //
    // 'compiler.fonts.max-cached-fonts' option
    //

    private String max_cached_fonts;

    public String getMaxCachedFonts()
    {
        return max_cached_fonts;
    }

    public void cfgMaxCachedFonts(ConfigurationValue cv, String val)
    {
        this.max_cached_fonts = val;
    }

    public static ConfigurationInfo getMaxCachedFontsInfo()
    {
        return new AdvancedConfigurationInfo();
    }

    //
    // 'compiler.fonts.max-glyphs-per-face' option
    //

    private String max_glyphs_per_face;

    public String getMaxGlyphsPerFace()
    {
        return max_glyphs_per_face;
    }

    public void cfgMaxGlyphsPerFace(ConfigurationValue cv, String val)
    {
        this.max_glyphs_per_face = val;
    }
}
