/**
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 *
 * Copyright 2015 Chiori Greene a.k.a. Chiori-chan <me@chiorichan.com>
 * All Right Reserved.
 */
package com.chiorichan.http;

import java.util.Collections;
import java.util.Map;
import java.util.Random;

import com.chiorichan.lang.NonceException;
import com.chiorichan.session.Session;
import com.chiorichan.tasks.Timings;
import com.chiorichan.util.SecureFunc;
import com.google.common.collect.Maps;

/**
 * Provides NONCE persistence and checking
 */
public class Nonce
{
	public static enum NonceLevel
	{
		Disabled, Flexible, PostOnly, GetOnly, Required;

		public static NonceLevel parse( String level )
		{
			if ( level == null || level.length() == 0 || level.equalsIgnoreCase( "flexible" ) )
				return Flexible;
			if ( level.equalsIgnoreCase( "postonly" ) )
				return PostOnly;
			if ( level.equalsIgnoreCase( "getonly" ) )
				return GetOnly;
			if ( level.equalsIgnoreCase( "disabled" ) )
				return Disabled;
			if ( level.equalsIgnoreCase( "required" ) || level.equalsIgnoreCase( "require" ) )
				return Required;
			throw new IllegalArgumentException( String.format( "Nonce level %s is not available, the available options are Disabled, Flexible, PostOnly, GetOnly, and Required.", level ) );
		}
	}

	private String key;
	private String value;
	private String sessId;
	private int created = Timings.epoch();

	private Map<String, String> mapValues = Maps.newHashMap();

	public Nonce( Session sess )
	{
		Random r = SecureFunc.random();

		key = SecureFunc.randomize( r, "Z1111Y2222" );
		value = SecureFunc.base64Encode( sess.getSessId() + created + SecureFunc.randomize( r, 16 ) );
		sessId = sess.getSessId();
	}

	public String key()
	{
		return key;
	}

	Map<String, String> mapValues()
	{
		return Collections.unmodifiableMap( mapValues );
	}

	public void mapValues( Map<String, String> values )
	{
		mapValues.putAll( values );
	}

	public void mapValues( String key, String val )
	{
		mapValues.put( key, val );
	}

	public String query()
	{
		return key + "=" + value;
	}

	@Override
	public String toString()
	{
		return "<input type=\"hidden\" name=\"" + key + "\" value=\"" + value + "\" />";
	}

	public boolean validate( String token )
	{
		try
		{
			validateWithException( token );
		}
		catch ( NonceException e )
		{
			return false;
		}
		return true;
	}

	public void validateWithException( String token ) throws NonceException
	{
		if ( !value.equals( token ) )
			throw new NonceException( "The NONCE token does not match" );

		String decoded = SecureFunc.base64DecodeString( token );

		if ( !sessId.equals( decoded.substring( 0, sessId.length() ) ) )
			// This was generated for a different Session
			throw new NonceException( "The NONCE did not match the current session" );

		int epoch = Integer.parseInt( decoded.substring( sessId.length(), decoded.length() - 16 ) );

		if ( epoch != created )
			throw new NonceException( "The NONCE has an invalid timestamp" );
	}

	public String value()
	{
		return value;
	}
}
