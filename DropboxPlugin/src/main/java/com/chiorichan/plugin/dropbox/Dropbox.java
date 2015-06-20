/**
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * Copyright 2015 Chiori-chan. All Right Reserved.
 */
package com.chiorichan.plugin.dropbox;

import java.util.Locale;

import com.chiorichan.lang.PluginException;
import com.chiorichan.lang.PluginUnconfiguredException;
import com.chiorichan.plugin.loader.Plugin;
import com.chiorichan.util.Versioning;
import com.dropbox.core.DbxAppInfo;
import com.dropbox.core.DbxAuthFinish;
import com.dropbox.core.DbxClient;
import com.dropbox.core.DbxException;
import com.dropbox.core.DbxRequestConfig;
import com.dropbox.core.DbxWebAuthNoRedirect;

/**
 * Provides the Dropbox Code API to the server
 * 
 * @author Chiori Greene, a.k.a. Chiori-chan {@literal <me@chiorichan.com>}
 */
public class Dropbox extends Plugin
{
	private String dbxApiKey = null;
	private String dbxApiSecret = null;
	private DbxRequestConfig dbxAppConfig = null;
	private DbxAppInfo dbxAppInfo = null;
	private DbxAuthFinish dbxAuthFinish = null;
	private DbxWebAuthNoRedirect dbxWebAuth = null;
	
	public String finishDropBoxAuth( String authCode ) throws DbxException
	{
		dbxAuthFinish = dbxWebAuth.finish( authCode );
		return dbxAuthFinish.accessToken;
	}
	
	public String getDropboxAuthUrl()
	{
		return dbxWebAuth.start();
	}
	
	/**
	 * Creates a new DbxClient
	 * 
	 * @param accessToken
	 *            The Access Token you wish to use
	 * @return
	 *         An instance of DbxClient
	 */
	public DbxClient getDropboxInstance( String accessToken )
	{
		return new DbxClient( dbxAppConfig, accessToken );
	}
	
	@Override
	public void onDisable() throws PluginException
	{
		
	}
	
	@Override
	public void onEnable() throws PluginException
	{
		saveDefaultConfig();
		
		dbxApiKey = getConfig().getString( "dropbox.apiKey" );
		dbxApiSecret = getConfig().getString( "dropbox.apiSecret" );
		
		if ( dbxApiKey == null || dbxApiKey.isEmpty() || dbxApiSecret == null || dbxApiSecret.isEmpty() )
			throw new PluginUnconfiguredException( "Config did not contain a Api Key nor Api Secret. You will need to edit the config.yml file located in folder `" + getDataFolder().getAbsolutePath() + "`." );
		
		dbxAppInfo = new DbxAppInfo( dbxApiKey, dbxApiSecret );
		dbxAppConfig = new DbxRequestConfig( Versioning.getProduct() + "/" + Versioning.getVersion(), Locale.getDefault().toString() );
		
		dbxWebAuth = new DbxWebAuthNoRedirect( dbxAppConfig, dbxAppInfo );
	}
	
	@Override
	public void onLoad() throws PluginException
	{
		
	}
}
