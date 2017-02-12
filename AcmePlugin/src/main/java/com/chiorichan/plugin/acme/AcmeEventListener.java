package com.chiorichan.plugin.acme;

import com.chiorichan.event.EventHandler;
import com.chiorichan.event.EventPriority;
import com.chiorichan.event.Listener;
import com.chiorichan.event.http.SslCertificateDefaultEvent;
import com.chiorichan.event.http.SslCertificateMapEvent;
import com.chiorichan.event.site.SiteDomainChangeEvent;
import com.chiorichan.event.site.SiteDomainChangeEvent.SiteDomainChangeEventType;
import com.chiorichan.event.site.SiteLoadEvent;
import com.chiorichan.logger.Log;
import com.chiorichan.plugin.acme.api.AcmeProtocol;
import com.chiorichan.plugin.acme.certificate.CertificateMaintainer;
import com.chiorichan.plugin.acme.lang.AcmeDisabledDomainException;
import com.chiorichan.plugin.acme.lang.AcmeException;
import io.netty.handler.ssl.SslContext;

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
		if ( AcmePlugin.instance().isDefaultCertificateAllowed() && CertificateMaintainer.getDefaultCertificate() != null )
			event.setContext( CertificateMaintainer.getDefaultCertificateContext() );
	}

	@EventHandler( priority = EventPriority.NORMAL )
	public void onCertificateMapEvent( SslCertificateMapEvent event )
	{
		SslContext context = CertificateMaintainer.map( event.getHostname() );
		if ( context != null )
			event.setContext( context );
	}

	@EventHandler( priority = EventPriority.NORMAL )
	public void onSiteDomainChangeEvent( SiteDomainChangeEvent event )
	{
		// TODO On domain remove, delete it from config

		if ( event.getType() == SiteDomainChangeEventType.ADD )
			try
			{
				client.checkDomainVerification( event.getSiteMapping().getRootDomain(), event.getSiteMapping().getChildDomain(), true );
			}
			catch ( AcmeDisabledDomainException e )
			{
				getLogger().warning( e.getMessage() );
			}
			catch ( AcmeException e )
			{
				e.printStackTrace();
			}
		if ( event.getType() == SiteDomainChangeEventType.REMOVE )
		{
			AcmePlugin.instance().getSubConfig().set( "domains." + event.getSiteMapping().getNamespace().getString( "_", true ), null );
		}
	}

	@EventHandler( priority = EventPriority.NORMAL )
	public void onSiteLoadEvent( SiteLoadEvent event )
	{
		event.getSite().getDomains().forEach( n ->
		{
			try
			{
				client.checkDomainVerification( n.getRootDomain(), n.getChildDomain(), true );
			}
			catch ( AcmeDisabledDomainException e1 )
			{
				getLogger().warning( e1.getMessage() );
			}
			catch ( AcmeException e1 )
			{
				e1.printStackTrace();
			}
		} );
	}

	public Log getLogger()
	{
		return AcmePlugin.instance().getLogger();
	}
}
