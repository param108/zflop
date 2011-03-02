////////////////////////////////////////////////////////////////////////////////
//
//  ADOBE SYSTEMS INCORPORATED
//  Copyright 2004-2006 Adobe Systems Incorporated
//  All Rights Reserved.
//
//  NOTICE: Adobe permits you to use, modify, and distribute this file
//  in accordance with the terms of the license agreement accompanying it.
//
////////////////////////////////////////////////////////////////////////////////

package flex.tools.debugger.cli;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class Help
{
    private Help()
    {
    }

    public static InputStream getResourceAsStream()
    {
		List names = calculateLocalizedFilenames("fdbhelp", ".txt", Locale.getDefault()); //$NON-NLS-1$ //$NON-NLS-2$
		for (int i=names.size()-1; i>=0; --i) {
			InputStream stm = Help.class.getResourceAsStream((String) names.get(i));
			if (stm != null) {
				return stm;
			}
		}
		return null;
    }

    /**
     * Returns an array of filenames that might match the given baseName and locale.
     * For example, if baseName is "fdbhelp", the extension is ".txt", and the locale
     * is "en_US", then the returned array will be (in this order):
     * 
     * <ul>
     *  <li> <code>fdbhelp.txt</code> </li>
     *  <li> <code>fdbhelp_en.txt</code> </li>
     * 	<li> <code>fdbhelp_en_US.txt</code> </li>
     * </ul>
     */
    private static List calculateLocalizedFilenames(String baseName, String extensionWithDot, Locale locale) {
    	List names = new ArrayList();
        String language = locale.getLanguage();
        String country = locale.getCountry();
        String variant = locale.getVariant();

        names.add(baseName + extensionWithDot);

        if (language.length() + country.length() + variant.length() == 0) {
            //The locale is "", "", "".
            return names;
        }
        final StringBuffer temp = new StringBuffer(baseName);
        temp.append('_');
        temp.append(language);
        if (language.length() > 0) {
            names.add(temp.toString() + extensionWithDot);
        }

        if (country.length() + variant.length() == 0) {
            return names;
        }
        temp.append('_');
        temp.append(country);
        if (country.length() > 0) {
            names.add(temp.toString() + extensionWithDot);
        }

        if (variant.length() == 0) {
            return names;
        }
        temp.append('_');
        temp.append(variant);
        names.add(temp.toString() + extensionWithDot);

        return names;
    }
}
