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

import flash.fonts.FontManager;
import flash.localization.LocalizationManager;
import flash.swf.*;
import flash.swf.tags.DefineFont;
import flash.swf.tags.DefineTag;
import flash.swf.types.Rect;
import flex2.compiler.common.Configuration;
import flex2.compiler.common.PathResolver;
import flex2.compiler.io.DeletedFile;
import flex2.compiler.io.FileUtil;
import flex2.compiler.io.ResourceFile;
import flex2.compiler.io.VirtualFile;
import flex2.compiler.util.*;
import flex2.compiler.ResourceBundlePath;

import java.io.*;
import java.util.*;
import java.util.Map.Entry;

/**
 * @author Clement Wong
 */
final class PersistenceStore
{
	// C: If you update the encoding/decoding algorithm, please increment the minor version by 1. Thanks.
	private static final int major_version = 3;
	private static final int minor_version = 15;

	PersistenceStore(Configuration configuration, RandomAccessFile file)
	{
        this(configuration, file, null);
	}

    PersistenceStore(Configuration configuration, RandomAccessFile file, FontManager fontManager)
    {
        assert file != null;

        this.file = file;
        key = new ArrayKey();

        this.fontManager = fontManager;
        this.configuration  = configuration;
    }

    private final Configuration configuration;
	private final RandomAccessFile file;
	private final ArrayKey key;
    private final FontManager fontManager;

	int write(FileSpec fileSpec,
			  SourceList sourceList,
			  SourcePath sourcePath,
			  ResourceContainer resources,
	          ResourceBundlePath bundlePath,
	          List sources,
	          List units,
	          int checksum,
	          int cmd_checksum,
	          int linker_checksum,
	          int swc_checksum,
	          Map swcDefSignatureChecksums,
	          Map swcFileChecksums,
	          String description,
	          Map archiveFiles) throws IOException
	{
		Map pool = new HashMap();

		ByteArrayOutputStream fs = new ByteArrayOutputStream();
		ByteArrayOutputStream sl = new ByteArrayOutputStream();
		ByteArrayOutputStream sp = new ByteArrayOutputStream();
		ByteArrayOutputStream rbp = new ByteArrayOutputStream();
		ByteArrayOutputStream src = new ByteArrayOutputStream();
		ByteArrayOutputStream cu = new ByteArrayOutputStream();
		ByteArrayOutputStream cp = new ByteArrayOutputStream();
		ByteArrayOutputStream h = new ByteArrayOutputStream();
		ByteArrayOutputStream s = new ByteArrayOutputStream();
		ByteArrayOutputStream cs1 = new ByteArrayOutputStream();
        ByteArrayOutputStream cs2 = new ByteArrayOutputStream();
        ByteArrayOutputStream af = new ByteArrayOutputStream();

		writeHeader(checksum, cmd_checksum, linker_checksum, swc_checksum, description, h);
		if (fileSpec != null)
		{
			writeFileSpec(fileSpec, pool, fs);
		}
		if (sourceList != null)
		{
			writeSourceList(sourceList, pool, sl);
		}
		if (sourcePath != null)
		{
			writeSourcePath(sourcePath, pool, sp);
		}
		// C: There is no need to have writeResourceContainer() because it has nothing we need to persist.
		// writeResourceContainer(resources, pool);
		if (bundlePath != null)
		{
			writeResourceBundlePath(bundlePath, pool, rbp);
		}

		int count = (sources == null) ? 0 : sources.size();
		if (sources != null)
		{
			writeSourceNames(sources, pool, s);
		}
		
		Collection c1 = fileSpec == null ? Collections.EMPTY_LIST : fileSpec.sources();
		Collection c2 = sourceList == null ? Collections.EMPTY_LIST : sourceList.sources();
		Map c3 = sourcePath == null ? Collections.EMPTY_MAP : sourcePath.sources();
		Collection c4 = resources == null ? Collections.EMPTY_LIST : resources.sources();
		Map c5 = bundlePath == null ? Collections.EMPTY_MAP : bundlePath.sources();

		int totalCount = c1.size() + c2.size() + c3.size() + c4.size() + c5.size();

		writeCompilationUnits(c1, pool, src, cu);
		writeCompilationUnits(c2, pool, src, cu);
		writeCompilationUnits(c3.values(), pool, src, cu);
		writeCompilationUnits(c5.values(), pool, src, cu);
		writeCompilationUnits(c4, pool, src, cu);

		if (swcDefSignatureChecksums != null)
		{
			writeSwcDefSignatureChecksums(swcDefSignatureChecksums, pool, cs1);
		}
		
		if (swcFileChecksums != null)
		{
			writeFileChecksums(swcFileChecksums, pool, cs2);
		}
		
		if (archiveFiles != null)
		{
		    writeFileChecksums(archiveFiles, pool, af);
		}
				
		writeConstantPool(pool, cp);

		file.write(h.toByteArray());

		file.writeInt(cp.size());
		file.write(cp.toByteArray());
		
		file.writeInt(fs.size());
		file.write(fs.toByteArray());

		file.writeInt(sl.size());
		file.write(sl.toByteArray());

		file.writeInt(sp.size());
		file.write(sp.toByteArray());

		file.writeInt(rbp.size());
		file.write(rbp.toByteArray());

		file.writeInt(count);
		file.write(s.toByteArray());

		file.writeInt(swcDefSignatureChecksums == null ? 0 : swcDefSignatureChecksums.size());
		file.write(cs1.toByteArray());

		file.writeInt(swcFileChecksums == null ? 0 : swcFileChecksums.size());
		file.write(cs2.toByteArray());

        file.writeInt(archiveFiles == null ? 0 : archiveFiles.size());
        file.write(af.toByteArray());

		file.writeInt(totalCount);
		file.writeInt(src.size());
		file.writeInt(cu.size());

		file.write(src.toByteArray());
		file.write(cu.toByteArray());

		return totalCount;
	}

	private void writeSwcDefSignatureChecksums(Map swcDefSignatureChecksums,
											   Map pool,
											   OutputStream cs1) throws IOException
	{
		for (Iterator i = swcDefSignatureChecksums.keySet().iterator(); i.hasNext(); )
		{
			QName qName = (QName) i.next();
			Long ts = (Long) swcDefSignatureChecksums.get(qName);
			writeU32(cs1, addQName(pool, qName));
			writeLong(cs1, ts.longValue());
		}
	}

    private void writeFileChecksums(Map m, Map pool, OutputStream os) throws IOException
    {
        for (Iterator i = m.keySet().iterator(); i.hasNext();)
        {
            String fileName = (String) i.next();
            Long ts = (Long) m.get(fileName);
            writeU32(os, addString(pool, fileName));
            writeLong(os, ts.longValue());
        }        
    }

	private void writeHeader(int checksum,
							 int cmd_checksum,
							 int linker_checksum,
							 int swc_checksum,
							 String description,
							 OutputStream out) throws IOException
	{
		writeU32(out, major_version); // major version
		writeU32(out, minor_version); // minor version

		writeU32(out, checksum); // checksum
		writeU32(out, cmd_checksum);
		writeU32(out, linker_checksum);
		writeU32(out, swc_checksum);

		byte[] b = description.getBytes("UTF8");
		writeU32(out, b.length);
		writeBytes(out, b);
	}

	private void writeConstantPool(Map pool, OutputStream out) throws IOException
	{
        TreeMap map = new TreeMap();

        for (Iterator i = pool.keySet().iterator(); i.hasNext();)
        {
            Object key = i.next();
            Object value = pool.get(key);
            map.put(value, key);
        }

        writeU32(out, map.size());

        for (Iterator i = map.keySet().iterator(); i.hasNext();)
        {
            Object value = map.get(i.next());

			if (value instanceof String)
			{
				writeU8(out, 1);

				byte[] b = ((String) value).getBytes("UTF8");
				writeU32(out, b.length);
				writeBytes(out, b);
			}
			else if (value instanceof ArrayKey)
			{
				writeU8(out, 2);

				String[] a = ((ArrayKey) value).a1;
				writeU32(out, a == null ? 0 : a.length);

				for (int j = 0, size = (a == null) ? 0 : a.length; j < size; j++)
				{
					writeU32(out, addString(pool, a[j]));
				}
			}
			else if (value instanceof byte[])
			{
				writeU8(out, 3);

				byte[] b = (byte[]) value;
				writeU32(out, b.length);
				if (b.length > 0)
				{
					writeBytes(out, b);
				}
			}
			else if (value instanceof QName)
			{
				writeU8(out, 4);

				QName qName = (QName) value;
				writeU32(out, addString(pool, qName.getNamespace()));
				writeU32(out, addString(pool, qName.getLocalPart()));
			}
			else if (value instanceof MultiName)
			{
				writeU8(out, 5);

				MultiName multiName = (MultiName) value;
				writeU32(out, addStrings(pool, multiName.namespaceURI));
				writeU32(out, addString(pool, multiName.localPart));
			}
			else if (value instanceof flex2.compiler.abc.MetaData)
			{
				writeU8(out, 6);

				flex2.compiler.abc.MetaData md = (flex2.compiler.abc.MetaData) value;
				writeU32(out, addString(pool, md.getID()));
				writeU32(out, md.count());
				for (int j = 0, count = md.count(); j < count; j++)
				{
					String key = md.getKey(j);
					String val = md.getValue(j);

					writeU32(out, addString(pool, key == null ? "" : key));
					writeU32(out, addString(pool, val));
				}
			}
			else
			{
				assert false;
			}
		}
	}

	private void writeSourceNames(List sources, Map pool, OutputStream out) throws IOException
	{
		for (int i = 0, size = sources.size(); i < size; i++)
		{
			Source s = (Source) sources.get(i);
			if (s != null)
			{
				writeU32(out, addString(pool, s.getName()));
				if (s.isFileSpecOwner())
				{
					writeU8(out, 0);
				}
				else if (s.isSourceListOwner())
				{
					writeU8(out, 1);
				}
				else if (s.isSourcePathOwner())
				{
					writeU8(out, 2);
				}
				else if (s.isResourceContainerOwner())
				{
					writeU8(out, 3);
				}
				else if (s.isResourceBundlePathOwner())
				{
					writeU8(out, 4);
				}
				else // if (s.isSwcScriptOwner())
				{
					writeU8(out, 5);
				}
			}
			else
			{
				writeU32(out, addString(pool, "null"));
			}
		}
	}

	private void writeFileSpec(FileSpec fileSpec, Map pool, OutputStream fs) throws IOException
	{
		String[] mimeTypes = fileSpec.getMimeTypes();
		Collection sources = fileSpec.sources();

		writeU32(fs, mimeTypes.length);

		for (int i = 0, length = mimeTypes.length; i < length; i++)
		{
			writeU32(fs, addString(pool, mimeTypes[i]));
		}

		writeU32(fs, sources.size());

		for (Iterator i = sources.iterator(); i.hasNext();)
		{
			Source s = (Source) i.next();
			writeU32(fs, addString(pool, s.getName()));
		}
	}

	private void writeSourceList(SourceList sourceList, Map pool, OutputStream sl) throws IOException
	{
		String[] mimeTypes = sourceList.getMimeTypes();
		List paths = sourceList.getPaths();
		Collection sources = sourceList.sources();

		writeU32(sl, mimeTypes.length);

		for (int i = 0, length = mimeTypes.length; i < length; i++)
		{
			writeU32(sl, addString(pool, mimeTypes[i]));
		}

		writeU32(sl, paths.size());

		for (int i = 0, length = paths.size(); i < length; i++)
		{
			writeU32(sl, addString(pool, FileUtil.getCanonicalPath((File) paths.get(i))));
		}

		writeU32(sl, sources.size());

		for (Iterator i = sources.iterator(); i.hasNext();)
		{
			Source s = (Source) i.next();
			writeU32(sl, addString(pool, s.getName()));
		}
	}

	private void writeSourcePath(SourcePath sourcePath, Map pool, OutputStream sp) throws IOException
	{
		String[] mimeTypes = sourcePath.getMimeTypes();
		List paths = sourcePath.getPaths();
		Map sources = sourcePath.sources();

		writeU32(sp, mimeTypes.length);

		for (int i = 0, length = mimeTypes.length; i < length; i++)
		{
			writeU32(sp, addString(pool, mimeTypes[i]));
		}

		writeU32(sp, paths.size());

		for (int i = 0, length = paths.size(); i < length; i++)
		{
			writeU32(sp, addString(pool, FileUtil.getCanonicalPath((File) paths.get(i))));
		}

		writeU32(sp, sources.size());

		for (Iterator i = sources.keySet().iterator(); i.hasNext();)
		{
			String className = (String) i.next();
			Source s = (Source) sources.get(className);

			writeU32(sp, addString(pool, className));
			writeU32(sp, addString(pool, s.getName()));
		}
	}

	private void writeResourceBundlePath(ResourceBundlePath bundlePath, Map pool, OutputStream sp) throws IOException
	{
		String[] mimeTypes = bundlePath.getMimeTypes();
		String[] locales = bundlePath.getLocales();
		Map rbDirectories = bundlePath.getResourceBundlePaths();
		Map sources = bundlePath.sources();

		writeU32(sp, mimeTypes.length);

		for (int i = 0, length = mimeTypes.length; i < length; i++)
		{
			writeU32(sp, addString(pool, mimeTypes[i]));
		}
		
		writeU32(sp, locales == null ? 0 : locales.length);

		for (int i = 0, length = locales == null ? 0 : locales.length; i < length; i++)
		{
			writeU32(sp, addString(pool, locales[i]));
			
			List paths = (List) rbDirectories.get(locales[i]);
			
			writeU32(sp, paths == null ? 0 : paths.size());

			for (int j = 0, size = paths.size(); j < size; j++)
			{
				writeU32(sp, addString(pool, FileUtil.getCanonicalPath((File) paths.get(j))));
			}
		}

		writeU32(sp, sources.size());

		for (Iterator i = sources.keySet().iterator(); i.hasNext();)
		{
			String bundleName = (String) i.next();
			Source s = (Source) sources.get(bundleName);

			writeU32(sp, addString(pool, bundleName));
			writeU32(sp, addString(pool, s.getName()));
			
			ResourceFile rf = (ResourceFile) s.getBackingFile();
			VirtualFile[] rFiles = rf.getResourceFiles();
			VirtualFile[] rRoots = rf.getResourcePathRoots();
			
			writeU32(sp, rFiles.length);			
			for (int j = 0, size = rFiles.length; j < size; j++)
			{
				writeU32(sp, addString(pool, rFiles[j] != null ? rFiles[j].getName() : "null"));
				writeU32(sp, addString(pool, rRoots[j] != null ? rRoots[j].getName() : "null"));
			}
		}
	}

	private void writeCompilationUnits(Collection sources, Map pool, OutputStream src, OutputStream cu) throws IOException
	{
		for (Iterator i = sources.iterator(); i.hasNext();)
        {
			Source s = (Source) i.next();
			writeSource(s, pool, src);

			CompilationUnit u = s.getCompilationUnit();
			if (u != null)
			{
				writeCompilationUnit(u, pool, cu);
			}
		}
	}

	private void writeSource(Source s, Map pool, OutputStream src) throws IOException
	{
	    final CompilationUnit unit = s.getCompilationUnit();
	    final boolean hasUnit = (unit != null);

        writeU32(src, addString(pool, s.getName()));
		writeU32(src, addString(pool, s.getRelativePath()));
		writeU32(src, addString(pool, s.getShortName()));
		if (s.isFileSpecOwner())
		{
			writeU8(src, 0);
		}
		else if (s.isSourceListOwner())
		{
			writeU8(src, 1);
			writeU32(src, addString(pool, s.getPathRoot().getName()));
		}
		else if (s.isSourcePathOwner())
		{
			writeU8(src, 2);
			writeU32(src, addString(pool, s.getPathRoot().getName()));
		}
		else if (s.isResourceContainerOwner())
		{
			writeU8(src, 3);
		}
		else //if (s.isResourceBundlePathOwner())
		{
			writeU8(src, 4);
			writeU32(src, addString(pool, s.getPathRoot().getName()));
		}

		writeU8(src, s.isInternal() ? 1 : 0);
		writeU8(src, s.isRoot() ? 1 : 0);
		writeU8(src, s.isDebuggable() ? 1 : 0);
        writeU8(src, hasUnit ? 1 : 0);
		writeLong(src, s.getFileTime());

        // signatures
        {
    		final boolean hasSignatureChecksum = hasUnit && unit.hasSignatureChecksum();
            writeU8(src, (hasSignatureChecksum ? 1 : 0));
            if (hasSignatureChecksum)
            {
                final Long signatureChecksum = unit.getSignatureChecksum();
                writeLong(src, signatureChecksum.longValue());
                // SignatureExtension.debug("WRITE     CRC32: " + signatureChecksum + "\t--> " + s.getName());
            }
        }

		writeU32(src, s.getFileIncludeSize());
		for (Iterator j = s.getFileIncludes(); j.hasNext();)
		{
			VirtualFile f = (VirtualFile) j.next();

			writeU32(src, addString(pool, f.getName()));
			writeLong(src, s.getFileIncludeTime(f));
		}

		List warnings = null;
		if (s.getLogger() != null && (warnings = s.getLogger().getWarnings()) != null)
		{
			writeU32(src, warnings.size());
		}
		else
		{
			writeU32(src, 0);
		}

		for (int i = 0, size = warnings == null ? 0 : warnings.size(); i < size; i++)
		{
			LocalLogger.Warning w = (LocalLogger.Warning) warnings.get(i);
			writeU32(src, addString(pool, w.path == null ? "" : w.path));
			writeU32(src, addString(pool, w.warning == null ? "" : w.warning));
			writeU32(src, addString(pool, w.source == null ? "" : w.source));
			writeU32(src, w.line == null ? -1 : w.line.intValue());
			writeU32(src, w.col == null ? -1 : w.col.intValue());
			writeU32(src, w.errorCode == null ? -1 : w.errorCode.intValue());
		}
	}

	private void writeCompilationUnit(CompilationUnit u, Map pool, OutputStream cu) throws IOException
	{
		writeU32(cu, addBytes(pool, u.bytes.toByteArray()));
		writeU32(cu, u.getWorkflow());
		writeU32(cu, u.getState());

		writeU32(cu, u.topLevelDefinitions.size());

		for (Iterator i = u.topLevelDefinitions.iterator(); i.hasNext();)
		{
			writeU32(cu, addQName(pool, (QName) i.next()));
		}

		writeU32(cu, u.inheritanceHistory.size());

		for (Iterator i = u.inheritanceHistory.keySet().iterator(); i.hasNext();)
		{
			MultiName multiName = (MultiName) i.next();
			QName qName = (QName) u.inheritanceHistory.get(multiName);

			writeU32(cu, addMultiName(pool, multiName));
			writeU32(cu, addQName(pool, qName));
		}

		writeU32(cu, u.typeHistory.size());

		for (Iterator i = u.typeHistory.keySet().iterator(); i.hasNext();)
		{
			MultiName multiName = (MultiName) i.next();
			QName qName = (QName) u.typeHistory.get(multiName);

			writeU32(cu, addMultiName(pool, multiName));
			writeU32(cu, addQName(pool, qName));
		}

		writeU32(cu, u.namespaceHistory.size());

		for (Iterator i = u.namespaceHistory.keySet().iterator(); i.hasNext();)
		{
			MultiName multiName = (MultiName) i.next();
			QName qName = (QName) u.namespaceHistory.get(multiName);

			writeU32(cu, addMultiName(pool, multiName));
			writeU32(cu, addQName(pool, qName));
		}

		writeU32(cu, u.expressionHistory.size());

		for (Iterator i = u.expressionHistory.keySet().iterator(); i.hasNext();)
		{
			MultiName multiName = (MultiName) i.next();
			QName qName = (QName) u.expressionHistory.get(multiName);

			writeU32(cu, addMultiName(pool, multiName));
			writeU32(cu, addQName(pool, qName));
		}

		if (u.auxGenerateInfo != null && u.auxGenerateInfo.size() > 0)
		{
			writeU8(cu, 1);

			String baseLoaderClass = (String) u.auxGenerateInfo.get( "baseLoaderClass");
			writeU32(cu, addString(pool, baseLoaderClass == null ? "" : baseLoaderClass));

			String generateLoaderClass = (String) u.auxGenerateInfo.get( "generateLoaderClass");
			writeU32(cu, addString(pool, generateLoaderClass == null ? "" : generateLoaderClass));

			String className = (String) u.auxGenerateInfo.get( "windowClass");
			writeU32(cu, addString(pool, className == null ? "" : className));

			String preLoader = (String) u.auxGenerateInfo.get( "preloaderClass");
			writeU32(cu, addString(pool, preLoader == null ? "" : preLoader));

			Boolean usePreloader = (Boolean) u.auxGenerateInfo.get( "usePreloader");
			writeU8(cu, usePreloader.booleanValue() ? 1 : 0);

			Map rootAttributeMap = (Map) u.auxGenerateInfo.get( "rootAttributes");
			writeU32(cu, rootAttributeMap.size());

			for (Iterator i = rootAttributeMap.keySet().iterator(); i.hasNext();)
			{
				String key = (String) i.next();
				String value = (String) rootAttributeMap.get(key);

				if (value != null)
				{
					writeU32(cu, addString(pool, key));
					writeU32(cu, addString(pool, value));
				}
				else
				{
					assert false : key + " can't be null.";
				}
			}
		}
		else
		{
			writeU8(cu, 0);
		}

		writeAssets(u, pool, cu);
	}

	private void writeAssets(CompilationUnit u, Map pool, OutputStream cu) throws IOException
	{
		int size = u.getAssets().count();
		writeU32(cu, size);

		if (size > 0)
		{
			Movie movie = new Movie();
			movie.version = configuration.getTargetPlayerMajorVersion();
			assert movie.version >= 9;
			movie.size = new Rect(100 * 20, 100 * 20);
			movie.framerate = 12;

			Frame frame = new Frame();
			movie.frames = new ArrayList();
			movie.frames.add(frame);

			writeU32(cu, u.getAssets().count());

			for (Iterator i = u.getAssets().iterator(); i.hasNext();)
			{
				Entry entry = (Entry) i.next();
				String className = (String) entry.getKey();
				AssetInfo assetInfo = (AssetInfo) entry.getValue();
				
				writeU32(cu, addString(pool, className));
				if (assetInfo.getPath() != null)
				{
					writeU32(cu, addString(pool, assetInfo.getPath().getName()));
				}
				else
				{
					writeU32(cu, addString(pool, ""));
				}
				writeLong(cu, assetInfo.getCreationTime());

				DefineTag asset = assetInfo.getDefineTag();
				frame.addSymbolClass(className, asset);

				if (asset.name != null)
				{
					frame.addExport(asset);
				}
			}

			TagEncoder handler = new TagEncoder();
			MovieEncoder encoder = new MovieEncoder(handler);

			encoder.export(movie);

			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			handler.writeTo(baos);

			writeU32(cu, baos.size());
			writeBytes(cu, baos.toByteArray());
		}
	}
	
	int read(FileSpec fileSpec,
			 SourceList sourceList,
			 SourcePath sourcePath,
			 ResourceContainer resources,
	         ResourceBundlePath bundlePath,
	         List sources,
	         List units,
	         int[] checksums,
	         Map swcDefSignatureChecksums,
	         Map swcFileChecksums,
	         Map archiveFileChecksums) throws IOException
	{
		if (!readVersion())
		{
			LocalizationManager l10n = ThreadLocalToolkit.getLocalizationManager();
			throw new IOException(l10n.getLocalizedTextString(new ObsoleteCacheFileFormat()));
		}

		if (!readChecksum(checksums))
		{
			return -1;
		}

		Object[] pool = readConstantPool();

		if (!readFileSpec(pool, fileSpec))
		{
			return -2;
		}

		if (!readSourceList(pool, sourceList))
		{
			return -3;
		}

		if (!readSourcePath(pool, sourcePath))
		{
			return -4;
		}

		if (!readResourceBundlePath(pool, bundlePath))
		{
			return -5;
		}
		
		Map owners = new HashMap();
		if (!readSourceNames(pool, sources, units, owners))
		{
			return -6;
		}
		
		if (!readSwcDefSignatureChecksums(pool, swcDefSignatureChecksums))
		{
			return -7;
		}

		if (!readFileChecksums(pool, swcFileChecksums))
		{
			return -8;
		}

        if (!readFileChecksums(pool, archiveFileChecksums))
        {
            return -9;
        }

		int count = readCompilationUnits(pool, fileSpec, sourceList, sourcePath, resources, bundlePath,
										 sources, units, owners);
		resources.refresh();

		return count;
	}

	private boolean readVersion() throws IOException
	{
		int major = readU32(); // major version
		int minor = readU32(); // minor version

		return (major == major_version) && (minor == minor_version);
	}

	private boolean readChecksum(int[] checksums) throws IOException
	{
		int targetChecksum = readU32(); // checksum
		int targetCmdChecksum = readU32();
		int targetLinkerChecksum = readU32();
		int targetSwcChecksum = readU32();
		/* String description = */ new String(readBytes(readU32()));

		boolean result = checksums[1] == targetCmdChecksum;
		
		checksums[0] = targetChecksum;
		checksums[1] = targetCmdChecksum;
		checksums[2] = targetLinkerChecksum;
		checksums[3] = targetSwcChecksum;
		
		return result;
	}

	private Object[] readConstantPool() throws IOException
	{
		readU32(); // skip size

		Object[] pool = new Object[readU32()];

		for (int i = 0; i < pool.length; i++)
		{
			switch (readU8())
			{
			case 1: // String
				pool[i] = new String(readBytes(readU32()), "UTF8");
				break;
			case 2: // String[]
				String[] strings = new String[readU32()];
				for (int j = 0; j < strings.length; j++)
				{
					strings[j] = (String) pool[readU32()];
				}
				pool[i] = strings;
				break;
			case 3: // byte[]
				pool[i] = readBytes(readU32());
				break;
			case 4: // QName
				pool[i] = new QName((String) pool[readU32()], (String) pool[readU32()]);
				break;
			case 5: // MultiName
				pool[i] = new MultiName((String[]) pool[readU32()], (String) pool[readU32()]);
				break;
			case 6: // MetaData
				MetaData md = new MetaData((String) pool[readU32()], readU32());
				for (int j = 0; j < md.count(); j++)
				{
					String key = (String) pool[readU32()];
					if (key.length() > 0)
					{
						md.setKeyValue(j, key, (String) pool[readU32()]);
					}
					else
					{
						md.setValue(j, (String) pool[readU32()]);
					}
				}
				pool[i] = md;
				break;
			default:
				assert false;
			}
		}

		return pool;
	}

	private boolean readSourceNames(Object[] pool, List sources, List units, Map owners) throws IOException
	{
		int size = readU32();
		
		if (sources != null) sources.clear();
		if (units != null) units.clear();
		
		for (int i = 0; i < size; i++)
		{
			String name = (String) pool[readU32()];
			if (!"null".equals(name))
			{
				owners.put(name, new Integer(readU8()));
				if (sources != null) sources.add(name);
			}
			else
			{
				if (sources != null) sources.add(null);
			}
			if (units != null) units.add(null);
		}
		
		return true;
	}
	
	private boolean readSwcDefSignatureChecksums(Object[] pool, Map swcDefSignatureChecksums) throws IOException
	{
		int size = readU32();
		
		for (int i = 0; i < size; i++)
		{
			QName qName = (QName) pool[readU32()];
			long ts = readLong();
			if (swcDefSignatureChecksums != null) swcDefSignatureChecksums.put(qName, new Long(ts));
		}

		return true;
	}

    private boolean readFileChecksums(Object[] pool, Map m) throws IOException
    {
        int size = readU32();

        for (int i = 0; i < size; i++)
        {
            String fileName = (String) pool[readU32()];
            long ts = readLong();
            if (m != null) m.put(fileName, new Long(ts));
        }

        return true;
    }

	private boolean readFileSpec(Object[] pool, FileSpec fileSpec) throws IOException
	{
		int size = readU32();
		if (size > 0 && fileSpec == null)
		{
			LocalizationManager l10n = ThreadLocalToolkit.getLocalizationManager();
			throw new IOException(l10n.getLocalizedTextString(new NoFileSpec()));
		}
		else if (size == 0)
		{
			return true;
		}

		int length = readU32();
		String[] mimeTypes = new String[length];

		for (int i = 0; i < length; i++)
		{
			mimeTypes[i] = (String) pool[readU32()];
		}

		length = readU32();
		String[] sources = new String[length];

		for (int i = 0; i < length; i++)
		{
			sources[i] = (String) pool[readU32()];
		}

		// check FileSpec

		String[] targetMimeTypes = fileSpec.getMimeTypes();

		if ((length = targetMimeTypes.length) == mimeTypes.length)
		{
			for (int i = 0; i < length; i++)
			{
				if (!mimeTypes[i].equals(targetMimeTypes[i]))
				{
					return false;
				}
			}
		}
		else
		{
			return false;
		}

		Collection c = fileSpec.sources();

		if (c.size() == sources.length)
		{
			Iterator it = c.iterator();
			for (int i = 0; it.hasNext() && i < sources.length; i++)
			{
				Source s = (Source) it.next();
				if (!s.getName().equals(sources[i]))
				{
					return false;
				}
			}
		}
		else
		{
			return false;
		}

		return true;
	}

	private boolean readSourceList(Object[] pool, SourceList sourceList) throws IOException
	{
		int size = readU32();
		if (size > 0 && sourceList == null)
		{
			LocalizationManager l10n = ThreadLocalToolkit.getLocalizationManager();
			throw new IOException(l10n.getLocalizedTextString(new NoSourceList()));
		}
		else if (size == 0)
		{
			return true;
		}

		int length = readU32();
		String[] mimeTypes = new String[length];

		for (int i = 0; i < length; i++)
		{
			mimeTypes[i] = (String) pool[readU32()];
		}

		length = readU32();
		String[] paths = new String[length];

		// classpath
		for (int i = 0; i < length; i++)
		{
			paths[i] = (String) pool[readU32()];
		}

		length = readU32();
		String[] sources = new String[length];

		for (int i = 0; i < length; i++)
		{
			sources[i] = (String) pool[readU32()];
		}

		// check SourceList

		String[] targetMimeTypes = sourceList.getMimeTypes();

		if ((length = targetMimeTypes.length) == mimeTypes.length)
		{
			for (int i = 0; i < length; i++)
			{
				if (!mimeTypes[i].equals(targetMimeTypes[i]))
				{
					return false;
				}
			}
		}
		else
		{
			return false;
		}

		Collection c = sourceList.sources();

		if (c.size() == sources.length)
		{
			Iterator it = c.iterator();
			for (int i = 0; it.hasNext() && i < sources.length; i++)
			{
				Source s = (Source) it.next();
				if (!s.getName().equals(sources[i]))
				{
					return false;
				}
			}
		}
		else
		{
			return false;
		}

		return true;
	}

	private boolean readSourcePath(Object[] pool, SourcePath sourcePath) throws IOException
	{
		int size = readU32();
		if (size > 0 && sourcePath == null)
		{
			LocalizationManager l10n = ThreadLocalToolkit.getLocalizationManager();
			throw new IOException(l10n.getLocalizedTextString(new NoSourcePath()));
		}
		else if (size == 0)
		{
			return true;
		}

		int length = readU32();
		String[] mimeTypes = new String[length];

		for (int i = 0; i < length; i++)
		{
			mimeTypes[i] = (String) pool[readU32()];
		}

		length = readU32();
		String[] paths = new String[length];

		// classpath
		for (int i = 0; i < length; i++)
		{
			paths[i] = (String) pool[readU32()];
		}

		length = readU32();
		String[] sources = new String[length];
		String[] classNames = new String[length];

		// filename
		for (int i = 0; i < length; i++)
		{
			classNames[i] = (String) pool[readU32()];
			sources[i] = (String) pool[readU32()];
		}

		// check SourcePath

		String[] targetMimeTypes = sourcePath.getMimeTypes();

		if ((length = targetMimeTypes.length) == mimeTypes.length)
		{
			for (int i = 0; i < length; i++)
			{
				if (!mimeTypes[i].equals(targetMimeTypes[i]))
				{
					return false;
				}
			}
		}
		else
		{
			return false;
		}

		List targetPaths = sourcePath.getPaths();

		if ((length = targetPaths.size()) == paths.length)
		{
			for (int i = 0; i < length; i++)
			{
				if (!paths[i].equals(FileUtil.getCanonicalPath((File) targetPaths.get(i))))
				{
					return false;
				}
			}
		}
		else
		{
			return false;
		}

		Map c = sourcePath.sources();

		assert c.size() == 0;

		for (int i = 0; i < classNames.length; i++)
		{
			// C: the value should be a Source, not a String... It's sort of okay because readCompilationUnit()
			//    will replace the String...
			c.put(classNames[i], sources[i]);
		}

		return true;
	}

	private boolean readResourceBundlePath(Object[] pool, ResourceBundlePath bundlePath) throws IOException
	{
		int size = readU32();
		if (size > 0 && bundlePath == null)
		{
			LocalizationManager l10n = ThreadLocalToolkit.getLocalizationManager();
			throw new IOException(l10n.getLocalizedTextString(new NoSourcePath()));
		}
		else if (size == 0)
		{
			return true;
		}

		int length = readU32();
		String[] mimeTypes = new String[length];

		for (int i = 0; i < length; i++)
		{
			mimeTypes[i] = (String) pool[readU32()];
		}

		length = readU32();
		String[] locales = new String[length];
		Map rbDirectories = new HashMap();
		
		for (int i = 0; i < length; i++)
		{
			locales[i] = (String) pool[readU32()];
			
			size = readU32();
			String[] paths = new String[size];

			// classpath
			for (int j = 0; j < size; j++)
			{
				paths[j] = (String) pool[readU32()];
			}
			
			rbDirectories.put(locales[i], paths);
		}

		length = readU32();
		String[] bundleNames = new String[length];
		String[] sources = new String[length], list = null, list2 = null;
		Object[] rFiles = new Object[length], rRoots = new Object[length];

		// filename
		for (int i = 0; i < length; i++)
		{
			bundleNames[i] = (String) pool[readU32()];
			sources[i] = (String) pool[readU32()];
			
			size = readU32();
			rFiles[i] = list = new String[size];
			rRoots[i] = list2 = new String[size];
			
			for (int j = 0; j < size; j++)
			{
				list[j] = (String) pool[readU32()];
				if ("null".equals(list[j]))
				{
					list[j] = null;
				}
				
				list2[j] = (String) pool[readU32()];
				if ("null".equals(list2[j]))
				{
					list2[j] = null;
				}
			}
		}

		// check ResourceBundlePath

		String[] targetMimeTypes = bundlePath.getMimeTypes();

		if ((length = targetMimeTypes.length) == mimeTypes.length)
		{
			for (int i = 0; i < length; i++)
			{
				if (!mimeTypes[i].equals(targetMimeTypes[i]))
				{
					return false;
				}
			}
		}
		else
		{
			return false;
		}

		String[] targetLocales = bundlePath.getLocales();

		if ((length = targetLocales.length) == locales.length)
		{
			for (int i = 0; i < length; i++)
			{
				if (!locales[i].equals(targetLocales[i]))
				{
					return false;
				}
			}
		}
		else
		{
			return false;
		}

		Map targetRBDirectories = bundlePath.getResourceBundlePaths();
		
		if ((size = targetRBDirectories.size()) == rbDirectories.size())
		{
			for (int i = 0; i < size; i++)
			{
				List targetPaths = (List) targetRBDirectories.get(targetLocales[i]);
				String[] paths = (String[]) rbDirectories.get(locales[i]);
				
				if ((length = targetPaths.size()) == paths.length)
				{
					for (int j = 0; j < length; j++)
					{
						if (!paths[j].equals(FileUtil.getCanonicalPath((File) targetPaths.get(j))))
						{
							return false;
						}
					}
				}
				else
				{
					return false;
				}
			}
		}
		else
		{
			return false;
		}

		Map c = bundlePath.sources();

		assert c.size() == 0;

		for (int i = 0; i < bundleNames.length; i++)
		{
			// C: the value should be a Source, not an Object[]... It's sort of okay because readCompilationUnit()
			//    will replace the Object[]...
			c.put(bundleNames[i], new Object[] { sources[i], rFiles[i], rRoots[i] });
		}

		return true;
	}

	private int readCompilationUnits(Object[] pool, FileSpec fileSpec, SourceList sourceList, SourcePath sourcePath,
	                                 ResourceContainer resources, ResourceBundlePath bundlePath,
	                                 List sources, List units, Map owners) throws IOException
	{
		int src_count = readU32(), src_size = readU32(), cu_size = readU32();

		ByteArrayInputStream src_in = new ByteArrayInputStream(readBytes(src_size));
		ByteArrayInputStream cu_in = new ByteArrayInputStream(readBytes(cu_size));

		Map m = sourcePath.sources(), mappings = new HashMap(), rbMappings = new HashMap();
		
		for (Iterator i = m.keySet().iterator(); i.hasNext();)
		{
			String className = (String) i.next();
			String fileName = (String) m.get(className);

			mappings.put(fileName, className);
		}

		m.clear();

		m = bundlePath.sources();
		for (Iterator i = m.keySet().iterator(); i.hasNext();)
		{
			String className = (String) i.next();
			Object[] value = (Object[]) m.get(className);
			String fileName = (String) value[0];
			String[] rFiles = (String[]) value[1];
			String[] rRoots = (String[]) value[2];

			rbMappings.put(fileName, new Object[] { className, rFiles, rRoots });
		}

		m.clear();

		for (int i = 0; i < src_count; i++)
		{
			readCompilationUnit(pool, mappings, rbMappings, src_in, cu_in, fileSpec, sourceList, sourcePath, resources, bundlePath, owners);
		}
		
		for (int i = 0, len = sources == null ? 0 : sources.size(); i < len; i++)
		{
			String n = (String) sources.get(i);
			Object obj = owners.get(n);
			if (obj instanceof Source)
			{
				Source s = (Source) obj;
				sources.set(i, s);
				units.set(i, s.getCompilationUnit());
			}
		}

		return src_count;
	}

	private void readCompilationUnit(Object[] pool, Map mappings, Map rbMappings, InputStream src_in, InputStream cu_in,
	                                 FileSpec fileSpec, SourceList sourceList, SourcePath sourcePath,
	                                 ResourceContainer resources, ResourceBundlePath bundlePath, Map owners)
	    throws IOException
	{
		PathResolver resolver = ThreadLocalToolkit.getPathResolver();

		String name = (String) pool[readU32(src_in)];
		String relativePath = (String) pool[readU32(src_in)];
		String shortName = (String) pool[readU32(src_in)];
		int owner = readU8(src_in);

		VirtualFile pathRoot = null;
		// 1 == SourceList
		// 2 == SourcePath
		// 4 == ResourceBundlePath
		if ((owner == 1 ) || (owner == 2) || (owner == 4))
		{
			// C: Unfortunately, PathResolver itself is not a complete solution. For each type
			//    of VirtualFile, there must be a mechanism to recognize the name format and
			//    construct an appropriate VirtualFile instance.
			pathRoot = resolver.resolve((String) pool[readU32(src_in)]);
		}

		boolean isInternal = (readU8(src_in) == 1);
		boolean isRoot = (readU8(src_in) == 1);
		boolean isDebuggable = (readU8(src_in) == 1);
		boolean hasUnit = (readU8(src_in) == 1);
		long fileTime = readLong(src_in);
        
        final boolean hasSignatureChecksum = (readU8(src_in) == 1);
        Long signatureChecksum = null;
        if (hasSignatureChecksum)
        {
            assert hasUnit;
            signatureChecksum = new Long(readLong(src_in));
            // SignatureExtension.debug("READ      CRC32: " + signatureChecksum + "\t--> " + name);
        }

		int size = readU32(src_in);
		Set includes = new HashSet(1); // Set<VirtualFile>
		Map includeTimes = new HashMap(1); // Map<VirtualFile, Long>

		for (int i = 0; i < size; i++)
		{
			String fileName = (String) pool[readU32(src_in)];
			VirtualFile f = resolver.resolve(fileName);
			long ts = readLong(src_in);

			if (f == null)
			{
				// C: create an instance of DeletedFile...
				f = new DeletedFile(fileName);
			}

			includes.add(f);
			includeTimes.put(f, new Long(ts));
		}

		size = readU32(src_in);
		LocalLogger logger = size == 0 ? null : new LocalLogger(null);

		for (int i = 0; i < size; i++)
		{
			String path = (String) pool[readU32(src_in)];
			if (path.length() == 0)
			{
				path = null;
			}
			String warning = (String) pool[readU32(src_in)];
			if (warning.length() == 0)
			{
				warning = null;
			}
			String source = (String) pool[readU32(src_in)];
			if (source.length() == 0)
			{
				source = null;
			}
			int line = readU32(src_in);
			int col = readU32(src_in);
			int errorCode = readU32(src_in);

			logger.recordWarning(path,
			                     line == -1 ? null : IntegerPool.getNumber(line),
			                     col == -1 ? null : IntegerPool.getNumber(col),
			                     warning,
			                     source,
			                     errorCode == -1 ? null : IntegerPool.getNumber(errorCode));
		}


		byte[] abc = (hasUnit) ? (byte[]) pool[readU32(cu_in)] : null;
		Source s = null;

		if (owner == 0) // FileSpec
		{
			Collection c = fileSpec.sources();
			for (Iterator i = c.iterator(); i.hasNext();)
			{
				s = (Source) i.next();
				if (s.getName().equals(name))
				{
					Source.populateSource(s, fileTime, pathRoot, relativePath, shortName, fileSpec, isInternal, isRoot, isDebuggable,
					                      includes, includeTimes, logger);
					break;
				}
			}
		}
		else if (owner == 1) // SourceList
		{
			Collection c = sourceList.sources();
			for (Iterator i = c.iterator(); i.hasNext();)
			{
				s = (Source) i.next();
				if (s.getName().equals(name))
				{
					Source.populateSource(s, fileTime, pathRoot, relativePath, shortName, sourceList, isInternal, isRoot, isDebuggable,
					                      includes, includeTimes, logger);
					break;
				}
			}
		}
		else if (owner == 2) // SourcePath
		{
			Map c = sourcePath.sources();
			String className = (String) mappings.get(name);

			if (className != null)
			{
				VirtualFile f = resolver.resolve(name);

				if (f == null)
				{
					f = new DeletedFile(name);
				}

				s = Source.newSource(f, fileTime, pathRoot, relativePath, shortName, sourcePath, isInternal, isRoot, isDebuggable,
				                     includes, includeTimes, logger);
				c.put(className, s);
			}
			else
			{
				assert false : name;
			}
		}
		else if (owner == 3) // ResourceContainer
		{
			if (resources == null)
			{
				LocalizationManager l10n = ThreadLocalToolkit.getLocalizationManager();
				throw new IOException(l10n.getLocalizedTextString(new NoResourceContainer()));
			}

			s = Source.newSource(abc, name, fileTime, pathRoot, relativePath, shortName, resources, isInternal, isRoot, isDebuggable,
			                     includes, includeTimes, logger);
			resources.addResource(s);
		}
		else // if (owner == 4) // ResourceBundlePath
		{
			Map c = bundlePath.sources();
			Object[] value = (Object[]) rbMappings.get(name);
			String bundleName = (String) value[0];
			String[] rNames = (String[]) value[1];
			String[] rRoots = (String[]) value[2];

			if (bundleName != null)
			{
				VirtualFile[] rFiles = new VirtualFile[rNames.length];

				for (int i = 0; i < rFiles.length; i++)
				{
					if (rNames[i] != null)
					{
						rFiles[i] = resolver.resolve(rNames[i]);
						if (rFiles[i] == null)
						{
							rFiles[i] = new DeletedFile(rNames[i]);
						}
					}
				}

				VirtualFile[] rRootFiles = new VirtualFile[rRoots.length];

				for (int i = 0; i < rRootFiles.length; i++)
				{
					if (rRoots[i] != null)
					{
						rRootFiles[i] = resolver.resolve(rRoots[i]);
						if (rRootFiles[i] == null)
						{
							rRootFiles[i] = new DeletedFile(rRoots[i]);
						}
					}
				}

				VirtualFile f = new ResourceFile(name, bundlePath.getLocales(), rFiles, rRootFiles);
				s = Source.newSource(f, fileTime, pathRoot, relativePath, shortName, bundlePath, isInternal, isRoot, isDebuggable,
				                     includes, includeTimes, logger);
				c.put(bundleName, s);
			}
			else
			{
				assert false : name;
			}
		}

		if (logger != null)
		{
			logger.setSource(s);
		}

		if (hasUnit)
		{
			CompilationUnit u = s.newCompilationUnit(null, new flex2.compiler.Context());
            
            u.setSignatureChecksum(signatureChecksum);

			u.bytes.addAll(abc);
			u.setWorkflow(readU32(cu_in));
			u.setState(readU32(cu_in));

			size = readU32(cu_in);
			for (int i = 0; i < size; i++)
			{
				u.topLevelDefinitions.add(pool[readU32(cu_in)]);
			}

			size = readU32(cu_in);
			for (int i = 0; i < size; i++)
			{
				MultiName mName = (MultiName) pool[readU32(cu_in)];
				QName qName = (QName) pool[readU32(cu_in)];
				u.inheritanceHistory.put(mName, qName);
				u.inheritance.add(qName);
			}

			size = readU32(cu_in);
			for (int i = 0; i < size; i++)
			{
				MultiName mName = (MultiName) pool[readU32(cu_in)];
				QName qName = (QName) pool[readU32(cu_in)];
				u.typeHistory.put(mName, qName);
				u.types.add(qName);
			}

			size = readU32(cu_in);
			for (int i = 0; i < size; i++)
			{
				MultiName mName = (MultiName) pool[readU32(cu_in)];
				QName qName = (QName) pool[readU32(cu_in)];
				u.namespaceHistory.put(mName, qName);
				u.namespaces.add(qName);
			}

			size = readU32(cu_in);
			for (int i = 0; i < size; i++)
			{
				MultiName mName = (MultiName) pool[readU32(cu_in)];
				QName qName = (QName) pool[readU32(cu_in)];
				u.expressionHistory.put(mName, qName);
				u.expressions.add(qName);
			}

			boolean hasAuxGenerateInfo = readU8(cu_in) == 1;

			if (hasAuxGenerateInfo)
			{
				u.auxGenerateInfo = new HashMap();

				String baseLoaderClass = (String) pool[readU32(cu_in)];
				u.auxGenerateInfo.put("baseLoaderClass", baseLoaderClass.length() > 0 ? baseLoaderClass : null);

				String generateLoaderClass = (String) pool[readU32(cu_in)];
				u.auxGenerateInfo.put("generateLoaderClass", generateLoaderClass.length() > 0 ? generateLoaderClass : null);

				String className = (String) pool[readU32(cu_in)];
				u.auxGenerateInfo.put("windowClass", className.length() > 0 ? className : null);

				String preLoader = (String) pool[readU32(cu_in)];
				u.auxGenerateInfo.put("preloaderClass", preLoader.length() > 0 ? preLoader : null);

				u.auxGenerateInfo.put("usePreloader", new Boolean(readU8(cu_in) == 1));

				Map rootAttributeMap = new HashMap();
				u.auxGenerateInfo.put("rootAttributes", rootAttributeMap);

				size = readU32(cu_in);
				for (int i = 0; i < size; i++)
				{
					String key = (String) pool[readU32(cu_in)];
					String value = (String) pool[readU32(cu_in)];

					rootAttributeMap.put(key, value);
				}
			}

			readAssets(pool, u, cu_in);
		}
		
		if (s != null)
		{
			name = s.getName();
			Object obj = owners.get(name);
			if (obj == null || obj instanceof Source)
			{
				return;
			}
			
			int value = ((Integer) owners.get(name)).intValue();
			if ((s.isFileSpecOwner() && value == 0) ||
				(s.isSourceListOwner() && value == 1) ||
				(s.isSourcePathOwner() && value == 2) ||
				(s.isResourceContainerOwner() && value == 3) ||
				(s.isResourceBundlePathOwner() && value == 4))
			{
				owners.put(name, s);
			}
		}
	}

	private void readAssets(final Object[] pool, final CompilationUnit u, final InputStream cu_in) throws IOException
	{
		int size = readU32(cu_in);
		if (size > 0)
		{
			int assetCount = readU32(cu_in);
			final Map assets = new HashMap();

			PathResolver resolver = ThreadLocalToolkit.getPathResolver();

			for (int i = 0; i < assetCount; i++)
			{
				String className = (String) pool[readU32(cu_in)];
				String pathName = (String) pool[readU32(cu_in)];

				VirtualFile f = null;
				if (pathName.length() == 0)
				{
					f = null;
				}
				else
				{
					f = resolver.resolve(pathName);
					if (f == null)
					{
						f = new DeletedFile(pathName);
					}
				}

				assets.put(className, new AssetInfo(null, f, readLong(cu_in), null));
			}

			int swfSize = readU32(cu_in);
			ByteArrayInputStream in = new ByteArrayInputStream(readBytes(cu_in, swfSize));

			Movie movie = new Movie();
			MovieDecoder movieDecoder = new MovieDecoder(movie);
			TagDecoder tagDecoder = new TagDecoder(in);
			tagDecoder.parse(movieDecoder);

            for (Iterator frames = movie.frames.iterator(); frames.hasNext(); )
			{
			    Frame frame = (Frame) frames.next();

			    for (Iterator it = frame.symbolClass.class2tag.entrySet().iterator(); it.hasNext();)
			    {
					Map.Entry e = (Map.Entry) it.next();
					String className = (String) e.getKey();
					DefineTag tag = (DefineTag) e.getValue();
					AssetInfo assetInfo = (AssetInfo) assets.get(className);
					assetInfo.setDefineTag(tag);

					u.getAssets().add(className, assetInfo);

                    // We special case DefineFont tags so that the FontManager 
					// can cache them and avoid re-creating them on subsequent
                    // compiles.
                    if (fontManager != null && tag instanceof DefineFont)
                    {
                        VirtualFile f = assetInfo.getPath();
                        String path = null;
                        if (f != null)
                        {
                            path = f.getURL();
                        }

                        fontManager.loadDefineFont((DefineFont)tag, path);
                    }
				}
			}
		}
	}

	// Methods for adding constant pool entries

	private int addObject(Map pool, Object obj)
	{
		assert obj != null;

		Integer index = (Integer) pool.get(obj);

		if (index == null)
		{
			index = IntegerPool.getNumber(pool.size());
			pool.put(obj, index);
		}

		return index.intValue();
	}

	private int addBytes(Map pool, byte[] b)
	{
		return addObject(pool, b);
	}

	private int addString(Map pool, String s)
	{
		return addObject(pool, s);
	}

	private int addStrings(Map pool, String[] array)
	{
		assert array != null;

		key.a1 = array;
		Integer index = (Integer) pool.get(key);

		if (index == null)
		{
			for (int i = 0, size = array.length; i < size; i++)
			{
				addString(pool, array[i]);
			}

			index = IntegerPool.getNumber(pool.size());
			pool.put(new ArrayKey(array), index);
		}

		return index.intValue();
	}

	private int addQName(Map pool, QName qName)
	{
		assert qName != null;

		Integer index = (Integer) pool.get(qName);

		if (index == null)
		{
			addString(pool, qName.getNamespace());
			addString(pool, qName.getLocalPart());

			index = IntegerPool.getNumber(pool.size());
			pool.put(qName, index);
		}

		return index.intValue();
	}

	private int addMultiName(Map pool, MultiName multiName)
	{
		assert multiName != null;

		Integer index = (Integer) pool.get(multiName);

		if (index == null)
		{
			addStrings(pool, multiName.namespaceURI);
			addString(pool, multiName.localPart);

			index = IntegerPool.getNumber(pool.size());
			pool.put(multiName, index);
		}

		return index.intValue();
	}

	/*
    private int addMetaData(Map pool, flex2.compiler.abc.MetaData md)
	{
		assert md != null;

		Integer index = (Integer) pool.get(md);

		if (index == null)
		{
			addString(pool, md.getID());
			for (int j = 0, count = md.count(); j < count; j++)
			{
				String key = md.getKey(j);
				String val = md.getValue(j);

				addString(pool, key == null ? "" : key);
				addString(pool, val);
			}

			index = IntegerPool.getNumber(pool.size());
			pool.put(md, index);
		}

		return index.intValue();
	}
    */

	// Low-level encoding methods

	private void writeBytes(OutputStream out, byte[] b) throws IOException
	{
		out.write(b);
	}

	private void writeLong(OutputStream out, long num) throws IOException
	{
		out.write((int) (num >>> 56) & 0xFF);
		out.write((int) (num >>> 48) & 0xFF);
		out.write((int) (num >>> 40) & 0xFF);
		out.write((int) (num >>> 32) & 0xFF);
		out.write((int) (num >>> 24) & 0xFF);
		out.write((int) (num >>> 16) & 0xFF);
		out.write((int) (num >>>  8) & 0xFF);
		out.write((int)  num         & 0xFF);
	}

	private void writeU32(OutputStream out, int num) throws IOException
	{
		out.write((num >>> 24) & 0xFF);
		out.write((num >>> 16) & 0xFF);
		out.write((num >>>  8) & 0xFF);
		out.write( num         & 0xFF);
	}

	private void writeU8(OutputStream out, int num) throws IOException
	{
		out.write(num & 0xFF);
	}

	private byte[] readBytes(int length) throws IOException
	{
		byte[] b = new byte[length];
		file.readFully(b);
		return b;
	}

	private int readU32() throws IOException
	{
		return file.readInt();
	}

	private int readU8() throws IOException
	{
		return file.read();
	}

	private byte[] readBytes(InputStream in, int length) throws IOException
	{
		byte[] b = new byte[length];

		for (int size = 0, start = 0, len = length - size; (size = in.read(b, start, len)) != -1 && start + size < length;)
		{
			start += size;
			len -= size;
		}

		return b;
	}

	private long readLong() throws IOException
	{
		return ((long) (readU32()) << 32) + (readU32() & 0xFFFFFFFFL);
	}
	
	private long readLong(InputStream in) throws IOException
	{
		return ((long) (readU32(in)) << 32) + (readU32(in) & 0xFFFFFFFFL);
	}

	private int readU32(InputStream in) throws IOException
	{
		return ((in.read() << 24) + (in.read() << 16) + (in.read() << 8) + in.read());
	}

	private int readU8(InputStream in) throws IOException
	{
		return in.read();
	}

	// Helper classes

	private class ArrayKey
	{
		ArrayKey()
		{
		}

		ArrayKey(String[] array)
		{
			a1 = array;
		}

		private String[] a1;

		public boolean equals(Object obj)
		{
			if (obj == this)
			{
				return true;
			}
			else if (obj instanceof ArrayKey)
			{
				String[] a2 = ((ArrayKey) obj).a1;

				if (a1 == a2)
				{
					return true;
				}

				if (a1.length != a2.length)
				{
					return false;
				}

				for (int i = 0, size = a1.length; i < size; i++)
				{
					if (!a1[i].equals(a2[i]))
					{
						return false;
					}
				}

				return true;
			}
			else
			{
				return false;
			}
		}

		public int hashCode()
		{
			int c = 0;
			for (int i = 0, size = a1.length; i < size; i++)
			{
				c = (i == 0) ? a1[i].hashCode() : c ^ a1[i].hashCode();
			}
			return c;
		}
	}

	public final class MetaData implements flex2.compiler.abc.MetaData
	{
		public MetaData(String id, int count)
		{
			this.id = id;
			this.keys = new String[count];
			this.values = new String[count];
		}

		private String id;
		private String[] keys;
		private String[] values;

		public String getID()
		{
			return id;
		}

		public void setValue(int index, String value)
		{
			values[index] = value;
		}

		public void setKeyValue(int index, String key, String value)
		{
			keys[index] = key;
			values[index] = value;
		}

		public String getKey(int index)
		{
			if (index < 0 || index >= count())
			{
				throw new ArrayIndexOutOfBoundsException();
			}
			else
			{
				return keys[index];
			}
		}

		public String getValue(String key)
		{
			for (int i = 0, length = count(); i < length; i++)
			{
				if (key.equals(keys[i]))
				{
					return values[i];
				}
			}
			return null;
		}

		public String getValue(int index)
		{
			if (index < 0 || index >= count())
			{
				throw new ArrayIndexOutOfBoundsException();
			}
			else
			{
				return values[index];
			}
		}

        public Map getValueMap()
        {
            Map result = new HashMap();

			for (int i = 0, length = count(); i < length; i++)
			{
                result.put(keys[i], values[i]);
			}

			return result;
        }

		public int count()
		{
			return values != null ? values.length : 0;
		}
	}

	// error messages

	public static class ObsoleteCacheFileFormat extends CompilerMessage.CompilerInfo
	{
        private static final long serialVersionUID = -8594915455219662842L;

        public ObsoleteCacheFileFormat()
		{
			super();
		}
	}

	public static class NoFileSpec extends CompilerMessage.CompilerInfo
	{
        private static final long serialVersionUID = -5780228997078423591L;

        public NoFileSpec()
		{
			super();
		}
	}

	public static class NoSourceList extends CompilerMessage.CompilerInfo
	{
        private static final long serialVersionUID = 1489613684797688310L;

        public NoSourceList()
		{
			super();
		}
	}

	public static class NoSourcePath extends CompilerMessage.CompilerInfo
	{
        private static final long serialVersionUID = -4989314191998065597L;

        public NoSourcePath()
		{
			super();
		}
	}

	public static class NoResourceContainer extends CompilerMessage.CompilerInfo
	{
        private static final long serialVersionUID = -384784734412773490L;

        public NoResourceContainer()
		{
			super();
		}
	}
}
