/**
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 *
 * Copyright 2016 Chiori Greene a.k.a. Chiori-chan <me@chiorichan.com>
 * All Right Reserved.
 */
package com.chiorichan.http.ssl;


public enum SslLevel
{
	Ignore, Deny, PostOnly, GetOnly, Preferred, Required;

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
		throw new IllegalArgumentException( String.format( "Ssl level %s is not available, the available options are Deny, Ignore, PostOnly, Preferred, GetOnly, and Required.", level ) );
	}
}
