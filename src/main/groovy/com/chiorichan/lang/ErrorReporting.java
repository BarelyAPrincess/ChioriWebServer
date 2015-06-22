/**
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 *
 * Copyright 2015 Chiori Greene a.k.a. Chiori-chan <me@chiorichan.com>
 * All Right Reserved.
 */
package com.chiorichan.lang;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import com.google.common.collect.Lists;

/**
 * Represents the current error reporting level
 */
public enum ErrorReporting
{
	E_ALL, E_DEPRECATED, E_ERROR, E_IGNORABLE, E_NOTICE, E_PARSE, E_STRICT, E_USER_DEPRECATED, E_USER_ERROR, E_USER_NOTICE, E_USER_WARNING, E_WARNING;
	
	private static final List<ErrorReporting> enabledErrorLevels = new ArrayList<ErrorReporting>( Arrays.asList( parse( "E_ALL ~E_NOTICE ~E_STRICT ~E_DEPRECATED" ) ) );
	
	private int lastLevel = 0;
	final int level;
	
	ErrorReporting()
	{
		level = lastLevel++;
	}
	
	public static boolean disableErrorLevel( ErrorReporting... level )
	{
		return enabledErrorLevels.removeAll( Arrays.asList( parse( level ) ) );
	}
	
	public static boolean enableErrorLevel( ErrorReporting... level )
	{
		return enabledErrorLevels.addAll( Arrays.asList( parse( level ) ) );
	}
	
	public static boolean enableErrorLevelOnly( ErrorReporting... level )
	{
		enabledErrorLevels.clear();
		return enableErrorLevel( level );
	}
	
	public static List<ErrorReporting> getEnabledErrorLevels()
	{
		return Collections.unmodifiableList( enabledErrorLevels );
	}
	
	public static boolean isEnabledLevel( ErrorReporting level )
	{
		return enabledErrorLevels.contains( level );
	}
	
	public static ErrorReporting[] parse( ErrorReporting... level )
	{
		List<ErrorReporting> levels = Lists.newArrayList();
		for ( ErrorReporting er : level )
			levels.addAll( Arrays.asList( parse( er ) ) );
		return levels.toArray( new ErrorReporting[0] );
	}
	
	public static ErrorReporting[] parse( ErrorReporting level )
	{
		switch ( level )
		{
			case E_ALL:
				return new ErrorReporting[] {E_ERROR, E_WARNING, E_PARSE, E_NOTICE, E_USER_ERROR, E_USER_WARNING, E_USER_NOTICE, E_STRICT, E_IGNORABLE, E_DEPRECATED, E_USER_DEPRECATED};
			case E_IGNORABLE:
				return new ErrorReporting[] {E_WARNING, E_NOTICE, E_USER_WARNING, E_USER_NOTICE, E_IGNORABLE, E_DEPRECATED, E_USER_DEPRECATED};
			default:
				return new ErrorReporting[] {level};
		}
	}
	
	public static ErrorReporting[] parse( int level )
	{
		for ( ErrorReporting er : values() )
			if ( er.level == level )
				return new ErrorReporting[] {er};
		return parse( E_ALL );
	}
	
	public static ErrorReporting[] parse( String level )
	{
		List<ErrorReporting> levels = Lists.newArrayList();
		level = level.replaceAll( "&", "" );
		for ( String s : level.split( " " ) )
			if ( s != null )
				if ( s.startsWith( "~" ) || s.startsWith( "!" ) )
					for ( ErrorReporting er : values() )
					{
						if ( er.name().equalsIgnoreCase( s.substring( 1 ) ) )
							levels.removeAll( Arrays.asList( parse( er ) ) );
					}
				else
					for ( ErrorReporting er : values() )
						if ( er.name().equalsIgnoreCase( s ) )
							levels.addAll( Arrays.asList( parse( er ) ) );
		
		return levels.toArray( new ErrorReporting[0] );
	}
	
	public static void throwExceptions( EvalException... exceptions ) throws EvalException, EvalMultipleException
	{
		List<EvalException> exps = Lists.newArrayList();
		
		for ( EvalException e : exceptions )
			if ( !e.isIgnorable() )
				exps.add( e );
		
		if ( exps.size() == 1 )
			throw exps.get( 0 );
		else if ( exps.size() > 0 )
			throw new EvalMultipleException( exps );
	}
	
	public int intValue()
	{
		return level;
	}
	
	public boolean isEnabledLevel()
	{
		return isEnabledLevel( this );
	}
}
