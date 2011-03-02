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

package flex2.compiler.as3.binding;

import flex2.compiler.CompilationUnit;
import flex2.compiler.Context;
import flex2.compiler.Source;
import flex2.compiler.as3.Extension;
import flex2.compiler.as3.reflect.TypeTable;
import flex2.compiler.common.PathResolver;
import flex2.compiler.io.FileUtil;
import flex2.compiler.io.TextFile;
import flex2.compiler.io.VirtualFile;
import flex2.compiler.mxml.SourceCodeBuffer;
import flex2.compiler.mxml.gen.VelocityUtil;
import flex2.compiler.util.*;
import macromedia.asc.parser.Node;
import org.apache.velocity.Template;
import org.apache.velocity.VelocityContext;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * @author Paul Reilly
 */
public final class DataBindingExtension implements Extension
{
    private static final String TEMPLATE_PATH = "flex2/compiler/as3/binding/";
    private static final String WATCHER_SETUP_UTIL_TEMPLATE = TEMPLATE_PATH + "WatcherSetupUtil.vm";
    private static final String DATA_BINDING_INFO_KEY = "dataBindingInfo";

    private String generatedOutputDirectory;
    private boolean showBindingWarnings;

    public DataBindingExtension(String generatedOutputDirectory, boolean showBindingWarnings)
    {
        this.generatedOutputDirectory = generatedOutputDirectory;
        this.showBindingWarnings = showBindingWarnings;
    }

    public void parse1(CompilationUnit compilationUnit, TypeTable typeTable)
    {
    }

    public void parse2(CompilationUnit compilationUnit, TypeTable typeTable)
    {
    }

    public void analyze1(CompilationUnit compilationUnit, TypeTable typeTable)
    {
    }

    public void analyze2(CompilationUnit compilationUnit, TypeTable typeTable)
    {
    }

    public void analyze3(CompilationUnit compilationUnit, TypeTable typeTable)
    {
    }

    public void analyze4(CompilationUnit compilationUnit, TypeTable typeTable)
    {
    }

    public void generate(CompilationUnit compilationUnit, TypeTable typeTable)
    {
        Context context = compilationUnit.getContext();
        macromedia.asc.util.Context cx = (macromedia.asc.util.Context) context.getAttribute("cx");
        Node node = (Node) compilationUnit.getSyntaxTree();
        DataBindingFirstPassEvaluator dataBindingFirstPassEvaluator =
            new DataBindingFirstPassEvaluator(compilationUnit, typeTable, showBindingWarnings);

        node.evaluate(cx, dataBindingFirstPassEvaluator);

        List dataBindingInfoList = dataBindingFirstPassEvaluator.getDataBindingInfoList();

        if (dataBindingInfoList.size() > 0)
        {
	        // watcher setup classes should match the originating source timestamp.
            Map generatedSources = generateWatcherSetupUtilClasses(compilationUnit, dataBindingInfoList);
            compilationUnit.addGeneratedSources(generatedSources);
        }
    }

    /**
     *
     */
    private Source createSource(String fileName, String shortName, long lastModified,
    							PathResolver resolver, SourceCodeBuffer sourceCodeBuffer)
    {
        Source result = null;

        if (sourceCodeBuffer.getBuffer() != null)
        {
            String sourceCode = sourceCodeBuffer.toString();

            if (generatedOutputDirectory != null)
            {
                try
                {
                    FileUtil.writeFile(generatedOutputDirectory + File.separatorChar + fileName, sourceCode);
                }
                catch (IOException e)
                {
                    ThreadLocalToolkit.log(new VelocityException.UnableToWriteGeneratedFile(fileName, e.getMessage()));
                }
            }

			// NOTE: watcher setup is dependent on bindable metadata changes in targets.
			// So we absolutely can *not* use the original MXML source's timestamp here. (176292)
			VirtualFile generatedFile = new TextFile(sourceCode, fileName, null, MimeMappings.AS, System.currentTimeMillis());

            result = new Source(generatedFile,
                                "",
                                shortName,
                                null,
                                false,
                                false,
                                false);
            result.setPathResolver(resolver);
        }

        return result;
    }

    /**
     *
     */
    private Source generateWatcherSetupUtil(CompilationUnit compilationUnit, DataBindingInfo dataBindingInfo)
    {
        Template template;

        try
        {
            template = VelocityManager.getTemplate(WATCHER_SETUP_UTIL_TEMPLATE);
        }
        catch (Exception exception)
        {
            ThreadLocalToolkit.log(new VelocityException.TemplateNotFound(WATCHER_SETUP_UTIL_TEMPLATE));
            return null;
        }

        String className = dataBindingInfo.getWatcherSetupUtilClassName();
        String shortName = className.substring(className.lastIndexOf('.') + 1);

        String generatedName = className.replace( '.', File.separatorChar ) + ".as";

        SourceCodeBuffer out = new SourceCodeBuffer();

        try
        {
            VelocityUtil util = new VelocityUtil(TEMPLATE_PATH, false, out, null);
            VelocityContext velocityContext = VelocityManager.getCodeGenContext(util);
            velocityContext.put(DATA_BINDING_INFO_KEY, dataBindingInfo);
            template.merge(velocityContext, out);
        }
        catch (Exception e)
        {
            ThreadLocalToolkit.log(new VelocityException.GenerateException(compilationUnit.getSource().getRelativePath(),
                                                                           e.getLocalizedMessage()));
            return null;
        }

        return createSource(generatedName, shortName, compilationUnit.getSource().getLastModified(),
        					compilationUnit.getSource().getPathResolver(), out);
    }

    public Map generateWatcherSetupUtilClasses(CompilationUnit compilationUnit, List dataBindingInfoList)
    {
        Map extraSources = new HashMap();

        Iterator iterator = dataBindingInfoList.iterator();

        while ( iterator.hasNext() )
        {
            DataBindingInfo dataBindingInfo = (DataBindingInfo) iterator.next();
	        QName classQName = new QName(dataBindingInfo.getWatcherSetupUtilClassName());
            extraSources.put( classQName, generateWatcherSetupUtil(compilationUnit, dataBindingInfo) );
        }

        return extraSources;
    }
}
