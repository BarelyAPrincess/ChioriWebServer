/**
 * This software may be modified and distributed under the terms
 * of the MIT license.  See the LICENSE file for details.
 * <p>
 * Copyright (c) 2017 Chiori Greene a.k.a. Chiori-chan <me@chiorichan.com>
 * Copyright (c) 2017 Penoaks Publishing LLC <development@penoaks.com>
 * <p>
 * All Rights Reserved.
 */
package com.chiorichan.net;

import com.chiorichan.AppConfig;
import com.chiorichan.event.EventBus;
import com.chiorichan.event.EventHandler;
import com.chiorichan.event.EventPriority;
import com.chiorichan.event.EventRegistrar;
import com.chiorichan.event.Listener;
import com.chiorichan.event.http.ErrorEvent;
import com.chiorichan.http.ApacheHandler;
import com.chiorichan.http.WebInterpreter;
import com.chiorichan.lang.EnumColor;
import com.chiorichan.lang.HttpError;
import com.chiorichan.site.Site;
import com.chiorichan.tasks.TaskRegistrar;
import com.chiorichan.tasks.Timings;
import com.chiorichan.zutils.ZHttp;
import com.chiorichan.zutils.ZIO;
import com.chiorichan.zutils.ZLists;
import org.apache.commons.lang3.Validate;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;

/**
 * Maintains the network security for all protocols, e.g., TCP, HTTP and HTTPS.
 */
public class NetworkSecurity implements EventRegistrar, TaskRegistrar, Listener
{
	public enum IpStrikeType
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
		 * @param countToBan  The number of times required until the IP is temp banned for abuse
		 * @param dropOffTime The maximum amount of time between strips to make it count towards the ban
		 * @param banFor      The maximum amount of time the IP will be banned for
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
					return AppConfig.get().getString( "server.defaultBanReason", "The Ban Hammer Has Spoken" );
			}
		}
	}

	@SuppressWarnings( "unused" )
	private static class IpTracker
	{
		static class Record
		{
			int count = 0;
			long time = 0;
		}

		private boolean banned = false;
		private String banReason = null;
		private long banTill = -1L;
		private long banWhen = -1L;
		private final String ipAddress;
		private final Map<IpStrikeType, Record> strikes = new ConcurrentHashMap<>();

		IpTracker( String ipAddress )
		{
			if ( !ZHttp.isValidIPv4( ipAddress ) && !ZHttp.isValidIPv6( ipAddress ) )
				throw new IllegalArgumentException( "The provided IP '" + ipAddress + "' is not a valid IPv4 or IPv6 address." );

			this.ipAddress = ipAddress;
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
				NetworkManager.getLogger().info( EnumColor.RED + "" + EnumColor.NEGATIVE + "The IP '" + ipAddress + "' has been banned for reason '" + type.getReason() + "'" );
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

	private static List<IpTracker> ips = new CopyOnWriteArrayList<>();

	static
	{
		ips.add( new IpTracker( "94.23.193.70" ).setBanned() );
		ips.add( new IpTracker( "204.15.135.116" ).setBanned() );
		ips.add( new IpTracker( "222.91.96.117" ).setBanned() );
		ips.add( new IpTracker( "190.213.166.12" ).setBanned() );

		List<String> linesCombined = new ArrayList<>();

		try
		{
			File bannedIpFile = new File( AppConfig.get().getDirectory(), "banned-ipv4.txt" );
			if ( !bannedIpFile.exists() )
				bannedIpFile.createNewFile();
			linesCombined.addAll( ZIO.readFileToLines( bannedIpFile ) );
		}
		catch ( IOException e )
		{
			NetworkManager.getLogger().severe( "Failed to load banned IPv4 file", e );
		}

		try
		{
			File bannedIpFile = new File( AppConfig.get().getDirectory(), "banned-ipv6.txt" );
			if ( !bannedIpFile.exists() )
				bannedIpFile.createNewFile();
			linesCombined.addAll( ZIO.readFileToLines( bannedIpFile ) );
		}
		catch ( IOException e )
		{
			NetworkManager.getLogger().severe( "Failed to load banned IPv6 file", e );
		}

		for ( String line : linesCombined )
			ips.add( new IpTracker( line ).setBanned() );
	}

	public static void shutdown()
	{
		// TODO Add and remove lines from banned file once an ip is either banned and unbanned.
		// TODO Save the ban timer so ips will be unbanned once they reach their thresholds.

		File bannedIp4File = new File( AppConfig.get().getDirectory(), "banned-ipv4.txt" );
		File bannedIp6File = new File( AppConfig.get().getDirectory(), "banned-ipv6.txt" );

		List<String> bannedIPv4 = new ArrayList<>();
		List<String> bannedIPv6 = new ArrayList<>();

		for ( IpTracker ip : ips )
		{
			if ( ip.banned )
				if ( ZHttp.isValidIPv4( ip.ipAddress ) )
					bannedIPv4.add( ip.ipAddress );
				else if ( ZHttp.isValidIPv6( ip.ipAddress ) )
					bannedIPv6.add( ip.ipAddress );
				else
					NetworkManager.getLogger().info( "The string did not match any IPv4 or IPv6 pattern, it will not be saved. Is this a bug?" );
		}

		try
		{
			ZIO.writeStringToFile( bannedIp4File, bannedIPv4.stream().collect( Collectors.joining( "\n" ) ) );
		}
		catch ( IOException e )
		{
			NetworkManager.getLogger().severe( "Failed to save banned IPv4 file", e );
		}

		try
		{
			ZIO.writeStringToFile( bannedIp6File, bannedIPv6.stream().collect( Collectors.joining( "\n" ) ) );
		}
		catch ( IOException e )
		{
			NetworkManager.getLogger().severe( "Failed to save banned IPv6 file", e );
		}
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
		return ZLists.findOrNew( ips, t -> t.ipAddress.equals( ip ), new IpTracker( ip ) );
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
		if ( !ZHttp.isValidIPv4( ip ) || !ZHttp.isValidIPv6( ip ) )
			throw new IllegalArgumentException( "The provided IP '" + ip + "' is not a valid IPv4 or IPv6 address." );

		return get( ip ).banned;
	}

	public static void unbanIp( String ip )
	{
		get( ip ).banned = false;
	}

	public NetworkSecurity()
	{
		EventBus.instance().registerEvents( this, this );
	}

	@Override
	public String getName()
	{
		return "NetworkSecurity";
	}

	@Override
	public boolean isEnabled()
	{
		return true;
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
