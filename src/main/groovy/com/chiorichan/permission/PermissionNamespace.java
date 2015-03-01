/**
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * Copyright 2015 Chiori-chan. All Right Reserved.
 * 
 * @author Chiori Greene
 * @email chiorigreene@gmail.com
 */
package com.chiorichan.permission;

import java.util.Arrays;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import com.chiorichan.util.StringUtil;
import com.google.common.base.Joiner;

public class PermissionNamespace
{
	protected static Pattern rangeExpression = Pattern.compile( "(0-9+)-(0-9+)" );
	
	private String[] nodes;
	
	public PermissionNamespace( String... namespace )
	{
		if ( namespace.length < 1 )
			namespace[0] = "";
		
		nodes = StringUtil.toLowerCase( namespace );
	}
	
	public PermissionNamespace( String namespace )
	{
		if ( namespace == null )
			namespace = "";
		
		nodes = namespace.toLowerCase().split( "\\." );
	}
	
	public int getNodeCount()
	{
		return nodes.length;
	}
	
	public String getLocalName()
	{
		return nodes[nodes.length - 1];
	}
	
	public String getRootName()
	{
		return nodes[0];
	}
	
	public String getNamespace()
	{
		return Joiner.on( "." ).join( nodes );
	}
	
	public String getParent()
	{
		if ( nodes.length == 1 )
			return "";
		
		if ( nodes.length < 1 )
			return "";
		
		return Joiner.on( "." ).join( Arrays.copyOf( nodes, nodes.length - 1 ) );
	}
	
	public PermissionNamespace getParentNamespace()
	{
		return new PermissionNamespace( getParent() );
	}
	
	public int matchPercentage( String namespace )
	{
		if ( namespace == null )
			namespace = "";
		
		String[] dest = namespace.toLowerCase().split( "\\." );
		
		int total = 0;
		int perNode = 99 / nodes.length;
		
		for ( int i = 0; i < Math.min( nodes.length, dest.length ); i++ )
		{
			if ( nodes[i].equals( dest[i] ) )
				total += perNode;
			else
				break;
		}
		
		if ( nodes.length == dest.length )
			total += 1;
		
		return total;
	}
	
	/**
	 * Checks is namespace only contains valid characters.
	 * 
	 * @return True if namespace contains only valid characters
	 */
	public boolean containsOnlyValidChars()
	{
		boolean isValid = true;
		
		for ( String n : nodes )
			if ( !n.matches( "[a-z0-9_]*" ) )
				isValid = false;
		
		return isValid;
	}
	
	/**
	 * Filters out invalid characters from namespace.
	 * 
	 * @return The fixed PermissionNamespace.
	 */
	public PermissionNamespace fixInvalidChars()
	{
		String[] result = new String[nodes.length];
		
		for ( int i = 0; i < nodes.length; i++ )
			result[i] = nodes[i].replaceAll( "[^a-z0-9_]", "" );
		
		return new PermissionNamespace( result );
	}
	
	public String[] getNodes()
	{
		return nodes;
	}
	
	public boolean containsRegex()
	{
		boolean containsRegex = false;
		
		for ( String s : nodes )
			if ( s.contains( "*" ) || s.matches( ".*[0-9]+-[0-9]+.*" ) )
				containsRegex = true;
		
		return containsRegex;
	}
	
	public boolean matches( Permission perm )
	{
		return matches( perm.getNamespace() );
	}
	
	public boolean matches( String perm )
	{
		/*
		 * We are not going to try and match a permission if it contains regex.
		 * This means someone must have gotten their strings backward.
		 */
		if ( perm.contains( "*" ) || perm.matches( ".*[0-9]+-[0-9]+.*" ) )
			return false;
		
		return prepareRegexp().matcher( perm ).matches();
	}
	
	/**
	 * Prepares a namespace for parsing via RegEx
	 * 
	 * @return The fully RegEx ready string
	 */
	public Pattern prepareRegexp()
	{
		String regexpOrig = Joiner.on( "\\." ).join( nodes );
		String regexp = regexpOrig.replace( "*", "(.*)" );
		
		try
		{
			Matcher rangeMatcher = rangeExpression.matcher( regexp );
			while ( rangeMatcher.find() )
			{
				StringBuilder range = new StringBuilder();
				int from = Integer.parseInt( rangeMatcher.group( 1 ) );
				int to = Integer.parseInt( rangeMatcher.group( 2 ) );
				
				range.append( "(" );
				
				for ( int i = Math.min( from, to ); i <= Math.max( from, to ); i++ )
				{
					range.append( i );
					if ( i < Math.max( from, to ) )
					{
						range.append( "|" );
					}
				}
				
				range.append( ")" );
				
				regexp = regexp.replace( rangeMatcher.group( 0 ), range.toString() );
			}
		}
		catch ( Throwable e )
		{
		}
		
		try
		{
			return Pattern.compile( regexp, Pattern.CASE_INSENSITIVE );
		}
		catch ( PatternSyntaxException e )
		{
			return Pattern.compile( Pattern.quote( regexpOrig.replace( "*", "(.*)" ) ), Pattern.CASE_INSENSITIVE );
		}
	}
}
