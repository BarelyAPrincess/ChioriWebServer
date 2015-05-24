/**
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * Copyright 2015 Chiori-chan. All Right Reserved.
 */
package com.chiorichan.util;

import java.util.Arrays;
import java.util.Random;
import java.util.Set;

import com.google.common.collect.Sets;
import com.google.common.primitives.Chars;

/**
 * Provides basic encryption and randomizing functions
 * 
 * @author Chiori Greene, a.k.a. Chiori-chan {@literal <me@chiorichan.com>}
 */
public class RandomFunc
{
	private static final char[] randomCharMap;
	private static final char[] allowedCharMap;
	
	static
	{
		Set<Character> newRandomCharMap = Sets.newHashSet();
		
		for ( int i = 33; i < 48; i++ )
			newRandomCharMap.add( ( char ) i );
		
		for ( int i = 58; i < 65; i++ )
			newRandomCharMap.add( ( char ) i );
		
		for ( int i = 91; i < 97; i++ )
			newRandomCharMap.add( ( char ) i );
		
		for ( int i = 123; i < 128; i++ )
			newRandomCharMap.add( ( char ) i );
		
		newRandomCharMap.addAll( Sets.newHashSet( Arrays.asList( new Character[] {128, 131, 134, 135, 138, 140, 142, 156, 158, 159, 161, 162, 163, 165, 167, 176, 181, 191} ) ) );
		
		for ( int i = 192; i < 256; i++ )
			newRandomCharMap.add( ( char ) i );
		
		randomCharMap = Chars.toArray( newRandomCharMap );
		newRandomCharMap = null;
		
		Set<Character> newAllowedCharMap = Sets.newHashSet();
		
		for ( int i = 33; i < 127; i++ )
			newAllowedCharMap.add( ( char ) i );
		
		newAllowedCharMap.addAll( Sets.newHashSet( Arrays.asList( new Character[] {128, 131, 134, 135, 138, 140, 142, 156, 158, 159, 161, 162, 163, 165, 167, 176, 181, 191} ) ) );
		
		for ( int i = 192; i < 256; i++ )
			newAllowedCharMap.add( ( char ) i );
		
		allowedCharMap = Chars.toArray( newAllowedCharMap );
		newAllowedCharMap = null;
	}
	
	/**
	 * Selects a random character between 0-255 using specified start and end arguments
	 * 
	 * @param start
	 *            The minimum character to select
	 * @param end
	 *            The maximum character to select
	 * @return The randomly selected character
	 */
	public static char randomize( int start, int end )
	{
		if ( start > end )
			throw new RuntimeException( "Start can't be greater than end!" );
		
		return ( char ) ( start + new Random().nextInt( end - start ) );
	}
	
	/**
	 * Selects a random character between 33-126, 128, 131, 134, 135, 138, 140, 142, 156, 158, 159, 161, 162, 163, 165, 167, 176, 181, 191, and 192-255
	 * 
	 * @return The randomly selected character
	 */
	public static char randomize()
	{
		return allowedCharMap[new Random().nextInt( allowedCharMap.length )];
	}
	
	/**
	 * Takes the input character and scrambles it
	 * 
	 * @param chr
	 *            Random base character<br>
	 *            <i>A-Z</i> will result in a random uppercase character<br>
	 *            <i>a-z</i> will result in a random lowercase character<br>
	 *            <i>0-9</i> will result in a random number character<br>
	 *            <i>All others will result in a random symbol or accented character</i>
	 * @return Randomized character based on the original
	 */
	public static char randomize( char chr )
	{
		if ( chr > 64 && chr < 91 ) // Uppercase
			return randomize( 65, 90 );
		
		if ( chr > 96 && chr < 123 ) // Lowercase
			return randomize( 97, 122 );
		
		if ( chr > 47 && chr < 58 ) // Numeric
			return randomize( 48, 57 );
		
		return randomCharMap[new Random().nextInt( randomCharMap.length )];
	}
	
	/**
	 * Takes each character of the provided string and scrambles it<br>
	 * Example: 0xx0000$X <i>could</i> result in 9at2015&Z
	 * 
	 * @param base
	 *            The base pattern to follow<br>
	 *            <i>A-Z</i> will result in a random uppercase character<br>
	 *            <i>a-z</i> will result in a random lowercase character<br>
	 *            <i>0-9</i> will result in a random number character<br>
	 *            <i>All others will result in a random symbol or accented character</i>
	 * @return String randomized using your original base string
	 */
	public static String randomize( String base )
	{
		String output = "";
		
		for ( int i = 0; i < base.length(); i++ )
		{
			char chr = base.charAt( i );
			chr = randomize( chr );
			output += chr;
		}
		
		return output;
	}
}
