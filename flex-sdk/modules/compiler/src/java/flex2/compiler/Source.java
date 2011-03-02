////////////////////////////////////////////////////////////////////////////////
//
//  ADOBE SYSTEMS INCORPORATED
//  Copyright 2004-2007 Adobe Systems Incorporated
//  All Rights Reserved.
//
//  NOTICE: Adobe permits you to use, modify, and distribute this file
//  in accordance with the terms of the license agreement accompanying it.
//
////////////////////////////////////////////////////////////////////////////////

package flex2.compiler;

import flex2.compiler.common.PathResolver;
import flex2.compiler.common.SinglePathResolver;
import flex2.compiler.io.InMemoryFile;
import flex2.compiler.io.VirtualFile;
import flex2.compiler.swc.SwcScript;
import flex2.compiler.util.LineNumberMap;
import flex2.compiler.util.LocalLogger;
import flex2.compiler.util.MimeMappings;
import flex2.compiler.util.ThreadLocalToolkit;

import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import java.util.Map.Entry;

/**
 * @author Clement Wong
 */
public final class Source
{
	// used by flex2.compiler.i18n.Compiler, InterfaceCompiler and ImplementationCompiler
	public Source(VirtualFile file, Source original)
	{
		this(file, original.pathRoot, original.relativePath, original.shortName, original.owner, original.isInternal, original.isRoot, original.isDebuggable);
		this.delegate = original;
	}

	// used by InterfaceCompiler.createInlineComponentUnit().  Note the owner will be set
	// later by the ResourceContainer when this is passed into addResource() by
	// API.addGeneratedSources().
	public Source(VirtualFile file, Source original, boolean isInternal, boolean isRoot)
	{
		this(file, original.pathRoot, original.relativePath, file.getName(), null, isInternal, isRoot, true);
		this.delegate = original;
	}

	// used by FileSpec
	public Source(VirtualFile file, String relativePath, String shortName, Object owner, boolean isInternal, boolean isRoot)
	{
		this(file, null, relativePath, shortName, owner, isInternal, isRoot, true);
	}

	// used by SourceList and SourcePath
	public Source(VirtualFile file, VirtualFile pathRoot, String relativePath, String shortName, Object owner, boolean isInternal, boolean isRoot)
	{
		this(file, pathRoot, relativePath, shortName, owner, isInternal, isRoot, true);
	}

	// used by StylesContainer, CompilerSwcContext, EmbedEvaluator, DataBindingExtension and PreLink
	public Source(VirtualFile file, String relativePath, String shortName, Object owner, boolean isInternal, boolean isRoot, boolean isDebuggable)
	{
		this(file, null, relativePath, shortName, owner, isInternal, isRoot, isDebuggable);
	}

	Source(VirtualFile file, VirtualFile pathRoot, String relativePath, String shortName, Object owner, boolean isInternal, boolean isRoot, boolean isDebuggable)
	{
		this.file = file;
		this.pathRoot = pathRoot;
		this.relativePath = relativePath;
		this.shortName = shortName;
		this.owner = owner;
		this.isInternal = isInternal;
		this.isRoot = isRoot;
		this.isDebuggable = isDebuggable;

		fileIncludes = new HashSet(4); // HashSet<VirtualFile>

		fileTime = file.getLastModified();
		fileIncludeTimes = new HashMap(4); // HashMap<VirtualFile, Long>
	}

	private VirtualFile file;
	private VirtualFile pathRoot;
	// 'resolver' doesn't need persistence because it's constructed from pathRoot.
	private PathResolver resolver;
	private String relativePath, shortName;
	private Object owner;
	private boolean isInternal;
	private boolean isRoot;
	private boolean isDebuggable;
	private boolean isPreprocessed;

	// the delegate should have whatever's in fileIncludes.
	private Set fileIncludes; // Set<VirtualFile>

	private CompilationUnit unit;

	private long fileTime;
	private Map fileIncludeTimes; // Map<VirtualFile, Long>

	// 1. path resolution
	// 2. backing file
	// 3. source fragments
	private Source delegate;

	/**
     * This is a per-Source/CompilationUnit logger.  The
     * ThreadLocalToolkit logger is a per-compile logger.  This logger
     * is usually wired to the ThreadLocalToolkit logger.  During the
     * life of a Source, this logger be nulled out, when it's no
     * longer needed to save memory.  If the Source is reused again, a
     * new LocalLogger is created by API.preprocess().
     */
	private LocalLogger logger;

	private Map fragments;
	private Map fragmentLineMaps;

	private AssetInfo assetInfo;

	public int lineCount;

	public CompilationUnit newCompilationUnit(Object syntaxTree, Context context)
	{
		unit = new CompilationUnit(this, syntaxTree, context);
		return unit;
	}

	public CompilationUnit getCompilationUnit()
	{
		return unit;
	}

	void removeCompilationUnit()
	{
		unit = null;
		fileTime = file.getLastModified();
		logger = null;
		isPreprocessed = false;

		fileIncludes.clear();
		fileIncludeTimes.clear();

		delegate = null;
		resolver = null;

		if (fragments != null)
		{
			fragments.clear();
			fragmentLineMaps.clear();
		}
	}

	public void setAssetInfo(AssetInfo assetInfo)
	{
		this.assetInfo = assetInfo;
	}

	void setPreprocessed()
	{
		isPreprocessed = true;
	}

	boolean isPreprocessed()
	{
		return isPreprocessed;
	}

	public void setLogger(LocalLogger logger)
	{
		if (this.logger == null)
		{
			this.logger = logger;
		}
	}

	LocalLogger getLogger()
	{
		return logger;
	}

	void disconnectLogger()
	{
		if (logger != null && logger.warningCount() == 0 && logger.errorCount() == 0)
		{
			logger = null;
		}
		else if (logger != null)
		{
			// if warning/error exists, keep the logger and just disconnect it from the original logger.
			logger.disconnect();
		}
	}

	boolean hasError()
	{
		return logger != null && logger.errorCount() > 0;
	}

	public Object getOwner()
	{
		return owner;
	}

	// C: do not make this public; only used by ResourceContainer.
	void setOwner(Object owner)
	{
		this.owner = owner;
	}

	public boolean isSourcePathOwner()
	{
		return owner != null && owner instanceof SourcePath && !isResourceBundlePathOwner();
	}

	public boolean isSourceListOwner()
	{
		return owner != null && owner instanceof SourceList;
	}

	public boolean isFileSpecOwner()
	{
		return owner != null && owner instanceof FileSpec;
	}

	public boolean isSwcScriptOwner()
	{
		return owner != null && owner instanceof SwcScript;
	}

	public boolean isResourceContainerOwner()
	{
		return owner != null && owner instanceof ResourceContainer;
	}

	public boolean isResourceBundlePathOwner()
	{
		return owner != null && owner instanceof ResourceBundlePath;
	}

	public boolean isInternal()
	{
		return isInternal;
	}

	public boolean isEntryPoint()
	{
		return isFileSpecOwner();
	}

	public boolean isRoot()
	{
		return isRoot;
	}

	public boolean isDebuggable()
	{
		return isDebuggable;
	}

	public boolean isCompiled()
	{
		return unit != null && unit.isBytecodeAvailable();
	}

	public boolean isUpdated()
	{
		if (file.getLastModified() != fileTime)
		{
			return true;
		}
		else
		{
			for (Iterator i = fileIncludeTimes.keySet().iterator(); i.hasNext();)
			{
				VirtualFile f = (VirtualFile) i.next();
				long ts = ((Long) fileIncludeTimes.get(f)).longValue();
				if (f.getLastModified() != ts)
				{
					return true;
				}
			}

			return false;
		}
	}

	public boolean isUpdated(Source source)
	{
		boolean result = false;

		if (assetInfo != null)
		{
			if (assetInfo.getArgs().size() != source.assetInfo.getArgs().size())
			{
				result = true;
			}
			else
			{
				for (Iterator i = assetInfo.getArgs().entrySet().iterator(); i.hasNext() && !result;)
				{
					Entry entry = (Entry) i.next();
					String key = (String) entry.getKey();
					String value = (String) entry.getValue();

					if (!value.equals(Transcoder.COLUMN) &&
						!value.equals(Transcoder.LINE) &&
						!value.equals(source.assetInfo.getArgs().get(key)))
					{
						result = true;
					}
				}
			}
		}

		return result;
	}

	public boolean exists()
	{
		return file.getLastModified() > 0;
	}

	public String getName()
	{
		return file.getName();
	}

	public String getNameForReporting()
	{
		return file.getNameForReporting();
	}

	// C: This is temporary... only use it when you need to set asc Context.setPath
	public String getParent()
	{
		return file.getParent();
	}

	public long size()
	{
		return file.size();
	}

	public InputStream getInputStream() throws IOException
	{
		return file.getInputStream();
	}

	public byte[] toByteArray() throws IOException
	{
		return file.toByteArray();
	}

	public boolean isTextBased()
	{
		return file.isTextBased();
	}

	public String getInputText()
	{
		return file.toString();
	}

	public String getMimeType()
	{
		return file.getMimeType();
	}

	public long getLastModified()
	{
		return fileTime;
	}

	public VirtualFile resolve(String include)
	{
		return getPathResolver().resolve(include);
	}

	public PathResolver getPathResolver()
	{
		if (resolver != null)
		{
			return resolver;
		}

		resolver = new PathResolver();
		resolver.addSinglePathResolver(new Resolver(delegate != null ? delegate.getPathResolver() : null, file, pathRoot));
		resolver.addSinglePathResolver(ThreadLocalToolkit.getPathResolver());

		return resolver;
	}

	static class Resolver implements SinglePathResolver
	{
		Resolver(PathResolver delegate, VirtualFile file, VirtualFile pathRoot)
		{
			this.delegate = delegate;
			this.file = file;
			this.pathRoot = pathRoot;
		}

		private PathResolver delegate;
		private VirtualFile file, pathRoot;

		public VirtualFile resolve(String relative)
		{
			// delegate.resolve() before this.resolve()
			if (delegate != null)
			{
				return delegate.resolve(relative);
			}
			else
			{
				VirtualFile f = null;

				if (relative != null && !relative.startsWith("/"))
				{
					f = file.resolve(relative);
				}

				if (f == null && relative != null && pathRoot != null)
				{
					f = pathRoot.resolve(relative.substring(1));
				}

				return f;
			}
		}
	}

	public void setPathResolver(PathResolver r)
	{
		resolver = r;
	}

	public VirtualFile getBackingFile()
	{
		return (delegate == null)? file : delegate.getBackingFile();
	}

	public String getRelativePath()
	{
		// C: should be /-separated (no backslash)
		return relativePath;
	}

	public String getShortName()
	{
		return shortName;
	}

	public VirtualFile getPathRoot()
	{
		return pathRoot;
	}

	public void addFileIncludes(Source s)
	{
		fileIncludes.addAll(s.fileIncludes);
		fileIncludeTimes.putAll(s.fileIncludeTimes);
	}

	public boolean addFileInclude(String path)
	{
		VirtualFile f = resolve(path);

		return addFileInclude(f);
	}

	public boolean addFileInclude(VirtualFile f)
	{
		if (f != null)
		{
			if (!fileIncludes.contains(f))
			{
				fileIncludes.add(f);
				fileIncludeTimes.put(f, new Long(f.getLastModified()));
			}

			if (delegate != null)
			{
				delegate.addFileInclude(f);
			}

			return true;
		}
		else
		{
			return false;
		}
	}

	public Iterator getFileIncludes()
	{
		return fileIncludes.iterator();
	}

    /**
     * Returns a copy.
     */
    public Set getFileIncludesSet()
    {
        return new HashSet(fileIncludes);
    }

    /**
     * Returns a copy.
     */
    public Map getFileIncludeTimes()
    {
        return new HashMap(fileIncludeTimes);
    }

	public boolean isIncludedFile(String name)
	{
		for (Iterator i = fileIncludes.iterator(); i.hasNext();)
		{
			VirtualFile f = (VirtualFile) i.next();
			if (f.getName().equals(name) || f.getNameForReporting().equals(name))
			{
				return true;
			}
		}

		return false;
	}

	public Iterator getUpdatedFileIncludes()
	{
		List updated = null;

		for (Iterator i = fileIncludeTimes.keySet().iterator(); i.hasNext();)
		{
			VirtualFile f = (VirtualFile) i.next();
			long ts = ((Long) fileIncludeTimes.get(f)).longValue();
			if (f.getLastModified() != ts)
			{
				if (updated == null)
				{
					updated = new ArrayList(fileIncludeTimes.size());
				}
				updated.add(f);
			}
		}

		return updated == null ? null : updated.iterator();
	}

	public int getFileIncludeSize()
	{
		return fileIncludes.size();
	}

	long getFileIncludeTime(VirtualFile f)
	{
		return ((Long) fileIncludeTimes.get(f)).longValue();
	}

	long getFileTime()
	{
		return fileTime;
	}

	public void addSourceFragment(String n, Object f, LineNumberMap m)
	{
		if (fragments == null)
		{
			fragments = new HashMap();
			fragmentLineMaps = new HashMap();
		}

		fragments.put(n, f);

        if (m != null)
        {
            fragmentLineMaps.put(n, m);
        }
    }

	public Object getSourceFragment(String n)
	{
		// this.fragment before delegate.fragment
		Object obj = (fragments == null) ? null : fragments.get(n);
		if (obj == null && delegate != null)
		{
			return delegate.getSourceFragment(n);
		}
		else
		{
			return obj;
		}
	}

	public Collection getSourceFragmentLineMaps()
	{
		if (fragmentLineMaps != null)
		{
			return fragmentLineMaps.values();
		}
		else if (delegate != null)
		{
			return delegate.getSourceFragmentLineMaps();
		}
		else
		{
			return null;
		}
	}

	void clearSourceFragments()
	{
		fragments = null;
		fragmentLineMaps = null;

		if (delegate != null)
		{
			delegate.clearSourceFragments();
		}
	}

	/**
	 * Used by the web tier for dependency tracking.
	 *
	 * returns the last modified time of the source file itself
	 * without taking last modified time of any dependent files into account
	 *
	 * does not return a last modified time for in-memory files
	 */
	public long getRawLastModified()
	{
		if (isSwcScriptOwner())
		{
			return ((SwcScript) owner).getLibrary().getSwcCreationTime();
		}
		else if (isFileSpecOwner() || isSourceListOwner() || isSourcePathOwner())
		{
			return fileTime;
		}
		else // if (isResourceBundlePathOwner() || isResourceContainerOwner())
		{
			return -1;
		}
	}

	/**
	 * Used by the web tier for dependency tracking.
	 */
	public String getRawLocation()
	{
		if (isSwcScriptOwner())
		{
			return ((SwcScript) owner).getLibrary().getSwcLocation();
		}
		else
		{
			return getName();
		}
	}

	public boolean equals(Object obj)
	{
		if (obj instanceof Source)
		{
			Source s = (Source)obj;
			return s.owner == owner && s.getName().equals(getName()) && s.getRelativePath().equals(getRelativePath());
		}
		else
		{
			return false;
		}
	}

	public void close()
	{
		file.close();
	}

	/**
	 * make a copy of this Source object. dependencies are multinames in the clone.
	 */
	Source copy()
	{
		if (unit != null && unit.isDone())
		{
			// copying Source
			VirtualFile f = new InMemoryFile(unit.getByteCodes(), getName(), MimeMappings.ABC, fileTime);
			Source s = new Source(f, pathRoot, relativePath, shortName, owner, isInternal, isRoot, isDebuggable);
			s.fileIncludes.addAll(fileIncludes);
			s.fileIncludeTimes.putAll(fileIncludeTimes);
			s.setPathResolver(this.getPathResolver());

			s.logger = logger;

			// copying CompilationUnit
			CompilationUnit u = s.newCompilationUnit(null, new flex2.compiler.Context());

			u.topLevelDefinitions.addAll(unit.topLevelDefinitions);
			u.inheritance.addAll(unit.inheritanceHistory.keySet());
			u.types.addAll(unit.typeHistory.keySet());
			u.namespaces.addAll(unit.namespaceHistory.keySet());
			u.expressions.addAll(unit.expressionHistory.keySet());
            u.setSignatureChecksum(unit.getSignatureChecksum());

			copyMetaData(unit, u);

			return s;
		}
		else
		{
			return null;
		}
	}

	/**
	 * These properties in CompilationUnit are derived from AS3 metadata. SyntaxTreeEvaluator is responsible for
	 * extracting relevant AS3 metadata and put them in these properties. However, SyntaxTreeEvaluator may not run
	 * during incremental compilation, that's why these properties must be copied over.
	 */
	static void copyMetaData(CompilationUnit oldUnit, CompilationUnit newUnit)
	{
		newUnit.getAssets().addAll(oldUnit.getAssets());

		newUnit.auxGenerateInfo = oldUnit.auxGenerateInfo;

		if (oldUnit.typeInfo != null)
		{
			// There is no need to persist these properties because they exists as AS3 metadata in abc[], which
			// PersistenceStore always persists.
			newUnit.styles = oldUnit.styles;
			newUnit.typeInfo = oldUnit.typeInfo;
			newUnit.hasTypeInfo = newUnit.typeInfo != null;
			newUnit.classTable.putAll(oldUnit.classTable);
			newUnit.swfMetaData = oldUnit.swfMetaData;
			newUnit.iconFile = oldUnit.iconFile;
			newUnit.loaderClass = oldUnit.loaderClass;
			newUnit.loaderClassBase = oldUnit.loaderClassBase;
			newUnit.extraClasses.addAll(oldUnit.extraClasses);
			newUnit.addAccessibilityClasses(oldUnit);
			newUnit.licensedClassReqs.putAll(oldUnit.licensedClassReqs);
			newUnit.remoteClassAliases.putAll(oldUnit.remoteClassAliases);
            newUnit.effectTriggers.putAll(oldUnit.effectTriggers);
            newUnit.mixins.addAll(oldUnit.mixins);
			newUnit.resourceBundleHistory.addAll(oldUnit.resourceBundleHistory);
		}
	}

	/**
	 * Creates a Source object, given a VirtualFile
	 */
	static Source newSource(VirtualFile f, long fileTime, VirtualFile pathRoot, String relativePath, String shortName, Object owner,
							boolean isInternal, boolean isRoot, boolean isDebuggable, Set includes, Map includeTimes,
							LocalLogger logger)
	{
		Source s = new Source(f, pathRoot, relativePath, shortName, owner, isInternal, isRoot, isDebuggable);
		s.fileTime = fileTime;
		s.fileIncludes.addAll(includes);
		s.fileIncludeTimes.putAll(includeTimes);
		s.logger = logger;

		return s;
	}

	/**
	 * Creates a Source object, given an abc[]
	 */
	static Source newSource(byte[] abc, String name, long fileTime, VirtualFile pathRoot, String relativePath, String shortName, Object owner,
							boolean isInternal, boolean isRoot, boolean isDebuggable, Set includes, Map includeTimes,
							LocalLogger logger)
	{
		VirtualFile f = new InMemoryFile(abc, name, MimeMappings.ABC, fileTime);
		return newSource(f, fileTime, pathRoot, relativePath, shortName, owner, isInternal, isRoot, isDebuggable,
		                 includes, includeTimes, logger);
	}

	/**
	 * Populates a Source object.
	 */
	static Source populateSource(Source s, long fileTime, VirtualFile pathRoot, String relativePath, String shortName, Object owner,
								 boolean isInternal, boolean isRoot, boolean isDebuggable, Set includes, Map includeTimes,
								 LocalLogger logger)
	{
		assert s != null;

		s.fileTime = fileTime;
		s.pathRoot = pathRoot;
		s.relativePath = relativePath;
		s.shortName = shortName;
		s.owner = owner;
		s.isInternal = isInternal;
		s.isRoot = isRoot;
		s.isDebuggable = isDebuggable;
		s.fileIncludes.addAll(includes);
		s.fileIncludeTimes.putAll(includeTimes);
		s.logger = logger;

		return s;
	}

	public String toString()
	{
		return getName();
	}

	public static void transferDefinitions(CompilationUnit from, CompilationUnit to)
	{
		to.topLevelDefinitions.addAll(from.topLevelDefinitions);
	}

	public static void transferTypeInfo(CompilationUnit from, CompilationUnit to)
	{
		to.typeInfo = from.typeInfo;
	}

	public static void clearDependencies(CompilationUnit unit)
	{
		unit.inheritance.clear();
		unit.types.clear();
		unit.expressions.clear();
		unit.namespaces.clear();
		unit.importPackageStatements.clear();
		unit.importDefinitionStatements.clear();
	}

	public static void transferInheritance(CompilationUnit from, CompilationUnit to)
	{
		to.inheritance.clear();
		to.inheritance.addAll(from.inheritance);
		to.inheritanceHistory.putAll(from.inheritanceHistory);
	}

	public static void transferDependencies(CompilationUnit from, CompilationUnit to)
	{
		clearDependencies(to);

		to.inheritance.addAll(from.inheritance);
		to.types.addAll(from.types);
		to.expressions.addAll(from.expressions);
		to.namespaces.addAll(from.namespaces);
		to.importPackageStatements.addAll(from.importPackageStatements);
		to.importDefinitionStatements.addAll(from.importDefinitionStatements);

		to.inheritanceHistory.putAll(from.inheritanceHistory);
		to.typeHistory.putAll(from.typeHistory);
		to.expressionHistory.putAll(from.expressionHistory);
		to.namespaceHistory.putAll(from.namespaceHistory);
	}

	public static void transferNamespaces(CompilationUnit from, CompilationUnit to)
	{
		to.namespaces.clear();
		to.namespaces.addAll(from.namespaces);
		to.namespaceHistory.putAll(from.namespaceHistory);
	}

	public static void transferExpressions(CompilationUnit from, CompilationUnit to)
	{
		to.expressions.clear();
		to.expressions.addAll(from.expressions);
		to.expressionHistory.putAll(from.expressionHistory);
	}

	public static void transferAssets(CompilationUnit from, CompilationUnit to)
	{
		to.getAssets().addAll(from.getAssets());
	}

	public static void transferMetaData(CompilationUnit from, CompilationUnit to)
	{
		to.metadata.addAll(from.metadata);
		to.swfMetaData = from.swfMetaData;
		to.iconFile = from.iconFile;
		to.loaderClass = from.loaderClass;
		to.extraClasses.addAll( from.extraClasses );
		to.addAccessibilityClasses(from);
		to.licensedClassReqs.putAll( from.licensedClassReqs );
		to.remoteClassAliases.putAll(from.remoteClassAliases);
		to.effectTriggers.putAll(from.effectTriggers);
		to.mixins.addAll(from.mixins);
		to.resourceBundles.addAll(from.resourceBundles);
		to.resourceBundleHistory.addAll(from.resourceBundleHistory);
        to.setSignatureChecksum(from.getSignatureChecksum());
	}

	public static void transferGeneratedSources(CompilationUnit from, CompilationUnit to)
	{
		to.addGeneratedSources(from.getGeneratedSources());
		from.clearGeneratedSources();
	}

	public static void transferClassTable(CompilationUnit from, CompilationUnit to)
	{
		to.classTable.putAll(from.classTable);
	}

	public static  void transferLoaderClassBase(CompilationUnit from, CompilationUnit to)
	{
		to.loaderClassBase = from.loaderClassBase;
	}

	public static void transferBytecodes(CompilationUnit from, CompilationUnit to)
	{
		to.bytes.clear();
		to.bytes.set(from.bytes.toByteArray(false), from.bytes.size());
		to.getSource().lineCount = from.getSource().lineCount;
	}

	public static void transferStyles(CompilationUnit from, CompilationUnit to)
	{
		to.styles = from.styles;
	}
}
