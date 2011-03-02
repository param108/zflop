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

package flex2.tools.flexbuilder;

import java.io.File;
import java.util.Locale;

import flex2.tools.oem.Configuration;

/**
 * BuilderConfiguration implements flex2.tools.oem.Configuration. It’s a wrapper of flex2.tools.oem.Configuration so the code in
 * BuilderConfiguration basically delegates calls to the underlying Configuration object. There is one new method,
 * setConfiguration(String[] args). It takes an array of mxmlc/compc command-line arguments. The processing of the arguments is
 * not in BuilderConfiguration. It’s in BuilderApplication.compile() and BuilderLibrary.compile().
 */
public class BuilderConfiguration implements Configuration
{
	BuilderConfiguration(Configuration c)
	{
		configuration = c;
	}
	
	final Configuration configuration;
	String[] extra;

	public void addActionScriptMetadata(String[] md)
	{
		configuration.addActionScriptMetadata(md);
	}

	public void addConfiguration(File file)
	{
		configuration.addConfiguration(file);
	}

	public void addExternalLibraryPath(File[] paths)
	{
		configuration.addExternalLibraryPath(paths);
	}

	public void addExterns(String[] definitions)
	{
		configuration.addExterns(definitions);
	}

	public void addExterns(File[] files)
	{
		configuration.addExterns(files);
	}

	public void addFontManagers(String[] classNames)
	{
		configuration.addFontManagers(classNames);
	}

	public void addIncludes(String[] definitions)
	{
		configuration.addIncludes(definitions);
	}

	public void addLibraryPath(File[] paths)
	{
		configuration.addLibraryPath(paths);
	}

	public void addRuntimeSharedLibraries(String[] libraries)
	{
		configuration.addRuntimeSharedLibraries(libraries);
	}

	public void addSourcePath(File[] paths)
	{
		configuration.addSourcePath(paths);
	}

	public void addTheme(File[] files)
	{
		configuration.addTheme(files);
	}

	public void allowSourcePathOverlap(boolean b)
	{
		configuration.allowSourcePathOverlap(b);
	}

	public void checkActionScriptWarning(int warningCode, boolean b)
	{
		configuration.checkActionScriptWarning(warningCode, b);
	}

	public void enableAccessibility(boolean b)
	{
		configuration.enableAccessibility(b);
	}

	public void enableDebugging(boolean b, String debugPassword)
	{
		configuration.enableDebugging(b, debugPassword);
	}

	public void enableStrictChecking(boolean b)
	{
		configuration.enableStrictChecking(b);
	}

	public void enableVerboseStacktraces(boolean b)
	{
		configuration.enableVerboseStacktraces(b);
	}
	
	public void enableFlashType(boolean b)
	{
		configuration.enableAdvancedAntiAliasing(b);
	}

	public void enableAdvancedAntiAliasing(boolean b)
	{
		configuration.enableAdvancedAntiAliasing(b);
	}

	public void includeLibraries(File[] libraries)
	{
		configuration.includeLibraries(libraries);
	}

	public void keepAllTypeSelectors(boolean b)
	{
		configuration.keepAllTypeSelectors(b);
	}

	public void keepCompilerGeneratedActionScript(boolean b)
	{
		configuration.keepCompilerGeneratedActionScript(b);
	}

	public void keepLinkReport(boolean b)
	{
		configuration.keepLinkReport(b);
	}

	public void keepConfigurationReport(boolean b)
	{
		configuration.keepConfigurationReport(b);
	}

	public void optimize(boolean b)
	{
		configuration.optimize(b);
	}

	public void setActionScriptMetadata(String[] md)
	{
		configuration.setActionScriptMetadata(md);
	}

	public void setActionScriptFileEncoding(String encoding)
	{
		configuration.setActionScriptFileEncoding(encoding);
	}

	public void setComponentManifest(String namespaceURI, File manifest)
	{
		configuration.setComponentManifest(namespaceURI, manifest);
	}

	public void setConfiguration(File file)
	{
		configuration.setConfiguration(file);
	}
	
	public void setConfiguration(String[] args)
	{
		extra = args;
	}

	public void setContextRoot(String path)
	{
		configuration.setContextRoot(path);
	}

	public void setDefaultBackgroundColor(int color)
	{
		configuration.setDefaultBackgroundColor(color);
	}

	public void setDefaultCSS(File url)
	{
		configuration.setDefaultCSS(url);
	}

	public void setDefaultFrameRate(int rate)
	{
		configuration.setDefaultFrameRate(rate);
	}

	public void setDefaultScriptLimits(int maxRecursionDepth, int maxExecutionTime)
	{
		configuration.setDefaultScriptLimits(maxRecursionDepth, maxExecutionTime);
	}

	public void setDefaultSize(int width, int height)
	{
		configuration.setDefaultSize(width, height);
	}

	public void setExternalLibraryPath(File[] paths)
	{
		configuration.setExternalLibraryPath(paths);
	}

	public void setExterns(String[] definitions)
	{
		configuration.setExterns(definitions);
	}

	public void setExterns(File[] files)
	{
		configuration.setExterns(files);
	}

	public void setFontLanguageRange(String language, String range)
	{
		configuration.setFontLanguageRange(language, range);
	}

	public void setFontManagers(String[] classNames)
	{
		configuration.setFontManagers(classNames);
	}

	public void setFrameLabel(String label, String[] classNames)
	{
		configuration.setFrameLabel(label, classNames);
	}

	public void setIncludes(String[] definitions)
	{
		configuration.setIncludes(definitions);
	}

	public void setLibraryPath(File[] paths)
	{
		configuration.setLibraryPath(paths);
	}

	public void setLicense(String productName, String serialNumber)
	{
		configuration.setLicense(productName, serialNumber);
	}

	public void setLocalFontSnapshot(File file)
	{
		configuration.setLocalFontSnapshot(file);
	}

	public void setLocale(String[] locales)
	{
		configuration.setLocale(locales);
	}
	
	public void setLocale(Locale locale)
	{
		configuration.setLocale(new String[] { locale.toString() });
	}

	public void setMaximumCachedFonts(int size)
	{
		configuration.setMaximumCachedFonts(size);
	}

	public void setMaximumGlyphsPerFace(int size)
	{
		configuration.setMaximumGlyphsPerFace(size);
	}

	/*
	public void setProjector(File file)
	{
		configuration.setProjector(file);
	}
	*/

	public void setRuntimeSharedLibraries(String[] libraries)
	{
		configuration.setRuntimeSharedLibraries(libraries);
	}

	public void setSWFMetaData(int field, Object value)
	{
		configuration.setSWFMetaData(field, value);
	}

	public void setSWFMetaData(String xml)
	{
		configuration.setSWFMetaData(xml);
	}

	public void setServiceConfiguration(File file)
	{
		configuration.setServiceConfiguration(file);
	}

	public void setSourcePath(File[] paths)
	{
		configuration.setSourcePath(paths);
	}

	public void setTheme(File[] files)
	{
		configuration.setTheme(files);
	}

	public void setToken(String name, String value)
	{
		configuration.setToken(name, value);
	}

	public void showActionScriptWarnings(boolean b)
	{
		configuration.showActionScriptWarnings(b);
	}

	public void showBindingWarnings(boolean b)
	{
		configuration.showBindingWarnings(b);
	}

	public void showDeprecationWarnings(boolean b)
	{
		configuration.showDeprecationWarnings(b);
	}

    public void showShadowedDeviceFontWarnings(boolean b)
    {
        configuration.showShadowedDeviceFontWarnings(b);
    }

	public void showUnusedTypeSelectorWarnings(boolean b)
	{
		configuration.showUnusedTypeSelectorWarnings(b);
	}

	public void useActionScript3(boolean b)
	{
		configuration.useActionScript3(b);
	}

	public void useECMAScript(boolean b)
	{
		configuration.useECMAScript(b);
	}

	public void useHeadlessServer(boolean b)
	{
		configuration.useHeadlessServer(b);
	}

	public void useNetwork(boolean b)
	{
		configuration.useNetwork(b);
	}

	public void useResourceBundleMetaData(boolean b)
	{
		configuration.useResourceBundleMetaData(b);
	}
	
	public String toString()
	{
		return configuration.toString();
	}

	public void setTargetPlayer(int major, int minor, int revision)
	{
		configuration.setTargetPlayer(major, minor, revision);		
	}

	public void setCompatibilityVersion(int major, int minor, int revision)
	{
		configuration.setCompatibilityVersion(major, minor, revision);		
	}

	public void enableDigestComputation(boolean compute)
	{
		configuration.enableDigestComputation(compute);
	}

	public void enableDigestVerification(boolean verify)
	{
		configuration.enableDigestVerification(verify);
	}

	public void addRuntimeSharedLibraryPath(String swc, String[] rslUrls, String[] policyFileUrls)
	{
		configuration.addRuntimeSharedLibraryPath(swc, rslUrls, policyFileUrls);
	}

	public void setRuntimeSharedLibraryPath(String swc, String[] rslUrls, String[] policyFileUrls)
	{
		configuration.setRuntimeSharedLibraryPath(swc, rslUrls, policyFileUrls);
	}

    public void addDefineDirective(String name, String value)
    {
        configuration.addDefineDirective(name, value);
    }

    public void setDefineDirective(String[] names, String[] values)
    {
        configuration.setDefineDirective(names, values);
    }

}
