package com.chiorichan.user;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.codec.digest.DigestUtils;

import com.chiorichan.Loader;
import com.chiorichan.database.SqlConnector;
import com.chiorichan.framework.Site;
import com.chiorichan.http.PersistentSession;

public class UserList
{
	private static final SimpleDateFormat d = new SimpleDateFormat( "yyyy-MM-dd \'at\' HH:mm:ss z" );
	private Loader server;
	private Site site;
	public final List<User> users = new java.util.concurrent.CopyOnWriteArrayList<User>();
	private final BanList banByName = new BanList( new File( "banned-users.txt" ) );
	private final BanList banByIP = new BanList( new File( "banned-ips.txt" ) );
	private Set operators = new HashSet();
	private Set whitelist = new LinkedHashSet();
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
	
	public User processLogin( User usr )
	{
		String s = usr.getName();
		ArrayList arraylist = new ArrayList();
		
		User user;
		
		for ( int i = 0; i < users.size(); ++i )
		{
			user = (User) users.get( i );
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
		Iterator iterator = users.iterator();
		
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
		for ( int i = 0; i < users.size(); ++i )
		{
			( (User) users.get( i ) ).save();
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
		return users.size();
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
		Iterator iterator = users.iterator();
		
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
		while ( !users.isEmpty() )
		{
			( (User) users.get( 0 ) ).kick( server.getShutdownMessage() );
		}
	}
	
	public void sendMessage( String msg )
	{
		server.getConsole().sendMessage( msg );
	}
	
	public User attemptLogin( PersistentSession sess, String username, String password )
	{
		SqlConnector sql = ( site != null ) ? site.getDatabase() : Loader.getPersistenceManager().getSql();
		
		User user = new User( sql, username, password );
		
		if ( !user.valid )
			return user;
		
		sql.queryUpdate( "UPDATE `users` SET `lastlogin` = '" + System.currentTimeMillis() + "', `numloginfail` = '0' WHERE `userID` = '" + user.getUserId() + "'" );
		
		if ( !sess.isSet( "user" ) )
			sess.setArgument( "user", user.getUserId() );
		
		if ( !sess.isSet( "pass" ) )
			sess.setArgument( "pass", DigestUtils.md5Hex( user.getPassword() ) );
		
		Object o = sess.getRequest().getAttribute( "remember" );
		boolean remember = ( o == null ) ? false : (boolean) o;
		
		if ( remember )
			sess.setCookieExpiry( 5 * 365 * 24 * 60 * 60 );
		else
			sess.setCookieExpiry( 604800 );
		
		return user;
	}
}
