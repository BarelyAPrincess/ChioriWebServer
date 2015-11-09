/**
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 *
 * Copyright 2015 Chiori Greene a.k.a. Chiori-chan <me@chiorichan.com>
 * All Right Reserved.
 */
package com.chiorichan.http;

import com.chiorichan.util.SecureFunc;

/**
 * 
 */
public class CSRFToken
{
	String csrfKey;
	String csrfValue;
	
	public CSRFToken()
	{
		csrfKey = SecureFunc.randomize( "Z1111Y2222" );
		csrfValue = SecureFunc.base64Encode( SecureFunc.seed( 64 ) );
	}
	
	public String formInput()
	{
		return "<input type=\"hidden\" name=\"" + csrfKey + "\" value=\"" + csrfValue + "\" />";
	}
	
	public String getKey()
	{
		return csrfKey;
	}
	
	public String getValue()
	{
		return csrfValue;
	}
	
	@Override
	public String toString()
	{
		return formInput();
	}
}
