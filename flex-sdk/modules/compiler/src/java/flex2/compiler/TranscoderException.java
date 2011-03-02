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

package flex2.compiler;

import flex2.compiler.util.CompilerMessage;

/**
 * Exception to be used when there's a problem in a Transcoder class rather than using ThreadLocalToolkit.logError().
 * This allows for delegation of transcoders, which Zorn is doing
 *
 * Note: path was removed because not all transcoding calls contain a source, and we really want to report
 * the error in transcoding in the context of the calling source code, not as if the transcoded object
 * owned itself.  (I.e. a failure to transcode a nonexistent file should be reported via the AS embed line,
 * not the missing asset file.)
 *
 * Notenote: origin/line re-added, won't be fed by the transcoder, but post-filled-in by the handler.
 * Kind of weird, but will make L10N easier, I think.  --rg
 *
 * @author Brian Deitte
 */
public class TranscoderException extends CompilerMessage.CompilerError implements ILocalizableMessage
{
    public TranscoderException()
    {
    }

    public static class UnrecognizedExtension extends TranscoderException
    {
        public UnrecognizedExtension( String source )
        {
            this.source = source;
        }
        public String source;
    }

    public static class NoMatchingTranscoder extends TranscoderException
    {
        public NoMatchingTranscoder( String mimeType )
        {
            this.mimeType = mimeType;
        }
        public String mimeType;
    }

    public static class NoAssociatedClass extends TranscoderException
    {
        public NoAssociatedClass( String tag, String transcoder )
        {
            this.tag = tag;
            this.transcoder = transcoder;
        }
        public String tag;
        public String transcoder;
    }

}
