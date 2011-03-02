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
import flex2.compiler.util.MimeMappings;
import flex2.compiler.util.NameFormatter;
import flex2.compiler.util.QName;
import flex2.compiler.util.ThreadLocalToolkit;

import java.io.File;
import java.util.*;

/**
 * A list of paths where dependencies, following the single public definition rule, can be
 * resolved.
 *
 * @author Clement Wong
 */
public class SourcePath extends SourcePathBase
{
	public SourcePath(VirtualFile[] classPath, VirtualFile appPath, String[] mimeTypes, boolean allowSourcePathOverlap)
	{
		this(mimeTypes, allowSourcePathOverlap);

		addApplicationParentToSourcePath(appPath, classPath, this.directories);
		addPathElements(classPath, this.directories, allowSourcePathOverlap, warnings);
	}
	
	public SourcePath(String[] mimeTypes, boolean allowSourcePathOverlap)
	{
		super(mimeTypes, allowSourcePathOverlap);
		directories = new LinkedList();
	}

	protected final List directories; // List<File>

	public void addPathElements(VirtualFile[] classPath)
	{
		addPathElements(classPath, directories, allowSourcePathOverlap, warnings);
	}	

	private Source newSource(File file, File pathRoot, String namespaceURI, String localPart)
	{
		return new Source(new LocalFile(file), new LocalFile(pathRoot),
						  namespaceURI.replace('.', '/'), localPart, this, false, false);
	}

	// see if the Source object continues to be the first choice given a QName.
	boolean checkPreference(Source s)
	{
		assert s.getOwner() == this;
		
		String relativePath = constructRelativePath(s), pathRoot = s.getPathRoot().getName();
		if (relativePath == null)
		{
			// not possible, but don't disrupt the flow...
			return true;
		}
		
		boolean thisPath = false;
		
		for (int i = 0, size = directories.size(); i < size; i++)
		{
			File d = (File) directories.get(i), f = null;
			if (pathRoot.equals(FileUtil.getCanonicalPath(d)))
			{
				thisPath = true;
			}
			
			try
			{
				f = findFile(d, relativePath, mimeTypes);
			}
			catch (CompilerException ex)
			{
				removeSource(s);
				return false;
			}

			if (f != null && !thisPath)
			{
				removeSource(s);
				return false;
			}
		}
		
		return true;
	}
	
	protected Source findFile(String className, String namespaceURI, String localPart) throws CompilerException
	{
		String p = className.replace(':', '.').replace('.', File.separatorChar);
		Source s = null;

		for (int i = 0, size = directories.size(); i < size; i++)
		{
			File f, d = (File) directories.get(i);

			if ((f = findFile(d, p, mimeTypes)) != null)
			{
				sources.put(className, s = newSource(f, d, namespaceURI, localPart));
				return s;
			}
		}
		
		return null;
	}
	
	public boolean hasPackage(String packageName)
	{
		for (int i = 0, size = directories.size(); i < size; i++)
		{
			File d = (File) directories.get(i);
			if (hasDirectory(d, packageName))
			{
				return true;
			}
		}

		return false;
	}

	public boolean hasDefinition(QName qName)
	{
		String className = constructClassName(qName.getNamespace(), qName.getLocalPart());

		if (misses.contains(className))
		{
			return false;
		}

		if (hits.contains(className))
		{
			return true;
		}

		String p = className.replace(':', '.').replace('.', File.separatorChar);

		for (int i = 0, size = directories.size(); i < size; i++)
		{
			File f, d = (File) directories.get(i);

			try
			{
				if ((f = findFile(d, p, mimeTypes)) != null)
				{
					hits.add(className);
					return true;
				}
			}
			catch (CompilerException ex)
			{
			}
		}

		misses.add(className);
		return false;
	}

	private boolean hasDirectory(File dir, String packageName)
	{
		if (packageName.length() == 0)
		{
			return true;
		}

		String relativePath = packageName.replace('.', File.separatorChar);
		String fullPath = dir.getPath() + File.separator + relativePath;

		if (dirs.get(fullPath) == NO_DIR)
		{
			return false;
		}

		boolean result = new File(dir, relativePath).isDirectory();
		dirs.put(fullPath, result ? fullPath : NO_DIR);

		return result;
	}
	
	public List getPaths()
	{
		return directories;
	}
}

/**
 * @author Clement Wong
 */
abstract class SourcePathBase
{
	protected final static Object NO_DIR = new Object();

	static void addApplicationParentToSourcePath(VirtualFile appPath, VirtualFile[] classPath, List directories)
	{
		if (appPath != null)
		{
			File f = FileUtil.openFile(appPath.getParent());
			// if (f != null && f.isDirectory())
			if (f != null && f.isDirectory() && (FileUtil.isSubdirectoryOf(appPath.getParent(), classPath) == -1))
			{
				directories.add(f);
			}
		}
	}

	static void addPathElements(VirtualFile[] classPath, List directories, boolean allowSourcePathOverlap, List warnings)
	{
		boolean badPaths = false;

		for (int i = 0, length = (classPath == null) ? 0 : classPath.length; i < length; i++)
		{
			String path = classPath[i].getName();
			File f = FileUtil.openFile(path);
			if (f != null && f.isDirectory())
			{
				if (!allowSourcePathOverlap && !badPaths)
				{
					int index = FileUtil.isSubdirectoryOf(f, directories);
					if (index != -1)
					{
						String dirPath = ((File) directories.get(index)).getAbsolutePath();
						if (checkValidPackageName(path, dirPath))
						{
							// C: don't want to use ThreadLocalToolkit here...

	                        // preilly: don't use logError below, because we don't stop
	                        // compiling and if the error count is non-zero downstream mayhem
	                        // occurs.  For example, no SWC's get loaded, which makes it
	                        // alittle tough to compile.

							warnings.add(new ClasspathOverlap(path, dirPath));
							badPaths = true;
						}
					}
				}
				directories.add(f);
			}
		}		
	}
	
	private static boolean checkValidPackageName(String path1, String path2)
	{
		if (path1.equals(path2)) return true;
		String packagePath = path1.length() > path2.length() ? path1.substring(path2.length()) : path2.substring(path1.length());

		for (StringTokenizer t = new StringTokenizer(packagePath, File.separator); t.hasMoreTokens(); )
		{
			String s = t.nextToken();
			if (!flex2.compiler.mxml.lang.TextParser.isValidIdentifier(s))
			{
				return false;
			}
		}
		
		return true;
	}

	public SourcePathBase(String[] mimeTypes, boolean allowSourcePathOverlap)
	{
		this.mimeTypes = mimeTypes;
		this.allowSourcePathOverlap = allowSourcePathOverlap;
		sources = new HashMap();

		hits = new HashSet();
		misses = new HashSet(1024);
		dirs = new HashMap();
		warnings = new ArrayList(5);
	}

	protected final String[] mimeTypes;
	protected final Map sources; // Map<String, Source>
	protected boolean allowSourcePathOverlap;

	protected final Set hits, misses;
	protected final HashMap dirs;
	protected final List warnings;

	protected String constructClassName(String namespaceURI, String localPart)
	{
		return (namespaceURI.length() == 0) ? localPart : new StringBuffer(namespaceURI.length() + localPart.length() + 1).append(namespaceURI).append(":").append(localPart).toString();
	}

	public Source findSource(String namespaceURI, String localPart) throws CompilerException
	{
		assert localPart.indexOf('.') == -1 && localPart.indexOf('/') == -1 && localPart.indexOf(':') == -1
                : "findSource(" + namespaceURI + "," + localPart + ") has bad localPart";

		// classname format is a.b:c
		String className = constructClassName(namespaceURI, localPart);
		
		return findSource(className, namespaceURI, localPart);
	}
	
	protected Source findSource(String className, String namespaceURI, String localPart) throws CompilerException
	{
		if (misses.contains(className))
		{
			return null;
		}

		Source s = null;

		if (!sources.containsKey(className))
		{
			if ((s = findFile(className, namespaceURI, localPart)) != null)
			{
				return s;
			}
		}

		s = (Source) sources.get(className);
		CompilationUnit u = (s != null) ? s.getCompilationUnit() : null;

		if (s != null && !s.exists())
		{
			sources.remove(className);
			s = null;
		}

		if (adjustDefinitionName(namespaceURI, localPart, s, u))
		{
			u = null;
			s = null;
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

		if (s == null)
		{
			misses.add(className);
		}

		return s;
	}

	protected boolean adjustDefinitionName(String namespaceURI, String localPart, Source s, CompilationUnit u)
	{
		// If the compilation unit does exist and the top level definition name doesn't match
		// the specified class name, we don't count it as a match.
		if (s != null && u != null && u.topLevelDefinitions.size() == 1)
		{
			if (!u.topLevelDefinitions.contains(namespaceURI, localPart))
			{
				String realName = ((QName) u.topLevelDefinitions.first()).toString();
				sources.put(realName, s);
				misses.remove(realName);
				return true;
			}
		}
		
		return false;
	}
	
	abstract boolean checkPreference(Source s);

	protected abstract Source findFile(String className, String namespaceURI, String localPart) throws CompilerException;

	protected File findFile(File directory, String relativePath, String[] mimeTypes) throws CompilerException
	{
		File found = null;

		for (int k = 0, length = mimeTypes.length; k < length; k++)
		{
			File f = findFile(directory, relativePath, mimeTypes[k]);

			if (f != null && found == null)
			{
				found = f;
				// break;
			}
			else if (f != null)
			{
				throw new MoreThanOneComponentOfTheSameName(found.getAbsolutePath(), f.getAbsolutePath());
			}
		}

		return found;
	}

	protected File findFile(File directory, String relativePath, String mimeType)
	{
		String fullPath = directory.getPath() + File.separator + relativePath;
		int lastSlash = fullPath.lastIndexOf(File.separator);
		String dir = null;
		if (lastSlash != -1)
		{
			dir = fullPath.substring(0, lastSlash);
			if (dirs.get(dir) == NO_DIR)
			{
				return null;
			}
		}

		String path = relativePath + MimeMappings.getExtension(mimeType);
		File f = FileUtil.openFile(directory, path);

		if ((f != null) && f.isFile() && FileUtil.getCanonicalPath(f).endsWith(path))
		{
			return f;
		}
		else if (f != null && dir != null && !dirs.containsKey(dir))
		{
			File p = f.getParentFile();
			dirs.put(dir, p != null && p.isDirectory() ? dir : NO_DIR);
		}

		return null;
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

	protected String constructRelativePath(Source s)
	{
		// + 1 removes the leading /
		String relativePath = s.getName().substring(s.getPathRoot().getName().length() + 1);
		for (int k = 0, length = mimeTypes.length; k < length; k++)
		{
			String ext = MimeMappings.getExtension(mimeTypes[k]);
			if (relativePath.endsWith(ext))
			{
				relativePath = relativePath.substring(0, relativePath.length() - ext.length());
				return relativePath;
			}
		}
		
		assert false;
		return null;
	}
	
	// used by API.validateCompilationUnits()... not efficient, but we rarely call it...
	public void removeSource(Source s)
	{
		for (Iterator i = sources.entrySet().iterator(); i.hasNext(); )
		{
			Map.Entry e = (Map.Entry) i.next();
			if (e.getValue() == s)
			{
				i.remove();
				break;
			}
		}
	}

	public void clearCache()
	{
		hits.clear();
		misses.clear();
		dirs.clear();
	}

	String[] getMimeTypes()
	{
		return mimeTypes;
	}

	Map sources()
	{
		return sources;
	}
	
	public void displayWarnings()
	{
		for (int i = 0, size = warnings.size(); i < size; i++)
		{
			ThreadLocalToolkit.log((ClasspathOverlap) warnings.get(i));
		}
	}

	// error messages

	public static class ClasspathOverlap extends CompilerMessage.CompilerWarning
	{
		public ClasspathOverlap(String path, String directory)
		{
			super();
			this.cpath = path;
			this.directory = directory;
		}

		public final String cpath, directory;
	}

	public static class MoreThanOneComponentOfTheSameName extends CompilerMessage.CompilerInfo
	{
		public MoreThanOneComponentOfTheSameName(String file1, String file2)
		{
			super();
			this.file1 = file1;
			this.file2 = file2;
		}

		public final String file1, file2;
	}
}
