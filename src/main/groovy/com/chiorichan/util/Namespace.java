/**
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 *
 * Copyright 2015 Chiori Greene a.k.a. Chiori-chan <me@chiorichan.com>
 * All Right Reserved.
 */
package com.chiorichan.util;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import org.apache.commons.lang3.ArrayUtils;

import com.google.common.base.CharMatcher;
import com.google.common.base.Joiner;
import com.google.common.base.Splitter;
import com.google.common.collect.Lists;

public class Namespace
{
	protected static Pattern rangeExpression = Pattern.compile( "(0-9+)-(0-9+)" );

	protected String[] nodes;

	public Namespace( List<String> nodes )
	{
		this.nodes = nodes.toArray( new String[0] );
	}

	public Namespace( String... namespace )
	{
		nodes = StringFunc.toLowerCase( namespace );
	}

	public Namespace( String namespace )
	{
		if ( namespace == null )
			namespace = "";

		nodes = namespace.toLowerCase().split( "\\." );
	}

	public Namespace append( String... nodes )
	{
		List<String> nodeList = Lists.newArrayList();
		for ( int i = 0; i < nodes.length; i++ )
			nodeList.addAll( Splitter.on( new CharMatcher()
			{
				@Override
				public boolean matches( char c )
				{
					return c == '|' || c == '.' || c == '/' || c == '\\';
				}
			} ).splitToList( nodes[i] ) );
		return new Namespace( ArrayUtils.addAll( this.nodes, nodeList.toArray( new String[0] ) ) );
	}

	/**
	 * Checks is namespace only contains valid characters.
	 *
	 * @return True if namespace contains only valid characters
	 */
	public boolean containsOnlyValidChars()
	{
		for ( String n : nodes )
			if ( !n.matches( "[a-z0-9_]*" ) )
				return false;
		return true;
	}

	public boolean containsRegex()
	{
		for ( String s : nodes )
			if ( s.contains( "*" ) || s.matches( ".*[0-9]+-[0-9]+.*" ) )
				return true;
		return false;
	}

	/**
	 * Filters out invalid characters from namespace.
	 *
	 * @return The fixed PermissionNamespace.
	 */
	public Namespace fixInvalidChars()
	{
		String[] result = new String[nodes.length];
		for ( int i = 0; i < nodes.length; i++ )
			result[i] = nodes[i].replaceAll( "[^a-z0-9_]", "" );
		return new Namespace( result );
	}

	public String getFirst()
	{
		return getNode( 0 );
	}

	public String getLast()
	{
		return getNode( getNodeCount() - 1 );
	}

	public String getLocalName()
	{
		return nodes[nodes.length - 1];
	}

	public String getNamespace()
	{
		return Joiner.on( "." ).join( nodes );
	}

	public String getNode( int inx )
	{
		try
		{
			return nodes[inx];
		}
		catch ( IndexOutOfBoundsException e )
		{
			return null;
		}
	}

	public int getNodeCount()
	{
		return nodes.length;
	}

	public String[] getNodes()
	{
		return nodes;
	}

	public String getNodeWithException( int inx )
	{
		return nodes[inx];
	}

	public String getParent()
	{
		if ( nodes.length == 1 )
			return "";

		if ( nodes.length < 1 )
			return "";

		return Joiner.on( "." ).join( Arrays.copyOf( nodes, nodes.length - 1 ) );
	}

	public Namespace getParentNamespace()
	{
		return new Namespace( getParent() );
	}

	public String getRootName()
	{
		return nodes[0];
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

	public int matchPercentage( String namespace )
	{
		if ( namespace == null )
			namespace = "";

		String[] dest = namespace.toLowerCase().split( "\\." );

		int total = 0;
		int perNode = 99 / nodes.length;

		for ( int i = 0; i < Math.min( nodes.length, dest.length ); i++ )
			if ( nodes[i].equals( dest[i] ) )
				total += perNode;
			else
				break;

		if ( nodes.length == dest.length )
			total += 1;

		return total;
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
						range.append( "|" );
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

	public Namespace prepend( String... nodes )
	{
		List<String> nodeList = Lists.newArrayList();
		for ( int i = 0; i < nodes.length; i++ )
			nodeList.addAll( Splitter.on( new CharMatcher()
			{
				@Override
				public boolean matches( char c )
				{
					return c == '|' || c == '.' || c == '/' || c == '\\';
				}
			} ).splitToList( nodes[i] ) );
		return new Namespace( ArrayUtils.addAll( nodeList.toArray( new String[0] ), this.nodes ) );
	}

	public Namespace reverseOrder()
	{
		List<String> tmpNodes = Arrays.asList( nodes );
		Collections.reverse( tmpNodes );
		return new Namespace( tmpNodes );
	}

	public Namespace subNamespace( int start )
	{
		return subNamespace( start, getNodeCount() - 1 );
	}

	public Namespace subNamespace( int start, int end )
	{
		return new Namespace( subNodes( start, end ) );
	}

	public String[] subNodes( int start, int end )
	{
		if ( start < 0 )
			throw new IllegalArgumentException( "Start can't be less than 0" );
		if ( start > nodes.length )
			throw new IllegalArgumentException( "Start can't be more than length " + nodes.length );
		if ( end > nodes.length )
			throw new IllegalArgumentException( "Start can't be more than node count" );
		return Arrays.copyOfRange( nodes, start, end );
	}

	@Override
	public String toString()
	{
		return getNamespace();
	}
}
