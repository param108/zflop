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

import flex2.compiler.SymbolTable;
import flex2.compiler.TranscoderException;
import flex2.compiler.common.PathResolver;
import flash.swf.tags.DefineTag;
import flash.swf.tags.DefineBinaryData;

import java.util.Map;
import java.io.BufferedInputStream;

/**
 * @author Roger Gonzalez
 */
public class DataTranscoder extends AbstractTranscoder
{
    public DataTranscoder()
    {
        super( new String[] {"application/octet-stream"}, null, false );
    }

    public TranscodingResults doTranscode( PathResolver context, SymbolTable symbolTable,
                                           Map args, String className, boolean generateSource )
        throws TranscoderException
    {
        TranscodingResults results = new TranscodingResults( resolveSource( context, args ) );
        loadData( results );
        if (generateSource)
            generateSource( results, className, args );
        return results;
    }

    public void loadData(TranscodingResults asset )
            throws TranscoderException
    {
        DefineBinaryData defineBinaryData = new DefineBinaryData();

        try
        {
            BufferedInputStream in = new BufferedInputStream( asset.assetSource.getInputStream() );

            int size = (int) asset.assetSource.size();
            defineBinaryData.data = new byte[size];

            int r = 0;
            while (r < size)
            {
                int result = in.read( defineBinaryData.data, r, size - r );
                if (result == -1)
                    break;
            }

            asset.defineTag = defineBinaryData;
        }
        catch (Exception e)
        {
            throw new AbstractTranscoder.UnableToReadSource( asset.assetSource.getName() );
        }
    }

    public boolean isSupportedAttribute( String attr )
    {
        return false;
    }

    public String getAssociatedClass( DefineTag tag )
    {
        return "mx.core.ByteArrayAsset";
    }

    public void clear()
    {
    }
}
