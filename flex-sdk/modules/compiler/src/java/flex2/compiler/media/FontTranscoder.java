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

package flex2.compiler.media;

import flash.css.LocalSource;
import flash.css.URLSource;
import flash.fonts.FontFace;
import flash.fonts.FontManager;
import flash.swf.TagValues;
import flash.swf.builder.tags.FontBuilder;
import flash.swf.tags.DefineFont;
import flash.util.Trace;
import flex2.compiler.ILocalizableMessage;
import flex2.compiler.SymbolTable;
import flex2.compiler.Transcoder;
import flex2.compiler.TranscoderException;
import flex2.compiler.common.CompilerConfiguration;
import flex2.compiler.common.Configuration;
import flex2.compiler.common.FontsConfiguration;
import flex2.compiler.common.PathResolver;
import flex2.compiler.io.FileUtil;
import flex2.compiler.io.VirtualFile;
import flex2.compiler.swc.SwcFile;
import flex2.compiler.util.CompilerMessage;
import flex2.compiler.util.MimeMappings;
import flex2.compiler.util.ThreadLocalToolkit;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;

/**
 * Transcodes fonts into DefineFonts for embedding
 *
 * @author Roger Gonzalez
 */
public class FontTranscoder extends AbstractTranscoder
{
    private FontsConfiguration fontsConfig;
    private boolean showShadowedDeviceFontWarnings;

    public FontTranscoder( Configuration config )
    {
        super(new String[]{MimeMappings.TTF, MimeMappings.OTF, MimeMappings.FONT, MimeMappings.TTC}, DefineFont.class, true);
        CompilerConfiguration compilerConfig = config.getCompilerConfiguration();
        fontsConfig = compilerConfig.getFontsConfiguration();
        showShadowedDeviceFontWarnings = compilerConfig.showShadowedDeviceFontWarnings();
    }

    public static final String UNICODERANGE = "unicodeRange";
    public static final String SYSTEMFONT = "systemFont";
    public static final String SOURCELIST = "sourceList";
	public static final String FLASHTYPE = "flashType";
	public static final String ADVANTIALIASING = "advancedAntiAliasing";

    public boolean isSupportedAttribute( String attr )
    {
        return FONTNAME.equals( attr )
               || FONTSTYLE.equals( attr )
               || FONTWEIGHT.equals( attr )
               || FONTFAMILY.equals( attr )
               || UNICODERANGE.equals( attr )
		       || FLASHTYPE.equals( attr )		        
		       || ADVANTIALIASING.equals( attr )		        
               || SYSTEMFONT.equals( attr )
               || SOURCELIST.equals( attr )
                ;
    }

    public TranscodingResults doTranscode( PathResolver context, SymbolTable symbolTable,
                                           Map args, String className, boolean generateSource )
            throws TranscoderException
    {
        TranscodingResults results = new TranscodingResults();
        String systemFont = null;
        List locations;

        if (args.containsKey( SOURCE ))
        {
            if (args.containsKey( SYSTEMFONT ) || args.containsKey( SOURCELIST ))
                throw new BadParameters();

            results.assetSource = resolveSource( context, args );
            results.modified = results.assetSource.getLastModified();
            locations = new LinkedList();
            locations.add( getURL(results.assetSource) );
        }
        else if (args.containsKey( SYSTEMFONT ))
        {
            if (args.containsKey( SOURCE ) || args.containsKey( SOURCELIST ))
                throw new BadParameters();

            systemFont = (String) args.get( SYSTEMFONT );
            locations = new LinkedList();
            locations.add(systemFont);
        }
        else if (args.containsKey( SOURCELIST ))
        {
            locations = resolveSourceList(context, args);
        }
        else
        {
            throw new BadParameters();
        }

        FontManager fontManager = fontsConfig.getTopLevelManager();
        int fontStyle = getFontStyle( args );
        boolean hasLayout = true;		// probably always want layout info here

        String family = (String) args.get( FONTFAMILY );
        String alias = (String) args.get( FONTNAME );
        if (alias == null)
        {
            alias = systemFont;
        }
        if (alias == null)
        {
            alias = family;     // FIXME, just either name it name or family, not both!
        }
        if (alias == null)
        {
            throw new BadParameters();
        }

        if (systemFont != null && systemFont.equals(alias) && showShadowedDeviceFontWarnings)
        {
            EmbeddedFontShadowsDeviceFont embeddedFontShadowsDeviceFont = new EmbeddedFontShadowsDeviceFont(alias);
            String path = (String) args.get(Transcoder.FILE);
            String pathSep = (String) args.get(Transcoder.PATHSEP);
            if ("true".equals(pathSep))
            {
                path = path.replace('/', '\\');
            }
            embeddedFontShadowsDeviceFont.path = path;
            if (args.containsKey(Transcoder.LINE))
            {
                int line = Integer.parseInt( (String) args.get(Transcoder.LINE) );
                embeddedFontShadowsDeviceFont.line = line;
            }
            ThreadLocalToolkit.log(embeddedFontShadowsDeviceFont);
        }
        
	    boolean flashType, flashTypeAsName = true;
        String flashTypeStr = (String)args.get(ADVANTIALIASING);
	    if (flashTypeStr == null)
	    {
		    flashTypeStr = (String)args.get(FLASHTYPE);
	    }
	    else
	    {
	    	flashTypeAsName = false;
	    }

	    if (flashTypeStr != null)
	    {
		    if (flashTypeStr.equalsIgnoreCase("true"))
		    {
			    flashType = true;
		    }
		    else if (flashTypeStr.equalsIgnoreCase("false"))
		    {
			    flashType = false;
		    }
		    else if (flashTypeAsName)
		    {
			    throw new BadFlashType();
		    }
		    else
		    {
		    	throw new BadAdvancedAntiAliasing();
		    }
	    }
	    else
	    {
		    flashType = fontsConfig.getFlashType();
	    }

        //String newName = (String) args.get( NEWNAME );          // fixme - export name is always font name?

        // just set the font name to the symbol name for now.....
        FontBuilder builder = getBuilder(alias, locations, fontStyle, hasLayout, flashType, args);
        try
        {
            //Add characters for unicode-range
            char[][] ranges = getUnicodeRanges( fontManager, (String) args.get(UNICODERANGE) );
            if (ranges != null)
            {
                for (int i = 0; i < ranges.length; i++)
                {
                    char[] range = ranges[i];

                    if (range != null && range.length == 2)
                    {
                        int count = range[1] - range[0] + 1; //Inclusive range
                        builder.addCharset( range[0], count );
                    }
                }
            }
            else
            {
                builder.addAllChars();
            }

            results.defineTag = builder.build();
            if (generateSource)
                generateSource( results, className, args );
        }
        catch (TranscoderException te)
        {
	        throw te;
        }
        catch (Exception e)
        {
	        if (Trace.error)
		        e.printStackTrace();

            throw new ExceptionWhileTranscoding( e );
        }
        return results;
    }

    private URL getURL(VirtualFile virtualFile) throws TranscoderException
    {
        URL result;

        if (virtualFile instanceof SwcFile)
        {
            try
            {
                String name = virtualFile.getName();
                String path = name.substring(name.indexOf("$") + 1);
                File file = File.createTempFile(path, null);
                FileUtil.writeBinaryFile(file, virtualFile.getInputStream());
                result = file.toURL();
            }
            catch (IOException ioException)
            {
                throw new UnableToExtract( virtualFile.getName() );
            }
        }
        else
        {
            try
            {
                result = new URL(virtualFile.getURL());
            }
            catch (java.net.MalformedURLException e)
            {
                throw new AbstractTranscoder.UnableToReadSource( virtualFile.getName() );
            }
        }

        return result;
    }

    private FontBuilder getBuilder( String alias, List locations, int fontStyle,
                                    boolean hasLayout, boolean useFlashType, Map args ) throws TranscoderException
    {
        FontManager fontManager = fontsConfig.getTopLevelManager();
        int defineFontTag = TagValues.stagDefineFont3;

	    FontBuilder builder = null;
        for (Iterator it = locations.iterator(); it.hasNext();)
        {
            Object o = it.next();

            try
            {
                if (o instanceof URL)
                {
                    builder	= new FontBuilder(defineFontTag, fontManager, alias, (URL) o, fontStyle, hasLayout, useFlashType);
                }
                else
                {
                    builder = new FontBuilder(defineFontTag, fontManager, alias, (String) o, fontStyle, hasLayout, useFlashType);
                }
            }
            catch (Exception e)
            {
	            if (Trace.error)
	            {
		            e.printStackTrace();
	            }

                ExceptionWhileTranscoding exceptionWhileTranscoding = new ExceptionWhileTranscoding(e);
                String path = (String) args.get(Transcoder.FILE);
                String pathSep = (String) args.get(Transcoder.PATHSEP);
                if ("true".equals(pathSep))
                {
                    path = path.replace('/', '\\');
                }
                exceptionWhileTranscoding.path = path;
                if (args.containsKey(Transcoder.LINE))
                {
                    int line = Integer.parseInt( (String) args.get(Transcoder.LINE) );
                    exceptionWhileTranscoding.line = line;
                }
                ThreadLocalToolkit.log(exceptionWhileTranscoding);
            }
            if (builder != null)
            {
                return builder;
            }
        }

        throw new UnableToBuildFont( alias );
    }

    public static int getFontStyle( Map args )
    {
        int s = FontFace.PLAIN;

        String style = (String) args.get( FONTSTYLE );
        if (style == null)
            style = "normal";

        String weight = (String) args.get( FONTWEIGHT );
        if (weight == null)
            weight = "normal";

        if (isBold( weight ))
            s += FontFace.BOLD;

        if (isItalic( style ))
            s += FontFace.ITALIC;

        return s;
    }

    public static boolean isBold(String value)
    {
        boolean bold = false;

        if (value != null)
        {
            String b = value.trim().toLowerCase();
            if (b.startsWith("bold"))
            {
                bold = true;
            }
            else
            {
                try
                {
                    int w = Integer.parseInt(b);
                    if (w >= 700)
                        bold = true;
                }
                catch (Throwable t)
                {
                }
            }
        }

        return bold;
    }

    public static boolean isItalic(String value)
    {
        boolean italic = false;

        if (value != null)
        {
            String ital = value.trim().toLowerCase();
            if (ital.equals("italic") || ital.equals("oblique"))
                italic = true;
        }

        return italic;
    }

    public char[][] getUnicodeRanges(FontManager fontManager, String r) throws TranscoderException
    {
        char[][] ranges = null;

        if (r != null)
        {
            String value = new String( r );
            //Check if it's a registered language name
            {
                String langRange = fontManager.getLanguageRange( value );
                if (langRange != null)
                    value = langRange;
            }

            //Remove extraneous formatting first
            value = value.replace( ';', ' ' ).replace( '\n', ' ' ).replace( '\r', ' ' ).replace( '\f', ' ' );

            StringTokenizer st = new StringTokenizer( value, "," );

            int count = st.countTokens();
            ranges = new char[count][2];
            parseRanges( st, ranges );
        }

        return ranges;
    }

    /**
     * Values are expressed as hexadecimal numbers, prefixed with &quot;U+&quot;.
     * For single numbers, the character '?' is assumed to mean 'any value' which
     * creates a range of character positions. Otherwise, the range can be specified
     * explicitly using a hyphen, e.g. U+00A0-U+00FF
     *
     * @param st
     * @param ranges
     * @throws InvalidUnicodeRangeException
     */
    private static void parseRanges(StringTokenizer st, char[][] ranges) throws TranscoderException
    {
        int i = 0;
        while (st.hasMoreElements())
        {
            String element = ((String)st.nextElement()).trim().toUpperCase();

            if (element.startsWith("U+"))
            {
                String range = element.substring(2).trim();
                String low;
                String high;

                if (range.indexOf('?') > 0) //Wild-Card Range, e.g. U+00??
                {
                    low = range.replace('?', '0');
                    high = range.replace('?', 'F');
                }
                else if (range.indexOf('-') > 0) //Basic Range, e.g. U+0020-U+007E
                {
                    low = range.substring(0, range.indexOf('-'));
                    String temp = range.substring(range.indexOf('-') + 1).trim();
                    if (temp.startsWith("U+"))
                    {
                        high = temp.substring(2).trim();
                    }
                    else
                    {
                        throw new InvalidUnicodeRangeException(temp);
                    }
                }
                else if (range.length() <= 4) //Single Char, e.g. U+0041
                {
                    low = range;
                    high = range;
                }
                else
                {
                    throw new InvalidUnicodeRangeException(range);
                }

                ranges[i][0] = (char)Integer.parseInt(low, 16);
                ranges[i][1] = (char)Integer.parseInt(high, 16);

                i++;
            }
            else if (element.length() == 0)
            {
                continue;
            }
            else
            {
                throw new InvalidUnicodeRangeException(element);
            }
        }
    }

    private List resolveSourceList( PathResolver context, Map args ) throws TranscoderException
    {
        List result = new LinkedList();

        Iterator iterator = ((List) args.get( SOURCELIST )).iterator();

        while ( iterator.hasNext() )
        {
            Object source = iterator.next();

            if (source instanceof URLSource)
            {
                URLSource urlSource = (URLSource) source;
                VirtualFile virtualFile = resolve(context, urlSource.getValue());
                result.add( getURL(virtualFile) );
            }
            else // if (source instanceof LocalSource)
            {
                LocalSource localSource = (LocalSource) source;
                result.add( localSource.getValue() );
            }
        }

        return result;
    }

    public static final class InvalidUnicodeRangeException extends TranscoderException
    {
        private static final long serialVersionUID = 3173208110428813980L;
        public InvalidUnicodeRangeException(String range)
        {
            this.range = range;
        }
        public String range;
    }

    public static final class BadParameters extends TranscoderException
    {
        private static final long serialVersionUID = -2390481014380505531L;
    }

	public static final class BadFlashType extends TranscoderException
	{
        private static final long serialVersionUID = 3971519462447951564L;
	}

	public static final class BadAdvancedAntiAliasing extends TranscoderException
	{
        private static final long serialVersionUID = 8425867739365188050L;
	}

    public static final class UnableToBuildFont extends TranscoderException
    {
        private static final long serialVersionUID = 1520596054636875393L;
        public UnableToBuildFont( String fontName )
        {
            this.fontName = fontName;
        }
        public String fontName;
    }

    public static final class UnableToExtract extends TranscoderException
    {
        private static final long serialVersionUID = -4585845590777360978L;
        public UnableToExtract( String fileName )
        {
            this.fileName = fileName;
        }
        public String fileName;
    }

    public static final class EmbeddedFontShadowsDeviceFont extends CompilerMessage.CompilerWarning implements ILocalizableMessage
    {
        private static final long serialVersionUID = -1125821048682931471L;
        public EmbeddedFontShadowsDeviceFont( String alias )
        {
            this.alias = alias;
        }
        public final String alias;
    }
}

