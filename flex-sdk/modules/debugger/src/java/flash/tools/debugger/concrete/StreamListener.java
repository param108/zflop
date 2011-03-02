////////////////////////////////////////////////////////////////////////////////
//
//  ADOBE SYSTEMS INCORPORATED
//  Copyright 2005-2006 Adobe Systems Incorporated
//  All Rights Reserved.
//
//  NOTICE: Adobe permits you to use, modify, and distribute this file
//  in accordance with the terms of the license agreement accompanying it.
//
////////////////////////////////////////////////////////////////////////////////

package flash.tools.debugger.concrete;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;

/**
 * Reads a stream, and sends the contents somewhere.
 * @author mmoreart
 */
public class StreamListener extends Thread {
	Reader fIn;
	Writer fOut;

	/**
	 * Creates a StreamListener which will copy everything from
	 * 'in' to 'out'.
	 * @param in the stream to read
	 * @param out the stream to write to, or 'null' to discard input
	 */
	public StreamListener(Reader in, Writer out)
	{
		super("DJAPI StreamListener"); //$NON-NLS-1$
		setDaemon(true);
		fIn = in;
		fOut = out;
	}

	public void run()
	{
		char[] buf = new char[4096];
		int count;

		try {
			for (;;) {
				count = fIn.read(buf);
				if (count == -1)
					return; // thread is done
				if (fOut != null)
				{
					try {
						fOut.write(buf, 0, count);
					} catch (IOException e) {
						// the write failed (unlikely), but we still
						// want to keep reading
					}
				}
			}
		} catch (IOException e) {
			// do nothing -- we're done
		}
	}
}
