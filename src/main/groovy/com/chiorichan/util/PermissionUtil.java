/**
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * Copyright 2015 Chiori-chan. All Right Reserved.
 * 
 * @author Chiori Greene
 * @email chiorigreene@gmail.com
 */
package com.chiorichan.util;

import org.apache.commons.lang3.StringUtils;

public class PermissionUtil
{
	public static String getLocalName( String permNamespace )
	{
		if ( !permNamespace.contains( "." ) )
			return permNamespace;
		
		int inx = StringUtils.reverse( permNamespace ).indexOf( "." );
		return permNamespace.substring( permNamespace.length() - inx );
	}
	
	public static boolean containsValidChars( String ref )
	{
		return ref.matches( "[a-z0-9_]*" );
	}
	
	public static String removeInvalidChars( String ref )
	{
		return ref.replaceAll( "[^a-z0-9_]", "" );
	}
}
