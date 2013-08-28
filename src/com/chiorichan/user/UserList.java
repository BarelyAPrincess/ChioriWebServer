package com.chiorichan.user;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import javax.servlet.http.Cookie;

import org.apache.commons.codec.digest.DigestUtils;

import com.chiorichan.Loader;
import com.chiorichan.database.SqlConnector;
import com.chiorichan.framework.Framework;
import com.chiorichan.framework.Site;
import com.chiorichan.server.PendingConnection;

public class UserList
{
	private static final SimpleDateFormat d = new SimpleDateFormat( "yyyy-MM-dd \'at\' HH:mm:ss z" );
	private Loader server;
	private Site site;
	public final List Users = new java.util.concurrent.CopyOnWriteArrayList();
	private final BanList banByName = new BanList();// = new BanList( new File( "banned-Users.txt" ) );
	private final BanList banByIP = new BanList();// = new BanList( new File( "banned-ips.txt" ) );
	private Set operators = new HashSet();
	private Set whitelist = new java.util.LinkedHashSet();
	public boolean hasWhitelist;
	protected int maxUsers;
	protected int c;
	private boolean m;
	private int n;
	
	public UserList(Loader server0)
	{
		this();
		server = server0;
	}
	
	public UserList(Site site0)
	{
		this();
		site = site0;
	}
	
	public UserList()
	{
		banByName.setEnabled( false );
		banByIP.setEnabled( false );
		maxUsers = 50;
	}
	
	public User attemptLogin( PendingConnection pc, String s, String hostname )
	{
		return null;
		/*
		 * User entity = new user( server, server.getWorldServer( 0 ), s, server.O() ? new DemoUserInteractManager(
		 * server.getWorldServer( 0 ) ) : new UserInteractManager( server.getWorldServer( 0 ) ) ); User User =
		 * entity.getBukkitEntity(); UserLoginEvent event = new UserLoginEvent( User, hostname,
		 * pendingconnection.getSocket().getInetAddress() );
		 * 
		 * SocketAddress socketaddress = pendingconnection.networkManager.getSocketAddress();
		 * 
		 * if ( banByName.isBanned( s ) ) { BanEntry banentry = (BanEntry) banByName.getEntries().get( s ); String s1 =
		 * "You are banned from this server!\nReason: " + banentry.getReason();
		 * 
		 * if ( banentry.getExpires() != null ) { s1 = s1 + "\nYour ban will be removed on " + d.format(
		 * banentry.getExpires() ); }
		 * 
		 * event.disallow( UserLoginEvent.Result.KICK_BANNED, s1 ); } else if ( !isWhitelisted( s ) ) { event.disallow(
		 * UserLoginEvent.Result.KICK_WHITELIST, "You are not white-listed on this server!" ); } else { String s2 =
		 * socketaddress.toString();
		 * 
		 * s2 = s2.substring( s2.indexOf( "/" ) + 1 ); s2 = s2.substring( 0, s2.indexOf( ":" ) ); if ( banByIP.isBanned(
		 * s2 ) ) { BanEntry banentry1 = (BanEntry) banByIP.getEntries().get( s2 ); String s3 =
		 * "Your IP address is banned from this server!\nReason: " + banentry1.getReason();
		 * 
		 * if ( banentry1.getExpires() != null ) { s3 = s3 + "\nYour ban will be removed on " + d.format(
		 * banentry1.getExpires() ); }
		 * 
		 * event.disallow( UserLoginEvent.Result.KICK_BANNED, s3 ); } else if ( Users.size() >= maxUsers ) {
		 * event.disallow( UserLoginEvent.Result.KICK_FULL, "The server is full!" ); } else { event.disallow(
		 * UserLoginEvent.Result.ALLOWED, s2 ); } }
		 * 
		 * cserver.getPluginManager().callEvent( event ); if ( event.getResult() != UserLoginEvent.Result.ALLOWED ) {
		 * pendingconnection.disconnect( event.getKickMessage() ); return null; }
		 * 
		 * return entity; // CraftBukkit end
		 */
	}
	
	public User processLogin( User usr )
	{
		String s = usr.getName();
		ArrayList arraylist = new ArrayList();
		
		User user;
		
		for ( int i = 0; i < Users.size(); ++i )
		{
			user = (User) Users.get( i );
			if ( user.getName().equalsIgnoreCase( s ) )
			{
				arraylist.add( user );
			}
		}
		
		Iterator iterator = arraylist.iterator();
		
		while ( iterator.hasNext() )
		{
			user = (User) iterator.next();
			user.kick( "You logged in from another location" );
		}
		
		return usr;
	}
	
	public void tick()
	{
		if ( ++n > 600 )
		{
			n = 0;
		}
	}
	
	public BanList getNameBans()
	{
		return banByName;
	}
	
	public BanList getIPBans()
	{
		return banByIP;
	}
	
	public void addOp( String s )
	{
		operators.add( s.toLowerCase() );
		
		User user = server.getUser( s );
		if ( user != null )
		{
			user.recalculatePermissions();
		}
	}
	
	public void removeOp( String s )
	{
		operators.remove( s.toLowerCase() );
		
		User user = server.getUser( s );
		if ( user != null )
		{
			user.recalculatePermissions();
		}
	}
	
	public boolean isWhitelisted( String s )
	{
		s = s.trim().toLowerCase();
		return !hasWhitelist || operators.contains( s ) || whitelist.contains( s );
	}
	
	public boolean isOp( String user )
	{
		return operators.contains( user.trim().toLowerCase() );
	}
	
	public User getUser( String s )
	{
		Iterator iterator = Users.iterator();
		
		User user;
		
		do
		{
			if ( !iterator.hasNext() )
			{
				return null;
			}
			
			user = (User) iterator.next();
		}
		while ( !user.getName().equalsIgnoreCase( s ) );
		
		return user;
	}
	
	public void saveUsers()
	{
		for ( int i = 0; i < Users.size(); ++i )
		{
			( (User) Users.get( i ) ).save();
		}
	}
	
	public void addWhitelist( String s )
	{
		whitelist.add( s );
	}
	
	public void removeWhitelist( String s )
	{
		whitelist.remove( s );
	}
	
	public Set getWhitelisted()
	{
		return whitelist;
	}
	
	public Set getOPs()
	{
		return operators;
	}
	
	public void reloadWhitelist()
	{
	}
	
	public int getUserCount()
	{
		return Users.size();
	}
	
	public int getMaxUsers()
	{
		return maxUsers;
	}
	
	public boolean getHasWhitelist()
	{
		return hasWhitelist;
	}
	
	public void setHasWhitelist( boolean flag )
	{
		hasWhitelist = flag;
	}
	
	public List searchUsers( String s )
	{
		ArrayList arraylist = new ArrayList();
		Iterator iterator = Users.iterator();
		
		while ( iterator.hasNext() )
		{
			User user = (User) iterator.next();
			
			if ( user.getName().equals( s ) )
			{
				arraylist.add( user );
			}
		}
		
		return arraylist;
	}
	
	public Loader getServer()
	{
		return server;
	}
	
	public void serverShutdown()
	{
		while ( !Users.isEmpty() )
		{
			( (User) Users.get( 0 ) ).kick( server.getShutdownMessage() );
		}
	}
	
	public void sendMessage( String msg )
	{
		server.getConsole().sendMessage( msg );
	}

	public User validateUser( Framework fw, String username, String password )
	{
		SqlConnector sql = ( site != null ) ? site.getDatabase() : Framework.getDatabase();
		
		User user = new User( sql, username, password );
		
		if ( !user.valid )
			return user;
		
		sql.queryUpdate( "UPDATE `users` SET `lastlogin` = '" + System.currentTimeMillis() + "', `numloginfail` = '0' WHERE `userID` = '" + user.getUserId() + "'" );
		
		fw.getServer().setSessionString( "user", user.getUserId() );
		fw.getServer().setSessionString( "pass", new String( DigestUtils.md5( user.getPassword() ) ) );
		
		Object o = fw.getRequest().getAttribute( "remember" );
		boolean remember = ( o == null ) ? false : (boolean) o;
		
		if ( remember )
			fw.getServer().setCookieExpiry( 5 * 365* 24 * 60 * 60 );
		else
			fw.getServer().setCookieExpiry( 604800 );
		
		return user;
	}
}