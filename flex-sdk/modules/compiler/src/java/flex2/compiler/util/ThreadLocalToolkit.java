////////////////////////////////////////////////////////////////////////////////
//
//  ADOBE SYSTEMS INCORPORATED
//  Copyright 2004-2006 Adobe Systems Incorporated
//  All Rights Reserved.
//
//  NOTICE: Adobe permits you to use, modify, and distribute this file
//  in accordance with the terms of the license agreement accompanying it.
//
////////////////////////////////////////////////////////////////////////////////

package flex2.compiler.util;

import flash.localization.LocalizationManager;
import flex2.compiler.ILocalizableMessage;
import flex2.compiler.Logger;
import flex2.compiler.Source;
import flex2.compiler.common.PathResolver;
import flex2.compiler.io.VirtualFile;
import flex2.tools.oem.ProgressMeter;

import java.util.HashMap;
import java.util.Map;

/**
 * @author Clement Wong
 */
public final class ThreadLocalToolkit
{
    private static ThreadLocal logger = new ThreadLocal();
    private static ThreadLocal resolver = new ThreadLocal();
    private static ThreadLocal resolved = new ThreadLocal();
    private static ThreadLocal stopWatch = new ThreadLocal();
    private static ThreadLocal localization = new ThreadLocal();
    private static ThreadLocal mimeMappings = new ThreadLocal();
    private static ThreadLocal progressMeter = new ThreadLocal();
    private static ThreadLocal compilerControl = new ThreadLocal();

    public static void setLogger(Logger logger)
    {
        ThreadLocalToolkit.logger.set(logger);
        if (logger != null)
        {
            logger.setLocalizationManager( getLocalizationManager() );
        }
    }

    public static Logger getLogger()
    {
        return (Logger) logger.get();
    }

    public static LocalizationManager getLocalizationManager()
    {
        return (LocalizationManager) localization.get();
    }

    public static void setLocalizationManager(LocalizationManager mgr)
    {
        localization.set( mgr );
    }

    public static int errorCount()
    {
        Logger l = (Logger) logger.get();
        if (l != null)
        {
            return l.errorCount();
        }
        else
        {
            return 0;
        }
    }

    public static int warningCount()
    {
        Logger l = (Logger) logger.get();
        if (l != null)
        {
            return l.warningCount();
        }
        else
        {
            return 0;
        }
    }

    public static void logInfo(String info)
    {
        Logger l = (Logger) logger.get();
        if (l != null)
        {
            l.logInfo(info);
        }
        else
        {
            System.out.println(info);
        }
    }

    public static void logDebug(String debug)
    {
        Logger l = (Logger) logger.get();
        if (l != null)
        {
            l.logDebug(debug);
        }
        else
        {
            System.err.println(debug);
        }
    }

    public static void logWarning(String warning)
    {
        Logger l = (Logger) logger.get();
        if (l != null)
        {
            l.logWarning(warning);
        }
        else
        {
            System.err.println(warning);
        }
    }

    public static void logError(String error)
    {
        Logger l = (Logger) logger.get();
        if (l != null)
        {
            l.logError(error);
        }
        else
        {
            System.err.println(error);
        }
    }

    public static void logInfo(String path, String info)
    {
        Logger l = (Logger) logger.get();
        if (l != null)
        {
            l.logInfo(path, info);
        }
        else
        {
            System.out.println(path + ":" + info);
        }
    }

    public static void logDebug(String path, String debug)
    {
        Logger l = (Logger) logger.get();
        if (l != null)
        {
            l.logDebug(path, debug);
        }
        else
        {
            System.err.println(path + ":" + debug);
        }
    }

    public static void logWarning(String path, String warning)
    {
        Logger l = (Logger) logger.get();
        if (l != null)
        {
            l.logWarning(path, warning);
        }
        else
        {
            System.err.println(path + ":" + warning);
        }
    }

	public static void logWarning(String path, String warning, int errorCode)
	{
	    Logger l = (Logger) logger.get();
	    if (l != null)
	    {
	        l.logWarning(path, warning, errorCode);
	    }
	    else
	    {
	        System.err.println(path + ":" + warning);
	    }
	}

    public static void logError(String path, String error)
    {
        Logger l = (Logger) logger.get();
        if (l != null)
        {
            l.logError(path, error);
        }
        else
        {
            System.err.println(path + ":" + error);
        }
    }

	public static void logError(String path, String error, int errorCode)
	{
	    Logger l = (Logger) logger.get();
	    if (l != null)
	    {
	        l.logError(path, error, errorCode);
	    }
	    else
	    {
	        System.err.println(path + ":" + error);
	    }
	}

    public static void logInfo(String path, int line, String info)
    {
        Logger l = (Logger) logger.get();
        if (l != null)
        {
            l.logInfo(path, line, info);
        }
        else
        {
            System.out.println(path + ": line " + line + " - " + info);
        }
    }

    public static void logDebug(String path, int line, String debug)
    {
        Logger l = (Logger) logger.get();
        if (l != null)
        {
            l.logDebug(path, line, debug);
        }
        else
        {
            System.err.println(path + ": line " + line + " - " + debug);
        }
    }

    public static void logWarning(String path, int line, String warning)
    {
        Logger l = (Logger) logger.get();
        if (l != null)
        {
            l.logWarning(path, line, warning);
        }
        else
        {
            System.err.println(path + ": line " + line + " - " + warning);
        }
    }

    public static void logError(String path, int line, String error)
    {
        Logger l = (Logger) logger.get();
        if (l != null)
        {
            l.logError(path, line, error);
        }
        else
        {
            System.err.println(path + ": line " + line + " - " + error);
        }
    }

    public static void logInfo(String path, int line, int col, String info)
    {
        Logger l = (Logger) logger.get();
        if (l != null)
        {
            l.logInfo(path, line, col, info);
        }
        else
        {
            System.out.println(path + ": line " + line + ", col " + col + " - " + info);
        }
    }

    public static void logDebug(String path, int line, int col, String debug)
    {
        Logger l = (Logger) logger.get();
        if (l != null)
        {
            l.logDebug(path, line, col, debug);
        }
        else
        {
            System.err.println(path + ": line " + line + ", col " + col + " - " + debug);
        }
    }

    public static void logWarning(String path, int line, int col, String warning)
    {
        Logger l = (Logger) logger.get();
        if (l != null)
        {
            l.logWarning(path, line, col, warning);
        }
        else
        {
            System.err.println(path + ": line " + line + ", col " + col + " - " + warning);
        }
    }

    public static void logError(String path, int line, int col, String error)
    {
        Logger l = (Logger) logger.get();
        if (l != null)
        {
            l.logError(path, line, col, error);
        }
        else
        {
            System.err.println(path + ": line " + line + ", col " + col + " - " + error);
        }
    }

    public static void logWarning(String path, int line, int col, String warning, String source)
    {
        Logger l = (Logger) logger.get();
        if (l != null)
        {
            l.logWarning(path, line, col, warning, source);
        }
        else
        {
            System.err.println(path + ": line " + line + ", col " + col + " - " + warning);
            System.err.println(source);
        }
    }

	public static void logWarning(String path, int line, int col, String warning, String source, int errorCode)
	{
	    Logger l = (Logger) logger.get();
	    if (l != null)
	    {
	        l.logWarning(path, line, col, warning, source, errorCode);
	    }
	    else
	    {
	        System.err.println(path + ": line " + line + ", col " + col + " - " + warning);
	        System.err.println(source);
	    }
	}

    public static void logError(String path, int line, int col, String error, String source)
    {
        Logger l = (Logger) logger.get();
        if (l != null)
        {
            l.logError(path, line, col, error, source);
        }
        else
        {
            System.err.println(path + ": line " + line + ", col " + col + " - " + error);
            System.err.println(source);
        }
    }

	public static void logError(String path, int line, int col, String error, String source, int errorCode)
	{
	    Logger l = (Logger) logger.get();
	    if (l != null)
	    {
	        l.logError(path, line, col, error, source, errorCode);
	    }
	    else
	    {
	        System.err.println(path + ": line " + line + ", col " + col + " - " + error);
	        System.err.println(source);
	    }
	}

	/**
	 * avoid passthrough ctors in CompilerMessages
	 */
	public static void log(CompilerMessage m, String path, int line, int column)
	{
		m.path = path;
		m.line = line;
		m.column = column;
		log(m);
	}

	public static void log(CompilerMessage m, String path, int line, int column, String source)
	{
		m.path = path;
		m.line = line;
		m.column = column;
		log((ILocalizableMessage) m, source);
	}

	/**
	 *
	 */
	public static void log(CompilerMessage m, String path, int line)
	{
		log(m, path, line, -1);
	}

	public static void log(CompilerMessage m, String path)
	{
		log(m, path, -1, -1);
	}

	public static void log(CompilerMessage m, Source s, int line)
	{
		m.path = s.getNameForReporting();
		m.line = line;
		log(m);
	}

	public static void log(CompilerMessage m, Source s)
	{
		m.path = s.getNameForReporting();
		log(m);
	}

    public static void log( ILocalizableMessage m )
    {
        getLogger().log( m );
    }

	public static void log( ILocalizableMessage m, String source)
	{
	    getLogger().log( m, source );
	}

    // PathResolver methods...
    public static void setPathResolver(PathResolver r)
    {
        resolver.set(r);
    }

    public static void resetResolvedPaths()
    {
    	resolved.set(null);
    }

    public static PathResolver getPathResolver()
    {
        return (PathResolver) resolver.get();
    }

    public static void addResolvedPath(String path, VirtualFile virtualFile)
    {
        Map resolvedMap = (Map) resolved.get();
        if (resolvedMap == null)
        {
            resolvedMap = new HashMap();
            resolved.set(resolvedMap);
        }

        resolvedMap.put(path, virtualFile);
    }

    public static VirtualFile getResolvedPath(String path)
    {
        Map resolvedMap = (Map) resolved.get();
        assert resolvedMap != null;
        return (VirtualFile) resolvedMap.get(path);
    }

    // Benchmarking methods...

    public static void setBenchmark(Benchmark b)
    {
        stopWatch.set(b);
    }

	public static Benchmark getBenchmark()
	{
		return (Benchmark) stopWatch.get();
	}

    public static void resetBenchmark()
    {
        Benchmark b = (Benchmark) stopWatch.get();
        if (b != null)
        {
            b.start();
        }
    }

    // Mime mappings...
    
    public static void setMimeMappings(MimeMappings mappings)
    {
        mimeMappings.set(mappings);
    }
    
    static MimeMappings getMimeMappings()
    {
    	return (MimeMappings) mimeMappings.get();
    }
    
    // Progress Meter...
    
    public static void setProgressMeter(ProgressMeter meter)
    {
        progressMeter.set(meter);
    }
    
    public static ProgressMeter getProgressMeter()
    {
    	return (ProgressMeter) progressMeter.get();
    }

    // Compiler Control...
    
    public static void setCompilerControl(CompilerControl cc)
    {
    	compilerControl.set(cc);
    }
    
    public static CompilerControl getCompilerControl()
    {
    	return (CompilerControl) compilerControl.get();
    }
}
