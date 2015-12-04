/**
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 *
 * Copyright 2015 Chiori Greene a.k.a. Chiori-chan <me@chiorichan.com>
 * All Right Reserved.
 */
package com.chiorichan.http;

import java.util.Random;

import com.chiorichan.session.Session;
import com.chiorichan.tasks.Timings;
import com.chiorichan.util.SecureFunc;

/**
 * 
 */
public class CSRFToken
{
	String csrfKey;
	String csrfValue;
	
	public CSRFToken( Session sess )
	{
		Random r = SecureFunc.random();
		
		csrfKey = SecureFunc.randomize( r, "Z1111Y2222" );
		csrfValue = SecureFunc.base64Encode( sess.getSessId() + Timings.epoch() + SecureFunc.randomize( r, 16 ) );
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
	
	public boolean validate( String token, Session sess )
	{
		if ( !csrfValue.equals( token ) )
			return false;
		
		String decoded = SecureFunc.base64DecodeString( token );
		String sessId = sess.getSessId();
		
		if ( !sessId.equals( decoded.substring( 0, sessId.length() ) ) )
			return false; // This was generated for a different Session
			
		// int epoch = Integer.parseInt( decoded.substring( sessId.length(), decoded.length() - 16 ) );
		
		// TODO Handle checking for replay attacks
		
		return true;
	}
}
