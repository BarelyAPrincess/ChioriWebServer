/**
 * This software may be modified and distributed under the terms
 * of the MIT license.  See the LICENSE file for details.
 *
 * Copyright (c) 2017 Chiori Greene a.k.a. Chiori-chan <me@chiorichan.com>
 * All Rights Reserved
 */
package com.chiorichan.http.multipart;

import java.io.IOException;

/**
 * Thrown to indicate an upload exceeded the maximum size.
 */
public class ExceededSizeException extends IOException
{
	private static final long serialVersionUID = 5121823618984272023L;
	
	/**
	 * Constructs a new ExceededSizeException with no detail message.
	 */
	public ExceededSizeException()
	{
		super();
	}
	
	/**
	 * Constructs a new ExceededSizeException with the specified
	 * detail message.
	 *
	 * @param s
	 *            the detail message
	 */
	public ExceededSizeException( String s )
	{
		super( s );
	}
}
