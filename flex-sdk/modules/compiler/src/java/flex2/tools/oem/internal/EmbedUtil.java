////////////////////////////////////////////////////////////////////////////////
//
//  ADOBE SYSTEMS INCORPORATED
//  Copyright 2007 Adobe Systems Incorporated
//  All Rights Reserved.
//
//  NOTICE: Adobe permits you to use, modify, and distribute this file
//  in accordance with the terms of the license agreement accompanying it.
//
////////////////////////////////////////////////////////////////////////////////

package flex2.tools.oem.internal;

import flex2.compiler.TranscoderException;
import flex2.compiler.media.SkinTranscoder;
import flex2.tools.oem.Logger;

/**
 * @version 3.0
 * @author Paul Reilly
 */
public class EmbedUtil
{
    /**
     * Generates the source code for an embedded skin.
     *
     * @param fullClassName The fully qualified class name in dot format.
     * @param baseClassName The base class for the embedded skin.
     * @param needsIBorder Whether IBorder needs to be implemented.
     * @param needsBorderMetrics Whether the borderMetrics property needs to be implemented.
     * @param needsIFlexDisplayObject Whether IFlexDisplayObject needs to be implemented.
     * @param needsMeasuredHeight Whether the measuredHeight property needs to be implemented.
     * @param needsMeasuredWidth Whether the measuredWidth property needs to be implemented.
     * @param needsMove Whether the move() function needs to be implemented.
     * @param needsSetActualSize Whether the setActualSize() function needs to be implemented.
     * @param flexMovieClipOrSprite Whether the base class is a MovieClip or Sprite.
     * @param logger The logger used to report transcoding exceptions.
     * @return The generated skin class.
     */
    public String generateSkinSource(String fullClassName,
                                     String baseClassName,
                                     boolean needsIBorder,
                                     boolean needsBorderMetrics,
                                     boolean needsIFlexDisplayObject,
                                     boolean needsMeasuredHeight,
                                     boolean needsMeasuredWidth,
                                     boolean needsMove,
                                     boolean needsSetActualSize,
                                     boolean flexMovieClipOrSprite,
                                     Logger logger)
    {
        String result = null;

        try
        {
            result = SkinTranscoder.generateSource(fullClassName, baseClassName, needsIBorder,
                                                   needsBorderMetrics, needsIFlexDisplayObject,
                                                   needsMeasuredHeight, needsMeasuredWidth,
                                                   needsMove, needsSetActualSize,
                                                   flexMovieClipOrSprite);
        }
        catch (TranscoderException transcoderException)
        {
            logger.log(transcoderException, -1, null);
        }

        return result;
    }
}