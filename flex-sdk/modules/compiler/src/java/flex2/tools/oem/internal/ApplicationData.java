////////////////////////////////////////////////////////////////////////////////
//
//  ADOBE SYSTEMS INCORPORATED
//  Copyright 2006-2007 Adobe Systems Incorporated
//  All Rights Reserved.
//
//  NOTICE: Adobe permits you to use, modify, and distribute this file
//  in accordance with the terms of the license agreement accompanying it.
//
////////////////////////////////////////////////////////////////////////////////

package flex2.tools.oem.internal;

import java.util.List;
import java.util.Map;
import java.util.Set;

import macromedia.asc.util.ContextStatics;

import flash.fonts.FontManager;
import flex2.compiler.FileSpec;
import flex2.compiler.ResourceBundlePath;
import flex2.compiler.ResourceContainer;
import flex2.compiler.SourceList;
import flex2.compiler.SourcePath;
import flex2.compiler.common.Configuration;
import flex2.compiler.swc.SwcCache;
import flex2.linker.ConsoleApplication;
import flex2.linker.SimpleMovie;

/**
 * @version 2.0.1
 * @author Clement Wong
 *
 */
public class ApplicationData
{
	/**
	 * for incremental compilation
	 */
	public int checksum;
	
	/**
	 * command-line checksum
	 */
	public int cmdChecksum;
	
	/**
	 * command-line linker-based checksum
	 */
	public int linkChecksum;
	
	/**
	 * SWC context checksum
	 */
	public int swcChecksum;
	
	/**
	 * some containers for CompilationUnits
	 */
	public FileSpec fileSpec;
	public SourceList sourceList;
	public SourcePath sourcePath;
	public ResourceContainer resources;
	public ResourceBundlePath bundlePath;
	
	/**
	 * sources and compilation units
	 */
	public List sources;
	public List units;
	
	public String cacheName;
	// public String outputName;
	
	public Configuration configuration;
	public ContextStatics perCompileData;
	public SwcCache swcCache;
	
	public SimpleMovie movie;
	public ConsoleApplication app;
    public FontManager fontManager;

	/**
	 * SWC context includes and excludes
	 */
	public Set includes, excludes;
	
	/**
	 * Map definition to signature checksum.
	 */
	public Map swcDefSignatureChecksums; // Map<QName, Long>
	
	/**
	 * Map swc file to checksum (modification timestamp).
	 */
	public Map swcFileChecksums; // Map<String, Long>
}
