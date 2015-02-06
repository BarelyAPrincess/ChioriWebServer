/**
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * Copyright 2015 Chiori-chan. All Right Reserved.
 * 
 * @author Chiori Greene
 * @email chiorigreene@gmail.com
 */
package vnet.java.util;

import java.sql.Connection;

/**
 * Mysql Utilities
 * Ralph Ritoch 2011 ALL RIGHTS RESERVED
 * 
 * @author Ralph Ritoch <rritoch@gmail.com>
 * @link http://www.vnetpublishing.com
 * 
 */
public class MySQLUtils
{
	/**
	 * Escape string to protected against SQL Injection
	 * 
	 * You must add a single quote ' around the result of this function for data, or a backtick ` around table and row
	 * identifiers. If this function returns null than the result should be changed to "NULL" without any quote or
	 * backtick.
	 * 
	 * @param link
	 * @param str
	 * @return String that has been escaped
	 * @throws Exception
	 */
	public static String mysql_real_escape_string( Connection link, String str ) throws Exception
	{
		if ( str == null )
		{
			return null;
		}
		
		if ( str.replaceAll( "[a-zA-Z0-9_!@#$%^&*()-=+~.;:,\\Q[\\E\\Q]\\E<>{}\\/? ]", "" ).length() < 1 )
		{
			return str;
		}
		
		String cleanString = str;
		cleanString = cleanString.replaceAll( "\\\\", "\\\\\\\\" );
		cleanString = cleanString.replaceAll( "\\n", "\\\\n" );
		cleanString = cleanString.replaceAll( "\\r", "\\\\r" );
		cleanString = cleanString.replaceAll( "\\t", "\\\\t" );
		cleanString = cleanString.replaceAll( "\\00", "\\\\0" );
		cleanString = cleanString.replaceAll( "'", "\\\\'" );
		cleanString = cleanString.replaceAll( "\\\"", "\\\\\"" );
		
		if ( cleanString.replaceAll( "[a-zA-Z0-9_!@#$%^&*()-=+~.;:,\\Q[\\E\\Q]\\E<>{}\\/?\\\\\"' ]", "" ).length() < 1 )
		{
			return cleanString;
		}
		
		java.sql.Statement stmt = link.createStatement();
		String qry = "SELECT QUOTE('" + cleanString + "')";
		
		stmt.executeQuery( qry );
		java.sql.ResultSet resultSet = stmt.getResultSet();
		resultSet.first();
		String r = resultSet.getString( 1 );
		return r.substring( 1, r.length() - 1 );
	}
	
	public static String escape( String str )
	{
		if ( str == null )
		{
			return null;
		}
		
		if ( str.replaceAll( "[a-zA-Z0-9_!@#$%^&*()-=+~.;:,\\Q[\\E\\Q]\\E<>{}\\/? ]", "" ).length() < 1 )
		{
			return str;
		}
		
		String cleanString = str;
		cleanString = cleanString.replaceAll( "\\\\", "\\\\\\\\" );
		cleanString = cleanString.replaceAll( "\\n", "\\\\n" );
		cleanString = cleanString.replaceAll( "\\r", "\\\\r" );
		cleanString = cleanString.replaceAll( "\\t", "\\\\t" );
		cleanString = cleanString.replaceAll( "\\00", "\\\\0" );
		cleanString = cleanString.replaceAll( "'", "\\\\'" );
		cleanString = cleanString.replaceAll( "\\\"", "\\\\\"" );
		
		if ( cleanString.replaceAll( "[a-zA-Z0-9_!@#$%^&*()-=+~.;:,\\Q[\\E\\Q]\\E<>{}\\/?\\\\\"' ]", "" ).length() < 1 )
		{
			return cleanString;
		}
		
		return cleanString;
	}
	
	/**
	 * Escape data to protected against SQL Injection
	 * 
	 * @param link
	 * @param str
	 * @return String thats been escaped
	 * @throws Exception
	 */
	
	public static String quote( Connection link, String str ) throws Exception
	{
		if ( str == null )
		{
			return "NULL";
		}
		return "'" + mysql_real_escape_string( link, str ) + "'";
	}
	
	/**
	 * Escape identifier to protected against SQL Injection
	 * 
	 * @param link
	 * @param str
	 * @return String that has been escaped
	 * @throws Exception
	 */
	
	public static String nameQuote( Connection link, String str ) throws Exception
	{
		if ( str == null )
		{
			return "NULL";
		}
		return "`" + mysql_real_escape_string( link, str ) + "`";
	}
	
}
