/**
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 *
 * Copyright 2015 Chiori Greene a.k.a. Chiori-chan <me@chiorichan.com>
 * All Right Reserved.
 */
package com.chiorichan;

import java.util.Set;
import java.util.logging.Handler;
import java.util.logging.Logger;

import com.google.common.collect.Sets;

public class ConsoleLogManager
{
	private static final Set<ConsoleLogger> loggers = Sets.newHashSet();
	private static final Logger logger = Logger.getLogger( "" );
	
	public ConsoleLogManager()
	{
		for ( Handler h : logger.getHandlers() )
			logger.removeHandler( h );
	}
	
	public void addHandler( Handler h )
	{
		logger.addHandler( h );
	}
	
	public ConsoleLogger getLogger()
	{
		return getLogger( "Core" );
	}
	
	public ConsoleLogger getLogger( String loggerId )
	{
		for ( ConsoleLogger log : loggers )
			if ( log.getId().equals( loggerId ) )
				return log;
		
		ConsoleLogger log = new ConsoleLogger( loggerId );
		loggers.add( log );
		return log;
	}
	
	public Logger getParent()
	{
		return logger;
	}
	
	public void removeHandler( Handler h )
	{
		logger.removeHandler( h );
	}
}
