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
import java.io.FileFilter;
import java.io.IOException;
import java.sql.SQLException;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.filefilter.WildcardFileFilter;

import com.chiorichan.ServerLogger;
import com.chiorichan.Loader;
import com.chiorichan.ServerManager;
import com.chiorichan.datastore.sql.bases.SQLDatastore;
import com.chiorichan.datastore.sql.query.SQLQuerySelect;
import com.chiorichan.lang.SiteException;
import com.chiorichan.lang.StartupException;
import com.chiorichan.util.FileFunc;
import com.google.common.collect.Lists;

/**
 * Manages and Loads Sites
 */
public class SiteManager implements ServerManager
{
	public static final SiteManager INSTANCE = new SiteManager();
	private static boolean isInitialized = false;
	
	Map<String, Site> siteMap = new LinkedHashMap<String, Site>();
	
	private SiteManager()
	{
		
	}
	
	public static ServerLogger getLogger()
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
	
	public String add( String siteId, String type )
	{
		if ( siteMap.containsKey( siteId ) )
			return "There already exists a site by the id you provided.";
		
		if ( !type.equalsIgnoreCase( "sql" ) && !type.equalsIgnoreCase( "file" ) )
			return "The only available site types are 'sql' and 'file'. '" + type + "' was not a valid option.";
		
		return "";
	}
	
	public Site getDefaultSite()
	{
		return getSiteById( "default" );
	}
	
	public Site getSiteByDomain( String domain )
	{
		if ( domain == null )
			domain = "default";
		
		for ( Site site : siteMap.values() )
			if ( site != null && site.getDomain() != null )
				if ( site.getDomain().equalsIgnoreCase( domain.trim() ) )
					return site;
		
		return null;
	}
	
	public Site getSiteById( String siteId )
	{
		if ( siteId == null )
			return null;
		
		return siteMap.get( siteId.toLowerCase().trim() );
	}
	
	public Collection<Site> getSites()
	{
		return Collections.unmodifiableCollection( siteMap.values() );
	}
	
	private void init0() throws StartupException
	{
		if ( siteMap.size() > 0 )
			throw new StartupException( "Site manager already has sites loaded. Please unload the existing sites first." );
		
		SQLDatastore sql = Loader.getDatabase();
		
		// Load sites from YAML file base.
		File siteFileBase = new File( "sites" );
		
		FileFunc.directoryHealthCheck( siteFileBase );
		
		File defaultSite = new File( siteFileBase, "000-default.yaml" );
		
		// We make sure the default default YAML FileBase exists and if not we copy it from the Jar.
		if ( !defaultSite.exists() )
			try
			{
				defaultSite.getParentFile().mkdirs();
				FileFunc.putResource( "com/chiorichan/default-site.yaml", defaultSite );
			}
			catch ( IOException e )
			{
				e.printStackTrace();
			}
		
		/**
		 * Add the dummy all sites site
		 * Used by the AccountManager to identify accounts that can belong to any site
		 */
		siteMap.put( "%", new Site( "%" ) );
		
		FileFilter fileFilter = new WildcardFileFilter( "*.yaml" );
		File[] files = siteFileBase.listFiles( fileFilter );
		
		if ( files != null && files.length > 0 )
			for ( File f : files )
				try
				{
					Site site = new Site( f );
					
					if ( site != null )
						siteMap.put( site.getSiteId(), site );
				}
				catch ( SiteException e )
				{
					getLogger().warning( "Exception encountered while loading a site from YAML FileBase, Reason: " + e.getMessage() );
					if ( e.getCause() != null )
						e.getCause().printStackTrace();
				}
		
		try
		{
			if ( !sql.table( "sites" ).exists() )
				sql.table( "sites" ).addColumnVar( "siteId", 255 ).addColumnVar( "title", 255, "Unnamed Chiori-chan Web Server Site" ).addColumnVar( "domain", 255 ).addColumnVar( "source", 255, "pages" ).addColumnVar( "resource", 255, "resources" ).addColumnVar( "subdomains", 255 ).addColumnVar( "protected", 255 ).addColumnVar( "metatags", 255 ).addColumnVar( "aliases", 255 ).addColumnVar( "configYaml", 255 );
			
			SQLQuerySelect select = sql.table( "sites" ).select().execute();
			
			if ( select.rowCount() > 0 )
				for ( Map<String, String> row : select.stringSet() )
					try
					{
						Site site = new Site( row );
						
						if ( site != null )
							siteMap.put( site.getSiteId(), site );
					}
					catch ( SiteException e )
					{
						getLogger().severe( "Exception encountered while loading a site from SQL Database, Reason: " + e.getMessage() );
						if ( e.getCause() != null )
							e.getCause().printStackTrace();
					}
		}
		catch ( SQLException e )
		{
			getLogger().warning( "Exception encountered while loading a sites from Database", e );
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
		siteMap = new LinkedHashMap<String, Site>();
		init();
	}
	
	public String remove( String siteId )
	{
		if ( siteId.equals( "default" ) )
			return "You can not delete the default site.";
		
		if ( siteMap.containsKey( siteId ) )
		{
			Site site = siteMap.get( siteId );
			
			// TODO Either confirm that someone want to delete a site or make it so they can be restored.
			
			switch ( site.siteType() )
			{
				case SQL:
					SQLDatastore sql = Loader.getDatabase();
					
					try
					{
						if ( sql.table( "sites" ).delete().where( "siteId" ).matches( siteId ).execute().rowCount() < 0 )
							return "There was a unknown reason the site could not be deleted.";
					}
					catch ( SQLException e )
					{
						return "There was a unknown reason the site could not be deleted.";
					}
					
					break;
				case FILE:
					File f = site.getFile();
					f.delete();
					
					break;
				default:
					return "There was an unknown reason the site could be deleted.";
			}
			
			siteMap.remove( siteId );
			
			return "The specified site was successfully removed.";
		}
		else
			return "The specified site was not found.";
	}
}
