package com.chiorichan;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.text.SimpleDateFormat;
import java.util.logging.Formatter;
import java.util.logging.LogRecord;

import com.chiorichan.server.Server;

public class ShortConsoleLogFormatter extends Formatter
{
	private final SimpleDateFormat date;
	
	public ShortConsoleLogFormatter(Server server)
	{
		SimpleDateFormat date = new SimpleDateFormat( "yyyy-MM-dd HH:mm:ss" );
		
		if ( date == null )
		{
			date = new SimpleDateFormat( "HH:mm:ss" );
		}
		
		this.date = date;
	}
	
	@Override
	public String format( LogRecord record )
	{
		StringBuilder builder = new StringBuilder();
		Throwable ex = record.getThrown();
		
		builder.append( date.format( record.getMillis() ) );
		builder.append( " [" );
		builder.append( record.getLevel().getLocalizedName().toUpperCase() );
		builder.append( "] " );
		builder.append( formatMessage( record ) );
		builder.append( '\n' );
		
		if ( ex != null )
		{
			StringWriter writer = new StringWriter();
			ex.printStackTrace( new PrintWriter( writer ) );
			builder.append( writer );
		}
		
		return builder.toString();
	}
	
}
