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
		if ( server.isRunning() )
			server.stop( "Daemon was stopped!" );
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
		if ( !server.isRunning() )
			server.start();
	}
	
	@Override
	public void stop() throws Exception
	{
		if ( server.isRunning() )
			server.stop( "Daemon was stopped!" );
	}
	
}
