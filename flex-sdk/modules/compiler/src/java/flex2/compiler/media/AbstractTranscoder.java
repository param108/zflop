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

import flash.swf.tags.DefineBits;
import flash.swf.tags.DefineBitsLossless;
import flash.swf.tags.DefineButton;
import flash.swf.tags.DefineFont;
import flash.swf.tags.DefineFont1;
import flash.swf.tags.DefineFont2;
import flash.swf.tags.DefineFont3;
import flash.swf.tags.DefineFont4;
import flash.swf.tags.DefineSound;
import flash.swf.tags.DefineSprite;
import flash.swf.tags.DefineTag;
import flash.swf.tags.DefineText;
import flash.util.Trace;
import flex2.compiler.SymbolTable;
import flex2.compiler.Transcoder;
import flex2.compiler.TranscoderException;
import flex2.compiler.common.PathResolver;
import flex2.compiler.io.NetworkFile;
import flex2.compiler.io.VirtualFile;
import flex2.compiler.util.ThreadLocalToolkit;
import flex2.compiler.util.VelocityManager;

import java.io.StringWriter;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.TreeMap;

import org.apache.velocity.Template;
import org.apache.velocity.VelocityContext;

/**
 * Default transcoder implementation.  The only things that need to be done to override is to give the right
 * information in the constructor and provide an implementation of doTranscode().
 *
 * @author Brian Deitte
 */
public abstract class AbstractTranscoder implements Transcoder
{
    private static final String CODEGEN_TEMPLATE_PATH = "flex2/compiler/as3/";

    // TODO - move these once ImageTranscoder gets refactored
    public static final String SCALE9TOP = "scaleGridTop";
    public static final String SCALE9LEFT = "scaleGridLeft";
    public static final String SCALE9BOTTOM = "scaleGridBottom";
    public static final String SCALE9RIGHT = "scaleGridRight";

    public static final String FONTSTYLE = "fontStyle";
    public static final String FONTWEIGHT = "fontWeight";
    public static final String FONTNAME = "fontName";
    public static final String FONTFAMILY = "fontFamily";

    protected String[] mimeTypes;
    protected Class defineTag;
    protected boolean cacheTags;

    protected Map transcodingCache = new HashMap();

    private static Map associatedClasses = new HashMap();
    public static final String SKIN_SPRITE = "mx.core.SpriteAsset";
    public static final String SKIN_MOVIECLIP = "mx.core.MovieClipAsset";
    public static final String SKIN_BITMAP = "mx.core.BitmapAsset";
    public static final String SKIN_BUTTON = "mx.core.ButtonAsset";
    public static final String SKIN_TEXTFIELD = "mx.core.TextFieldAsset";
    public static final String FONT = "mx.core.FontAsset";
    public static final String SOUND = "mx.core.SoundAsset";
    public static final String ASSET_TYPE = "flash.display.DisplayObject";

    static
    {
        // TODO - make these configurable - skinsprite is mx framework specific
        associatedClasses.put(DefineSprite.class.getName(), SKIN_SPRITE);
        associatedClasses.put(DefineBits.class.getName(), SKIN_BITMAP);
        associatedClasses.put(DefineBitsLossless.class.getName(), SKIN_BITMAP);
        associatedClasses.put(DefineFont.class.getName(), FONT);
        associatedClasses.put(DefineFont1.class.getName(), FONT);
        associatedClasses.put(DefineFont2.class.getName(), FONT);
        associatedClasses.put(DefineFont3.class.getName(), FONT);
        associatedClasses.put(DefineFont4.class.getName(), FONT);
        associatedClasses.put(DefineSound.class.getName(), SOUND);
        associatedClasses.put(DefineButton.class.getName(), SKIN_BUTTON);
        associatedClasses.put(DefineText.class.getName(), SKIN_TEXTFIELD);
    }

    public AbstractTranscoder(String[] mimeTypes, Class defineTag, boolean cacheTags)
    {
        this.mimeTypes = mimeTypes;
        this.defineTag = defineTag;
        this.cacheTags = cacheTags;
    }

    public boolean isSupported(String mimeType)
    {
        for (int i = 0; i < mimeTypes.length; i++)
        {
            if (mimeTypes[i].equalsIgnoreCase(mimeType))
            {
                return true;
            }
        }
        return false;
    }

    public TranscodingResults transcode( PathResolver context, SymbolTable symbolTable,
                                         Map args, String className, boolean generateSource )
            throws TranscoderException
    {
        for (Iterator it = args.keySet().iterator(); it.hasNext();)
        {
            String attr = (String) it.next();
            if (attr.startsWith( "_") || Transcoder.SOURCE.equalsIgnoreCase( attr ) || Transcoder.MIMETYPE.equalsIgnoreCase( attr ) || Transcoder.NEWNAME.equalsIgnoreCase( attr ))
            {
                continue;
            }
            if (!Transcoder.ORIGINAL.equals(attr) && !isSupportedAttribute( attr ))
            {
                throw new UnsupportedAttribute( attr, getClass().getName() );
            }
        }

        String cacheKey = null;

        TranscodingResults results = null;

        if (cacheTags)
        {
            cacheKey = getCacheKey( args );
            results = (TranscodingResults) transcodingCache.get( cacheKey );
        }

        if (results == null)
        {
            results = doTranscode( context, symbolTable, args, className, generateSource );

            if (cacheTags)
            {
	            // reget the cacheKey, since RESOLVED_SOURCE could have been added to the args
	            cacheKey = getCacheKey( args );
                transcodingCache.put(cacheKey, results);
            }
        }
        else if (Trace.embed)
        {
            Trace.trace("Found cached DefineTag for " + cacheKey);
        }

        return results;
    }

    private String getCacheKey(Map args)
    {
        TreeMap m = new TreeMap( args );
        String key = "" + m.hashCode(); //

        if (Trace.embed)
        {
            key += "_" + m.toString();  // TODO: don't hard-code key
        }

        return key;
    }

    public VirtualFile resolve( PathResolver context, String path ) throws TranscoderException
    {
        String p = path;
        if (path.startsWith( "file:"))
            p = p.substring( "file:".length() );    // hate hate hate hate

        VirtualFile f = context.resolve( p );
        if (f == null)
        {
            throw new UnableToResolve( path );
        }
        if (f instanceof NetworkFile)
        {
            throw new NetworkTranscodingSource( path );
        }
        return f;
    }

    public VirtualFile resolveSource( PathResolver context, Map args ) throws TranscoderException
    {
        VirtualFile result = null;
        String resolvedSource = (String) args.get( Transcoder.RESOLVED_SOURCE );

        if (resolvedSource != null)
        {
            result = ThreadLocalToolkit.getResolvedPath(resolvedSource);
        }

        if (result == null)
        {
            String source = (String) args.get( Transcoder.SOURCE );

            if (source == null)
            {
                throw new MissingSource();
            }

            result = resolve( context, source );
        }

        return result;
    }

    public abstract TranscodingResults doTranscode( PathResolver context, SymbolTable symbolTable,
                                                    Map args, String className, boolean generateSource )
        throws TranscoderException;

    public abstract boolean isSupportedAttribute( String attr );

    public String getAssociatedClass(DefineTag tag)
    {
        if (tag == null)
        {
            return null;
        }

        String cls = (String)associatedClasses.get(tag.getClass().getName());

        if (cls != null && (defineTag == null || defineTag.isAssignableFrom(tag.getClass())))
        {
            if (((tag instanceof DefineSprite) && ((DefineSprite)tag).framecount > 1) && (cls.equals( SKIN_SPRITE )))
            {
                cls = SKIN_MOVIECLIP;
            }
            return cls;
        }

        if (defineTag == null)
        {
            if (Trace.embed)
            {
                Trace.trace("Couldn't find a matching class, so associating " + tag + " with " + SKIN_SPRITE);
            }
            return SKIN_SPRITE;
        }
        else
        {
            return null;
        }
    }

    public void clear()
    {
        if (transcodingCache.size() != 0)
        {
            transcodingCache = new HashMap();
        }
    }
    
    public void generateSource(TranscodingResults asset, String fullClassName, Map embedMap)
    		throws TranscoderException
    {
    	generateSource(asset, fullClassName, embedMap, new HashMap());
    }

    public void generateSource(TranscodingResults asset, String fullClassName, Map embedMap, Map embedProps)
            throws TranscoderException
    {
        String baseClassName = getAssociatedClass( asset.defineTag );
        String packageName = "";
        String className = fullClassName;
        int dot = fullClassName.lastIndexOf( '.' );
        if (dot != -1)
        {
            packageName = fullClassName.substring( 0, dot );
            className = fullClassName.substring( dot + 1 );
        }

	    if (asset.assetSource != null)
	    {
            String path = asset.assetSource.getName().replace('\\', '/');
            embedMap.put(Transcoder.RESOLVED_SOURCE, path);
            ThreadLocalToolkit.addResolvedPath(path, asset.assetSource);
	    }

        try
        {
            String templateName = "EmbedClass.vm";
            Template template = VelocityManager.getTemplate(CODEGEN_TEMPLATE_PATH + templateName);
            if (template == null)
            {
                throw new TemplateException( templateName );
            }

            VelocityContext velocityContext = VelocityManager.getCodeGenContext();
            velocityContext.put( "packageName", packageName );
            velocityContext.put( "baseClass", baseClassName );
            if (embedProps.size() != 0)
            {
            	velocityContext.put("assetType", ASSET_TYPE );
            }
            velocityContext.put( "embedClass", className );
            velocityContext.put( "embedMap", embedMap );
        	velocityContext.put( "embedProps", embedProps );

            StringWriter stringWriter = new StringWriter();

            template.merge(velocityContext, stringWriter);
            // once we figure out a non-AS2 way to call stop, add this and put the generated code in EmbedClass.vm
            //velocityContext.put("needsStop", "" + (data.defineTag instanceof DefineSprite && ((DefineSprite)data.defineTag).needsStop));

            //long t2 = System.currentTimeMillis();
            //VelocityManager.parseTime += t2 - start;
            //VelocityManager.mergeTime += System.currentTimeMillis() - t2;

            asset.generatedCode = stringWriter.toString();

        }
        catch (Exception e)
        {
            if (Trace.error)
            {
                e.printStackTrace();
            }
            throw new UnableToGenerateSource( fullClassName );
        }
    }

    public static class TemplateException extends TranscoderException
    {
        public TemplateException( String templateName )
        {
            this.templateName = templateName;
        }
        public String templateName;
    }
    public static class SourceException extends TranscoderException
    {
        public SourceException( String className )
        {
            this.className = className;
        }
        public String className;
    }

    public static class UnsupportedAttribute extends TranscoderException
    {
        public UnsupportedAttribute( String attribute, String className )
        {
            this.attribute = attribute;
            this.className = className;
        }
        public String attribute;
        public String mimeType;
        public String className;
    }

    public static class UnableToResolve extends TranscoderException
    {
        public UnableToResolve( String source )
        {
            this.source = source;
        }
        public String source;
    }
    public static class NetworkTranscodingSource extends TranscoderException
    {
        public NetworkTranscodingSource( String url )
        {
            this.url = url;
        }
        public String url;
    }

    public static class MissingSource extends TranscoderException
    {
    }

    public static class UnableToGenerateSource extends TranscoderException
    {
        public UnableToGenerateSource( String className )
        {
            this.className = className;
        }
        public String className;
    }

    public static class UnableToReadSource extends TranscoderException
    {
        public UnableToReadSource( String source )
        {
            this.source = source;
        }
        public String source;
    }

    public static class ExceptionWhileTranscoding extends TranscoderException
    {
        public ExceptionWhileTranscoding( Exception exception )
        {
            this.exception = exception.getMessage();
        }
        public String exception;
    }

    public static class EmbedRequiresCodegen extends TranscoderException
    {
        public EmbedRequiresCodegen( String source, String className )
        {
            this.source = source;
            this.className = className;
        }
        public String source;
        public String className;
    }

    public static final class IncompatibleTranscoderParameters extends TranscoderException
    {
        public IncompatibleTranscoderParameters( String param1, String param2 )
        {
            this.param1 = param1;
            this.param2 = param2;
        }
        public String param1;
        public String param2;
    }
}
