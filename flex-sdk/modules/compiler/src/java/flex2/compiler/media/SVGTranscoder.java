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

package flex2.compiler.media;

import flex2.compiler.SymbolTable;
import flex2.compiler.TranscoderException;
import flex2.compiler.common.PathResolver;
import flex2.compiler.io.VirtualFile;
import flex2.compiler.util.MimeMappings;
import flash.svg.SpriteTranscoder;
import flash.swf.tags.DefineSprite;
import org.apache.batik.transcoder.TranscoderInput;

import java.io.BufferedInputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import java.util.zip.GZIPInputStream;

/**
 * Transcodes SVGs into DefineSprites for embedding
 *
 * @author Pete Farland
 * @author Roger Gonzalez
 * @author Clement Wong
 */
public class SVGTranscoder extends AbstractTranscoder
{
	public SVGTranscoder()
	{
		super(new String[]{MimeMappings.SVG, MimeMappings.SVG_XML}, DefineSprite.class, true);
	}

	public TranscodingResults doTranscode( PathResolver context, SymbolTable symbolTable,
                                           Map args, String className, boolean generateSource )
        throws TranscoderException
	{
        TranscodingResults results = new TranscodingResults( resolveSource( context, args ));
        String newName = (String) args.get( NEWNAME );

        results.defineTag = svg(results.assetSource, newName, args);
        if (generateSource)
            generateSource( results, className, args );
        
        return results;
	}

    public boolean isSupportedAttribute(String attr)
    {
        return SCALE9TOP.equals( attr ) || SCALE9LEFT.equals( attr ) || SCALE9BOTTOM.equals( attr ) || SCALE9RIGHT.equals( attr );
    }

	private DefineSprite svg(VirtualFile source, String newName, Map args)
            throws TranscoderException
	{
		InputStream is = null;

		try
		{
			String docURI = source.getURL();

			is = new BufferedInputStream(source.getInputStream());

			if (isGZIPCompressed((BufferedInputStream) is))
			{
				is = new GZIPInputStream(is);
			}

			TranscoderInput ti = new TranscoderInput(is);
			ti.setURI(docURI);

			DefineSprite sprite = transcodeSVG(ti, source, newName);
            if (args.containsKey(SCALE9LEFT) || args.containsKey(SCALE9RIGHT) || args.containsKey(SCALE9TOP) || args.containsKey(SCALE9BOTTOM))
            {
                MovieTranscoder.defineScalingGrid( sprite, args );
            }
            return sprite;
        }
		catch (IOException ex)
		{
			throw new AbstractTranscoder.ExceptionWhileTranscoding( ex );
		}
		finally
		{
			if (is != null)
			{
				try
				{
					is.close();
				}
				catch (IOException e)
				{
				}
			}
		}
	}

	private DefineSprite transcodeSVG(TranscoderInput ti, VirtualFile source, String symbolName)
            throws TranscoderException
	{
		try
		{
			SpriteTranscoder transcoder = new SpriteTranscoder();
			transcoder.transcode(ti, null);

			DefineSprite defineSprite = new DefineSprite(symbolName);
			defineSprite.tagList = transcoder.getTags();
			defineSprite.framecount = 1; //SVG is static for now

            return defineSprite;
		}
		catch (Exception ex)
		{
			throw new AbstractTranscoder.ExceptionWhileTranscoding( ex );
		}
	}


	private static boolean isGZIPCompressed(BufferedInputStream in)
	{
		try
		{
			in.mark(4);

			if (readUShort(in) == GZIPInputStream.GZIP_MAGIC // Check header magic
					&& readUByte(in) == 8)// Check compression method
			{
				return true;
			}
		}
		catch (Throwable t)
		{
			t.printStackTrace();
		}
		finally
		{
			try
			{
				in.reset();
			}
			catch (IOException ex)
			{
				//This would be bad... but unexpected for this BufferedInputStream.
			}
		}

		return false;
	}

	/*
	 * Util from java.zip.GZIPInputStream
	 * Reads unsigned short in Intel byte order.
	 */
	private static int readUShort(InputStream in) throws IOException
	{
		int b = readUByte(in);
		return (int) readUByte(in) << 8 | b;
	}

	/*
	 * Util from java.zip.GZIPInputStream
	 * Reads unsigned byte.
	 */
	private static int readUByte(InputStream in) throws IOException
	{
		int b = in.read();
		if (b == -1)
		{
			throw new EOFException();
		}

		return b;
	}
}
