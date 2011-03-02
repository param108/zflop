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

package flex2.compiler.media;

import flex2.compiler.common.PathResolver;
import flex2.compiler.SymbolTable;
import flex2.compiler.TranscoderException;
import flex2.compiler.io.VirtualFile;
import flex2.compiler.util.MimeMappings;

import java.util.Map;
import java.io.BufferedInputStream;
import java.io.Reader;
import java.io.InputStreamReader;

import flash.swf.tags.DefineTag;
import flash.util.FileUtils;

/**
 * @author Roger Gonzalez
 */
public class XMLTranscoder extends AbstractTranscoder
{
    public final static String ENCODING = "encoding";
    public XMLTranscoder()
    {
        super( new String[] {MimeMappings.XML}, null, false );
    }

    public TranscodingResults doTranscode( PathResolver context, SymbolTable symbolTable,
                                           Map args, String className, boolean generateSource )
        throws TranscoderException
    {
        VirtualFile source = resolveSource( context, args );
        TranscodingResults results = new TranscodingResults(source);

        if (generateSource)
        {
            generateSource( results, className, args );
        }
        else
        {
            throw new EmbedRequiresCodegen( source.getName(), className );
        }

        return results;
    }

    public void generateSource(TranscodingResults asset, String fullClassName, Map embedMap )
            throws TranscoderException
    {
        String encoding = (String) embedMap.get( ENCODING );
        String packageName = "";
        String className = fullClassName;
        int dot = fullClassName.lastIndexOf( '.' );
        if (dot != -1)
        {
            packageName = fullClassName.substring( 0, dot );
            className = fullClassName.substring( dot + 1 );
        }

        StringBuffer source = new StringBuffer( 1024 );
        source.append( "package " );
        source.append( packageName );
        source.append( " { public class " );
        source.append( className );
        source.append( " { public static var data:XML = " );

        try
        {
            BufferedInputStream in = new BufferedInputStream( asset.assetSource.getInputStream() );
            in.mark(3);

			Reader reader = new InputStreamReader(in, FileUtils.consumeBOM(in, encoding));

            char[] line = new char[2000];
            int count = 0;

            while ((count = reader.read(line, 0, line.length)) >= 0)
            {
                source.append(line, 0, count);
            }
		}
        catch (Exception e)
        {
            throw new AbstractTranscoder.UnableToReadSource( asset.assetSource.getName() );
        }

        source.append( "; } }" );

        asset.generatedCode = source.toString();
    }


    public boolean isSupportedAttribute( String attr )
    {
        return ENCODING.equals( attr );
    }


    public String getAssociatedClass( DefineTag tag )
    {
        return "Object";
    }

    public void clear()
    {
    }

}
