/**
 * This software may be modified and distributed under the terms
 * of the MIT license.  See the LICENSE file for details.
 * <p>
 * Copyright (c) 2017 Chiori Greene a.k.a. Chiori-chan <me@chiorichan.com>
 * Copyright (c) 2017 Penoaks Publishing LLC <development@penoaks.com>
 * <p>
 * All Rights Reserved.
 */
package com.chiorichan.site;

import com.chiorichan.AppConfig;
import com.chiorichan.helpers.Namespace;
import com.chiorichan.http.ssl.CertificateWrapper;
import com.chiorichan.lang.SiteConfigurationException;
import com.chiorichan.zutils.ZHttp;
import com.chiorichan.zutils.ZIO;
import com.chiorichan.zutils.ZObjects;
import io.netty.handler.ssl.SslContext;

import javax.net.ssl.SSLException;
import java.io.File;
import java.io.FileNotFoundException;
import java.security.cert.CertificateException;
import java.util.Collections;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class DomainMapping
{
	protected final Site site;
	protected final DomainParser domain;
	protected final Map<String, String> config = new TreeMap<>();

	public DomainMapping( Site site, String fullDomain )
	{
		this.site = site;
		this.domain = new DomainParser( fullDomain );
	}

	public Boolean matches( String domain )
	{
		if ( ZHttp.needsNormalization( domain ) )
			domain = ZHttp.normalize( domain );
		return ZHttp.matches( domain, this.domain.getFullDomain().getString() );
	}

	public File directory()
	{
		try
		{
			return directory0( false );
		}
		catch ( SiteConfigurationException e )
		{
			// IGNORED - NEVER THROWN
		}
		return null;
	}

	public String getNamespaceString()
	{
		return getNamespace().getString( "_", true );
	}

	public Namespace getNamespace()
	{
		return domain.getFullDomain().reverseOrderNew();
	}

	public Namespace getDomainNamespace()
	{
		return domain.getFullDomain().clone();
	}

	public String getFullDomain()
	{
		return domain.getFullDomain().getString();
	}

	public File directoryWithException() throws SiteConfigurationException
	{
		return directory0( true );
	}

	protected File directory0( boolean throwException ) throws SiteConfigurationException
	{
		try
		{
			if ( hasConfig( "directory" ) )
			{
				String directory = getConfig( "directory" );
				if ( ZIO.isAbsolute( directory ) )
				{
					if ( !AppConfig.get().getBoolean( "sites.allowPublicOutsideWebroot" ) && !directory.startsWith( site.directoryPublic().getAbsolutePath() ) )
						throw new SiteConfigurationException( String.format( "The public directory [%s] is not allowed outside the webroot.", ZIO.relPath( new File( directory ) ) ) );

					return new File( directory );
				}

				return new File( site.directoryPublic(), directory );
			}
		}
		catch ( SiteConfigurationException e )
		{
			/* Should an exception be thrown instead of returning the default directory */
			if ( throwException )
				throw e;
		}

		return new File( site.directoryPublic(), getNamespace().replace( "_", "-" ).getString( "_" ) );
	}

	public Site getSite()
	{
		return site;
	}

	public void putConfig( String key, String value )
	{
		ZObjects.notEmpty( key );
		key = key.trim().toLowerCase();
		if ( key.startsWith( "__" ) )
			key = key.substring( 2 );
		if ( value == null )
			config.remove( key );
		else
			config.put( key, value );
	}

	public void clearConfig()
	{
		config.clear();
	}

	public boolean hasConfig( String key )
	{
		return config.containsKey( key.toLowerCase() );
	}

	public String getConfig( String key )
	{
		return config.get( key.toLowerCase() );
	}

	public void save()
	{
		throw new IllegalStateException( "Not Implemented!" );
		// isCommitted = true;

		// EventBus.instance().callEvent( new SiteDomainChangeEvent( SiteDomainChangeEventType.ADD, site, domain, this ) );
	}

	/**
	 * Try to initialize the SslContext
	 *
	 * @param recursive Should we look backwards for a valid SSL Context?, e.g., look at our parents.
	 * @return SslContext
	 */
	public SslContext getSslContext( boolean recursive )
	{
		Map<String, DomainMapping> nodes = new TreeMap<>( Collections.reverseOrder() );
		nodes.putAll( getParentMappings().filter( p -> p.hasSslContext() ).collect( Collectors.toMap( DomainMapping::getFullDomain, p -> p ) ) );

		for ( Map.Entry<String, DomainMapping> entry : nodes.entrySet() )
		{
			CertificateWrapper wrapper = entry.getValue().initSsl();
			if ( wrapper != null && ( entry.getValue() == this || ZHttp.normalize( wrapper.getCommonName() ).equals( getFullDomain() ) || wrapper.getSubjectAltDNSNames().contains( getFullDomain() ) ) )
				try
				{
					return wrapper.context();
				}
				catch ( SSLException | FileNotFoundException | CertificateException e )
				{
					e.printStackTrace();
					// Ignore
				}
		}

		return site.getDefaultSslContext();
	}

	public boolean hasSslContext()
	{
		return hasConfig( "sslCert" ) && hasConfig( "sslKey" );
	}

	private CertificateWrapper initSsl()
	{
		File ssl = site.directory( "ssl" );
		ZIO.setDirectoryAccessWithException( ssl );

		try
		{
			if ( hasConfig( "sslCert" ) && hasConfig( "sslKey" ) )
			{
				String sslCertPath = getConfig( "sslCert" );
				String sslKeyPath = getConfig( "sslKey" );

				File sslCert = ZIO.isAbsolute( sslCertPath ) ? new File( sslCertPath ) : new File( ssl, sslCertPath );
				File sslKey = ZIO.isAbsolute( sslKeyPath ) ? new File( sslKeyPath ) : new File( ssl, sslKeyPath );

				return new CertificateWrapper( sslCert, sslKey, getConfig( "sslSecret" ) );
			}
		}
		catch ( FileNotFoundException | CertificateException e )
		{
			SiteManager.getLogger().severe( String.format( "Failed to load SslContext for site '%s' using cert '%s', key '%s', and hasSecret? %s", site.getId(), getConfig( "sslCert" ), getConfig( "sslKey" ), hasConfig( "sslSecret" ) ), e );
		}

		return null;
	}

	public File directory( String subdir ) throws SiteConfigurationException
	{
		return new File( directory(), subdir );
	}

	public Stream<DomainMapping> getParentMappings()
	{
		return Stream.of( getParentMapping() ).flatMap( DomainMapping::getParentMappings0 );
	}

	public Stream<DomainMapping> getParentMappings0()
	{
		DomainMapping mapping = getParentMapping();
		if ( mapping == null )
			return Stream.of( this );
		else
			return Stream.concat( Stream.of( this ), mapping.getParentMappings0() );
	}

	public DomainMapping getParentMapping()
	{
		Namespace ns = getDomainNamespace();
		if ( ns.getNodeCount() <= 1 )
			return null;
		return site.getMappings( ns.subNamespace( 1 ).getString() ).findFirst().orElse( null );
	}

	public DomainMapping getChildMapping( String child )
	{
		return site.getMappings( getDomainNamespace().prepend( child ).getString() ).findFirst().orElse( null );
	}

	public DomainNode getDomainNode()
	{
		return DomainTree.parseDomain( getFullDomain() );
	}

	public boolean isMapped()
	{
		DomainNode node = getDomainNode();
		return node != null && node.getSite() == site;
	}

	public DomainNode map()
	{
		DomainNode node = getDomainNode();
		if ( node != null )
			node.setSite( site );
		site.mappings.add( this );
		return node;
	}

	public void unmap()
	{
		DomainNode node = getDomainNode();
		if ( node != null )
			node.setSite( null );
	}

	public boolean isDefault()
	{
		return hasConfig( "default" ) && ZObjects.castToBool( getConfig( "default" ) );
	}

	public String getRootDomain()
	{
		return domain.getRootDomain().getString();
	}

	public String getChildDomain()
	{
		return domain.getChildDomain().getString();
	}

	@Override
	public String toString()
	{
		return String.format( "DomainMapping{site=%s,rootDomain=%s,childDomain=%s,config=[%s]}", site.getId(), domain.getTld(), domain.getSub(), config.entrySet().stream().map( e -> e.getKey() + "=\"" + e.getValue() + "\"" ).collect( Collectors.joining( "," ) ) );
	}
}
