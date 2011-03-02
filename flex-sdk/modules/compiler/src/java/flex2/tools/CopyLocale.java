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

package flex2.tools;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;

/**
 * @author Gordon Smith
 * 
 * This class implements the SDK tool bin/copylocale.
 * 
 * This tool is used to create the necessary files for developing
 * in a new locale for which the framework has not been localized.
 * For example, if you try to compile an application for the fr_FR
 * (French) locale, you will get compilation errors because the SDK
 * has neither .properties files nor resource bundle SWCs for this locale.
 * But if you execute
 *   copylocale en_US fr_FR
 * you will then have what you need for the fr_FR locale; the resources
 * for this locale will simply be copies of those for the en_US locale.
 */
public class CopyLocale
{
    /**
     * The entry-point for the bin/copylocale script.
     */
    public static void main(String[] args)
    {
	    CopyLocale copyLocale = new CopyLocale();
	    copyLocale.run(args);
    }
    
    private CopyLocale()
    {
    }
    
	/**
	 * The SDK directory in which the bin/copylocale script
	 * that called main() lives.
	 */
	private File sdkDir;

    /**
	 * The locale from which to copy the properties files.
	 * This is the first argument of copylocale.
	 * For example, if you execute
	 *   copylocale en_US fr_FR
	 * then srcLocale is "en_US".
	 */
    private String srcLocale;
    
	/**
	 * The new locale to create properties files and SWCs for.
	 * This is the second argument of copylocale.
	 * For example, if you execute
	 *   copylocale en_US fr_FR
	 * then dstLocale is "fr_FR".
	 */
	private String dstLocale;
	
	/**
	 * The names of the SWC projects in the frameworks/projects
	 * directory of the SDK which have resource bundles
	 * (as identified by having a 'bundles' subdirectory).
	 */
	private String[] projects;
	
	/**
	 * A Map (String -> String[]) mapping a project name
	 * to an array of bundle names, e.g. "rpc" -> [ "messaging", "rpc" ],
	 * as determined by enumerating the .properties files
	 * in the project's 'bundle' directory for the sourceLocale.
	 */
	private HashMap bundleNamesForProject = new HashMap();
    
    private void run(String[] args)
    {
		if (!checkArguments(args))
			return;
		
		System.out.println("In Flex SDK at " + sdkDir + " ...");
		
		int n = projects.length;
		for (int i = 0; i < n; i++)
		{
			copyPropertiesFiles(projects[i]);
			compileSWC(projects[i]);
		}
    }
    
	private boolean checkArguments(String[] args)
	{
		// The bin/copylocale script passes its arguments to main().
		// For example, if you execute
		//   copylocale en_US fr_FR
		// then args will be
		//   [0] en_US -> srcLocale
		//   [1] fr_FR -> dstLocale

		if (args.length != 2)
		{
			System.out.println("Usage: copylocale src_locale dst_locale");
    		return false;
		}

		boolean success = true;
		
		// On Windows, the system property "application.home"
		// is set by the JAR launcher, copylocale.exe.
		sdkDir = new File(System.getProperty("application.home"));
		
		srcLocale = args[0];
		dstLocale = args[1];
		
		// Check whether each project with resource bundles
		// has a 'src' folder for the srcLocale.
		File projectsDir = new File(sdkDir, "frameworks/projects");
		File[] projectDirs = projectsDir.listFiles();
		ArrayList projectList = new ArrayList();
		int n = projectDirs.length;
		for (int i = 0; i < n; i++)
		{
			File projectDir = projectDirs[i];
			File bundlesDir = new File(projectDir, "bundles");
			if (!bundlesDir.exists())
				continue;
			
			String project = projectDir.getName();
			projectList.add(project);

			File srcLocaleDir = new File(bundlesDir, srcLocale);
			File srcDir = new File(srcLocaleDir, "src");
			if (!srcLocaleDir.exists() || !srcDir.exists())
			{
				System.err.println("Error: Directory \"" + srcDir + "\" does not exist");
				success = false;
			}
		}
		
		projects = new String[projectList.size()];
		projectList.toArray(projects);

		return success;
	}
	
	private void copyPropertiesFiles(String project)
	{
		File bundlesDir = new File(sdkDir, "frameworks/projects/" + project + "/bundles");
		File fromSrcDir = new File(bundlesDir, srcLocale + "/src");
		
		File[] propertiesFiles = fromSrcDir.listFiles();
		
		ArrayList bundleNameList = new ArrayList();
		int m = propertiesFiles.length;
		for (int j = 0; j < m; j++)
		{
			String name = propertiesFiles[j].getName();
			if (name.endsWith(".properties"))
			{
				String bundleName = name.substring(0, name.length() - 11);
				bundleNameList.add(bundleName);
			}
		}
		String[] bundleNames = new String[bundleNameList.size()];
		bundleNameList.toArray(bundleNames);
		bundleNamesForProject.put(project, bundleNames);
		
		File toSrcDir = new File(bundlesDir, dstLocale + "/src");
		
		int len = sdkDir.toString().length() + 1;
		System.out.println();
		System.out.println("Copying files from " + fromSrcDir.toString().substring(len));
		System.out.println("                to " + toSrcDir.toString().substring(len) + ":");
		System.out.println();

		try
		{
			copyDirectory(fromSrcDir, toSrcDir);
		}
		catch (IOException e)
		{
			System.err.println("Error: Can't copy .properties files");
		}
	}

	public void copyDirectory(File fromDir, File toDir) throws IOException
	{
		if (fromDir.isDirectory())
		{
			toDir.mkdirs();
            
			String[] children = fromDir.list();
			int n = children.length;
			for (int i = 0; i < n; i++)
			{
				copyDirectory(new File(fromDir, children[i]),
							  new File(toDir, children[i]));
            }
        }
		else
		{
			System.out.println(fromDir.getName());
			
			FileInputStream in = new FileInputStream(fromDir);
			FileOutputStream out = new FileOutputStream(toDir);
 			
			byte[] buffer = new byte[32768];
			int length;
			while ((length = in.read(buffer)) > 0)
			{
				out.write(buffer, 0, length);
			}
			
			in.close();
			out.close();
        }
    } 
	
	private void compileSWC(String project)
	{
		File srcDir = new File(sdkDir, "frameworks/projects/" + project + "/bundles/" + dstLocale + "/src");
		
		String bundleNameString = "";
    	String[] bundleNames = (String[])bundleNamesForProject.get(project);
     	int n = bundleNames.length;
		for (int i = 0; i < n; i++)
    	{
			bundleNameString += bundleNames[i];
    		if (i < n - 1)
    			bundleNameString += ",";
    	}
			
     	File outputDir = new File(sdkDir, "frameworks/locale/" + dstLocale);
     	File outputSWC = new File(outputDir, project + "_rb.swc");
		
		int len = sdkDir.toString().length() + 1;
		System.out.println();
     	System.out.println("Compiling resource bundle SWC " + outputSWC.toString().substring(len) + ":");
		System.out.println();

		String[] args = new String[]
		{
			"-locale=" + dstLocale,
			"-source-path",
			srcDir.toString(),
			"-include-resource-bundles=" + bundleNameString,
			"-output",
			outputSWC.toString()
		};
		
		outputDir.mkdirs();
		Compc.compc(args);
	}
}
