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

package flash.util;

import java.util.Comparator;
import java.util.Arrays;

/**
 * This class servers as a proxy to java.util.Arrays so that the
 * J# implementation does not need to directly reference this class.
 *
 * See macromedia/util/ArrayUtil.jsl for the .NET implementation.
 *  
 */
public class ArrayUtil
{
    public ArrayUtil()
    {
    }

    public static void sort(Object[] a)
    {
        Arrays.sort(a);
    }

    public static void sort(Object[] a, Comparator c)
    {
        Arrays.sort(a, c);
    }

    public static boolean equals(Object[] a1, Object[] a2)
    {
        return Arrays.equals(a1, a2);
    }

    public static boolean equals(byte[] a1, byte[] a2)
    {
        return Arrays.equals(a1, a2);
    }

    public static boolean equals(long[] a1, long[] a2)
    {
        return Arrays.equals(a1, a2);
    }

    public static boolean equals(int[] a1, int[] a2)
    {
        return Arrays.equals(a1, a2);
    }

    public static boolean equals(double[] a1, double[] a2)
    {
        return Arrays.equals(a1, a2);
    }

    public static boolean equals(char[] a1, char[] a2)
    {
        return Arrays.equals(a1, a2);
    }

    public static boolean equals(short[] a1, short a2[])
    {
        return Arrays.equals(a1, a2);
    }
}
