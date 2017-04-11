/**
 * This software may be modified and distributed under the terms
 * of the MIT license.  See the LICENSE file for details.
 *
 * Copyright (c) 2017 Chiori Greene a.k.a. Chiori-chan <me@chiorichan.com>
 * Copyright (c) 2017 Penoaks Publishing LLC <development@penoaks.com>
 *
 * All Rights Reserved.
 */
package com.chiorichan.site;

import com.chiorichan.AppConfig;
import com.chiorichan.Loader;
import com.chiorichan.account.AccountLocation;
import com.chiorichan.account.LocationService;
import com.chiorichan.configuration.types.yaml.YamlConfiguration;
import com.chiorichan.datastore.file.FileDatastore;
import com.chiorichan.factory.env.Env;
import com.chiorichan.lang.ApplicationException;
import com.chiorichan.lang.SiteException;
import com.chiorichan.lang.StartupException;
import com.chiorichan.logger.Log;
import com.chiorichan.logger.LogSource;
import com.chiorichan.services.AppManager;
import com.chiorichan.services.ObjectContext;
import com.chiorichan.services.ServiceManager;
import com.chiorichan.services.ServicePriority;
import com.chiorichan.services.ServiceProvider;
import com.chiorichan.tasks.TaskRegistrar;
import com.chiorichan.utils.UtilHttp;
import com.chiorichan.utils.UtilIO;
import com.chiorichan.utils.UtilObjects;
import com.chiorichan.utils.UtilStrings;
import com.google.common.collect.Lists;
import com.google.common.io.Files;

import java.io.File;
import java.io.FileInputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;

/**
 * Manages and Loads Sites
 */
public class SiteManager implements ServiceProvider, LogSource, ServiceManager, TaskRegistrar, LocationService
{
	public Stream<DomainNode> getDomainsBySite( Site site )
	{
		return DomainTree.getChildren().filter( n -> n.getSite() == site );
	}

	public Stream<DomainRoot> getDomainsByTLD( String tld )
	{
		return DomainTree.getDomains( tld );
	}

	public Stream<DomainNode> getDomainsByPrefix( String prefix )
	{
		final String domain = UtilHttp.normalize( prefix );
		return DomainTree.getChildren().filter( n -> n.getFullDomain().startsWith( domain ) );
	}

	public Stream<DomainMapping> getDomainMappingsByIp( String ip )
	{
		return DomainTree.getChildren().map( DomainNode::getDomainMapping ).filter( m -> m != null && m.getConfig( "ip" ) != null && ( m.getConfig( "ip" ).equals( ip ) || m.getConfig( "ip" ).matches( ip ) ) );
	}

	public Stream<DomainMapping> getDomainMappingsById( String id )
	{
		return DomainTree.getChildren().map( DomainNode::getDomainMapping ).filter( m -> m != null && m.getConfig( "id" ) != null && ( m.getConfig( "id" ).equals( id ) || m.getConfig( "id" ).matches( id ) ) );
	}

	public Stream<DomainMapping> getDomainMappings()
	{
		return DomainTree.getChildren().map( DomainNode::getDomainMapping );
	}

	public DomainMapping getDomainMapping( String fullDomain )
	{
		return getDomain( fullDomain ).getDomainMapping();
	}

	public DomainNode getDomain( String fullDomain )
	{
		return DomainTree.parseDomain( fullDomain );
	}

	/**
	 * Returns a Stream of DomainNodes that match the provided IP address.
	 * Uses both literal and regex comparison.
	 *
	 * @param ip The IP address to match, regex is permitted.
	 * @return The Stream of DomainNodes
	 */
	public Stream<DomainNode> getDomainsByIp( String ip )
	{
		return DomainTree.getChildren().filter( n ->
		{
			DomainMapping mapping = n.getDomainMapping();
			return mapping != null && ( mapping.getConfig( "ip" ) != null && ( mapping.getConfig( "ip" ).equals( ip ) || mapping.getConfig( "ip" ).matches( ip ) ) );
		} );
	}

	public Stream<DomainNode> getDomains()
	{
		return DomainTree.getChildren();
	}

	public Stream<String> getTLDsInuse()
	{
		return DomainTree.getTLDsInuse();
	}

	protected static File checkSiteRoot( String name )
	{
		File site = new File( Loader.getWebRoot(), name );

		UtilIO.setDirectoryAccessWithException( site );

		File publicDir = new File( site, "public" );
		File resourceDir = new File( site, "resource" );

		UtilIO.setDirectoryAccessWithException( publicDir );
		UtilIO.setDirectoryAccessWithException( resourceDir );

		return site;
	}

	public static void cleanupBackups( final String siteId, final String suffix, int limit )
	{
		File dir = new File( AppConfig.get().getDirectory( "archive", "archive" ), siteId );
		if ( !dir.exists() )
			return;
		File[] files = dir.listFiles( new FilenameFilter()
		{
			@Override
			public boolean accept( File dir, String name )
			{
				return name.toLowerCase().endsWith( suffix );
			}
		} );

		if ( files == null || files.length < 1 )
			return;

		// Delete all logs, no archiving!
		if ( limit < 1 )
		{
			for ( File f : files )
				f.delete();
			return;
		}

		UtilIO.SortableFile[] sfiles = new UtilIO.SortableFile[files.length];

		for ( int i = 0; i < files.length; i++ )
			sfiles[i] = new UtilIO.SortableFile( files[i] );

		Arrays.sort( sfiles );

		if ( sfiles.length > limit )
			for ( int i = 0; i < sfiles.length - limit; i++ )
				sfiles[i].f.delete();
	}

	public static Log getLogger()
	{
		return AppManager.manager( SiteManager.class ).getLogger();
	}

	public static SiteManager instance()
	{
		return AppManager.manager( SiteManager.class ).instance();
	}

	Map<String, Site> sites = new ConcurrentHashMap<>();

	private SiteManager()
	{

	}

	public boolean delete( String siteId, boolean deleteFiles ) throws SiteException
	{
		UtilObjects.notNull( siteId );

		if ( "default".equalsIgnoreCase( siteId ) )
			throw new SiteException( "You can not delete the default site" );

		if ( sites.containsKey( siteId ) )
		{
			Site site = getSiteById( siteId );
			sites.remove( siteId );

			site.unload();

			if ( deleteFiles )
			{
				File deleted = new File( Loader.getWebRoot().getAbsolutePath() + "_deleted" );
				if ( !deleted.exists() )
					deleted.mkdir();
				File newDirectory = new File( deleted, site.directory().getName() );

				if ( newDirectory.exists() )
					newDirectory.delete();

				if ( !site.directory().renameTo( newDirectory ) )
					throw new SiteException( String.format( "Failed to trash the site directory [%s]", UtilIO.relPath( site.directory() ) ) );
			}

			return true;
		}

		return false;
	}

	public Site getDefaultSite()
	{
		return getSiteById( "default" );
	}

	@Override
	public String getLoggerId()
	{
		return "SiteMgr";
	}

	@Override
	public String getName()
	{
		return "SiteMgr";
	}

	public Site getSiteById( String siteId )
	{
		if ( siteId == null || siteId.length() == 0 || siteId.equalsIgnoreCase( "%" ) )
			siteId = "default";

		return sites.get( siteId.toLowerCase().trim() );
	}

	public Stream<Site> getSiteByIp( String ip )
	{
		if ( !UtilHttp.isValidIPv4( ip ) && !UtilHttp.isValidIPv6( ip ) )
			throw new IllegalArgumentException( "The provided IP address does not match IPv4 nor IPv6" );

		Stream<Site> sites = getSites().filter( s -> s.getIps().contains( ip ) );
		return sites.count() == 0 ? Stream.of( getDefaultSite() ) : sites;
	}

	public Stream<Site> getSites()
	{
		return sites.values().stream();
	}

	@Override
	public void init() throws ApplicationException
	{
		AppManager.registerService( SiteManager.class, this, new ObjectContext( this ), ServicePriority.Normal );
		AppManager.registerService( Site.class, this, new ObjectContext( this ), ServicePriority.Normal );
		loadSites();
	}

	@Override
	public boolean isEnabled()
	{
		return true;
	}

	public void loadSites() throws ApplicationException
	{
		if ( sites.size() > 0 )
			throw new StartupException( "Site manager already has sites loaded. You must unload first." );

		sites.put( "default", new DefaultSite( this ) );

		FileDatastore ds = FileDatastore.loadDirectory( Loader.getWebRoot(), "(.*)(?:\\\\|\\/)config.yaml" );

		for ( Entry<File, YamlConfiguration> entry : ds.asEntrySet() )
		{
			File configFile = entry.getKey();
			YamlConfiguration yaml = entry.getValue();

			if ( yaml.has( "site.id" ) )
			{
				String id = yaml.getString( "site.id" ).toLowerCase();
				String siteDir = UtilStrings.regexCapture( configFile.getAbsolutePath(), "\\/([^\\/]*)\\/config.yaml" );

				if ( !UtilStrings.isCamelCase( id ) )
					getLogger().warning( String.format( "The site id %s does not match our camelCase convention. It must start with a lowercase letter or number and each following word should start with an uppercase letter.", id ) );

				if ( !id.equals( siteDir ) )
				{
					getLogger().warning( String.format( "We found a site configuration file at '%s' but the containing directory did not match the site id of '%s', we will now correct this by moving the config to the correct directory.", configFile.getAbsolutePath(), id ) );

					File oldSiteDir = configFile.getParentFile();
					File newSiteDir = new File( Loader.getWebRoot(), id );

					if ( newSiteDir.exists() && ( newSiteDir.isFile() || !UtilIO.isDirectoryEmpty( newSiteDir ) ) )
					{
						getLogger().severe( String.format( "Could not correct the site directory, the destination [%s] is a file and/or is not empty. Please manually correct the site id and directory mismatch to ensure proper operation.", UtilIO.relPath( newSiteDir ) ) );
						continue;
					}

					newSiteDir.mkdirs();

					for ( File child : oldSiteDir.listFiles() )
					{
						try
						{
							Files.move( child, new File( newSiteDir, child.getName() ) );
						}
						catch ( IOException e )
						{
							getLogger().severe( String.format( "Could not correct the site directory, failed to rename the directory %s to %s name. Site will now be ignored.", UtilIO.relPath( configFile.getParentFile() ), UtilIO.relPath( newSiteDir ) ), e );
							continue;
						}
					}

					oldSiteDir.delete();
					configFile = new File( newSiteDir, "config.yaml" );

					if ( !configFile.exists() )
						throw new SiteException( String.format( "Oops! I think we just broke site [%s]. We tried to move the site to [%s] but the configuration file was not found after the move.", id, UtilIO.relPath( newSiteDir ) ) );
				}

				Properties envProperties = new Properties();

				File envFile = new File( configFile.getParentFile(), ".env" );
				if ( envFile.exists() )
					try
					{
						envProperties.load( new FileInputStream( envFile ) );
					}
					catch ( IOException e )
					{
						getLogger().severe( String.format( "Detected an environment file [%s] but an exception was thrown.", UtilIO.relPath( envFile ) ), e );
					}

				try
				{
					sites.put( id, new Site( this, configFile, yaml, new Env( envProperties ) ) );
				}
				catch ( SiteException e )
				{
					getLogger().severe( String.format( "Exception encountered while loading site '%s'", id ), e );
				}
			}
			else
				getLogger().warning( String.format( "The site '%s' is missing the site id `site.id`, site will not be loaded.", yaml.loadedFrom() ) );
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

	public void reload() throws ApplicationException
	{
		sites = new LinkedHashMap<String, Site>();
		init();
	}

	public void unloadSites()
	{
		for ( Site site : sites.values() )
			try
			{
				site.save( true );
			}
			catch ( IOException e )
			{
				Log.get().severe( e );
			}

		sites.clear();
	}

	@Override
	public AccountLocation getLocation( String locId )
	{
		return getSiteById( locId );
	}

	@Override
	public AccountLocation getDefaultLocation()
	{
		return getDefaultSite();
	}

	@Override
	public Stream<AccountLocation> getLocations()
	{
		return getSites().map( s -> ( AccountLocation ) s );
	}
}
