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

package flex2.compiler.mxml.gen;

import flash.util.StringUtils;
import flex2.compiler.mxml.SourceCodeBuffer;
import flex2.compiler.util.DualModeLineNumberMap;
import flex2.compiler.util.VelocityManager;

/**
 * augment generic utils with:
 * - debug flag taken from config
 * - template path
 */
public class VelocityUtil extends VelocityManager.Util
{
	private final String path;
	private final boolean debug;
	private final SourceCodeBuffer sourceCodeBuffer;
	private final DualModeLineNumberMap lineNumberMap;

	public VelocityUtil(String path, boolean debug, SourceCodeBuffer sourceCodeBuffer, DualModeLineNumberMap lineNumberMap)
	{
		this.path = path;
		this.debug = debug;
		this.sourceCodeBuffer = sourceCodeBuffer;
		this.lineNumberMap = lineNumberMap;
	}

	/**
	 * $util.templatePath returns the path of the currently executing template
	 */
	public final String getTemplatePath()
	{
		return path;
	}

	/**
	 * $util.debug returns true if config has debug turned on
	 */
	public final boolean getDebug()
	{
		return debug;
	}

	/**
	 *
	 */
	public final boolean getLineMappingEnabled()
	{
		return lineNumberMap != null && sourceCodeBuffer != null;
	}

    public final void mapLines(int origLine, String text)
    {
        mapLines(origLine, text, false);
    }

    public final void mapCompileErrorLines(int origLine, String text)
    {
        mapLines(origLine, text, true);
    }

    /**
	 * @param compileOnly true iff mapping should *not* be encoded into bytecode
	 */
	public final void mapLines(int origLine, String text, boolean compileOnly)
	{
		if (getLineMappingEnabled() && origLine > 0)
			lineNumberMap.put(origLine, sourceCodeBuffer.getLineNumber(), StringUtils.countLines(text) + 1, compileOnly);
	}
}
