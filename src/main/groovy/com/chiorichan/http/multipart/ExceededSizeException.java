/**
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 *
 * Copyright 2015 Chiori Greene a.k.a. Chiori-chan <me@chiorichan.com>
 * All Right Reserved.
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
