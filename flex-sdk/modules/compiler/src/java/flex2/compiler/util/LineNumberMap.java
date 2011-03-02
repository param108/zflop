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

package flex2.compiler.util;

import java.util.SortedMap;
import java.util.TreeMap;

public class LineNumberMap {
	private String oldName, newName;
	private final SortedMap map;
	private final Range lookup;		//	avoid creating new objects on lookup

	public LineNumberMap(String name, String newName) {
		this.oldName = name;
		this.newName = newName;
		this.map = new TreeMap();
		this.lookup = new Range(0, 1);
	}

	public LineNumberMap(String name) {
		this(name, name);
	}

	public final void setNewName(String newName) {
		this.newName = newName;
	}

	public final String getOldName() {
		return oldName;
	}

	public final String getNewName() {
		return newName;
	}

	public final void put(int oldStart, int newStart, int extent) {
		map.put(new Range(newStart, extent), new Integer(newStart - oldStart));
	}

	/**
	 * Maps line numbers in the 'new' code to the 'original' code
	 * @param newLine line number, 1-based
	 * @return 0 if the specified line is not in the ranges.
	 */
	public int get(int newLine) {
		lookup.start = newLine;
		Integer delta = (Integer) map.get(lookup);
		return delta == null ? 0 : newLine - delta.intValue();
	}

	private final class Range implements Comparable {
		private int start, extent;

		Range(int start, int extent) {
			this.start = start;
			this.extent = extent;
		}

		/**
		 * Compares Range starts, then ends. Containment is considered equality. This gives a stable sort and the desired
		 * lookup behavior when a) keys are nonoverlapping, b) lookup values are subranges of keys. Both are true here.
		 * @param o Range object to compare
		 * @return
		 */
		public final int compareTo(Object o) {
			if (!(o instanceof Range)) {
				throw new IllegalArgumentException("argument must be Range");
			}
			Range e = (Range) o;
			return start < e.start ? -1 : (start + extent > e.start + e.extent) ? 1 : 0;
		}

		public String toString()
		{
			return "Range: start = " + start + ", extent = " + extent;
		}
	}

	// error messages
}
