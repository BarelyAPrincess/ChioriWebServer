/**
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * Copyright 2015 Chiori-chan. All Right Reserved.
 */
package com.chiorichan.net;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.List;

import com.chiorichan.Loader;
import com.chiorichan.event.EventBus;
import com.chiorichan.event.EventCreator;
import com.chiorichan.event.EventHandler;
import com.chiorichan.event.EventPriority;
import com.chiorichan.event.Listener;
import com.chiorichan.event.http.ErrorEvent;
import com.chiorichan.http.WebInterpreter;
import com.chiorichan.lang.ApacheParser;
import com.chiorichan.lang.HttpError;
import com.chiorichan.plugin.PluginDescriptionFile;
import com.chiorichan.site.Site;

/**
 * Maintains the network security for all protocols, e.g., TCP, HTTP and HTTPS.
 * 
 * @author Chiori Greene
 * @email chiorigreene@gmail.com
 */
public class NetworkSecurity implements Listener, EventCreator
{
	private static List<String> bannedIps = Arrays.asList( new String[] {"94.23.193.70", "204.15.135.116", "222.91.96.117"} );
	
	public NetworkSecurity()
	{
		EventBus.INSTANCE.registerEvents( this, this );
	}
	
	public static boolean isIPBanned( String remoteAddr )
	{
		try
		{
			// Checks if the provided address is valid.
			InetAddress.getByName( remoteAddr );
		}
		catch ( UnknownHostException e )
		{
			return false;
		}
		
		return bannedIps.contains( remoteAddr );
	}
	
	public static void isForbidden( ApacheParser htaccess, Site site, WebInterpreter fi ) throws HttpError
	{
		// String[] allowed = htaccess.getAllowed();
		
		
		if ( fi.hasFile() && site.protectCheck( fi.getFilePath() ) )
			throw new HttpError( 401, "Loading of this page (" + fi.getFilePath() + ") is not allowed since its hard protected in the configs." );
	}
	
	@EventHandler( priority = EventPriority.MONITOR )
	public void onErrorEvent( ErrorEvent event )
	{
		if ( event.getStatus() == 404 )
		{
			
		}
	}
	
	@Override
	public PluginDescriptionFile getDescription()
	{
		return null;
	}
	
	@Override
	public boolean isEnabled()
	{
		return true;
	}
	
	@Override
	public String getName()
	{
		return "NetworkSecurity";
	}
}
