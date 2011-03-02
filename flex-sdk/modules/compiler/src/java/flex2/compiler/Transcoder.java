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

package flex2.compiler;

import flash.swf.tags.DefineTag;
import flex2.compiler.common.PathResolver;
import flex2.compiler.io.VirtualFile;

import java.util.Map;

/**
 * Interface for transcoding resources which are used in Embed
 *
 * @author Clement Wong
 */
public interface Transcoder
{
	/**
	 * If this transcoder can process the specified file, return true.
	 */
	boolean isSupported(String mimeType);

	/**
	 * Read the media file and create DefineTag
	 */
    TranscodingResults transcode(PathResolver context, SymbolTable symbolTable,
                                 Map args, String className, boolean generateSource)
        throws TranscoderException;

    /**
     * Returns class that should be extended with given DefineTag for this transcoder
     */
    String getAssociatedClass(DefineTag tag);

    /**
     * Clears the caches associated with this transcoder after a compilation.
     */
    void clear();

    String FILE = "_file";
    String PATHSEP = "_pathsep";
    String LINE = "_line";
    String COLUMN = "_column";
    String SOURCE = "source";
    String SYMBOL = "symbol";
    String NEWNAME = "exportSymbol";
    String MIMETYPE = "mimeType";
    String RESOLVED_SOURCE = "_resolvedSource";
    String ORIGINAL = "original";
    String SKINCLASS = "skinClass";

    public class TranscodingResults
    {
        public TranscodingResults() {}
        public TranscodingResults( VirtualFile assetSource )
        {
            this.assetSource = assetSource;
            this.modified = assetSource.getLastModified();
        }
        public DefineTag defineTag;
        public String generatedCode;
        public VirtualFile assetSource;
        public long modified;
        public boolean requireCodegen;
    }
}
