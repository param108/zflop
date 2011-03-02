////////////////////////////////////////////////////////////////////////////////
//
//  ADOBE SYSTEMS INCORPORATED
//  Copyright 2006 Adobe Systems Incorporated
//  All Rights Reserved.
//
//  NOTICE: Adobe permits you to use, modify, and distribute this file
//  in accordance with the terms of the license agreement accompanying it.
//
////////////////////////////////////////////////////////////////////////////////

package flex2.tools;

import org.apache.batik.svggen.font.Font;
import org.apache.batik.svggen.font.table.Os2Table;

import java.io.File;
import java.util.TreeMap;
import java.util.Iterator;
import java.util.Map;
import java.util.ArrayList;

import flash.fonts.FSType;

/**
 * Prints out a report on what fonts can and can't be used in the given directory and its subdirectories.
 * We check the fsType of the fonts found.  More information on this can be found here:
 * http://partners.adobe.com/public/developer/en/acrobat/sdk/FontPolicies.pdf
 *
 * @author Brian Deitte
 */
public class FontLicenseChecker
{
	public static void main(String[] args) throws Exception
	{
		listFonts(args);
	}

	public static void listFonts(String[] args)
	{
		File file = new File(args[0]);
		TreeMap fonts = new TreeMap();
		getLicenseTypes(file, fonts);
		for (Iterator iterator = fonts.entrySet().iterator(); iterator.hasNext();)
		{
			Map.Entry entry = (Map.Entry)iterator.next();
			Integer integ = (Integer)entry.getKey();
			ArrayList list = (ArrayList)entry.getValue();
			FSType type = FSType.getFSType(integ.intValue());
			String licenseType = type.description + ", fsType = '" + type.fsType + "'";
			System.out.println("");
			System.out.println("---------");
			System.out.println(licenseType);
			System.out.println("Flex will" + (type.usableByFlex ? "" : " not") + " embed any of the " + list.size() + " fonts listed below.");
			System.out.println("");

			for (Iterator interator2 = list.iterator(); interator2.hasNext();)
			{
				String fontStr = (String)interator2.next();
				System.out.println(fontStr);
			}
		}
	}

	public static void getLicenseTypes(File file, Map fonts)
	{
		if (! file.exists())
		{
			throw new RuntimeException("Font or dir not found: " + file);
		}

		if (file.isDirectory())
		{
			File[] children = file.listFiles();
			if (children != null)
			{
				for (int i = 0; i < children.length; i++)
				{
					File child = children[i];
					if (child.isDirectory() || child.toString().toLowerCase().endsWith(".ttf"))
					{
						getLicenseTypes(child, fonts);
					}
				}
			}
		}
		else
		{
			Font font = null;
			String err = null;
			try
			{
				font = Font.create(file.toString());
			}
			catch(Exception e)
			{
				err = e.toString();
			}

			if (font == null || font.getOS2Table() == null)
			{
				System.err.println("Error reading " + file + ": " + err);
			}
			else
			{
				Os2Table table = font.getOS2Table();
				Integer integ = new Integer(table.getLicenseType());
				ArrayList list = (ArrayList)fonts.get(integ);
				if (list == null)
				{
					list = new ArrayList();
					fonts.put(integ, list);
				}
				list.add(file.toString());
			}
		}
	}
}
