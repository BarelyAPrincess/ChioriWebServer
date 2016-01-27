package com.chiorichan.plugin.acme;

import io.netty.handler.ssl.SslContext;

import java.util.Map.Entry;
import java.util.Set;

import com.chiorichan.Loader;
import com.chiorichan.event.EventHandler;
import com.chiorichan.event.EventPriority;
import com.chiorichan.event.Listener;
import com.chiorichan.event.http.SslCertificateDefaultEvent;
import com.chiorichan.event.http.SslCertificateMapEvent;
import com.chiorichan.event.site.SiteDomainChangeEvent;
import com.chiorichan.event.site.SiteDomainChangeEvent.SiteDomainChangeEventType;
import com.chiorichan.event.site.SiteLoadEvent;
import com.chiorichan.plugin.acme.api.AcmeProtocol;
import com.chiorichan.plugin.acme.lang.AcmeException;

public class AcmeEventListener implements Listener
{
	AcmeProtocol client;

	public AcmeEventListener( AcmePlugin plugin )
	{
		client = plugin.getClient();
	}

	@EventHandler( priority = EventPriority.NORMAL )
	public void onCertificateDefaultEvent( SslCertificateDefaultEvent event )
	{
		if ( CertificateMaintainer.getDefaultCertificate() != null )
			event.setContext( CertificateMaintainer.getDefaultCertificateContext() );

		Loader.getLogger().debug( "Default result " + CertificateMaintainer.getDefaultCertificate() );
	}

	@EventHandler( priority = EventPriority.NORMAL )
	public void onCertificateMapEvent( SslCertificateMapEvent event )
	{
		SslContext context = CertificateMaintainer.map( event.getHostname() );
		if ( context != null )
			event.setContext( context );

		Loader.getLogger().debug( "Map result " + context + " for " + event.getHostname() );
	}

	@EventHandler( priority = EventPriority.NORMAL )
	public void onSiteDomainChangeEvent( SiteDomainChangeEvent event )
	{
		// TODO On domain remove, delete it from config

		if ( event.getType() == SiteDomainChangeEventType.ADD )
			try
			{
				client.checkDomainVerification( event.getDomain(), event.getSiteDomain().getSubdomain(), true );
			}
			catch ( AcmeException e )
			{
				e.printStackTrace();
			}
	}

	@EventHandler( priority = EventPriority.NORMAL )
	public void onSiteLoadEvent( SiteLoadEvent event )
	{
		for ( Entry<String, Set<String>> e : event.getSite().getDomains().entrySet() )
			try
			{
				for ( String s : e.getValue() )
					client.checkDomainVerification( e.getKey(), s, true );
			}
			catch ( AcmeException e1 )
			{
				e1.printStackTrace();
			}
	}
}
