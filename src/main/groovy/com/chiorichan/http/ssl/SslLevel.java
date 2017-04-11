/**
 * This software may be modified and distributed under the terms
 * of the MIT license.  See the LICENSE file for details.
 *
 * Copyright (c) 2017 Chiori Greene a.k.a. Chiori-chan <me@chiorichan.com>
 * Copyright (c) 2017 Penoaks Publishing LLC <development@penoaks.com>
 *
 * All Rights Reserved.
 */
package com.chiorichan.http.ssl;

public enum SslLevel
{
	Ignore, Deny, PostOnly, GetOnly, Preferred, Required;

	public static SslLevel parse( String level, SslLevel def )
	{
		try
		{
			return parse( level );
		}
		catch ( IllegalArgumentException e )
		{
			return def;
		}
	}

	public static SslLevel parse( String level )
	{
		if ( level == null || level.length() == 0 || level.equalsIgnoreCase( "ignore" ) )
			return Ignore;
		if ( level.equalsIgnoreCase( "deny" ) || level.equalsIgnoreCase( "disabled" ) )
			return Deny;
		if ( level.equalsIgnoreCase( "postonly" ) )
			return PostOnly;
		if ( level.equalsIgnoreCase( "getonly" ) )
			return GetOnly;
		if ( level.equalsIgnoreCase( "preferred" ) )
			return Preferred;
		if ( level.equalsIgnoreCase( "required" ) || level.equalsIgnoreCase( "require" ) )
			return Required;
		throw new IllegalArgumentException( String.format( "Ssl level %s is invalid, the available options are Deny, Ignore, PostOnly, Preferred, GetOnly, and Required.", level ) );
	}
}
