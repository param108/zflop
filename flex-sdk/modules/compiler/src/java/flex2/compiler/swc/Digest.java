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

package flex2.compiler.swc;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * The Digest class represents the values of the digest or signed-digest xml
 * in catalog.xml
 * 
 * @author dloverin
 *
 */
public class Digest
{
	
	/**
	 * Supported hash-types
	 */
	public static final String SHA_256 = "SHA-256";	// $NON-NLS-1$
	
	private String type = SHA_256;	// type of hash used to compute the digest
	private String value;		// digest value obtained using hashType
	private boolean signed;		// true if the digest is of a signed file
	
	
	/**
	 * Create an unsigned digest.
	 *
	 */
	public Digest()
	{
		this(false);
	}
	
	/**
	 * Create a signed or unsigned digest.
	 * 
	 * @param signed - true to create a signed digest, false otherwise.
	 */
	public Digest(boolean signed)
	{
		this.signed = signed;
	}
	
	
	public String getType()
	{
		return type == null ? "" : type;
	}
	
	public void setType(String hashType)
	{
		this.type = hashType;
	}
	
	/**
	 * 
	 * @return digest value or an empty string if there is no digest.
	 */
	public String getValue()
	{
		return value == null ? "" : value;
	}
	
	public void setValue(String value)
	{
		this.value = value;
	}


	public boolean isSigned()
	{
		return signed;
	}

	public void setSigned(boolean signed)
	{
		this.signed = signed;
	}

	/**
	 * Calculates the digest of a specified byte array using the hash type specified
	 * by <code>setType()</code>. The default hash type is "SHA-256".
	 * 
	 * Updates the digest value of this object. To get the new value call 
	 * <code>getValue()</code>.
	 * 
	 * @param byteArray
	 * @return String - the digest value
	 */
	public String computeDigest(byte[] byteArray)
	{
		MessageDigest digest = null;
		try
		{
			digest = MessageDigest.getInstance(type);
		} catch (NoSuchAlgorithmException e)
		{
			// Eat the exception, this should never happen in a
			// production environment.
			value = "Hash not supported"; // $NON-NLS-1$
			return value;
		}

		byte[] digestBytes = digest.digest(byteArray);
		StringBuffer buf = new StringBuffer();
		for (int i = 0; i < digestBytes.length; i++)
		{
			String s = Integer.toHexString(((int)digestBytes[i] & 0xff));
			if (s.length() == 1)
			{
				buf.append("0");
			}

			buf.append(s);
		}
		
		value = buf.toString();
		
		return value;
		
	}
	
	/**
	 * Test if a digest value is available.
	 * 
	 * @return true if a digest if available, false otherwise. 
	 */
	public boolean hasDigest() 
	{
		return value != null;
	}
	
}
