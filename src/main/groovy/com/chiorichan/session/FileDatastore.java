/**
 * This software may be modified and distributed under the terms
 * of the MIT license.  See the LICENSE file for details.
 *
 * Copyright (c) 2017 Chiori Greene a.k.a. Chiori-chan <me@chiorichan.com>
 * Copyright (c) 2017 Penoaks Publishing LLC <development@penoaks.com>
 *
 * All Rights Reserved.
 */
package com.chiorichan.session;

import com.chiorichan.AppConfig;
import com.chiorichan.configuration.types.yaml.YamlConfiguration;
import com.chiorichan.permission.PermissionManager;
import com.chiorichan.tasks.Timings;
import com.chiorichan.zutils.ZIO;
import com.google.common.collect.Lists;
import com.google.common.reflect.TypeToken;
import com.google.gson.Gson;
import org.apache.commons.io.filefilter.FileFilterUtils;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;

public class FileDatastore extends SessionDatastore
{
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

			ipAddress = wrapper.getIpAddress();
			site = wrapper.getLocation() == null ? null : wrapper.getLocation().getId();

			save();
		}

		@Override
		void destroy() throws SessionException
		{
			file.delete();
		}

		private void readSession() throws SessionException
		{
			if ( file == null || !file.exists() )
				return;

			YamlConfiguration yaml = YamlConfiguration.loadConfiguration( file );

			if ( yaml.getInt( "timeout", 0 ) > timeout )
				timeout = yaml.getLong( "timeout", timeout );

			ipAddress = yaml.getString( "ipAddress" );

			if ( yaml.getString( "sessionName" ) != null && !yaml.getString( "sessionName" ).isEmpty() )
				sessionName = yaml.getString( "sessionName", sessionName );
			sessionId = yaml.getString( "sessionId", sessionId );

			site = yaml.getString( "site" );

			if ( !yaml.getString( "data", "" ).isEmpty() )
				data = new Gson().fromJson( yaml.getString( "data" ), new TypeToken<Map<String, String>>()
				{
					private static final long serialVersionUID = -1734352198651744570L;
				}.getType() );
		}

		@Override
		void reload() throws SessionException
		{
			readSession();
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
			yaml.set( "ipAddress", ipAddress );
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
	}

	private static File sessionsDirectory = null;

	public static File getSessionsDirectory()
	{
		if ( sessionsDirectory == null )
			sessionsDirectory = new File( AppConfig.get().getDirectory().getAbsolutePath(), "sessions" );

		ZIO.setDirectoryAccessWithException( sessionsDirectory );

		return sessionsDirectory;
	}

	@Override
	SessionData createSession( String sessionId, SessionWrapper wrapper ) throws SessionException
	{
		return new FileSessionData( sessionId, wrapper );
	}

	@Override
	List<SessionData> getSessions() throws SessionException
	{
		List<SessionData> data = Lists.newArrayList();

		File[] files = getSessionsDirectory().listFiles();
		Timings.start( this );

		if ( files == null )
			return data;

		for ( File f : files )
			if ( FileFilterUtils.and( FileFilterUtils.suffixFileFilter( "yaml" ), FileFilterUtils.fileFileFilter() ).accept( f ) )
				try
				{
					data.add( new FileSessionData( f ) );
				}
				catch ( SessionException e )
				{
					e.printStackTrace();
				}

		PermissionManager.getLogger().info( "FileSession loaded " + data.size() + " sessions from the datastore in " + Timings.finish( this ) + "ms!" );

		return data;
	}
}
