/**
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 *
 * Copyright 2015 Chiori Greene a.k.a. Chiori-chan <me@chiorichan.com>
 * All Right Reserved.
 */
package com.chiorichan.http.multipart;

/**
 * A <code>Part</code> is an abstract upload part which represents an <code>INPUT</code> form element in a <code>multipart/form-data</code> form
 * submission.
 */
public abstract class Part
{
	private String name;
	
	/**
	 * Constructs an upload part with the given name.
	 */
	Part( String name )
	{
		this.name = name;
	}
	
	/**
	 * Returns the name of the form element that this Part corresponds to.
	 * 
	 * @return the name of the form element that this Part corresponds to.
	 */
	public String getName()
	{
		return name;
	}
	
	/**
	 * Returns true if this Part is a FilePart.
	 * 
	 * @return true if this is a FilePart.
	 */
	public boolean isFile()
	{
		return false;
	}
	
	/**
	 * Returns true if this Part is a ParamPart.
	 * 
	 * @return true if this is a ParamPart.
	 */
	public boolean isParam()
	{
		return false;
	}
}
