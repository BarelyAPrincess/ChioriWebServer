/**
 * This software may be modified and distributed under the terms
 * of the MIT license.  See the LICENSE file for details.
 *
 * Copyright (c) 2017 Chiori Greene a.k.a. Chiori-chan <me@chiorichan.com>
 * Copyright (c) 2017 Penoaks Publishing LLC <development@penoaks.com>
 *
 * All Rights Reserved.
 */
package com.chiorichan.http.multipart;

import java.io.IOException;
import java.io.UnsupportedEncodingException;

/**
 * A <code>ParamPart</code> is an upload part which represents a normal <code>INPUT</code> (for example a non <code>TYPE="file"</code>) form
 * parameter.
 */
public class ParamPart extends Part
{
	/** contents of the parameter */
	private byte[] value;
	
	/**
	 * Constructs a parameter part; this is called by the parser.
	 * 
	 * @param name
	 *            the name of the parameter.
	 * @param buffer
	 *            the servlet input stream to read the parameter value from.
	 * @param boundary
	 *            the MIME boundary that delimits the end of parameter value.
	 * @param encoding
	 *            the byte-to-char encoding to use by default
	 *            value.
	 */
	ParamPart( String name, byte[] data ) throws IOException
	{
		super( name );
		value = data;
	}
	
	/**
	 * Returns the value of the parameter as an array of bytes or a zero length
	 * array if the user entered no value for this parameter.
	 * 
	 * @return value of parameter as raw bytes
	 */
	public byte[] getValue()
	{
		return value;
	}
	
	/**
	 * Returns the value of the parameter in as a string (using the
	 * parser-specified encoding to convert from bytes) or the empty string
	 * if the user entered no value for this parameter.
	 * 
	 * @return value of parameter as a string.
	 */
	public String getStringValue() throws UnsupportedEncodingException
	{
		return getStringValue( "UTF-8" );
	}
	
	/**
	 * Returns the value of the parameter in the supplied encoding
	 * or empty string if the user entered no value for this parameter.
	 * 
	 * @return value of parameter as a string.
	 */
	public String getStringValue( String encoding ) throws UnsupportedEncodingException
	{
		return new String( value, encoding );
	}
	
	/**
	 * Returns <code>true</code> to indicate this part is a parameter.
	 * 
	 * @return true.
	 */
	@Override
	public boolean isParam()
	{
		return true;
	}
}
