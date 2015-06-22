/**
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 *
 * Copyright 2015 Chiori Greene a.k.a. Chiori-chan <me@chiorichan.com>
 * All Right Reserved.
 */
package com.chiorichan;

import java.util.logging.LogRecord;
import java.util.logging.Logger;

public class ConsoleSubLogger extends Logger
{
	protected ConsoleSubLogger( String id )
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
