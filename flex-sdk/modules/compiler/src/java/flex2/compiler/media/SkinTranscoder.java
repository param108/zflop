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

package flex2.compiler.media;

import flash.swf.tags.DefineTag;
import flash.util.Trace;
import flex2.compiler.Source;
import flex2.compiler.SymbolTable;
import flex2.compiler.Transcoder;
import flex2.compiler.TranscoderException;
import flex2.compiler.as3.binding.ClassInfo;
import flex2.compiler.as3.binding.TypeAnalyzer;
import flex2.compiler.common.PathResolver;
import flex2.compiler.io.VirtualFile;
import flex2.compiler.mxml.lang.StandardDefs;
import flex2.compiler.util.MimeMappings;
import flex2.compiler.util.NameFormatter;
import flex2.compiler.util.MultiName;
import flex2.compiler.util.QName;
import flex2.compiler.util.VelocityManager;
import java.io.StringWriter;
import java.util.Map;
import macromedia.asc.parser.Node;
import org.apache.velocity.Template;
import org.apache.velocity.VelocityContext;

/**
 * @author Paul Reilly
 */
public class SkinTranscoder extends AbstractTranscoder
{
    private static final String CODEGEN_TEMPLATE_PATH = "flex2/compiler/media/";

    public SkinTranscoder()
    {
        super(new String[] {MimeMappings.SKIN}, null, false);
    }

    public TranscodingResults doTranscode(PathResolver context, SymbolTable symbolTable,
                                          Map args, String className, boolean generateSource)
        throws TranscoderException
    {
        TranscodingResults result = null;

        if (generateSource)
        {
            String skinClassName = (String) args.get(Transcoder.SKINCLASS);
            Source skinSource = symbolTable.findSourceByQName(new QName(NameFormatter.toColon(skinClassName)));

            if (skinSource == null)
            {
                throw new SkinClassNotFound(skinClassName);
            }

            result = new TranscodingResults();
            TypeAnalyzer typeAnalyzer = symbolTable.getTypeAnalyzer();
            ClassInfo skinClassInfo = typeAnalyzer.analyzeClass(null, new MultiName(NameFormatter.toColon(skinClassName)));
            result.generatedCode = generateSource(className, skinClassName, skinClassInfo);
        }
        else
        {
            throw new EmbedRequiresCodegen((String) args.get(Transcoder.ORIGINAL), className);
        }

        return result;
    }

    private String generateSource(String fullClassName, String baseClassName, ClassInfo classInfo)
        throws TranscoderException
    {
        boolean needsIBorder = !classInfo.implementsInterface(StandardDefs.PACKAGE_MX_CORE, "IBorder");
        boolean needsBorderMetrics = !classInfo.definesGetter("borderMetrics", true);
        boolean needsIFlexDisplayObject = !classInfo.implementsInterface(StandardDefs.PACKAGE_MX_CORE,
                                                                         "IFlexDisplayObject");
        boolean needsMeasuredHeight = !classInfo.definesGetter("measuredHeight", true);
        boolean needsMeasuredWidth = !classInfo.definesGetter("measuredWidth", true);
        boolean needsMove = !classInfo.definesFunction("move", true);
        boolean needsSetActualSize = !classInfo.definesFunction("setActualSize", true);
        boolean flexMovieClipOrSprite = (classInfo.extendsClass(NameFormatter.toColon(StandardDefs.PACKAGE_MX_CORE,
                                                                                      "FlexMovieClip")) ||
                                         classInfo.extendsClass(NameFormatter.toColon(StandardDefs.PACKAGE_MX_CORE,
                                                                                      "FlexSprite")));

        return generateSource(fullClassName, baseClassName, needsIBorder, needsBorderMetrics,
                              needsIFlexDisplayObject, needsMeasuredHeight, needsMeasuredWidth,
                              needsMove, needsSetActualSize, flexMovieClipOrSprite);
    }

    public static String generateSource(String fullClassName, String baseClassName,
                                        boolean needsIBorder, boolean needsBorderMetrics,
                                        boolean needsIFlexDisplayObject, boolean needsMeasuredHeight,
                                        boolean needsMeasuredWidth, boolean needsMove,
                                        boolean needsSetActualSize, boolean flexMovieClipOrSprite)
        throws TranscoderException
    {
        String result = null;

        try
        {
            String templateName = "SkinClass.vm";
            Template template = VelocityManager.getTemplate(CODEGEN_TEMPLATE_PATH + templateName);

            if (template == null)
            {
                throw new TemplateException( templateName );
            }

            int dot = fullClassName.lastIndexOf( '.' );
            String packageName;
            String className;

            if (dot != -1)
            {
                packageName = fullClassName.substring(0, dot);
                className = fullClassName.substring(dot + 1);
            }
            else
            {
                packageName = "";
                className = fullClassName;
            }

            VelocityContext velocityContext = VelocityManager.getCodeGenContext();
            velocityContext.put("packageName", packageName);
            velocityContext.put("baseClassName", baseClassName);
            velocityContext.put("className", className);
            velocityContext.put("needsIBorder", new Boolean(needsIBorder));
            velocityContext.put("needsBorderMetrics", new Boolean(needsBorderMetrics));
            velocityContext.put("needsIFlexDisplayObject", new Boolean(needsIFlexDisplayObject));
            velocityContext.put("needsMeasuredHeight", new Boolean(needsMeasuredHeight));
            velocityContext.put("needsMeasuredWidth", new Boolean(needsMeasuredWidth));
            velocityContext.put("needsMove", new Boolean(needsMove));
            velocityContext.put("needsSetActualSize", new Boolean(needsSetActualSize));
            velocityContext.put("needsName", new Boolean(!flexMovieClipOrSprite));
            velocityContext.put("needsToString", new Boolean(!flexMovieClipOrSprite));

            StringWriter stringWriter = new StringWriter();
            template.merge(velocityContext, stringWriter);
            result = stringWriter.toString();
        }
        catch (Exception e)
        {
            if (Trace.error)
            {
                e.printStackTrace();
            }
            throw new UnableToGenerateSource(fullClassName);
        }

        return result;
    }

    public boolean isSupportedAttribute(String attribute)
    {
        boolean result = true;

        if (!attribute.equals(Transcoder.SKINCLASS))
        {
            result = false;
        }

        return result;
    }

    // Override super.clear(), because we don't use transcodingCache.
    public void clear()
    {
    }

    public static class SkinClassNotFound extends TranscoderException
    {
        public String className;

        public SkinClassNotFound(String className)
        {
            this.className = className;
        }
    }
}
