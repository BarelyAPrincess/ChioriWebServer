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
import com.chiorichan.util.Application;
import com.dropbox.core.DbxAppInfo;
import com.dropbox.core.DbxClient;
import com.dropbox.core.DbxRequestConfig;
import com.dropbox.core.DbxWebAuthNoRedirect;

/**
 * Provides the Dropbox Code API to the server
 * 
 * @author Chiori Greene, a.k.a. Chiori-chan {@literal <me@chiorichan.com>}
 */
// @Plugin( name = "Dropbox Plugin", author = "Chiori Greene", version = "1.0" )
public class Dropbox extends Plugin
{
	private String dbxApiKey = null;
	private String dbxApiSecret = null;
	private DbxRequestConfig dbxAppConfig = null;
	private DbxAppInfo dbxAppInfo = null;
	
	public DbxClient getClient( String accessToken )
	{
		return new DbxClient( dbxAppConfig, accessToken );
	}
	
	public DropboxAuth getWebAuth()
	{
		return new DropboxAuth( dbxAppConfig, dbxAppInfo );
	}
	
	public DropboxAuth getWebAuth( String redirectUri )
	{
		return new DropboxAuth( dbxAppConfig, dbxAppInfo, redirectUri );
	}
	
	public DbxWebAuthNoRedirect getWebAuthNoRedirect()
	{
		return new DbxWebAuthNoRedirect( dbxAppConfig, dbxAppInfo );
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
		dbxAppConfig = new DbxRequestConfig( Application.getProduct() + "/" + Application.getVersion(), Locale.getDefault().toString() );
	}
	
	@Override
	public void onLoad() throws PluginException
	{
		
	}
}
