/**
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 *
 * Copyright 2015 Chiori Greene a.k.a. Chiori-chan <me@chiorichan.com>
 * All Right Reserved.
 */
package com.chiorichan.site;

import java.io.File;
import java.util.List;
import java.util.Map;

import com.chiorichan.Loader;
import com.chiorichan.configuration.file.YamlConfiguration;
import com.chiorichan.datastore.sql.bases.SQLDatastore;
import com.chiorichan.http.HttpCookie;
import com.chiorichan.http.Routes;
import com.chiorichan.util.Versioning;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

/**
 * Implements the builtin server site with site id 'default'
 */
public class DefaultSite implements Site
{
	private File siteDirectory;
	
	public DefaultSite()
	{
		siteDirectory = SiteManager.checkSiteRoot( "default" );
	}
	
	@Override
	public HttpCookie createSessionCookie( String sessionId )
	{
		return new HttpCookie( getSessionKey(), sessionId ).setPath( "/" ).setHttpOnly( true );
	}
	
	@Override
	public Map<String, String> getAliases()
	{
		return Maps.newHashMap();
	}
	
	@Override
	public YamlConfiguration getConfig()
	{
		return new YamlConfiguration();
	}
	
	@Override
	public SQLDatastore getDatastore()
	{
		return Loader.getDatabaseWithException();
	}
	
	@Override
	public String getDomain()
	{
		return "";
	}
	
	@Override
	public List<String> getMetatags()
	{
		return Lists.newArrayList();
	}
	
	@Override
	public String getName()
	{
		return getSiteId();
	}
	
	@Override
	public Routes getRoutes()
	{
		// Currently no routes exist
		return new Routes( this );
	}
	
	@Override
	public String getSessionKey()
	{
		return "_wsSessionId";
	}
	
	@Override
	public String getSiteId()
	{
		return "default";
	}
	
	@Override
	public String getTitle()
	{
		return Versioning.getProduct();
	}
	
	@Override
	public File publicDirectory()
	{
		return new File( siteDirectory, "public/root" );
	}
	
	@Override
	public File resourceDirectory()
	{
		return new File( siteDirectory, "resouce" );
	}
	
	@Override
	public File resourceFile( String file )
	{
		return null;// TODO New Empty Method
	}
	
	@Override
	public File resourcePackage( String pack )
	{
		return null;// TODO New Empty Method
	}
	
	@Override
	public File rootDirectory()
	{
		return siteDirectory;
	}
	
	@Override
	public File subDomainDirectory( String subDomain )
	{
		// Default site has no subdomains
		return null;
	}
	
	@Override
	public boolean subDomainExists( String subDomain )
	{
		// Default site has no subdomains
		return false;
	}
	
	@Override
	public File tempDirectory()
	{
		return Loader.getTempFileDirectory( "default" );
	}
	
	@Override
	public void unload()
	{
		// Do Nothing
	}
}
