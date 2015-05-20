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

import com.chiorichan.configuration.file.YamlConfiguration;
import com.chiorichan.permission.PermissionManager;
import com.chiorichan.util.FileFunc;
import com.chiorichan.util.TimingFunc;
import com.google.common.collect.Lists;
import com.google.common.reflect.TypeToken;
import com.google.gson.Gson;

public class FileDatastore extends SessionDatastore
{
	private static File sessionsDirectory = null;
	
	@Override
	List<SessionData> getSessions() throws SessionException
	{
		List<SessionData> data = Lists.newArrayList();
		
		File[] files = getSessionsDirectory().listFiles();
		TimingFunc.start( this );
		
		if ( files == null )
			return data;
		
		for ( File f : files )
			if ( FileFilterUtils.and( FileFilterUtils.suffixFileFilter( "yaml" ), FileFilterUtils.fileFileFilter() ).accept( f ) )
			{
				try
				{
					data.add( new FileSessionData( f ) );
				}
				catch ( SessionException e )
				{
					e.printStackTrace();
				}
			}
		
		PermissionManager.getLogger().info( "FileSession loaded " + data.size() + " sessions from the datastore in " + TimingFunc.finish( this ) + "ms!" );
		
		return data;
	}
	
	@Override
	SessionData createSession( String sessionId, SessionWrapper wrapper ) throws SessionException
	{
		return new FileSessionData( sessionId, wrapper );
	}
	
	public static File getSessionsDirectory()
	{
		if ( sessionsDirectory == null )
			sessionsDirectory = new File( "sessions" );
		
		FileFunc.directoryHealthCheck( sessionsDirectory );
		
		return sessionsDirectory;
	}
	
	class FileSessionData extends SessionData
	{
		File file;
		
		FileSessionData( File file ) throws SessionException
		{
			super( FileDatastore.this, true );
			this.file = file;
			
			readSession();
		}
		
		FileSessionData( String sessionId, SessionWrapper wrapper ) throws SessionException
		{
			super( FileDatastore.this, false );
			this.sessionId = sessionId;
			
			ipAddr = wrapper.getIpAddr();
			site = wrapper.getSite().getSiteId();
			
			save();
		}
		
		@Override
		void reload() throws SessionException
		{
			readSession();
		}
		
		private void readSession() throws SessionException
		{
			if ( file == null || !file.exists() )
				return;
			
			YamlConfiguration yaml = YamlConfiguration.loadConfiguration( file );
			
			if ( yaml.getInt( "timeout", 0 ) > timeout )
				timeout = yaml.getInt( "timeout", timeout );
			
			ipAddr = yaml.getString( "ipAddr" );
			
			if ( yaml.getString( "sessionName" ) != null && !yaml.getString( "sessionName" ).isEmpty() )
				sessionName = yaml.getString( "sessionName", sessionName );
			sessionId = yaml.getString( "sessionId", sessionId );
			
			site = yaml.getString( "site" );
			
			if ( !yaml.getString( "data", "" ).isEmpty() )
			{
				data = new Gson().fromJson( yaml.getString( "data" ), new TypeToken<Map<String, String>>()
				{
					private static final long serialVersionUID = -1734352198651744570L;
				}.getType() );
			}
		}
		
		@Override
		void save() throws SessionException
		{
			String dataJson = new Gson().toJson( data );
			
			if ( file == null || !file.exists() )
				file = new File( getSessionsDirectory(), sessionId + ".yaml" );
			
			YamlConfiguration yaml = new YamlConfiguration();
			
			yaml.set( "sessionName", sessionName );
			yaml.set( "sessionId", sessionId );
			yaml.set( "timeout", timeout );
			yaml.set( "ipAddr", ipAddr );
			yaml.set( "site", site );
			yaml.set( "data", dataJson );
			
			try
			{
				yaml.save( file );
			}
			catch ( IOException e )
			{
				throw new SessionException( "There was an exception thrown while trying to save the session.", e );
			}
		}
		
		@Override
		void destroy() throws SessionException
		{
			file.delete();
		}
	}
}
