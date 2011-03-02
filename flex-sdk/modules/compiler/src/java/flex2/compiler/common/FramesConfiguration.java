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

package flex2.compiler.common;

import flex2.compiler.config.ConfigurationValue;
import flex2.compiler.config.ConfigurationInfo;
import flex2.compiler.util.NameFormatter;

import java.util.List;
import java.util.LinkedList;
import java.util.Iterator;

/**
 * @author Roger Gonzalez
 *
 * <frames>
 *   <frame>
 *     <label>ick</label>
 *     <className>foo</className>
 *     <className>bar</className>
 *   </frame>
 *   <frame>
 *     <label>asd</label>
 *     <classname>moo</classname>
 *   </frame>
 * </frames>
 *
 */
public class FramesConfiguration
{
    public static class FrameInfo
    {
        public String label = null;
        public List frameClasses = new LinkedList();
    }
    
    //
    // 'frames.frame' option
    //
    
    private List frameList = new LinkedList();

    public List getFrameList()
    {
        return frameList;
    }

    public void cfgFrame( ConfigurationValue cv, List args ) throws flex2.compiler.config.ConfigurationException
    {
        FrameInfo info = new FrameInfo();

        if (args.size() < 2)
            throw new ConfigurationException.BadFrameParameters( cv.getVar(), cv.getSource(), cv.getLine() );

        for (Iterator it = args.iterator(); it.hasNext();)
        {
            if (info.label == null)
            {
                info.label = (String) it.next();
            }
            else
            {
	            String clsName = (String)it.next();
                info.frameClasses.add( NameFormatter.toColon(clsName) );
            }
        }

        frameList.add( info );
    }

    public static ConfigurationInfo getFrameInfo()
    {
        return new ConfigurationInfo( -1, new String[] {"label", "classname"} )
        {
            public boolean isAdvanced()
            {
                return true;
            }

            public boolean allowMultiple()
            {
                return true;
            }
        };
    }
}
