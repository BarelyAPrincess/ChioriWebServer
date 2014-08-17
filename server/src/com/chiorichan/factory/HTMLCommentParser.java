package com.chiorichan.factory;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.Validate;

import com.chiorichan.exceptions.ShellExecuteException;

public abstract class HTMLCommentParser
{
	private Pattern p1;
	private Pattern p2;
	
	public HTMLCommentParser(String argumentName)
	{
		Validate.notEmpty( argumentName );
		
		p1 = Pattern.compile( "<!-- *" + argumentName + "\\((.*)\\) *-->" );
		p2 = Pattern.compile( "(<!-- *" + argumentName + "\\(.*\\) *-->)" );
	}
	
	public String runParser( String source ) throws ShellExecuteException
	{
		if ( source == null || source.isEmpty() )
			return "";
		
		Matcher m1 = p1.matcher( source );
		Matcher m2 = p2.matcher( source );
		Pattern p3 = Pattern.compile( "[\"`']?(.*)[\"`']?" );
		
		while ( m1.find() && m2.find() )
		{
			String[] args = m1.group( 1 ).split( "[ ]?,[ ]?" );
			String[] args2 = new String[args.length];
			
			for ( int i = 0; i < args.length; i++ )
			{
				Matcher m3 = p3.matcher( args[i] );
				if ( m3.find() )
					args2[i] = m3.group( 1 );
			}
			
			String result = resolveMethod( args2 );
			
			if ( result == null )
				result = "";
			
			source = new StringBuilder( source ).replace( m2.start( 1 ), m2.end( 1 ), result ).toString();
			
			// We have to reset the matcher since the source changes with each loop
			m1 = p1.matcher( source );
			m2 = p2.matcher( source );
		}
		
		return source;
	}
	
	public abstract String resolveMethod( String... args ) throws ShellExecuteException;
}
