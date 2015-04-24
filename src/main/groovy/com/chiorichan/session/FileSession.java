/**
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * Copyright 2015 Chiori-chan. All Right Reserved.
 * 
 * @author Chiori Greene
 * @email chiorigreene@gmail.com
 */
package com.chiorichan.session;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.filefilter.FileFilterUtils;

import com.chiorichan.ConsoleColor;
import com.chiorichan.Loader;
import com.chiorichan.configuration.file.YamlConfiguration;
import com.chiorichan.http.Candy;
import com.chiorichan.permission.PermissionManager;
import com.chiorichan.util.CommonFunc;
import com.chiorichan.util.FileFunc;
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
		
		if ( SessionManager.isDebug() )
			PermissionManager.getLogger().info( ConsoleColor.DARK_AQUA + "Session Restored `" + this + "`" );
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
		
		if ( yaml.getInt( "timeout", 0 ) > timeout )
			timeout = yaml.getInt( "timeout", timeout );
		
		lastIpAddr = yaml.getString( "ipAddr", lastIpAddr );
		
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
		
		if ( timeout > 0 && timeout < CommonFunc.getEpoch() )
			SessionManager.getLogger().warning( "The session '" + getSessId() + "' expired at epoch '" + timeout + "', might have expired while offline or this is a bug!" );
		
		if ( yaml.getString( "sessionSite" ) == null || yaml.getString( "sessionSite" ).isEmpty() )
			setSite( Loader.getSiteManager().getFrameworkSite() );
		else
			setSite( Loader.getSiteManager().getSiteById( yaml.getString( "sessionSite" ) ) );
		
		List<Session> sessions = Loader.getSessionManager().getSessionsByIp( lastIpAddr );
		if ( sessions.size() > Loader.getConfig().getInt( "sessions.maxSessionsPerIP" ) )
		{
			long oldestTime = CommonFunc.getEpoch();
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
		String origIpAddr = lastIpAddr;
		
		try
		{
			readSessionFile();
		}
		catch ( SessionException e )
		{
			e.printStackTrace();
		}
		
		// Possible Session Hijacking! nullify!!!
		if ( lastIpAddr != null && !lastIpAddr.equals( origIpAddr ) && !Loader.getConfig().getBoolean( "sessions.allowIPChange" ) )
		{
			sessionCandy = null;
			lastIpAddr = origIpAddr;
		}
	}
	
	public static File getSessionsDirectory()
	{
		if ( sessionsDirectory == null )
			sessionsDirectory = new File( "sessions" );
		
		FileFunc.directoryHealthCheck( sessionsDirectory );
		
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
		catch ( IOException e )
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
		long start = System.currentTimeMillis();
		
		if ( files == null )
			return sessions;
		
		for ( File f : files )
			if ( FileFilterUtils.and( FileFilterUtils.suffixFileFilter( "yaml" ), FileFilterUtils.fileFileFilter() ).accept( f ) )
			{
				try
				{
					sessions.add( new FileSession( f ) );
				}
				catch ( SessionException e )
				{
					e.printStackTrace();
				}
			}
		
		PermissionManager.getLogger().info( "FileSession loaded " + sessions.size() + " sessions from the data store in " + ( System.currentTimeMillis() - start ) + "ms!" );
		
		return sessions;
	}
}
