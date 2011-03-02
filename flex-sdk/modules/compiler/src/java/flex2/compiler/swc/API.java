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

package flex2.compiler.swc;

import flex2.compiler.CompilerException;
import flex2.compiler.Source;
import flex2.compiler.SourcePath;
import flex2.compiler.common.ConfigurationException;
import flex2.compiler.CompilationUnit;
import flex2.compiler.io.FileUtil;
import flex2.compiler.io.InMemoryFile;
import flex2.compiler.io.VirtualFile;
import flex2.compiler.util.NameFormatter;
import flex2.compiler.util.NameMappings;
import flex2.compiler.util.ThreadLocalToolkit;
import flex2.linker.LinkerException;
import flex2.tools.CompcConfiguration;
import flash.util.Trace;

import java.io.IOException;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.TreeMap;

/**
 * API for SWC creation.  See flex2.tools.Compc for use
 *
 * @author Brian Deitte
 */
public class API
{
    public static List setupNamespaceComponents(CompcConfiguration configuration, NameMappings mappings, SourcePath sourcePath, Map classes)
            throws flex2.compiler.config.ConfigurationException, CompilerException
    {
        return setupNamespaceComponents(configuration.getNamespaces(), mappings, sourcePath, classes, configuration.getIncludeLookupOnly());
    }

    public static List setupNamespaceComponents(List targets, NameMappings mappings, SourcePath sourcePath, Map classes)
        throws flex2.compiler.config.ConfigurationException, CompilerException
    {
        return setupNamespaceComponents(targets, mappings, sourcePath, classes, false);
    }

    public static List setupNamespaceComponents(List targets, NameMappings mappings, SourcePath sourcePath, Map classes, boolean includeLookupOnly)
            throws flex2.compiler.config.ConfigurationException, CompilerException
    {
        List nsComponents = new LinkedList();

        try
        {
            for (Iterator iterator = targets.iterator(); iterator.hasNext();)
            {
                String nsTarget = (String)iterator.next();
                if (nsTarget != null)
                {
                    Map map = mappings.getNamespace(nsTarget);
                    if (map == null)
                    {
                        // fixme - pass enough info down to actually format this exception reasonably?
                        throw new ConfigurationException.UnknownNamespace( nsTarget, null, null, -1 );
                    }
                    for (Iterator iter2 = map.entrySet().iterator(); iter2.hasNext();)
                    {
                        Map.Entry entry = (Map.Entry)iter2.next();
                        String compName = (String) entry.getKey();
                        String className = (String) entry.getValue();
                        String packageName = NameFormatter.retrievePackageName(className);
                        String leafName = NameFormatter.retrieveClassName(className);
                        if (! mappings.isLookupOnly(className))
                        {

                            Source s = sourcePath.findSource(packageName, leafName);
                            if (s == null)
                            {
                                SwcException e = new SwcException.NoSourceForClass( className, nsTarget );
                                ThreadLocalToolkit.log(e);
                                throw e;
                            }
                            classes.put(s.getName(), s);

                            Component component = new Component(className, compName, nsTarget);
                            nsComponents.add(component);
                        }
                        else if (includeLookupOnly)
                        {
                            nsComponents.add(new Component(className, compName, nsTarget));
                        }
                    }
                }
            }
        }
        catch(CompilerException ce)
        {
            ThreadLocalToolkit.logError(ce.getMessage());
            throw ce;
        }
        return nsComponents;
    }

    public static void setupClasses(CompcConfiguration configuration, SourcePath sourcePath, Map classes)
        throws CompilerException
    {
        setupClasses(configuration.getClasses(), sourcePath, classes);
    }

    public static void setupClasses(List list, SourcePath sourcePath, Map classes)
        throws CompilerException
    {
        if (list != null)
        {
            try
            {
                for (Iterator iterator = list.iterator(); iterator.hasNext();)
                {
                    String className = (String) iterator.next();
                    String tempName = className.replace('/', '.').replace('\\', '.');
                    String packageName = NameFormatter.retrievePackageName(tempName);
                    String leafName = NameFormatter.retrieveClassName(tempName);
                    Source s = sourcePath.findSource(packageName, leafName);
                    if (s == null)
                    {
                        SwcException msg;
                        if (className.endsWith(".as") || className.endsWith(".mxml"))
                        {
                            msg = new SwcException.CouldNotFindFileSource(className);
                        }
                        else
                        {
                            msg = new SwcException.CouldNotFindSource(className);
                        }
                        ThreadLocalToolkit.log(msg);
                        throw msg;
                    }
                    classes.put(s.getName(), s);
                }
            }
            catch(CompilerException ce)
            {
                ThreadLocalToolkit.logError(ce.getMessage());
                throw ce;
            }
        }
    }

    public static SwcMovie link(flex2.linker.Configuration configuration, List units) throws LinkerException
    {
        // instantiate an empty movie
        SwcMovie movie = new SwcMovie(configuration);

        // give all the compilation units to the movie object - it will setup dependencies and use a linker
        // to generate movie export order.
        // todo - break dep on CompilationUnit, take ABCs?
        movie.generate( units );
        return movie;
    }

    public static void exportSwc(CompcConfiguration configuration, List units,
                                 List nsComponents, SwcCache cache, Map rbFiles)
            throws Exception
    {
    	Map m = new TreeMap();
    	if (configuration.getCSSArchiveFiles() != null) m.putAll(configuration.getCSSArchiveFiles());
    	if (configuration.getL10NArchiveFiles() != null) m.putAll(configuration.getL10NArchiveFiles());
    	if (configuration.getFiles() != null) m.putAll(configuration.getFiles());
    	
        exportSwc(configuration.getOutput(), configuration.isDirectory(), m,
        		  configuration.getStylesheets(), configuration,
                  units, nsComponents, cache, rbFiles);
    }

    public static void exportSwc(String swcStr, boolean isDirectory, Map files, Map stylesheets,
    							 flex2.linker.Configuration configuration,
                                 List units, List nsComponents, SwcCache cache, Map rbFiles)
            throws Exception
    {
        // CompC just blindly overwrites existing SWCs.
        SwcArchive archive = isDirectory?
                             (SwcArchive) new SwcDirectoryArchive( swcStr ) : new SwcLazyReadArchive( swcStr );
        exportSwc(archive, files, stylesheets, configuration, units, nsComponents, cache, rbFiles);
    }

    private static void exportSwc(SwcArchive archive, Map files, Map stylesheets, flex2.linker.Configuration configuration,
                                 List units, List nsComponents, SwcCache cache, Map rbFiles)
            throws Exception
    {
        SwcMovie m = link(configuration, units);
        exportSwc(archive, files, stylesheets, configuration, m, nsComponents, cache, rbFiles);
    }

    public static void exportSwc(SwcArchive archive, Map files, Map stylesheets, flex2.linker.Configuration configuration,
                                 SwcMovie m, List nsComponents, SwcCache cache, Map rbFiles)
            throws Exception
    {
        try
        {
            Swc swc = new Swc( archive );

            if (configuration.generateLinkReport() && configuration.getLinkReportFileName() != null)
            {
                FileUtil.writeFile(configuration.getLinkReportFileName(), m.getLinkReport());
            }
            if (configuration.generateRBList() && configuration.getRBListFileName() != null)
            {
                FileUtil.writeFile(configuration.getRBListFileName(), m.getRBList());
            }

            if (ThreadLocalToolkit.errorCount() > 0)
            {
                return;
            }

            // Step 1: get map of all components known to swcs referenced from exported units
            Map allClassComp = new HashMap();
            //Map refClassComp = new HashMap();

            for (Iterator e = m.getExportedUnits().iterator(); e.hasNext();)
            {
                CompilationUnit unit = (CompilationUnit) e.next();
                if (unit.getSource().isSwcScriptOwner())
                {
                    Swc unitswc = ((SwcScript) unit.getSource().getOwner()).getLibrary().getSwc();
                    for (Iterator ci = unitswc.getComponentIterator(); ci.hasNext();)
                    {
                        Component c = (Component) ci.next();
                        allClassComp.put( c.getClassName(), c );
                    }
                }
            }

            for (Iterator nsc = nsComponents.iterator(); nsc.hasNext();)
            {
                Component c = (Component) nsc.next();
                allClassComp.put(c.getClassName(), c);
            }

            // Now pare this down to just referenced classes (and thus components)

            for (Iterator e = m.getExportedUnits().iterator(); e.hasNext();)
            {
                CompilationUnit unit = (CompilationUnit) e.next();
                for (int i = 0, s = unit.topLevelDefinitions.size();i < s; i++)
                {
                    String def = unit.topLevelDefinitions.get(i).toString();
                    if (allClassComp.containsKey( def ))
                    {
                        swc.addComponent( (Component)allClassComp.get( def ) );
                    }
                }
            }

            // fixme - for now, we'll have a single canned library name
            // eventually support building multi-library swcs
            swc.buildLibrary( "library", configuration, m );

            addArchiveFiles(files, swc);
            addArchiveFiles(rbFiles, swc);
            addArchiveFiles(stylesheets, swc);

            cache.export(swc);

            if (ThreadLocalToolkit.errorCount() > 0)
            {
                return;
            }
        }
        catch (Exception e)
        {
            if (e instanceof CompilerException || e instanceof LinkerException ||
                e instanceof SwcException.SwcNotExported)
            {
                throw e;
            }

            if (Trace.error)
            {
                e.printStackTrace();
            }
            SwcException ex = (e instanceof SwcException) ? (SwcException) e : new SwcException.SwcNotExported(archive.getLocation(), e);
            ThreadLocalToolkit.log(ex);
            throw ex;
        }

        if (ThreadLocalToolkit.getBenchmark() != null)
        {
            ThreadLocalToolkit.getBenchmark().benchmark("Exporting " + archive.getLocation() + "...");
        }
    }
    
    private static void addArchiveFiles(Map files, Swc swc) throws IOException
    {
        for (Iterator iterator = files.entrySet().iterator(); iterator.hasNext();)
        {
            Map.Entry entry = (Map.Entry)iterator.next();
            String fileName = (String)entry.getKey();
            VirtualFile f = (VirtualFile)entry.getValue();
            if (swc.getArchive().getFile( fileName ) == null)   // icons were already added, don't overwrite
            {
                VirtualFile swcFile = new InMemoryFile(f.getInputStream(), fileName,
                                                       f.getMimeType(), f.getLastModified());
                swc.addFile(swcFile);
            }
        }
    }
}
