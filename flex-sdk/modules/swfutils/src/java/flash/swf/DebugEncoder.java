////////////////////////////////////////////////////////////////////////////////
//
//  ADOBE SYSTEMS INCORPORATED
//  Copyright 2003-2006 Adobe Systems Incorporated
//  All Rights Reserved.
//
//  NOTICE: Adobe permits you to use, modify, and distribute this file
//  in accordance with the terms of the license agreement accompanying it.
//
////////////////////////////////////////////////////////////////////////////////

package flash.swf;

import flash.swf.debug.DebugModule;
import flash.swf.debug.LineRecord;
import flash.swf.debug.RegisterRecord;

import flash.swf.types.FlashUUID;

import java.io.IOException;
import java.io.OutputStream;
import java.io.File;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Iterator;

/**
 * Encoder for writing out SWD files in a canonical order.
 *
 * @author Edwin Smith
 * @author Gordon Smith
 *
 * The MXML compiler uses DebugEncoder to output a SWD file
 * containing debugging information for use by fdb.
 *
 * The compiler incrementally feeds debugging data to this class
 * by calling its offset() method. The debugging data is stored
 * in DebugScript, DebugOffset, and DebugBreakpoint objects
 * so that it can be massaged and sorted into a canonical order
 * appropriate for fdb before being serialized as a SWD.
 *
 * The organization of a canonical Royale SWD is as follows:
 *
 * The kDebugID tag follows immediately after the SWD header.
 *
 * The kDebugScript tags appear in the order in which fdb
 * wants to display them:
 * - first the main MXML file for the application,
 *   with its "bitmap" field set to 1;
 * - then other authored files in alphabetical order, with bitmap=2;
 * - then framework files in alphabetical order, with bitmap=3;
 * - then pairs of synthetic files for the various components
 *   in alphabetical order, with bitmap=4;
 * - finally the synthetic "frame 1 actions" of the application,
 *   with bitmap=5.
 *
 * Alphabetical order refers to the short fdb display name
 * ("Button.as"), not the complete module name
 * ("mx.controls.Button: C:\Royale\...\mx\controls\Button.as").
 *
 * The synthetic modules get renamed to <MyComponent.1>,
 * <MyComponent.2>, and <main>.
 *
 * After this sorting by name, the script id numbers are renumbered
 * to be sequential, starting with 1.
 *
 * Immediately after each kDebugScript tag come the kDebugOffset tags
 * that reference that kDebugScript, ordered by line number and
 * subordered by byte offset.
 *
 * Royale emits no kDebugBreakpoint tags in its SWDs, but if it did
 * they would come last, ordered by offset.
 *
 */
public class DebugEncoder implements DebugHandler
{
    /*
     * Storage for the information that DebugEncoder
     * will encode into the header of the SWD file.
     * This is set when a client of DebugEncoder calls header().
     */
    private int version;

    /*
     * Storage for the information that DebugEncoder
     * will encode into a kDebugID tag in the SWD file.
     * This is set when a client of DebugEncoder calls uuid() or updateUUID().
     */
    private byte[] debugID;

    /*
     * A collection of DebugScript objects to be encoded into kDebugScript
     * tags in the SWD file. This collection is built by repeated calls
     * to offset() by clients of DebugEncoder. (Clients can also call module()
     * directly, but the MXML compiler only calls offset().)
     */
    private List debugScripts;

    /*
     * DebugScript objects are used only by this DebugEncoder class.
     * Each DebugScript object contains the information that DebugEncoder
     * will encode into one kDebugScript tag in the SWD file.
     */
    private class DebugScript implements Comparable
    {
        int bitmap;
        String name;
        String text;

        /*
         * A collection of DebugOffset objects to be encoded into kDebugOffset
         * tags in the SWD file. This collection is built by repeated calls
         * to offset() by clients of DebugEncoder. Since each DebugOffset
         * is associated with one DebugScript, they are attached to their
         * corresponding DebugScript rather than directly to the DebugEncoder.
         */
        List debugOffsets;

        String comparableName;

        DebugScript(String name, String text)
        {
            this.bitmap = 0;
            this.name = name;
            this.text = text;

            this.debugOffsets = new ArrayList();
        }

        /**
         * Implement Comparable interface for sorting DebugScripts
         */
        public int compareTo(Object o)
        {
            DebugScript other = (DebugScript)o;
            return comparableName.compareTo(other.comparableName);
        }
    }

    /*
     * DebugOffset objects are used only by this DebugEncoder class.
     * Each DebugOffset object stores the information that DebugEncoder
     * will encode into one kDebugOffset tag in the SWD file.
     * They are created by repeated calls to offset() by a client
     * of DebugEncoder.
     */
    private class DebugOffset implements Comparable
    {
        DebugOffset(int lineNumber, int byteOffset)
        {
            this.lineNumber = lineNumber;
            this.byteOffset = byteOffset;
        }
        int lineNumber;
        int byteOffset;

        /**
          * Implement Comparable interface for sorting DebugOffsets.
          */
         public int compareTo(Object o)
         {
             DebugOffset other = (DebugOffset)o;
             long a = (((long)lineNumber) << 32) | byteOffset;
             long b = (((long)other.lineNumber) << 32) | other.byteOffset;
             if (a < b)
                return -1;
             else if (a > b)
                return 1;
             else
                return 0;
         }
    }

    /*
     * A name -> DebugScript map used to quickly determine whether we
     * have already created a DebugScript with a particular name.
     * Note: We can't use the id in the DebugModule passed to offset()
     * for this purpose, because it isn't unique! All the MXML files
     * for the application and its components come in with id 0!
     */
    private HashMap debugScriptsByName;

    /*
     * A collection of DebugBreakpoint objects to be encoded into
     * kDebugBreakpoint tags in the SWD file. This collection is built
     * by repeated calls to breakpoint() by clients of DebugEncoder.
     */
    private List debugBreakpoints;

    /*
     * DebugOffset objects are used only by this DebugEncoder class.
     * Each DebugBreakpoint object stores the information that DebugEncoder
     * will encode into one kDebugBreakpoint tag in the SWD file.
     */
    private class DebugBreakpoint implements Comparable
    {
        DebugBreakpoint(int offset)
        {
            this.offset = offset;
        }

        int offset;

        /**
         * Implement Comparable interface for sorting DebugBreakpoints.
         */
        public int compareTo(Object o)
        {
            DebugBreakpoint other = (DebugBreakpoint)o;
            return new Integer(offset).compareTo(new Integer(other.offset));
        }
    }

    /*
     * A collection of DebugRegister objects to be encoded into
     * kDebugRegister tags in the SWD file. This collection is built
     * by repeated calls to registers() by clients of DebugEncoder.
     */
    private List debugRegisters;

    /*
     * DebugRegisters objects are used only by this DebugEncoder class.
     * Each DebugRegisters object stores the information that DebugEncoder
     * will encode into one kDebugRegisters tag in the SWD file.
     */
    private class DebugRegisters implements Comparable
    {
        DebugRegisters(int offset, RegisterRecord r)
        {
            this.offset = offset;
			this.registerNumbers = r.registerNumbers;
			this.variableNames = r.variableNames;
        }

        int offset;
		int[] registerNumbers;
		String[] variableNames;

        /**
         * Implement Comparable interface for sorting DebugRegisters.
         */
        public int compareTo(Object o)
        {
            DebugRegisters other = (DebugRegisters)o;
            return new Integer(offset).compareTo(new Integer(other.offset));
        }
    }

    /*
     * The MXML compiler sets this by calling setMainDebugScript()
     * in order to specify which DebugScript should be first in the SWD,
     * with id 1. fdb can then make this script the initial current script
     * for a debugging session.
     */
    private String mainDebugScriptName;

    /*
     * A public property of DebugEncoder used by clients to adjust
     * the bytecode offsets for subsequent calls to offset().
     * (From relative-to-beginning-of-byte-code-for-one-module
     * to relative-to-beginning-of-SWD-file?)
     */
    public int adjust;

    public DebugEncoder()
    {
        debugScripts = new ArrayList();
        debugScriptsByName = new HashMap();
        debugBreakpoints = new ArrayList();
		debugRegisters = new ArrayList();
   }

    public void header(int version)
    {
        this.version = version;
    }

    public void uuid(FlashUUID uuid)
    {
        debugID = uuid.bytes;
    }

    void updateUUID(byte[] uuid)
    {
        debugID = uuid;
    }

    public void offset(int offset, LineRecord lr)
    {
        //System.out.print(lr.module.id + " " + lr.module.name + " " + lr.lineno + " " + offset + "\n");

        // NOTE: Each DebugModule coming in to this method via the
        // LineRecord doesn't have a unique id!
        // In particular, lr.module.id is 0 for all MXML files.
        // And the others are generated by a random number generator
        // that could conceivably repeat.
        // Therefore there is no point at even looking at the id.

		// Module name strings arrive here in various formats:
        // An MXML file (application or component) is a full path like
        // "C:\Royale\flash\experience\royale\apps\dev.war\checkinTest\checkinTest.mxml".
        // An ActionScript component or a framework package combines
        // a package name with a full path, as in
        // "custom.as.myBox: C:\Royale\flash\experience\royale\apps\dev.war\checkinTest\custom\as\myBox.as"
        // or
        // "mx.core.UIComponent: C:\Royale\flash\experience\royale\apps\dev.war\WEB-INF\flex\frameworks\mx\core\UIComponent.as".
        // A framework ActionScript file is a package an
        // Various autogenerated modules look like this:
        // "synthetic: checkinTest";
        // "synthetic: Object.registerClass() for checkinTest";
        // "synthetic: main frame 1 actions".
        // #include files may have non-canonical full paths like
        // "C:\Royale\flash\experience\royale\apps\dev.war\WEB-INF\flex\frameworks\mx\core\..\core\ComponentVersion.as"
        // and must be canonicalized to
        // "C:\Royale\flash\experience\royale\apps\dev.war\WEB-INF\flex\frameworks\mx\core\ComponentVersion.as"
        // so that they don't show up multiple times in an fdb file listing.

		/* C: The debug module name conversion is now centralized in DebugDecoder.adjustModuleName().
        if (lr.module.name.indexOf(": ") < 0)
        {
            lr.module.name = FileUtils.canonicalPath(lr.module.name);
            //System.out.print("*** " + lr.module.name + "\n");
        }
		*/

		// Don't bother to record corrupted modules
		if (lr.module.corrupt)
			return;

        // If we haven't already created a DebugScript for the
        // module referenced by the specified LineRecord, do so.
        String name = lr.module.name;
        if (!debugScriptsByName.containsKey(name))
        {
            module(lr.module);
        }

        // Get the DebugScript for this script.
        DebugScript script = (DebugScript)debugScriptsByName.get(name);

        // Create a DebugOffset for the specified lineNumber/byteOffset pair.
        DebugOffset debugOffset =
            new DebugOffset(lr.lineno, offset + adjust);

        // Attach the DebugOffset to the DebugScript it is associated with.
        script.debugOffsets.add(debugOffset);
    }

	public void module(DebugModule m)
    {
		if (m.corrupt)
			return;

        DebugScript script = new DebugScript(m.name, m.text);
        debugScripts.add(script);
        debugScriptsByName.put(script.name, script);
    }

    public void breakpoint(int offset)
    {
        debugBreakpoints.add(new Integer(offset));
    }

	public void registers(int offset, RegisterRecord r)
	{
        // Create a DebugRegister for the specified registers/byteOffset pair.
        DebugRegisters debug = new DebugRegisters(offset + adjust, r);
        debugRegisters.add(debug);
	}

    public void setMainDebugScript(String path)
    {
        mainDebugScriptName = path;
     }

    private static String generateShortName(String name)
    {
        String s = name;

        /* do we have a file name? */
        int dotAt = name.lastIndexOf('.');
        if (dotAt != -1)
        {
            /* yes let's strip the directory off */
            int lastSlashAt = name.lastIndexOf(File.separatorChar, dotAt);
            if (lastSlashAt == -1 && File.separatorChar == '\\')
                lastSlashAt = name.lastIndexOf('/', dotAt);
            s = name.substring(lastSlashAt+1);
        }
        else
        {
            /* not a file name ... */
            s = name;
        }
        return s.trim();
    }

    private void fixNamesAndBitmaps()
    {
        String synthetic = "synthetic: ";
		String actions = "Actions for ";

        Iterator debugScriptIter = debugScripts.iterator();
        while (debugScriptIter.hasNext())
        {
            DebugScript debugScript = (DebugScript)debugScriptIter.next();
            if (isFrameworkClass(debugScript.name))
            {
                // bitmap = 3 => Framework file
                debugScript.bitmap = 3;
            }
            else if (debugScript.name.startsWith(synthetic))
            {
                // bitmap = 4 => Other per-component synthetic files
                // produced by MXML compiler
                debugScript.bitmap = 4;

                String lookFor = "synthetic: Object.registerClass() for ";
                if (debugScript.name.startsWith(lookFor))
                {
                    String componentName = debugScript.name.substring(lookFor.length());
                    debugScript.name = "<" + componentName + ".2>";
                }
                else
                {
					// R: should really check for a collision here...
                    String componentName = debugScript.name.substring(synthetic.length());
					debugScript.name = "<" + componentName + ".1>";
                }
            }
			else if (debugScript.name.startsWith(actions))
			{
                // bitmap = 5 => Actions ...
				debugScript.bitmap = 5;
			}
			else if (debugScript.name.equals(mainDebugScriptName))
			{
				// bitmap = 1 => Main MXML file for application
				debugScript.bitmap = 1;
			}
            else
            {
                // bitmap = 2 => Other file, presumably an MXML or AS file
                // written by the application author
				debugScript.name = DebugDecoder.adjustModuleName(debugScript.name);
                debugScript.bitmap = 2;
            }

            // Set the comparableName field of each DebugScript
            // to the concatenation of the bitmap and the "short name" that fdb uses.
            // This will ensure that DebugScripts are sorted alphabetically
            // within each "bitmap" category.
            debugScript.comparableName = (new Integer(debugScript.bitmap)).toString() +
                                         generateShortName(debugScript.name);
//            System.out.print(debugScript.comparableName + " " + debugScript.name);
         }
    }

    private void encodeSwdData(SwfEncoder buffer)
    {
        // Encode the header.
        buffer.write32(('F') | ('W' << 8) | ('D' << 16) | (version << 24));

        // Encode one kDebugID tag.
        buffer.write32(kDebugID);
        buffer.write(debugID);

        // Encode the kDebugScript and kDebugOffset tags.
        // The kDebugScript tags are in module number order (1,2,3,...).
        // After each one of these are the associated kDebugOffset tags
        // for that module number, in ascending order
        // by line number and byte offset.

        Collections.sort(debugScripts);
        int id = 0;
        Iterator debugScriptIter = debugScripts.iterator();
        while (debugScriptIter.hasNext())
        {
            DebugScript debugScript = (DebugScript)debugScriptIter.next();
            id++;

            buffer.write32(kDebugScript);
            buffer.write32(id);
            buffer.write32(debugScript.bitmap);
            buffer.writeString(debugScript.name);
            buffer.writeString(debugScript.text);

            Collections.sort(debugScript.debugOffsets);
            Iterator debugOffsetIter = debugScript.debugOffsets.iterator();
            while (debugOffsetIter.hasNext())
            {
                DebugOffset debugOffset = (DebugOffset)debugOffsetIter.next();

                buffer.write32(kDebugOffset);
                buffer.write32(id);
                buffer.write32(debugOffset.lineNumber);
                buffer.write32(debugOffset.byteOffset);
            }
        }

        // Encode the kDebugRegister tags
        Collections.sort(debugRegisters);
        Iterator itr = debugRegisters.iterator();
        while (itr.hasNext())
        {
            DebugRegisters debug = (DebugRegisters)itr.next();
			int size = debug.registerNumbers.length;

            buffer.write32(kDebugRegisters);
			buffer.write32(debug.offset);
			buffer.writeUI8(size);
			for(int i=0; i<debug.registerNumbers.length; i++)
			{
				buffer.writeUI8(debug.registerNumbers[i]);
				buffer.writeString(debug.variableNames[i]);
			}
        }
		
        // Encode the kDebugBreakpoint tags
        Collections.sort(debugBreakpoints);
        Iterator debugBreakpointIterator = debugBreakpoints.iterator();
        while (debugBreakpointIterator.hasNext())
        {
            DebugBreakpoint debugBreakpoint =
                (DebugBreakpoint)debugBreakpointIterator.next();

            buffer.write32(kDebugBreakpoint);
            buffer.write32(debugBreakpoint.offset);
        }
    }

    public void writeTo(OutputStream out) throws IOException
    {
        SwfEncoder buffer = new SwfEncoder(version);
        fixNamesAndBitmaps();
        encodeSwdData(buffer);
        buffer.writeTo(out);
    }

	public void error(String msg)
	{
	}

	/**
	 * C: The SWD generation for fdb is... flaky, especially the way to figure out
	 *    the bitmap category based on debug module names. DebugModule and DebugScript
	 *    must be set with a flag indicating whether they are classes, frame actions,
	 *    etc, etc, etc.
	 * 
	 * R: I don't particularly like it either and would prefer it if this stuff
	 *    lived on the fdb side, not in here.
	 */
	private boolean isFrameworkClass(String name)
	{
		boolean isIt = ( name.startsWith("mx.") && name.indexOf(":") != -1 && name.endsWith(".as") )
			|| ( name.indexOf("/mx/") > -1 );

		return isIt;
	}
}
