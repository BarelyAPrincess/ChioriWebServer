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
public enum ReportingLevel
{
	E_ALL( 0xff, false ),
	E_DEPRECATED( 0x09, true ),
	E_ERROR( 0x00, false ),
	E_IGNORABLE( 0x08, true ),
	E_NOTICE( 0x03, true ),
	E_PARSE( 0x02, false ),
	E_STRICT( 0x07, false ),
	E_USER_ERROR( 0x04, false ),
	E_USER_NOTICE( 0x06, true ),
	E_USER_WARNING( 0x05, true ),
	E_WARNING( 0x01, true ),
	L_DEFAULT( 0xf0, true ),
	L_SECURITY( 0xf3, false ),
	L_PERMISSION( 0xf2, false ),
	L_SUCCESS( 0xf1, true ),
	L_DENIED( 0xf5, false ),
	L_ERROR( 0xf6, false ),
	L_EXPIRED( 0xf4, false );
	
	private static final List<ReportingLevel> enabledErrorLevels = new ArrayList<ReportingLevel>( Arrays.asList( parse( "E_ALL ~E_NOTICE ~E_STRICT ~E_DEPRECATED" ) ) );
	
	final int level;
	final boolean ignorable;
	
	ReportingLevel( int level, boolean ignorable )
	{
		this.level = level;
		this.ignorable = ignorable;
	}
	
	public static boolean disableErrorLevel( ReportingLevel... level )
	{
		return enabledErrorLevels.removeAll( Arrays.asList( parse( level ) ) );
	}
	
	public static boolean enableErrorLevel( ReportingLevel... level )
	{
		return enabledErrorLevels.addAll( Arrays.asList( parse( level ) ) );
	}
	
	public static boolean enableErrorLevelOnly( ReportingLevel... level )
	{
		enabledErrorLevels.clear();
		return enableErrorLevel( level );
	}
	
	public static List<ReportingLevel> getEnabledErrorLevels()
	{
		return Collections.unmodifiableList( enabledErrorLevels );
	}
	
	public static boolean isEnabledLevel( ReportingLevel level )
	{
		return enabledErrorLevels.contains( level );
	}
	
	public static ReportingLevel[] parse( int level )
	{
		for ( ReportingLevel er : values() )
			if ( er.level == level )
				return new ReportingLevel[] {er};
		return parse( E_ALL );
	}
	
	public static ReportingLevel[] parse( ReportingLevel... level )
	{
		List<ReportingLevel> levels = Lists.newArrayList();
		for ( ReportingLevel er : level )
			levels.addAll( Arrays.asList( parse( er ) ) );
		return levels.toArray( new ReportingLevel[0] );
	}
	
	public static ReportingLevel[] parse( ReportingLevel level )
	{
		switch ( level )
		{
			case E_ALL:
				return ReportingLevel.values();
			case E_IGNORABLE:
				List<ReportingLevel> levels = Lists.newArrayList();
				for ( ReportingLevel l : ReportingLevel.values() )
					if ( l.isIgnorable() )
						levels.add( l );
				return levels.toArray( new ReportingLevel[0] );
			default:
				return new ReportingLevel[] {level};
		}
	}
	
	public static ReportingLevel[] parse( String level )
	{
		List<ReportingLevel> levels = Lists.newArrayList();
		level = level.replaceAll( "&", "" );
		for ( String s : level.split( " " ) )
			if ( s != null )
				if ( s.startsWith( "~" ) || s.startsWith( "!" ) )
					for ( ReportingLevel er : values() )
					{
						if ( er.name().equalsIgnoreCase( s.substring( 1 ) ) )
							levels.removeAll( Arrays.asList( parse( er ) ) );
					}
				else
					for ( ReportingLevel er : values() )
						if ( er.name().equalsIgnoreCase( s ) )
							levels.addAll( Arrays.asList( parse( er ) ) );
		
		return levels.toArray( new ReportingLevel[0] );
	}
	
	public static String printExceptions( EvalException... exceptions )
	{
		// Might need some better handling for this!
		StringBuilder sb = new StringBuilder();
		for ( EvalException e : exceptions )
			sb.append( e.getMessage() + "\n" );
		return sb.toString();
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
	
	public boolean isIgnorable()
	{
		return ignorable;
	}
	
	public boolean isSuccess()
	{
		return this == ReportingLevel.L_SUCCESS;
	}
}
