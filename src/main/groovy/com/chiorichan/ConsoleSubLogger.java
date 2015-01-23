package com.chiorichan;

import java.util.logging.LogRecord;
import java.util.logging.Logger;

public class ConsoleSubLogger extends Logger
{
	protected ConsoleSubLogger(String id)
	{
		super( id, null );
	}
	
	@Override
	public void log( LogRecord logRecord )
	{
		if ( Loader.getConfig() != null && !Loader.getConfig().getBoolean( "console.hideLoggerName" ) )
			logRecord.setMessage( "&7[" + getName() + "]&f " + logRecord.getMessage() );
		
		super.log( logRecord );
	}
}
