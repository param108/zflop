////////////////////////////////////////////////////////////////////////////////
//
//  ADOBE SYSTEMS INCORPORATED
//  Copyright 2005-2006 Adobe Systems Incorporated
//  All Rights Reserved.
//
//  NOTICE: Adobe permits you to use, modify, and distribute this file
//  in accordance with the terms of the license agreement accompanying it.
//
////////////////////////////////////////////////////////////////////////////////

package flex2.compiler.media;

import flash.swf.tags.DefineSprite;
import flash.swf.builder.tags.DefineBitsBuilder;
import flash.graphics.images.JPEGImage;

import java.util.Map;

import flex2.compiler.io.VirtualFile;
import flex2.compiler.util.MimeMappings;
import flex2.compiler.TranscoderException;

/**
 * @author Roger Gonzalez
 */
public class JPEGTranscoder extends ImageTranscoder
{
    public JPEGTranscoder()
    {
        super(new String[]{MimeMappings.JPG, MimeMappings.JPEG}, DefineSprite.class, true);
    }

    public ImageInfo getImage( VirtualFile source, Map args ) throws TranscoderException
    {
        ImageTranscoder.ImageInfo info = new ImageInfo();
		JPEGImage image = null;

		try
		{
			image = new JPEGImage(source.getName(),
                                  source.getLastModified(),
                                  source.size(),
                                  source.getInputStream());
            info.width = image.getWidth();
            info.height = image.getHeight();
            info.defineBits = DefineBitsBuilder.build( image );
            return info;
        }

		catch (Exception ex)
		{
            throw new AbstractTranscoder.ExceptionWhileTranscoding( ex );
		}
    }
}
