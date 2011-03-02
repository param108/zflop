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

package flex2.compiler.as3;

import flash.util.Trace;
import flex2.compiler.AssetInfo;
import flex2.compiler.CompilationUnit;
import flex2.compiler.Source;
import flex2.compiler.SymbolTable;
import flex2.compiler.Transcoder;
import flex2.compiler.TranscoderException;
import flex2.compiler.common.LocalFilePathResolver;
import flex2.compiler.common.PathResolver;
import flex2.compiler.common.SinglePathResolver;
import flex2.compiler.io.VirtualFile;
import flex2.compiler.util.MimeMappings;
import flex2.compiler.util.ThreadLocalToolkit;

import java.util.Map;
import java.util.Iterator;

/**
 * Methods for using Transcoders for Embed
 *
 * @author Brian Deitte
 */
public class EmbedUtil
{
    public static Transcoder.TranscodingResults transcode(Transcoder[] transcoders,
                                                          CompilationUnit unit, SymbolTable symbolTable,
                                                          String className, Map args, int line, int col,
                                                          boolean generateCode)
    {
		PathResolver context = new PathResolver();
		Transcoder.TranscodingResults results = null;
        Source source = unit.getSource();

        if (!args.containsKey(Transcoder.RESOLVED_SOURCE))
        {
            String embedSource = (String) args.get(Transcoder.SOURCE);

            // paths starting with slash are either relative to a source path root or
            // fully qualified.
            if (embedSource != null && embedSource.charAt(0) == '/')
            {
                VirtualFile pathRoot = source.getPathRoot();
                if (pathRoot != null)
                {
                    context.addSinglePathResolver(pathRoot);
                }
                Object owner = source.getOwner();
                if (owner instanceof SinglePathResolver)
                {
                    context.addSinglePathResolver((SinglePathResolver) owner);
                }
            }
            else
            {
                if ( args.containsKey(Transcoder.FILE) )
                {
                	String path = (String) args.get(Transcoder.FILE);
                	String pathSep = (String) args.get(Transcoder.PATHSEP);
                	if ("true".equals(pathSep))
                	{
                		path = path.replace('/', '\\');
                	}
                	
                    VirtualFile contextFile = LocalFilePathResolver.getSingleton().resolve(path);

                    // If the contextFile is the same as the Source's file, then don't add
                    // it as a path resolver, because we'll rely on the Source's
                    // delegate/backing file.  If we don't do this, then some relative
                    // paths might incorrectly be resolved relative to the generated .as
                    // file, instead of the original mxml file.
                    if ((contextFile != null) && !contextFile.getName().equals(source.getName()))
                    {
                        context.addSinglePathResolver(contextFile);
                    }
                }

                VirtualFile backingFile = source.getBackingFile();

                if (backingFile != null)
                {
                    context.addSinglePathResolver(backingFile);
                }
            }
            context.addSinglePathResolver( ThreadLocalToolkit.getPathResolver() );
        }

		if (!unit.getAssets().contains(className))
		{
            results = transcode(transcoders, symbolTable, className, args, line, col, generateCode, source, context);
 			if (results != null && results.defineTag != null)  // else there was an error or its a pure-code asset
   			{
   				unit.getAssets().add(className, new AssetInfo(results.defineTag, results.assetSource, results.modified, args));
   			}
        }
        else
		{
			assert false : "Asset already added for " + className;
		}

		return results;
	}

    // Flex Builder is using this temporarily.
    public static Transcoder.TranscodingResults transcode(Transcoder[] transcoders, String className,
                                                          Map args, int line, int col,
                                                          boolean generateCode, Source s,
                                                          PathResolver context)
    {
        return transcode(transcoders, null, className, args, line, col, generateCode, s, context);
    }

    private static Transcoder.TranscodingResults transcode(Transcoder[] transcoders, SymbolTable symbolTable,
                                                           String className, Map args, int line, int col,
                                                           boolean generateCode, Source s,
                                                           PathResolver context)
    {
        String request = formatTranscodeRequest( args );
        Transcoder.TranscodingResults results = null;
        // Brian: one thing that we could still try here for performance is to have a switch that allows the className
        // that is passed in to be overriden.  For mxml and var level Embeds this could happen.  When this is allowed,
        // if we have already transcoded the given source location (as found out from a source->defineTag cache),
        // we could just return this location as well as the new className that should be used

        String mimeType = (String) args.get( Transcoder.MIMETYPE );
        String origin = (String) args.get( Transcoder.FILE );
        String pathSep = (String) args.get( Transcoder.PATHSEP );
        if ("true".equals(pathSep))
        {
        	origin = origin.replace('/', '\\');
        }
        String nameForReporting = "";

        long mem = -1;
        if (Trace.embed)
        {
            Trace.trace("Transcoding " + request);
            if (ThreadLocalToolkit.getBenchmark() != null)
            {
	            ThreadLocalToolkit.getBenchmark().startTime("Transcoded " + request);
	            mem = ThreadLocalToolkit.getBenchmark().peakMemoryUsage(false);
            }
        }

        try
        {
            if (s != null)
            {
                if (origin == null)
                    origin = s.getName();

                nameForReporting = s.getNameForReporting();
            }

            if (mimeType == null)
            {
                if (args.containsKey(Transcoder.SOURCE))
                {
                    String source = (String) args.get( Transcoder.SOURCE );

                    // this is wrong for network URLs, but it solves a chicken and egg problem with
                    // moving the source processing down to the child transcoders
                    mimeType = MimeMappings.getMimeType( source );

                    if (mimeType == null)
                    {
                        logTranscoderException(new TranscoderException.UnrecognizedExtension(request), nameForReporting, line, col);
                        return null;
                    }
                }
                else if (args.containsKey(Transcoder.SKINCLASS))
                {
                    mimeType = MimeMappings.SKIN;
                }
            }

            Transcoder t = getTranscoder(transcoders, mimeType);

            if (t == null)
            {
                logTranscoderException(new TranscoderException.NoMatchingTranscoder(mimeType), nameForReporting, line, col);
            }
            else
            {
                if (!args.containsKey( Transcoder.SYMBOL ) &&
                    !args.containsKey( Transcoder.NEWNAME )) // FIXME - this should probably go away, no exports in fp9
                {
                    args.put( Transcoder.NEWNAME, className );
                }

                // put the transcoding output into the compilation unit
                results = t.transcode( context, symbolTable, args, className, generateCode );
            }
        }
        catch(TranscoderException transcoderException)
        {
            logTranscoderException(transcoderException, origin, line, col);
        }

        if (Trace.embed)
        {
	        if (ThreadLocalToolkit.getBenchmark() != null)
	        {
		        ThreadLocalToolkit.getBenchmark().stopTime("Transcoded " + request);
	        }

            if (mem != -1 && ThreadLocalToolkit.getBenchmark() != null)
            {
                long endMem = ThreadLocalToolkit.getBenchmark().peakMemoryUsage(false);
                Trace.trace("Increase in peak memory from transcoding: " + (endMem - mem) + " MB");
            }
        }

        return results;
    }

    public static Transcoder getTranscoder(Transcoder[] transcoders, String mimeType)
    {
        assert transcoders != null;
        for (int i = 0; i < transcoders.length; ++i)
        {
            if (transcoders[i].isSupported(mimeType))
            {
                return transcoders[i];
            }
        }

        return null;
    }

    public static String formatTranscodeRequest( Map args )
    {
        String s = (String) args.get( Transcoder.SOURCE );

        if (s != null)
            return s;

        s = "[";
        for (Iterator it = args.entrySet().iterator(); it.hasNext();)
        {
            Map.Entry e = (Map.Entry) it.next();
            s += (e.getKey() + "='" + e.getValue() + "'");
            if (it.hasNext()) s += ", ";
        }
        s += "]";
        return s;
    }

    public static void logTranscoderException(TranscoderException transcoderException, String path, int line, int column)
    {
        transcoderException.path = path;
        transcoderException.line = line;
        transcoderException.column = column;
        ThreadLocalToolkit.log(transcoderException);
    }
}
