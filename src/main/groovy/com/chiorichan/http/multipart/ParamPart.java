/**
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * Copyright 2015 Chiori-chan. All Right Reserved.
 * 
 * @author Chiori Greene
 * @email chiorigreene@gmail.com
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
	public boolean isParam()
	{
		return true;
	}
}
