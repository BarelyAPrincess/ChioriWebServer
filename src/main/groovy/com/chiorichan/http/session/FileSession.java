/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * Copyright 2015 Chiori-chan. All Right Reserved.
 * @author Chiori Greene
 * @email chiorigreene@gmail.com
 */
package com.chiorichan.http.session;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.filefilter.FileFilterUtils;

import com.chiorichan.ChatColor;
import com.chiorichan.Loader;
import com.chiorichan.configuration.file.YamlConfiguration;
import com.chiorichan.http.Candy;
import com.chiorichan.util.Common;
import com.chiorichan.util.FileUtil;
import com.google.common.collect.Lists;
import com.google.common.reflect.TypeToken;
import com.google.gson.Gson;

public class FileSession extends Session
{
	private File cachedFile = null;
	private static File sessionsDirectory = null;
	
	public FileSession( File file ) throws SessionException
	{
		cachedFile = file;
		
		readSessionFile();
		
		stale = true;
		
		sessionCandy = new Candy( candyName, candyId );
		candies.put( candyName, sessionCandy );
		
		loginSessionUser();
		
		Loader.getLogger().info( ChatColor.DARK_AQUA + "Session Restored `" + this + "`" );
	}
	
	private void readSessionFile() throws SessionException
	{
		if ( cachedFile == null || !cachedFile.exists() )
		{
			saveSession();
			return;
		}
		
		YamlConfiguration yaml = YamlConfiguration.loadConfiguration( cachedFile );
		
		// int defaultLife = ( getSite().getYaml() != null ) ? getSite().getYaml().getInt( "default-life", 604800 ) : 604800;
		
		if ( yaml.getLong( "timeout", 0 ) > timeout )
			timeout = yaml.getLong( "timeout", timeout );
		
		ipAddr = yaml.getString( "ipAddr", ipAddr );
		
		if ( !yaml.getString( "data", "" ).isEmpty() )
		{
			Map<String, String> tmpData = new Gson().fromJson( yaml.getString( "data" ), new TypeToken<Map<String, String>>()
			{
				private static final long serialVersionUID = -1734352198651744570L;
			}.getType() );
			
			if ( changesMade )
			{
				tmpData.putAll( data );
				data = tmpData;
			}
			else
				data.putAll( tmpData );
		}
		
		if ( yaml.getString( "sessionName" ) != null && !yaml.getString( "sessionName" ).isEmpty() )
			candyName = yaml.getString( "sessionName", candyName );
		candyId = yaml.getString( "sessionId", candyId );
		
		if ( timeout > 0 && timeout < Common.getEpoch() )
			throw new SessionException( "This session expired at " + timeout + " epoch!" );
		
		if ( yaml.getString( "sessionSite" ) == null || yaml.getString( "sessionSite" ).isEmpty() )
			setSite( Loader.getSiteManager().getFrameworkSite() );
		else
			setSite( Loader.getSiteManager().getSiteById( yaml.getString( "sessionSite" ) ) );
		
		List<Session> sessions = Loader.getSessionManager().getSessionsByIp( ipAddr );
		if ( sessions.size() > Loader.getConfig().getInt( "sessions.maxSessionsPerIP" ) )
		{
			long oldestTime = Common.getEpoch();
			Session oldest = null;
			
			for ( Session s : sessions )
			{
				if ( s != this && s.getTimeout() < oldestTime )
				{
					oldest = s;
					oldestTime = s.getTimeout();
				}
			}
			
			if ( oldest != null )
				SessionManager.destroySession( oldest );
		}
	}
	
	protected FileSession()
	{
		
	}
	
	@Override
	public void reloadSession()
	{
		String origIpAddr = ipAddr;
		
		try
		{
			readSessionFile();
		}
		catch( SessionException e )
		{
			e.printStackTrace();
		}
		
		// Possible Session Hijacking! nullify!!!
		if ( ipAddr != null && !ipAddr.equals( origIpAddr ) && !Loader.getConfig().getBoolean( "sessions.allowIPChange" ) )
		{
			sessionCandy = null;
			ipAddr = origIpAddr;
		}
	}
	
	public static File getSessionsDirectory()
	{
		if ( sessionsDirectory == null )
			sessionsDirectory = new File( Loader.getRoot(), "sessions" );
		
		FileUtil.directoryHealthCheck( sessionsDirectory );
		
		return sessionsDirectory;
	}
	
	@Override
	public void saveSession()
	{
		String dataJson = new Gson().toJson( data );
		
		if ( cachedFile == null || !cachedFile.exists() )
			cachedFile = new File( getSessionsDirectory(), candyId + ".yaml" );
		
		YamlConfiguration yaml = new YamlConfiguration();
		
		yaml.set( "sessionName", sessionCandy.getKey() );
		yaml.set( "sessionId", sessionCandy.getValue() );
		yaml.set( "timeout", getTimeout() );
		yaml.set( "ipAddr", getIpAddr() );
		yaml.set( "sessionSite", getSite().getName() );
		yaml.set( "data", dataJson );
		
		try
		{
			yaml.save( cachedFile );
		}
		catch( IOException e )
		{
			Loader.getLogger().severe( "There was an exception thorwn while trying to save the session.", e );
		}
	}
	
	@Override
	protected void destroySession()
	{
		cachedFile.delete();
	}
	
	protected static List<Session> getActiveSessions()
	{
		List<Session> sessions = Lists.newCopyOnWriteArrayList();
		
		File[] files = getSessionsDirectory().listFiles();
		
		if ( files == null )
			return sessions;
		
		for ( File f : files )
			if ( FileFilterUtils.and( FileFilterUtils.suffixFileFilter( "yaml" ), FileFilterUtils.fileFileFilter() ).accept( f ) )
			{
				try
				{
					sessions.add( new FileSession( f ) );
				}
				catch( SessionException e )
				{
					e.printStackTrace();
				}
			}
		
		return sessions;
	}
}
