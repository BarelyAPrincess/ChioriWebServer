/**
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
 *
 * Copyright 2015 Chiori Greene a.k.a. Chiori-chan <me@chiorichan.com> All Right Reserved.
 */
package com.chiorichan.site;

import java.io.File;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.Validate;

import com.chiorichan.APILogger;
import com.chiorichan.Loader;
import com.chiorichan.ServerManager;
import com.chiorichan.configuration.file.YamlConfiguration;
import com.chiorichan.datastore.DatastoreManager;
import com.chiorichan.datastore.file.FileDatastore;
import com.chiorichan.lang.SiteException;
import com.chiorichan.lang.StartupException;
import com.chiorichan.util.FileFunc;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

/**
 * Manages and Loads Sites
 */
public class SiteManager implements ServerManager
{
	public static final SiteManager INSTANCE = new SiteManager();
	private static boolean isInitialized = false;

	protected static File checkSiteRoot( String name )
	{
		File site = new File( Loader.getWebRoot(), name );

		FileFunc.patchDirectory( site );

		File publicDir = new File( site, "public/root" );
		File resourceDir = new File( site, "resource" );

		FileFunc.patchDirectory( publicDir );
		FileFunc.patchDirectory( resourceDir );

		return site;
	}

	public static APILogger getLogger()
	{
		return Loader.getLogger( "SiteMgr" );
	}

	public static void init()
	{
		if ( isInitialized )
			throw new IllegalStateException( "The Site Manager has already been initialized." );

		assert INSTANCE != null;

		INSTANCE.init0();

		isInitialized = true;
	}

	Map<String, Site> sites = Maps.newConcurrentMap();

	private SiteManager()
	{

	}

	public String add( String siteId, String type )
	{
		if ( sites.containsKey( siteId ) )
			return "There already exists a site by the id you provided.";

		if ( !type.equalsIgnoreCase( "sql" ) && !type.equalsIgnoreCase( "file" ) )
			return "The only available site types are 'sql' and 'file'. '" + type + "' was not a valid option.";

		return "";
	}

	public boolean delete( String siteId, boolean deleteFiles ) throws SiteException
	{
		Validate.notNull( siteId );

		if ( siteId.equals( "default" ) )
			throw new SiteException( "You can not delete the default site." );

		if ( sites.containsKey( siteId ) )
		{
			Site site = getSiteById( siteId );
			sites.remove( siteId );

			site.unload();

			if ( site instanceof FileSite )
				( ( FileSite ) site ).getFile().delete();
			if ( deleteFiles )
				site.rootDirectory().delete();
			return true;
		}

		return false;
	}

	public Site getDefaultSite()
	{
		return getSiteById( "default" );
	}

	public Site getSiteByDomain( String domain )
	{
		if ( domain == null )
			domain = "";

		for ( Site site : sites.values() )
			if ( site != null && site.getDomain() != null )
				if ( site.getDomain().equalsIgnoreCase( domain.trim() ) )
					return site;

		return null;
	}

	public Site getSiteById( String siteId )
	{
		if ( siteId == null || siteId.length() == 0 || siteId.equalsIgnoreCase( "%" ) )
			siteId = "default";

		return sites.get( siteId.toLowerCase().trim() );
	}

	public Collection<Site> getSites()
	{
		return Collections.unmodifiableCollection( sites.values() );
	}

	private void init0() throws StartupException
	{
		loadSites();
	}

	public void loadSites()
	{
		if ( sites.size() > 0 )
			throw new StartupException( "Site manager already has sites loaded. You must unload first." );

		sites.put( "default", new DefaultSite() );

		FileDatastore ds = FileDatastore.loadDirectory( Loader.getWebRoot(), "(.*)/config.yaml" );

		for ( YamlConfiguration yaml : ds.asList() )
		{
			if ( !yaml.has( "site.id" ) )
				if ( yaml.has( "site.siteId" ) )
				{ // Temp until later version
					yaml.set( "site.id", yaml.get( "site.siteId" ) );
					yaml.set( "site.siteId", null );
				}

			if ( yaml.has( "site.id" ) )
			{
				String id = yaml.getString( "site.id" ).toLowerCase();

				try
				{
					sites.put( id, new FileSite( yaml ) );
				}
				catch ( SiteException e )
				{
					throw new StartupException( String.format( "Exception encountered while loading site '%s'", id ), e );
				}
			}
			else
				DatastoreManager.getLogger().warning( String.format( "The site '%s' is missing the site id `site.id`, site will not be loaded.", yaml.loadedFrom() ) );
		}
	}

	public List<Site> parseSites( String sites )
	{
		return parseSites( sites, "|" );
	}

	public List<Site> parseSites( String sites, String regExSplit )
	{
		List<Site> siteList = Lists.newArrayList();
		String[] sitesArray = sites.split( regExSplit );

		for ( String siteId : sitesArray )
		{
			Site site = getSiteById( siteId );
			if ( site != null )
				siteList.add( site );
		}

		return siteList;
	}

	public void reload()
	{
		sites = new LinkedHashMap<String, Site>();
		init();
	}
}
