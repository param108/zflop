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

package flex2.compiler;

import flex2.compiler.i18n.I18nUtils;
import flex2.compiler.common.CompilerConfiguration;
import flex2.compiler.io.FileUtil;
import flex2.compiler.io.ResourceFile;
import flex2.compiler.io.VirtualFile;
import flex2.compiler.io.LocalFile;

import java.io.File;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * Looks for resource bundles property files
 *
 * @author Clement Wong
 * @author Brian Deitte
 */
public class ResourceBundlePath extends SourcePathBase
{
	public ResourceBundlePath(CompilerConfiguration config, VirtualFile appPath)
	{
		// C: allowSourcePathOverlap is true here because SourcePath will take care of that.
		super(I18nUtils.getTranslationFormat(config).getSupportedMimeTypes(), true);
		
		rbDirectories = new HashMap();
		locales = config.getLocales();
		for (int i = 0, size = locales == null ? 0 : locales.length; i < size; i++)
		{
			VirtualFile[] classPath = config.getResourceBundlePathForLocale(locales[i]);
			List directories = new LinkedList();
			
			addApplicationParentToSourcePath(appPath, classPath, directories);
			addPathElements(classPath, directories, allowSourcePathOverlap, warnings);

			rbDirectories.put(locales[i], directories);
		}
	}
	
	private String[] locales;
	private Map rbDirectories;
	
	private Source newSource(String name, VirtualFile[] files, VirtualFile[] pathRoots, File pathRoot, String namespaceURI, String localPart)
	{
		return new Source(new ResourceFile(name, locales, files, pathRoots), new LocalFile(pathRoot),
						  namespaceURI.replace('.', '/'), localPart , this, false, false, false);
	}
	
	// see if the Source object continues to be the first choice given a QName.
	boolean checkPreference(Source s)
	{
		assert s.getOwner() == this;
		
		String p, relativePath = s.getRelativePath();
		if (relativePath.length() == 0)
		{
			p = s.getShortName();
		}
		else
		{
			p = (relativePath + "/" + s.getShortName()).replace('/', File.separatorChar);
		}
		ResourceFile rf = (ResourceFile) s.getBackingFile();

		for (int i = 0, length = locales == null ? 0 : locales.length; i < length; i++)
		{
			rf.setLocale(locales[i]);
			VirtualFile resourceFile = rf.getResourceFile();
			List directories = (List) rbDirectories.get(locales[i]);
						
			for (int j = 0, size = directories == null ? 0 : directories.size(); j < size; j++)
			{
				File f, d = (File) directories.get(j);

				try
				{
					if ((f = findFile(d, p, mimeTypes)) != null)
					{
						if (!resourceFile.getName().equals(FileUtil.getCanonicalPath(f)))
						{
							removeSource(s);
							return false;
						}
						else
						{
							break;
						}
					}
				}
				catch (CompilerException ex)
				{
					removeSource(s);
					return false;
				}
			}
		}

		return true;
	}
	
	protected boolean adjustDefinitionName(String namespaceURI, String localPart, Source s, CompilationUnit u)
	{
		return false;
	}

	protected Source findFile(String className, String namespaceURI, String localPart) throws CompilerException
	{
		String p = className.replace(':', '.').replace('.', File.separatorChar);
		VirtualFile[] files = null;
		VirtualFile[] pathRoots = null;
		File pathRoot = null;
		String name = null;
		Source s = null;
		
		for (int i = 0, length = locales == null ? 0 : locales.length; i < length; i++)
		{
			List directories = (List) rbDirectories.get(locales[i]);
			
			for (int j = 0, size = directories == null ? 0 : directories.size(); j < size; j++)
			{
				File f, d = (File) directories.get(j);

				if ((f = findFile(d, p, mimeTypes)) != null)
				{
					if (files == null) files = new VirtualFile[length];
					if (pathRoots == null) pathRoots = new VirtualFile[length];
					
					if (name == null)
					{
						pathRoot = d;
						name = FileUtil.getCanonicalPath(f);
					}
					
					files[i] = new LocalFile(f);
					pathRoots[i] = new LocalFile(d);
					break;
				}
			}
		}

		if (files != null)
		{
			sources.put(className, s = newSource(name, files, pathRoots, pathRoot, namespaceURI, localPart));
		}
		
		return s;
	}
	
	String[] getLocales()
	{
		return locales;
	}
	
	Map getResourceBundlePaths()
	{
		return rbDirectories;
	}
	
	public VirtualFile[] findVirtualFiles(String rbName)
	{
		String p = rbName.replace(':', '.').replace('.', File.separatorChar);
		VirtualFile[] files = null;
		
		for (int i = 0, length = locales == null ? 0 : locales.length; i < length; i++)
		{
			List directories = (List) rbDirectories.get(locales[i]);
			
			for (int j = 0, size = directories == null ? 0 : directories.size(); j < size; j++)
			{
				File f, d = (File) directories.get(j);
				try
				{
					if ((f = findFile(d, p, mimeTypes)) != null)
					{
						if (files == null) files = new VirtualFile[length];
						files[i] = new LocalFile(f);
						break;
					}
				}
				catch (CompilerException ex)
				{
				}
			}
		}

		return files;
	}
}
