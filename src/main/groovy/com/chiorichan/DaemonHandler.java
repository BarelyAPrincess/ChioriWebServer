/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * Copyright 2014 Chiori-chan. All Right Reserved.
 *
 * @author Chiori Greene
 * @email chiorigreene@gmail.com
 */
package com.chiorichan;

import joptsimple.OptionParser;
import joptsimple.OptionSet;

import org.apache.commons.daemon.Daemon;
import org.apache.commons.daemon.DaemonContext;
import org.apache.commons.daemon.DaemonInitException;

public class DaemonHandler implements Daemon
{
	Loader server;
	
	@Override
	public void destroy()
	{
		if ( Loader.isRunning() )
			Loader.stop( "Daemon was stopped!" );
	}
	
	@Override
	public void init( DaemonContext arg0 ) throws DaemonInitException, Exception
	{
		OptionParser parser = Loader.getOptionParser();
		
		OptionSet options = parser.parse( "noconsole" );
		
		server = new Loader( options );
	}
	
	@Override
	public void start() throws Exception
	{
		if ( !Loader.isRunning() )
			server.start();
	}
	
	@Override
	public void stop() throws Exception
	{
		if ( Loader.isRunning() )
			Loader.stop( "Daemon was stopped!" );
	}
	
}
