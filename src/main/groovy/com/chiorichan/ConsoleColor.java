/**
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * Copyright 2015 Chiori-chan. All Right Reserved.
 * 
 * @author Chiori Greene
 * @email chiorigreene@gmail.com
 */
package com.chiorichan;

import java.util.EnumMap;
import java.util.Map;
import java.util.regex.Pattern;

import org.apache.commons.lang3.Validate;
import org.fusesource.jansi.Ansi;
import org.fusesource.jansi.Ansi.Attribute;

import com.google.common.collect.Maps;

/**
 * All supported color values for chat
 */
public enum ConsoleColor
{
	/**
	 * Represents black
	 */
	BLACK( '0', 0x00 ),
	/**
	 * Represents dark blue
	 */
	DARK_BLUE( '1', 0x1 ),
	/**
	 * Represents dark green
	 */
	DARK_GREEN( '2', 0x2 ),
	/**
	 * Represents dark blue (aqua)
	 */
	DARK_AQUA( '3', 0x3 ),
	/**
	 * Represents dark red
	 */
	DARK_RED( '4', 0x4 ),
	/**
	 * Represents dark purple
	 */
	DARK_PURPLE( '5', 0x5 ),
	/**
	 * Represents gold
	 */
	GOLD( '6', 0x6 ),
	/**
	 * Represents gray
	 */
	GRAY( '7', 0x7 ),
	/**
	 * Represents dark gray
	 */
	DARK_GRAY( '8', 0x8 ),
	/**
	 * Represents blue
	 */
	BLUE( '9', 0x9 ),
	/**
	 * Represents green
	 */
	GREEN( 'a', 0xA ),
	/**
	 * Represents aqua
	 */
	AQUA( 'b', 0xB ),
	/**
	 * Represents red
	 */
	RED( 'c', 0xC ),
	/**
	 * Represents light purple
	 */
	LIGHT_PURPLE( 'd', 0xD ),
	/**
	 * Represents yellow
	 */
	YELLOW( 'e', 0xE ),
	/**
	 * Represents white
	 */
	WHITE( 'f', 0xF ),
	/**
	 * Represents magical characters that change around randomly
	 */
	MAGIC( 'k', 0x10, true ),
	/**
	 * Makes the text bold.
	 */
	BOLD( 'l', 0x11, true ),
	/**
	 * Makes a line appear through the text.
	 */
	STRIKETHROUGH( 'm', 0x12, true ),
	/**
	 * Makes the text appear underlined.
	 */
	UNDERLINE( 'n', 0x13, true ),
	/**
	 * Makes the text italic.
	 */
	ITALIC( 'o', 0x14, true ),
	/**
	 * Resets all previous chat colors or formats.
	 */
	RESET( 'r', 0x15 ),
	
	FAINT( 'z', 0x16 ),
	
	NEGATIVE( 'x', 0x17 );
	
	/**
	 * The special character which prefixes all chat colour codes. Use this if you need to dynamically convert colour
	 * codes from your custom format.
	 */
	public static final char COLOR_CHAR = '\u00A7';
	private static final Pattern STRIP_COLOR_PATTERN = Pattern.compile( "(?i)" + String.valueOf( COLOR_CHAR ) + "[0-9A-FK-OR]" );
	
	private final int intCode;
	private final char code;
	private final boolean isFormat;
	private final String toString;
	private static final Map<Integer, ConsoleColor> BY_ID = Maps.newHashMap();
	private static final Map<Character, ConsoleColor> BY_CHAR = Maps.newHashMap();
	private static Map<ConsoleColor, String> replacements = new EnumMap<ConsoleColor, String>( ConsoleColor.class );
	
	static
	{
		replacements.put( ConsoleColor.BLACK, Ansi.ansi().fg( Ansi.Color.BLACK ).boldOff().toString() );
		replacements.put( ConsoleColor.DARK_BLUE, Ansi.ansi().fg( Ansi.Color.BLUE ).boldOff().toString() );
		replacements.put( ConsoleColor.DARK_GREEN, Ansi.ansi().fg( Ansi.Color.GREEN ).boldOff().toString() );
		replacements.put( ConsoleColor.DARK_AQUA, Ansi.ansi().fg( Ansi.Color.CYAN ).boldOff().toString() );
		replacements.put( ConsoleColor.DARK_RED, Ansi.ansi().fg( Ansi.Color.RED ).boldOff().toString() );
		replacements.put( ConsoleColor.DARK_PURPLE, Ansi.ansi().fg( Ansi.Color.MAGENTA ).boldOff().toString() );
		replacements.put( ConsoleColor.GOLD, Ansi.ansi().fg( Ansi.Color.YELLOW ).boldOff().toString() );
		replacements.put( ConsoleColor.GRAY, Ansi.ansi().fg( Ansi.Color.WHITE ).boldOff().toString() );
		replacements.put( ConsoleColor.DARK_GRAY, Ansi.ansi().fg( Ansi.Color.BLACK ).bold().toString() );
		replacements.put( ConsoleColor.BLUE, Ansi.ansi().fg( Ansi.Color.BLUE ).bold().toString() );
		replacements.put( ConsoleColor.GREEN, Ansi.ansi().fg( Ansi.Color.GREEN ).bold().toString() );
		replacements.put( ConsoleColor.AQUA, Ansi.ansi().fg( Ansi.Color.CYAN ).bold().toString() );
		replacements.put( ConsoleColor.RED, Ansi.ansi().fg( Ansi.Color.RED ).bold().toString() );
		replacements.put( ConsoleColor.LIGHT_PURPLE, Ansi.ansi().fg( Ansi.Color.MAGENTA ).bold().toString() );
		replacements.put( ConsoleColor.YELLOW, Ansi.ansi().fg( Ansi.Color.YELLOW ).bold().toString() );
		replacements.put( ConsoleColor.WHITE, Ansi.ansi().fg( Ansi.Color.WHITE ).bold().toString() );
		replacements.put( ConsoleColor.MAGIC, Ansi.ansi().a( Attribute.BLINK_SLOW ).toString() );
		replacements.put( ConsoleColor.BOLD, Ansi.ansi().a( Attribute.INTENSITY_BOLD ).toString() );
		replacements.put( ConsoleColor.STRIKETHROUGH, Ansi.ansi().a( Attribute.STRIKETHROUGH_ON ).toString() );
		replacements.put( ConsoleColor.UNDERLINE, Ansi.ansi().a( Attribute.UNDERLINE ).toString() );
		replacements.put( ConsoleColor.ITALIC, Ansi.ansi().a( Attribute.ITALIC ).toString() );
		replacements.put( ConsoleColor.FAINT, Ansi.ansi().a( Attribute.INTENSITY_FAINT ).toString() );
		replacements.put( ConsoleColor.NEGATIVE, Ansi.ansi().a( Attribute.NEGATIVE_ON ).toString() );
		replacements.put( ConsoleColor.RESET, Ansi.ansi().a( Attribute.RESET ).fg( Ansi.Color.DEFAULT ).toString() );
	}
	
	private ConsoleColor( char code, int intCode )
	{
		this( code, intCode, false );
	}
	
	private ConsoleColor( char code, int intCode, boolean isFormat )
	{
		this.code = code;
		this.intCode = intCode;
		this.isFormat = isFormat;
		this.toString = new String( new char[] {COLOR_CHAR, code} );
	}
	
	/**
	 * Gets the char value associated with this color
	 * 
	 * @return A char value of this color code
	 */
	public char getChar()
	{
		return code;
	}
	
	@Override
	public String toString()
	{
		return toString;
	}
	
	/**
	 * Checks if this code is a format code as opposed to a color code.
	 */
	public boolean isFormat()
	{
		return isFormat;
	}
	
	/**
	 * Checks if this code is a color code as opposed to a format code.
	 */
	public boolean isColor()
	{
		return !isFormat && this != RESET;
	}
	
	/**
	 * Gets the color represented by the specified color code
	 * 
	 * @param code
	 *            Code to check
	 * @return Associative ConsoleColor with the given code, or null if it doesn't exist
	 */
	public static ConsoleColor getByChar( char code )
	{
		return BY_CHAR.get( code );
	}
	
	/**
	 * Gets the color represented by the specified color code
	 * 
	 * @param code
	 *            Code to check
	 * @return Associative ConsoleColor with the given code, or null if it doesn't exist
	 */
	public static ConsoleColor getByChar( String code )
	{
		Validate.notNull( code, "Code cannot be null" );
		Validate.isTrue( code.length() > 0, "Code must have at least one char" );
		
		return BY_CHAR.get( code.charAt( 0 ) );
	}
	
	/**
	 * Strips the given message of all color codes
	 * 
	 * @param input
	 *            String to strip of color
	 * @return A copy of the input string, without any coloring
	 */
	public static String stripColor( final String input )
	{
		if ( input == null )
		{
			return null;
		}
		
		return STRIP_COLOR_PATTERN.matcher( input ).replaceAll( "" );
	}
	
	/**
	 * Translates a string using an alternate color code character into a string that uses the internal
	 * ConsoleColor.COLOR_CODE color code character. The alternate color code character will only be replaced if it is
	 * immediately followed by 0-9, A-F, a-f, K-O, k-o, R or r.
	 * 
	 * @param altColorChar
	 *            The alternate color code character to replace. Ex: &
	 * @param textToTranslate
	 *            Text containing the alternate color code character.
	 * @return Text containing the ChatColor.COLOR_CODE color code character.
	 */
	public static String translateAlternateColorCodes( char altColorChar, String textToTranslate )
	{
		char[] b = textToTranslate.toCharArray();
		for ( int i = 0; i < b.length - 1; i++ )
		{
			if ( b[i] == altColorChar && "0123456789AaBbCcDdEeFfKkLlMmNnOoRr".indexOf( b[i + 1] ) > -1 )
			{
				b[i] = ConsoleColor.COLOR_CHAR;
				b[i + 1] = Character.toLowerCase( b[i + 1] );
			}
		}
		return new String( b );
	}
	
	public static String removeAltColors( String var )
	{
		var = var.replaceAll( "&.", "" );
		var = var.replaceAll( "§.", "" );
		return var;
	}
	
	public static String transAltColors( String var1 )
	{
		var1 = translateAlternateColorCodes( '&', var1 ) + ConsoleColor.RESET;
		
		for ( ConsoleColor color : values() )
		{
			if ( replacements.containsKey( color ) )
				var1 = var1.replaceAll( "(?i)" + color.toString(), replacements.get( color ) );
			else
				var1 = var1.replaceAll( "(?i)" + color.toString(), "" );
		}
		
		return var1;
	}
	
	/**
	 * Gets the ChatColors used at the end of the given input string.
	 * 
	 * @param input
	 *            Input string to retrieve the colors from.
	 * @return Any remaining ChatColors to pass onto the next line.
	 */
	public static String getLastColors( String input )
	{
		String result = "";
		int length = input.length();
		
		// Search backwards from the end as it is faster
		for ( int index = length - 1; index > -1; index-- )
		{
			char section = input.charAt( index );
			if ( section == COLOR_CHAR && index < length - 1 )
			{
				char c = input.charAt( index + 1 );
				ConsoleColor color = getByChar( c );
				
				if ( color != null )
				{
					result = color.toString() + result;
					
					// Once we find a color or reset we can stop searching
					if ( color.isColor() || color.equals( RESET ) )
					{
						break;
					}
				}
			}
		}
		
		return result;
	}
	
	static
	{
		for ( ConsoleColor color : values() )
		{
			BY_ID.put( color.intCode, color );
			BY_CHAR.put( color.code, color );
		}
	}
}
