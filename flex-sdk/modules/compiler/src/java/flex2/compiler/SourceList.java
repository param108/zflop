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

import flex2.compiler.io.FileUtil;
import flex2.compiler.io.LocalFile;
import flex2.compiler.io.VirtualFile;
import flex2.compiler.util.CompilerMessage;
import flex2.compiler.util.NameFormatter;
import flex2.compiler.util.QName;
import flex2.compiler.util.ThreadLocalToolkit;

import java.io.File;
import java.util.*;

/**
 * A SourceList represents a list of files, following the single public definition rule,
 * and an associated path where dependencies can be found.
 *
 * @author Clement Wong
 */
public final class SourceList
{
	public SourceList(List files, VirtualFile[] classPath, VirtualFile appPath, String[] mimeTypes)
		throws CompilerException
	{
		this(files, classPath, appPath, mimeTypes, true);
	}

	public SourceList(List files, VirtualFile[] classPath, VirtualFile appPath, String[] mimeTypes, boolean lastIsRoot)
		throws CompilerException
	{
		VirtualFile[] vfiles = new VirtualFile[files.size()];
		files.toArray(vfiles);
		init(vfiles, classPath, appPath, mimeTypes, lastIsRoot);
	}

	private void init(VirtualFile[] files, VirtualFile[] classPath, VirtualFile appPath, String[] mimeTypes, boolean lastIsRoot)
		throws CompilerException
	{
		this.mimeTypes = mimeTypes;
		sources = new LinkedHashMap(files.length);
		directories = new ArrayList(classPath == null ? 0 : classPath.length);

		SourcePath.addApplicationParentToSourcePath(appPath, classPath, directories);
		SourcePath.addPathElements(classPath, directories, true, null);

		for (int i = 0, length = files.length; i < length; i++)
		{
            // No need to check to see if the appPath is supported again.
			if ((appPath != null && files[i].getName().equals(appPath.getName())) || isSupported(files[i]))
			{
				String name = files[i].getName();
				VirtualFile pathRoot = calculatePathRoot(files[i]);
				if (pathRoot != null)
				{
					String relativePath = calculateRelativePath(name);
					String namespaceURI = relativePath.replace('/', '.'), localPart = calculateLocalPart(name);
					Source s = new Source(files[i], pathRoot, relativePath, localPart, this, false, (i == length - 1) && lastIsRoot);					
					sources.put(constructClassName(namespaceURI, localPart), s);
				}
				else
				{
					// Files in SourceList must be in --source-path.
					// output an error here...
					FileNotInSourcePath ex = new FileNotInSourcePath(name);
					ThreadLocalToolkit.log(ex);
					throw ex;
				}
			}
			else
			{
				UnsupportedFileType ex = new UnsupportedFileType(files[i].getName());
				ThreadLocalToolkit.log(ex);
				throw ex;
			}
		}
	}

	private VirtualFile calculatePathRoot(VirtualFile f)
	{
		return calculatePathRoot(f, directories);
	}
	
	static VirtualFile calculatePathRoot(VirtualFile f, List directories)
	{
		String name = f.getName();
		for (int i = 0, size = directories == null ? 0 : directories.size(); i < size; i++)
		{
			String dir = ((File) directories.get(i)).getAbsolutePath();
			if (name.startsWith(dir))
			{
				return new LocalFile(FileUtil.openFile(dir));
			}
		}
		// return new LocalFile(FileUtil.openFile(f.getParent()));
		return null;
	}
	
	private String calculateRelativePath(String name)
	{
		// C: name is canonical.
		for (int i = 0, size = directories == null ? 0 : directories.size(); i < size; i++)
		{
			String dir = ((File) directories.get(i)).getAbsolutePath();
			if (name.startsWith(dir))
			{
				name = name.substring(dir.length() + 1);
				int index = name.lastIndexOf(File.separatorChar);
				if (index != -1)
				{
					return name.substring(0, index).replace(File.separatorChar, '/');
				}
				else
				{
					return "";
				}
			}
		}

		return "";
	}
	
	private String calculateLocalPart(String name)
	{
		String leafName = name.substring(name.lastIndexOf(File.separatorChar) + 1);
		String localPart = leafName.substring(0, leafName.lastIndexOf('.'));
		return localPart;
	}

	private Map sources; // Map<String, Source>
	private List directories; // List<File>
	private String[] mimeTypes;

	public List retrieveSources()
	{
		List sources = new ArrayList(this.sources.size());

		for (Iterator i = this.sources.keySet().iterator(); i.hasNext();)
		{
			String name = (String) i.next();
			Source s = (Source) this.sources.get(name);
			CompilationUnit u = (s != null) ? s.getCompilationUnit() : null;

			if (s != null && !s.exists())
			{
				// C: This is a SourceList. If the source doesn't exist, the compiler should get a warning...
				s = null;
			}
			else if ((u != null && !u.isDone()) || (s != null && s.isUpdated()))
			{
				// s.removeCompilationUnit();
			}
			else if (u != null)
			{
				s = s.copy();
				assert s != null;
			}

			if (s != null)
			{
				sources.add(s);
			}
		}

		return sources;
	}

	private String constructClassName(String namespaceURI, String localPart)
	{
		return (namespaceURI.length() == 0) ? localPart : new StringBuffer(namespaceURI.length() + localPart.length() + 1).append(namespaceURI).append(":").append(localPart).toString();
	}

	public Source findSource(String namespaceURI, String localPart)
	{
		if (sources.size() == 0)
		{
			return null;
		}
		
		assert localPart.indexOf('.') == -1 && localPart.indexOf('/') == -1 && localPart.indexOf(':') == -1
                : "findSource(" + namespaceURI + "," + localPart + ") has bad localPart";

		// classname format is a.b:c
		String className = constructClassName(namespaceURI, localPart);
		Source s = (Source) sources.get(className);
		CompilationUnit u = (s != null) ? s.getCompilationUnit() : null;

		if (s != null && !s.exists())
		{
			s = null;
		}

		// If the compilation unit does exist and the top level definition name doesn't match
		// the specified class name, we don't count it as a match.
		if (s != null && u != null && u.topLevelDefinitions.size() == 1)
		{
			if (!u.topLevelDefinitions.contains(namespaceURI, localPart))
			{
				String realName = u.topLevelDefinitions.first().toString();
				sources.put(realName, s);
				s = null;
				u = null;
			}
		}

		if (s != null && ((u != null && !u.isDone()) || s.isUpdated()))
		{
			// s.removeCompilationUnit();
		}
		else if (s != null && u != null)
		{
			s = s.copy();
			assert s != null;
		}

		return s;
	}
	
	String[] checkClassNameFileName(Source s)
	{
		String defName = null, pathName = null;
		
		if (s.getOwner() == this)
		{
			QName def = s.getCompilationUnit().topLevelDefinitions.last();
			
			defName = def.getLocalPart();
			pathName = s.getShortName();
			
			if (defName.equals(pathName))
			{
				return null;
			}
		}
		
		return new String[] { pathName, defName };
	}

	String[] checkPackageNameDirectoryName(Source s)
	{
		String defPackage = null, pathPackage = null;
		
		if (s.getOwner() == this)
		{
			QName def = s.getCompilationUnit().topLevelDefinitions.last();
			
			defPackage = NameFormatter.normalizePackageName(def.getNamespace());
			pathPackage = NameFormatter.toDot(s.getRelativePath(), '/');
			
			if (defPackage.equals(pathPackage))
			{
				return null;
			}
		}
		
		return new String[] { pathPackage, defPackage };
	}

	private boolean isSupported(VirtualFile file)
	{
		for (int i = 0, length = mimeTypes.length; i < length; i++)
		{
			if (mimeTypes[i].equals(file.getMimeType()))
			{
				return true;
			}
		}

		return false;
	}

	List getPaths()
	{
		return directories;
	}

	String[] getMimeTypes()
	{
		return mimeTypes;
	}

	Collection sources()
	{
		return sources.values();
	}

	// error messages

	public static class FileNotInSourcePath extends CompilerMessage.CompilerError
	{
		public FileNotInSourcePath(String name)
		{
			super();
			this.name = name;
		}

		public final String name;
	}

	public static class UnsupportedFileType extends CompilerMessage.CompilerError
	{
		public UnsupportedFileType(String name)
		{
			super();
			this.name = name;
		}

		public final String name;
	}
}
