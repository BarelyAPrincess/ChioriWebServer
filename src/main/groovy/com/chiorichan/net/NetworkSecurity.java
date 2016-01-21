/**
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 *
 * Copyright 2015 Chiori Greene a.k.a. Chiori-chan <me@chiorichan.com>
 * All Right Reserved.
 */
package com.chiorichan.net;

import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.Validate;

import com.chiorichan.LogColor;
import com.chiorichan.Loader;
import com.chiorichan.event.BuiltinRegistrar;
import com.chiorichan.event.EventBus;
import com.chiorichan.event.EventHandler;
import com.chiorichan.event.EventPriority;
import com.chiorichan.event.Listener;
import com.chiorichan.event.http.ErrorEvent;
import com.chiorichan.http.ApacheHandler;
import com.chiorichan.http.WebInterpreter;
import com.chiorichan.lang.HttpError;
import com.chiorichan.site.Site;
import com.chiorichan.tasks.Timings;
import com.chiorichan.util.NetworkFunc;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

/**
 * Maintains the network security for all protocols, e.g., TCP, HTTP and HTTPS.
 */
public class NetworkSecurity extends BuiltinRegistrar implements Listener
{
	public static enum IpStrikeType
	{
		CLOSED_EARLY( 3, 1000, Timings.DAY * 3 ), HTTP_ERROR_400( 6, 2000, Timings.DAY ), HTTP_ERROR_500( 24, 1000, Timings.MINUTE * 15 ), IGNORING_COOKIES( 12, 1000, Timings.DAY );
		
		public final int banFor;
		
		/**
		 * Indicates that after x strikes the IP Should be banned
		 */
		public final int countToBan;
		
		/**
		 * Indicates the maximum amount of time between strikes to count to a ban
		 */
		public final int dropOffTime;
		
		/**
		 * Constructor for a new IP Strike Type
		 * 
		 * @param countToBan
		 *            The number of times required until the IP is temp banned for abuse
		 * @param dropOffTime
		 *            The maximum amount of time between strips to make it count towards the ban
		 * @param banFor
		 *            The maximum amount of time the IP will be banned for
		 */
		IpStrikeType( int countToBan, int dropOffTime, int banFor )
		{
			this.countToBan = countToBan;
			this.dropOffTime = dropOffTime;
			this.banFor = banFor;
		}
		
		public String getMessage()
		{
			switch ( this )
			{
				case HTTP_ERROR_400:
					return "There was a http error, continued abuse will lead to banishment!";
				case HTTP_ERROR_500:
					return "There was a http error, continued abuse will lead to banishment!";
				case CLOSED_EARLY:
					return "The connection was closed before we could finish the request, continued abuse will lead to banishment!";
				default:
					return "<No Message>";
			}
		}
		
		public String getReason()
		{
			switch ( this )
			{
				case HTTP_ERROR_400:
					return "Banned for having far too many repeated Http Errors between 400-499";
				case HTTP_ERROR_500:
					return "Banned for having far too many repeated Http Errors between 500-599";
				case CLOSED_EARLY:
					return "Banned for closing the connection early repeatedly";
				default:
					return Loader.getConfig().getString( "server.defaultBanReason", "The Ban Hammer Has Spoken" );
			}
		}
	}
	
	@SuppressWarnings( "unused" )
	private static class IpTracker
	{
		static class Record
		{
			int count = 0;
			int time = 0;
		}
		
		private boolean banned = false;
		private String banReason = null;
		private int banTill = -1;
		private int banWhen = -1;
		private final String ipAddr;
		private final Map<IpStrikeType, Record> strikes = Maps.newConcurrentMap();
		
		IpTracker( String ipAddr )
		{
			if ( !NetworkFunc.isValidIPv4( ipAddr ) && !NetworkFunc.isValidIPv6( ipAddr ) )
				throw new IllegalArgumentException( "The provided IP '" + ipAddr + "' is not a valid IPv4 or IPv6 address." );
			
			this.ipAddr = ipAddr;
		}
		
		void addStrike( IpStrikeType type, String... args )
		{
			// If already banned there is no need to track reasons to ban
			if ( banned )
				return;
			
			Validate.notNull( type );
			Validate.notNull( args );
			
			Record r = strikes.containsKey( type ) ? strikes.get( type ) : new Record();
			
			if ( Timings.epoch() - r.time > type.dropOffTime )
				r.count = 1;
			else
				r.count++;
			
			if ( r.count >= type.countToBan )
			{
				NetworkManager.getLogger().info( LogColor.RED + "" + LogColor.NEGATIVE + "The IP '" + ipAddr + "' has been banned for reason '" + type.getReason() + "'" );
				banned = true;
				banReason = type.getReason();
				banWhen = Timings.epoch();
				banTill = Timings.epoch() + type.banFor;
				strikes.remove( type );
				return;
			}
			
			r.time = Timings.epoch();
			
			strikes.put( type, r );
		}
		
		public IpTracker setBanned()
		{
			return setBanned( true );
		}
		
		public IpTracker setBanned( boolean banned )
		{
			this.banned = banned;
			return this;
		}
	}
	
	private static List<IpTracker> ips = Lists.newCopyOnWriteArrayList();
	
	static
	{
		ips.add( new IpTracker( "94.23.193.70" ).setBanned() );
		ips.add( new IpTracker( "204.15.135.116" ).setBanned() );
		ips.add( new IpTracker( "222.91.96.117" ).setBanned() );
		ips.add( new IpTracker( "190.213.166.12" ).setBanned() );
	}
	
	public NetworkSecurity()
	{
		EventBus.INSTANCE.registerEvents( this, this );
	}
	
	public static void addStrikeToIp( String ip, IpStrikeType type, String... args )
	{
		get( ip ).addStrike( type, args );
	}
	
	public static void banIp( String ip )
	{
		get( ip ).banned = true;
	}
	
	private static IpTracker get( String ip )
	{
		for ( IpTracker t : ips )
			if ( t.ipAddr.equals( ip ) )
				return t;
		
		IpTracker it = new IpTracker( ip );
		ips.add( it );
		return it;
	}
	
	public static void isForbidden( ApacheHandler htaccess, Site site, WebInterpreter fi ) throws HttpError
	{
		// String[] allowed = htaccess.getAllowed();
		
		// TODO Reimplement protected site file check
		
		/*
		 * if ( fi.hasFile() && site.protectCheck( fi.getFilePath() ) )
		 * throw new HttpError( 401, "Loading of this page (" + fi.getFilePath() + ") is not allowed since its hard protected in the configs." );
		 */
	}
	
	public static boolean isIpBanned( String ipAddr )
	{
		try
		{
			return isIpBannedWithException( ipAddr );
		}
		catch ( IllegalArgumentException e )
		{
			return false;
		}
	}
	
	public static boolean isIpBannedWithException( String ip )
	{
		if ( !NetworkFunc.isValidIPv4( ip ) || !NetworkFunc.isValidIPv6( ip ) )
			throw new IllegalArgumentException( "The provided IP '" + ip + "' is not a valid IPv4 or IPv6 address." );
		
		return get( ip ).banned;
	}
	
	public static void unbanIp( String ip )
	{
		get( ip ).banned = false;
	}
	
	@Override
	public String getName()
	{
		return "NetworkSecurity";
	}
	
	@EventHandler( priority = EventPriority.MONITOR )
	public void onErrorEvent( ErrorEvent event )
	{
		if ( event.getStatus() == 404 )
		{
			// Nothing
		}
	}
}
