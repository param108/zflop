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

import java.io.File;
import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import flash.swf.Frame;
import flex2.compiler.AssetInfo;
import flex2.compiler.CompilationUnit;
import flex2.compiler.Source;
import flex2.compiler.common.Configuration;
import flex2.compiler.io.VirtualFile;
import flex2.compiler.swc.SwcScript;
import flex2.compiler.util.MultiNameSet;
import flex2.compiler.util.QName;
import flex2.compiler.util.QNameList;
import flex2.linker.SimpleMovie;
import flex2.tools.VersionInfo;
import flex2.tools.oem.Message;
import flex2.tools.oem.Report;

/**
 * 
 * @version 2.0.1
 * @author Clement Wong
 */
public class OEMReport implements Report
{
	public OEMReport(List sources, SimpleMovie movie, Configuration configuration,
					 String configurationReport, List messages)
	{
		init(sources, movie == null ? null : movie.getExportedUnits(),
			 configuration == null ? null : configuration.getResourceBundles());

		processFrames(movie);
		processMessages(messages);
		
		this.frameCount = movie == null ? 0 : movie.frames.size();
		this.configurationReport = configurationReport;
		
		defaultWidth = configuration != null ? configuration.defaultWidth() : 0;
		defaultHeight = configuration != null ? configuration.defaultHeight() : 0;

		if (movie != null)
		{
			linkReport = movie.getLinkReport();
			bgColor = movie.bgcolor.color;
			pageTitle = movie.pageTitle;
			
			if (movie.userSpecifiedWidth)
			{
				width = movie.width;
				widthPercent = 0.0;
			}
			else
			{
				width = 0;
			    String percent = configuration == null ? null : configuration.widthPercent();
			    percent = percent == null ? "0%" : percent.trim();
			    percent = percent.substring(0, percent.length() - 1);
			    
			    try
			    {
			    	widthPercent = Double.parseDouble(percent) / (double) 100;
			    }
			    catch(NumberFormatException ex)
			    {
			    	widthPercent = 0.0;
			    }
			}

			if (movie.userSpecifiedHeight)
			{
				height = movie.height;
				heightPercent = 0.0;
			}
			else
			{
				height = 0;
			    String percent = configuration == null ? null : configuration.heightPercent();
			    percent = percent == null ? "0%" : percent.trim();
			    percent = percent.substring(0, percent.length() - 1);
			    
			    try
			    {
			    	heightPercent = Double.parseDouble(percent) / (double) 100;
			    }
			    catch(NumberFormatException ex)
			    {
			    	heightPercent = 0.0;
			    }
			}
		}
		else
		{
			linkReport = null;
			bgColor = 0;
			width = 0;
			height = 0;
			pageTitle = null;
			widthPercent = 0.0;
			heightPercent = 0.0;
		}
	}
	
	private void init(List sourceList, List exportedUnits, Set resourceBundles)
	{
		TreeSet sources = new TreeSet();
		TreeSet assets = new TreeSet();
		TreeSet libraries = new TreeSet();
		
		data = new HashMap();
		locations = new HashMap();
		
		processSources(sourceList, sources, assets, libraries, data, locations);
		
		compiler_SourceNames = toArray(sources);
		compiler_AssetNames = toArray(assets);
		compiler_LibraryNames = toArray(libraries);
		
		resourceBundleNames = toArray(resourceBundles);
		
		sources.clear();
		assets.clear();
		libraries.clear();
		
		processCompilationUnits(exportedUnits, sources, assets, libraries);
			
		linker_SourceNames = toArray(sources);
		linker_AssetNames = toArray(assets);
		linker_LibraryNames = toArray(libraries);
		
		timestamps = new HashMap();
		storeTimestamps(linker_SourceNames);
		storeTimestamps(linker_AssetNames);
		storeTimestamps(linker_LibraryNames);
	}
	
	private void storeTimestamps(String[] a)
	{
		for (int i = 0, len = a == null ? 0 : a.length; i < len; i++)
		{
			if (!timestamps.containsKey(a[i]))
			{
				timestamps.put(a[i], new Long(new File(a[i]).lastModified()));
			}
		}
	}
	
	private String[] compiler_SourceNames, compiler_AssetNames, compiler_LibraryNames;
	private String[] linker_SourceNames, linker_AssetNames, linker_LibraryNames;
	private String[] resourceBundleNames;
	private Map data, locations, timestamps;
	
	private int frameCount;
	private int bgColor, defaultWidth, defaultHeight, width, height;
	private String pageTitle;
	private double widthPercent, heightPercent;
	
	private String linkReport, configurationReport;
	private Message[] messages;
	
	private String[][] assetNames, definitionNames;
	
	public boolean contentUpdated()
	{
		for (Iterator i = timestamps.keySet().iterator(); i.hasNext(); )
		{
			String path = (String) i.next();
			Long ts = (Long) timestamps.get(path);
			File f = new File(path);
			
			if (!f.exists() || f.lastModified() != ts.longValue())
			{
				return true;
			}
		}
		return false;
	}
	
	public String[] getSourceNames(Object report)
	{
		return (COMPILER.equals(report)) ? compiler_SourceNames : (LINKER.equals(report)) ? linker_SourceNames : null;
	}

	public String[] getAssetNames(int frame)
	{
		return assetNames[frame - 1];
	}
	
	public String[] getAssetNames(Object report)
	{
		return (COMPILER.equals(report)) ? compiler_AssetNames : (LINKER.equals(report)) ? linker_AssetNames : null;
	}

	public String[] getLibraryNames(Object report)
	{
		return (COMPILER.equals(report)) ? compiler_LibraryNames : (LINKER.equals(report)) ? linker_LibraryNames : null;
	}

	public String[] getResourceBundleNames()
	{
		return resourceBundleNames;
	}
	
	public String[] getDefinitionNames(int frame)
	{
		return definitionNames[frame - 1];
	}
	
	public String[] getDefinitionNames(String sourceName)
	{
		Data d = (Data) data.get(sourceName);
		return d == null ? null : d.definitions;
	}
	
	public String getLocation(String definition)
	{
		return (String) locations.get(definition);
	}
	
	public String[] getDependencies(String definition)
	{
		String location = getLocation(definition);
		
		if (location != null)
		{
			Data d = (Data) data.get(location);
			return d == null ? null : d.dependencies;
		}
		else
		{
			return null;
		}
	}

	public String[] getPrerequisites(String definition)
	{
		String location = getLocation(definition);
		
		if (location != null)
		{
			Data d = (Data) data.get(location);
			return d == null ? null : d.prerequisites;
		}
		else
		{
			return null;
		}
	}
	
	public long writeLinkReport(Writer out) throws IOException
	{
		long size = 0;
		
		if (linkReport != null)
		{
			out.write(linkReport);
			out.flush();
			size = linkReport.length();
		}
		
		return size;
	}

	public long writeConfigurationReport(Writer out) throws IOException
	{
		long size = 0;
		
		if (configurationReport != null)
		{
			out.write(configurationReport);
			out.flush();
			size = configurationReport.length();
		}
		
		return size;
	}
	
	public int getBackgroundColor()
	{
		return bgColor;
	}
	
	public String getPageTitle()
	{
		return pageTitle;
	}
	
	public int getDefaultWidth()
	{
		return defaultWidth;
	}
	
	public int getDefaultHeight()
	{
		return defaultHeight;
	}
	
	public int getWidth()
	{
		return width;
	}
	
	public int getHeight()
	{
		return height;
	}
	
	public double getWidthPercent()
	{
		return widthPercent;
	}
	
	public double getHeightPercent()
	{
		return heightPercent;
	}
	
	public String getCompilerVersion()
	{
		return VersionInfo.buildMessage();
	}

	public Message[] getMessages()
	{
		return messages;
	}
	
	public int getFrameCount()
	{
		return frameCount;
	}
	
	private void processCompilationUnits(List units, TreeSet sources, TreeSet assets, TreeSet libraries)
	{
		for (int i = 0, length = units == null ? 0 : units.size(); i < length; i++)
		{
			CompilationUnit u = (CompilationUnit) units.get(i);
			Source s = (u == null) ? null : u.getSource();
			
			if (s == null)
			{
				continue;
			}
			
			if (s.isFileSpecOwner() || s.isSourceListOwner() || s.isSourcePathOwner() || s.isResourceBundlePathOwner())
			{
				sources.add(s.getName());
				
				for (Iterator j = s.getFileIncludes(); j.hasNext(); )
				{
					VirtualFile f = (VirtualFile) j.next();
					sources.add(f.getName());
				}
				
  				for (Iterator j = u.getAssets().iterator(); j.hasNext(); )
  				{
  					Map.Entry e = (Map.Entry) j.next();
  					AssetInfo assetInfo = (AssetInfo) e.getValue();
					VirtualFile path = assetInfo.getPath();
					if (path != null)
					{
						assets.add(path.getName());
					}
  				}
			}
			else if (s.isSwcScriptOwner())
			{
				String location = ((SwcScript) s.getOwner()).getLibrary().getSwcLocation(); 
				libraries.add(location);
			}
		}
	}
	
	private void processSources(List sourceList, TreeSet sources, TreeSet assets, TreeSet libraries,
								Map data, Map locations)
	{
		for (int i = 0, length = sourceList == null ? 0 : sourceList.size(); i < length; i++)
		{
			Source s = (Source) sourceList.get(i);
			CompilationUnit u = (s == null) ? null : s.getCompilationUnit();
			
			if (s == null)
			{
				continue;
			}
			
			if (s.isFileSpecOwner() || s.isSourceListOwner() || s.isSourcePathOwner() || s.isResourceBundlePathOwner())
			{
				sources.add(s.getName());
				
				for (Iterator j = s.getFileIncludes(); j.hasNext(); )
				{
					VirtualFile f = (VirtualFile) j.next();
					sources.add(f.getName());
				}
				
  				for (Iterator j = u.getAssets().iterator(); j.hasNext(); )
  				{
  					Map.Entry e = (Map.Entry) j.next();
  					AssetInfo assetInfo = (AssetInfo) e.getValue();
					VirtualFile path = assetInfo.getPath();
					if (path != null)
					{
						assets.add(assetInfo.getPath().getName());
					}
  				}

				if (locations != null)
				{
					for (int j = 0, size = u.topLevelDefinitions.size(); j < size; j++)
					{
						locations.put(u.topLevelDefinitions.get(j).toString(), s.getName());
					}
				}
			}
			else if (s.isSwcScriptOwner())
			{
				String location = ((SwcScript) s.getOwner()).getLibrary().getSwcLocation(); 
				libraries.add(location);
				
				if (locations != null)
				{
					for (int j = 0, size = u.topLevelDefinitions.size(); j < size; j++)
					{
						locations.put(u.topLevelDefinitions.get(j).toString(), location);
					}
				}
			}
		}

		for (int i = 0, length = sourceList == null ? 0 : sourceList.size(); i < length; i++)
		{
			Source s = (Source) sourceList.get(i);
			CompilationUnit u = (s == null) ? null : s.getCompilationUnit();
			
			if (s == null)
			{
				continue;
			}
			
			if (s.isFileSpecOwner() || s.isSourceListOwner() || s.isSourcePathOwner() || s.isResourceBundlePathOwner())
			{
				Data d = new Data();
				d.definitions = toArray(u.topLevelDefinitions);
				d.prerequisites = toArray(new MultiNameSet[] { u.inheritance }, null, locations);
				d.dependencies = toArray(new MultiNameSet[] { u.namespaces, u.types, u.expressions },
										 new Set[] { u.extraClasses, u.resourceBundleHistory }, locations);
					
				data.put(s.getName(), d);
			}
		}
	}

	private void processFrames(SimpleMovie movie)
	{
		int count = movie == null ? 0 : movie.frames.size();
		assetNames = new String[count][];
		definitionNames = new String[count][];
		
		for (int i = 0; i < count; i++)
		{
			Frame f = (Frame) movie.frames.get(i);
			List units = movie.getExportedUnitsByFrame(f), aList = new ArrayList(), dList = new ArrayList();
			for (int j = 0, size = units == null ? 0 : units.size(); j < size; j++)
			{
				CompilationUnit u = (CompilationUnit) units.get(j);
				Source s = u.getSource();
				
				for (Iterator k = u.getAssets().iterator(); k.hasNext(); )
				{
  					Map.Entry e = (Map.Entry) k.next();
  					AssetInfo assetInfo = (AssetInfo) e.getValue();
					VirtualFile path = assetInfo.getPath();
					if (path != null)
					{
						String assetName = path.getName();
						if (!aList.contains(assetName))
						{
							aList.add(assetName);
						}
					}
				}

				if (s.isFileSpecOwner() || s.isResourceBundlePathOwner() || s.isSourceListOwner() ||
					s.isSourcePathOwner() || s.isSwcScriptOwner())
				{
					for (Iterator k = u.topLevelDefinitions.iterator(); k.hasNext(); )
					{
						String definitionName = k.next().toString();
						dList.add(definitionName);
					}
				}
			}
			
			if (aList.size() > 0)
			{
				assetNames[i] = new String[aList.size()];
				aList.toArray(assetNames[i]);
			}
			
			if (dList.size() > 0)
			{
				definitionNames[i] = new String[dList.size()];
				dList.toArray(definitionNames[i]);
			}
		}
	}
	
	private void processMessages(List messages)
	{
		if (messages != null && messages.size() > 0)
		{
			List filtered = new ArrayList();
			
			for (int i = 0, length = messages.size(); i < length; i++)
			{
				Message m = (Message) messages.get(i);
				if (m != null && !Message.INFO.equals(m.getLevel()))
				{
					filtered.add(m);
				}
			}
			
			messages = filtered;
		}
			
		if (messages != null && messages.size() > 0)
		{
			this.messages = new Message[messages.size()];
			for (int i = 0, length = this.messages.length; i < length; i++)
			{
				this.messages[i] = new GenericMessage((Message) messages.get(i));
			}
		}
		else
		{
			this.messages = null;
		}
	}
	
	private String[] toArray(Set set)
	{
		String[] a = new String[set == null ? 0 : set.size()];
		int j = 0;
		
		if (set != null)
		{
			for (Iterator i = set.iterator(); i.hasNext(); j++)
			{
				a[j] = (String) i.next();
			}
		}
		
		return a.length == 0 ? null : a;
	}

	private String[] toArray(QNameList definitions)
	{
		String[] a = new String[definitions == null ? 0 : definitions.size()];
		
		for (int i = 0; i < a.length; i++)
		{
			a[i] = definitions.get(i).toString();
		}
		
		return a.length == 0 ? null : a;
	}

	private String[] toArray(MultiNameSet[] mnSets, Set[] sets, Map locations)
	{
		TreeSet set = new TreeSet();
		
		for (int i = 0, length = mnSets == null ? 0 : mnSets.length; i < length; i++)
		{
			if (mnSets[i] != null)
			{
				for (Iterator j = mnSets[i].iterator(); j.hasNext(); )
				{
					Object obj = j.next();
					String qName = null;
					if (obj instanceof QName && (locations == null || locations.containsKey(qName = obj.toString())))
					{
						set.add(qName);
					}
				}
			}
		}

		for (int i = 0, length = sets == null ? 0 : sets.length; i < length; i++)
		{
			if (sets[i] != null)
			{
				for (Iterator j = sets[i].iterator(); j.hasNext(); )
				{
					Object obj = j.next();
					if (obj instanceof String && (locations == null || locations.containsKey(obj)))
					{
						set.add(obj);
					}
				}
			}
		}

		return toArray(set);
	}

	static class Data
	{
		String[] definitions;
		String[] prerequisites;
		String[] dependencies;
	}
}
