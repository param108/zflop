////////////////////////////////////////////////////////////////////////////////
//
//  ADOBE SYSTEMS INCORPORATED
//  Copyright 2003-2007 Adobe Systems Incorporated
//  All Rights Reserved.
//
//  NOTICE: Adobe permits you to use, modify, and distribute this file
//  in accordance with the terms of the license agreement accompanying it.
//
////////////////////////////////////////////////////////////////////////////////

package flash.fonts;

import java.net.URL;
import java.util.List;
import java.util.Properties;
import java.util.Map;

import flash.swf.tags.DefineFont;

/**
 * The FontManager provides a common interface to locating fonts from either
 * locally (i.e. from the Operating System) or externally (i.e. from URL locations).
 *
 * @author Peter Farland
 */
public abstract class FontManager
{
    protected Properties languageRanges;
    protected FontManager parent;

    protected FontManager()
    {
    }

    /**
     * Provides the ability to chain managers.
     *
     * @param parent
     */
    public void setParent(FontManager parent)
    {
        this.parent = parent;
    }

    public void setLanguageRange(Properties languageRanges)
    {
        this.languageRanges = languageRanges;
    }

    /**
     * If a given language token is registered, the corresponding unicode range (specified as a CSS-2 formatted
     * string) is returned.
     *
     * @param lang
     */
    public String getLanguageRange(String lang)
    {
        String range = null;

        if (languageRanges != null)
            range = languageRanges.getProperty(lang);

        return range;
    }

    /**
     * Initialization properties can be provided as name/value pairs.
     *
     * @param map
     */
    public abstract void initialize(Map map);

    /**
     * Attempts to load a font from the cache by location or from disk if it is the first
     * request at this address. The location is bound to a font family name and defineFont type after the initial
     * loading, and the relationship exists for the lifetime of the cache.
     *
     * @param location
     * @param style
     * @return FontSet.FontFace
     */
    public abstract FontFace getEntryFromLocation(URL location, int style, boolean useTwips);

    /**
     * Attempts to locate a font by family name, style, and defineFont type from the runtime's list of
     * fonts, which are primarily operating system registered fonts.
     *
     * @param familyName
     * @param style      either FontFace.PLAIN, FontFace.BOLD, FontFace.ITALIC or FontFace.BOLD+FontFace.ITALIC
     * @return FontFace
     */
    public abstract FontFace getEntryFromSystem(String familyName, int style, boolean useTwips);

    /**
     * Allows a DefineFont SWF tag to be the basis of a FontFace.
     * 
     * @param tag The DefineFont tag
     * @param location The original location of the asset that created the
     * DefineFont SWF tag.
     */
    public void loadDefineFont(DefineFont tag, Object location)
    {
        // No-op
    }

    /**
     * Allows a DefineFont SWF tag to be the basis of a FontFace.
     * 
     * @param tag The DefineFont tag.
     */
    public void loadDefineFont(DefineFont tag)
    {
        loadDefineFont(tag, null);
    }

    /**
     * Given a list of class names, this utility method attempts to
     * construct a chain of FontManagers.
     *
     * The class must extend FontManager and have a public no-args constructor.
     * Invalid classes are skipped.
     *
     * @param managerClasses
     * @return the last FontManager in the chain
     */
    public static FontManager create(List managerClasses, Map map)
    {
        FontManager manager = null;

        if (managerClasses != null)
        {
            for (int i = 0; i < managerClasses.size(); i++)
            {
                try
                {
                    Object className = managerClasses.get(i);
                    if (className != null)
                    {
                        Class clazz = Class.forName(className.toString());
                        Object obj = clazz.newInstance();
                        if (obj instanceof FontManager)
                        {
                            FontManager fm = (FontManager)obj;
                            fm.initialize(map);

                            if (manager != null)
                                fm.setParent(manager);

                            manager = fm;
                        }
                    }
                }
                catch (Throwable t)
                {
                }
            }
        }

        return manager;
    }

}
