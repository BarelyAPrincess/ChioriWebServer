/**
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 *
 * Copyright 2015 Chiori Greene a.k.a. Chiori-chan <me@chiorichan.com>
 * All Right Reserved.
 */
package com.chiorichan.plugin.dropbox;

import java.io.IOException;
import java.util.Map;
import java.util.Map.Entry;

import com.chiorichan.http.HttpRequestWrapper;
import com.chiorichan.plugin.PluginManager;
import com.dropbox.core.DbxAppInfo;
import com.dropbox.core.DbxAuthFinish;
import com.dropbox.core.DbxClient;
import com.dropbox.core.DbxException;
import com.dropbox.core.DbxRequestConfig;
import com.dropbox.core.DbxSessionStore;
import com.dropbox.core.DbxWebAuth;
import com.dropbox.core.DbxWebAuth.BadRequestException;
import com.dropbox.core.DbxWebAuth.BadStateException;
import com.dropbox.core.DbxWebAuth.CsrfException;
import com.dropbox.core.DbxWebAuth.NotApprovedException;
import com.dropbox.core.DbxWebAuth.ProviderException;
import com.dropbox.core.DbxWebAuthNoRedirect;
import com.google.common.collect.Maps;

/**
 * Dropbox Auth Wrapper
 */
public class DropboxAuth
{
	private String accessToken = null;
	private DbxWebAuthNoRedirect dbxWebAuthNoRedirect = null;
	private DbxWebAuth dbxWebAuth = null;
	private final DbxRequestConfig dbxAppConfig;
	
	DropboxAuth( DbxRequestConfig dbxAppConfig, DbxAppInfo dbxAppInfo )
	{
		this.dbxAppConfig = dbxAppConfig;
		
		dbxWebAuthNoRedirect = new DbxWebAuthNoRedirect( dbxAppConfig, dbxAppInfo );
	}
	
	DropboxAuth( DbxRequestConfig dbxAppConfig, DbxAppInfo dbxAppInfo, String redirectUri )
	{
		this.dbxAppConfig = dbxAppConfig;
		
		HttpRequestWrapper request = HttpRequestWrapper.getRequest();
		
		if ( request == null )
			throw new RuntimeException( "Could not get the current HttpRequestWrapper. Did you call this plugin from the Scripting Factory?" );
		
		DbxSessionStore csrfTokenStore = new DropboxChioriSessionStore( request, "dropbox-auth-csrf-token" );
		
		dbxWebAuth = new DbxWebAuth( dbxAppConfig, dbxAppInfo, redirectUri, csrfTokenStore );
	}
	
	public String getAccessToken() throws IOException
	{
		if ( dbxWebAuth == null )
			throw new RuntimeException( "Wrong Dropbox Type" );
		
		HttpRequestWrapper request = HttpRequestWrapper.getRequest();
		
		if ( request == null )
			throw new RuntimeException( "Could not get the current HttpRequestWrapper. Did you call this plugin from the Scripting Factory?" );
		
		Map<String, String[]> params = Maps.newHashMap();
		
		for ( Entry<String, String> entry : request.getGetMapRaw().entrySet() )
			params.put( entry.getKey(), new String[] {entry.getValue()} );
		
		try
		{
			DbxAuthFinish finish = dbxWebAuth.finish( params );
			accessToken = finish.accessToken;
			return finish.accessToken;
		}
		catch ( DbxWebAuth.BadRequestException ex )
		{
			PluginManager.getLogger().severe( "On /dropbox-auth-finish: Bad request: " + ex.getMessage() );
			request.getResponse().sendError( 400 );
			return null;
		}
		catch ( DbxWebAuth.BadStateException ex )
		{
			// Send them back to the start of the auth flow.
			request.getResponse().sendRedirect( "http://my-server.com/dropbox-auth-start" );
			return null;
		}
		catch ( DbxWebAuth.CsrfException ex )
		{
			PluginManager.getLogger().severe( "On /dropbox-auth-finish: CSRF mismatch: " + ex.getMessage() );
			return null;
		}
		catch ( DbxWebAuth.NotApprovedException ex )
		{
			return null;
		}
		catch ( DbxWebAuth.ProviderException ex )
		{
			PluginManager.getLogger().severe( "On /dropbox-auth-finish: Auth failed: " + ex.getMessage() );
			request.getResponse().sendError( 503, "Error communicating with Dropbox." );
			return null;
		}
		catch ( DbxException ex )
		{
			PluginManager.getLogger().severe( "On /dropbox-auth-finish: Error getting token: " + ex.getMessage() );
			request.getResponse().sendError( 503, "Error communicating with Dropbox." );
			return null;
		}
	}
	
	public String getAccessToken( String authToken ) throws IOException
	{
		if ( dbxWebAuthNoRedirect == null )
			throw new RuntimeException( "Wrong Dropbox Type" );
		
		try
		{
			DbxAuthFinish finish = dbxWebAuthNoRedirect.finish( authToken );
			accessToken = finish.accessToken;
			return finish.accessToken;
		}
		catch ( DbxException ex )
		{
			HttpRequestWrapper request = HttpRequestWrapper.getRequest();
			
			if ( request == null )
				throw new RuntimeException( "Could not get the current HttpRequestWrapper. Did you call this plugin from the Scripting Factory?" );
			
			PluginManager.getLogger().severe( "On /dropbox-auth-finish: Error getting token: " + ex.getMessage() );
			request.getResponse().sendError( 503, "Error communicating with Dropbox." );
			return null;
		}
	}
	
	public String getAccessTokenWithException( DbxWebAuth auth ) throws BadRequestException, BadStateException, CsrfException, NotApprovedException, ProviderException, DbxException
	{
		HttpRequestWrapper request = HttpRequestWrapper.getRequest();
		
		if ( request == null )
			throw new RuntimeException( "Could not get the current HttpRequestWrapper. Did you call this plugin from the Scripting Factory?" );
		
		Map<String, String[]> params = Maps.newHashMap();
		
		for ( Entry<String, String> entry : request.getGetMapRaw().entrySet() )
			params.put( entry.getKey(), new String[] {entry.getValue()} );
		
		return auth.finish( params ).accessToken;
	}
	
	public String getAccessTokenWithException( String authToken ) throws DbxException
	{
		if ( dbxWebAuthNoRedirect == null )
			throw new RuntimeException( "Wrong Dropbox Type" );
		
		return dbxWebAuthNoRedirect.finish( authToken ).accessToken;
	}
	
	public String getAuthUrl()
	{
		if ( dbxWebAuth != null )
			return dbxWebAuth.start();
		if ( dbxWebAuthNoRedirect != null )
			return dbxWebAuthNoRedirect.start();
		return null;
	}
	
	public String getAuthUrl( String urlState )
	{
		if ( dbxWebAuth != null )
			return dbxWebAuth.start( urlState );
		if ( dbxWebAuthNoRedirect != null )
			return dbxWebAuthNoRedirect.start();
		return null;
	}
	
	public DbxClient getClient()
	{
		if ( accessToken == null )
			throw new RuntimeException( "We have no Access Token" );
		
		return new DbxClient( dbxAppConfig, accessToken );
	}
}
