package com.chiorichan.util;

import java.awt.Color;
import java.lang.reflect.Field;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.util.Collection;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.lang3.Validate;

public class StringUtil
{
	
	/**
	 * Copies all elements from the iterable collection of originals to the collection provided.
	 * 
	 * @param token
	 *             String to search for
	 * @param originals
	 *             An iterable collection of strings to filter.
	 * @param collection
	 *             The collection to add matches to
	 * @return the collection provided that would have the elements copied into
	 * @throws UnsupportedOperationException
	 *              if the collection is immutable and originals contains a string which starts with the specified search
	 *              string.
	 * @throws IllegalArgumentException
	 *              if any parameter is is null
	 * @throws IllegalArgumentException
	 *              if originals contains a null element. <b>Note: the collection may be modified before this is thrown</b>
	 */
	public static <T extends Collection<String>> T copyPartialMatches( final String token, final Iterable<String> originals, final T collection ) throws UnsupportedOperationException, IllegalArgumentException
	{
		Validate.notNull( token, "Search token cannot be null" );
		Validate.notNull( collection, "Collection cannot be null" );
		Validate.notNull( originals, "Originals cannot be null" );
		
		for ( String string : originals )
		{
			if ( startsWithIgnoreCase( string, token ) )
			{
				collection.add( string );
			}
		}
		
		return collection;
	}
	
	/**
	 * This method uses a substring to check case-insensitive equality. This means the internal array does not need to be
	 * copied like a toLowerCase() call would.
	 * 
	 * @param string
	 *             String to check
	 * @param prefix
	 *             Prefix of string to compare
	 * @return true if provided string starts with, ignoring case, the prefix provided
	 * @throws NullPointerException
	 *              if prefix is null
	 * @throws IllegalArgumentException
	 *              if string is null
	 */
	public static boolean startsWithIgnoreCase( final String string, final String prefix ) throws IllegalArgumentException, NullPointerException
	{
		Validate.notNull( string, "Cannot check a null string for a match" );
		if ( string.length() < prefix.length() )
		{
			return false;
		}
		return string.substring( 0, prefix.length() ).equalsIgnoreCase( prefix );
	}
	
	public static byte[] stringToBytesASCII( String str )
	{
		byte[] b = new byte[str.length()];
		for ( int i = 0; i < b.length; i++ )
		{
			b[i] = (byte) str.charAt( i );
		}
		return b;
	}
	
	public static byte[] stringToBytesUTF( String str )
	{
		byte[] b = new byte[str.length() << 1];
		for ( int i = 0; i < str.length(); i++ )
		{
			char strChar = str.charAt( i );
			int bpos = i << 1;
			b[bpos] = (byte) ( ( strChar & 0xFF00 ) >> 8 );
			b[bpos + 1] = (byte) ( strChar & 0x00FF );
		}
		return b;
	}
	
	public static String bytesToStringUTFNIO( byte[] bytes )
	{
		CharBuffer cBuffer = ByteBuffer.wrap( bytes ).asCharBuffer();
		return cBuffer.toString();
	}
	
	public static String md5( String str )
	{
		return DigestUtils.md5Hex( str );
	}
	
	public static boolean isTrue( String argument )
	{
		Validate.notNull( argument );
		argument = argument.toLowerCase();
		return ( argument.equals( "true" ) || argument.equals( "1" ) );
	}
	
	public static String replaceAt( String par, int at, String rep )
	{
		StringBuilder sb = new StringBuilder( par );
		sb.setCharAt( at, rep.toCharArray()[0] );
		return sb.toString();
	}
	
	public static Color parseColor( String color )
	{
		Pattern c = Pattern.compile( "rgb *\\( *([0-9]+), *([0-9]+), *([0-9]+) *\\)" );
		Matcher m = c.matcher( color );
		
		// First try to parse RGB(0,0,0);
		if ( m.matches() )
			return new Color( Integer.valueOf( m.group( 1 ) ), // r
			Integer.valueOf( m.group( 2 ) ), // g
			Integer.valueOf( m.group( 3 ) ) ); // b
			
		try
		{
			Field field = Class.forName( "java.awt.Color" ).getField( color.trim().toUpperCase() );
			return (Color) field.get( null );
		}
		catch ( Exception e )
		{}
		
		try
		{
			return Color.decode( color );
		}
		catch ( Exception e )
		{}
		
		return null;
	}
	
	/**
	 * Trim specified charcater from front of string
	 * 
	 * @param text
	 *             Text
	 * @param character
	 *             Character to remove
	 * @return Trimmed text
	 */
	public static String trimFront( String text, char character )
	{
		String normalizedText;
		int index;
		
		if ( text == null )
		{
			return text;
		}
		
		normalizedText = text.trim();
		index = 0;
		
		while ( normalizedText.charAt( index ) == character )
		{
			index++;
		}
		return normalizedText.substring( index ).trim();
	}
	
	/**
	 * Trim specified character from end of string
	 * 
	 * @param text
	 *             Text
	 * @param character
	 *             Character to remove
	 * @return Trimmed text
	 */
	public static String trimEnd( String text, char character )
	{
		String normalizedText;
		int index;
		
		if ( text == null )
		{
			return text;
		}
		
		normalizedText = text.trim();
		index = normalizedText.length() - 1;
		
		while ( normalizedText.charAt( index ) == character )
		{
			if ( --index < 0 )
			{
				return "";
			}
		}
		return normalizedText.substring( 0, index + 1 ).trim();
	}
	
	/**
	 * Trim specified charcater from both ends of a String
	 * 
	 * @param text
	 *             Text
	 * @param character
	 *             Character to remove
	 * @return Trimmed text
	 */
	public static String trimAll( String text, char character )
	{
		String normalizedText = trimFront( text, character );
		
		return trimEnd( normalizedText, character );
	}
}
