package com.chiorichan.user;

import java.io.File;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.codec.digest.DigestUtils;

import com.chiorichan.ChatColor;
import com.chiorichan.Loader;
import com.chiorichan.event.user.UserLoginEvent;
import com.chiorichan.event.user.UserLoginEvent.Result;
import com.chiorichan.framework.Site;
import com.chiorichan.http.PersistentSession;
import com.chiorichan.user.builtin.SqlAdapter;
import com.chiorichan.user.builtin.UserLookupAdapter;

public class UserManager
{
	private static final SimpleDateFormat d = new SimpleDateFormat( "yyyy-MM-dd \'at\' HH:mm:ss z" );
	private Loader server;
	public final List<User> users = new java.util.concurrent.CopyOnWriteArrayList<User>();
	private final BanList banByName = new BanList( new File( "banned-users.txt" ) );
	private final BanList banByIP = new BanList( new File( "banned-ips.txt" ) );
	private Set<String> operators = new HashSet<String>();
	private Set<String> whitelist = new LinkedHashSet<String>();
	public boolean hasWhitelist;
	protected int maxUsers;
	protected int c;
	private boolean m;
	private int n;
	
	public UserManager(Loader server0)
	{
		banByName.setEnabled( false );
		banByIP.setEnabled( false );
		maxUsers = Loader.getConfig().getInt( "server.maxLogins", -1 );
		
		server = server0;
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
		Loader.getConsole().sendMessage( msg );
	}
	
	public User attemptLogin( PersistentSession sess, String username, String password ) throws LoginException
	{
		Site site = sess.getRequest().getSite();
		
		if ( username == null || username.isEmpty() )
			throw new LoginException( LoginException.ExceptionReasons.emptyUsername );
		
		User user = new User( username, site.getUserLookupAdapter() );
		
		try
		{
			if ( password == null || password.isEmpty() )
				throw new LoginException( LoginException.ExceptionReasons.emptyPassword );
			
			if ( !user.validatePassword( password ) )
				throw new LoginException( LoginException.ExceptionReasons.incorrectLogin );
			
			UserLoginEvent event = new UserLoginEvent( user );
			Loader.getPluginManager().callEvent( event );
			
			if ( !user.isWhitelisted() )
				throw new LoginException( LoginException.ExceptionReasons.notWhiteListed );
			
			if ( user.isBanned() )
				throw new LoginException( LoginException.ExceptionReasons.banned );
			
			if ( event.getResult() != Result.ALLOWED && event.getResult() != Result.PRELOGIN )
				if ( event.getKickMessage().isEmpty() )
					throw new LoginException( LoginException.ExceptionReasons.incorrectLogin );
				else
					throw new LoginException( LoginException.customExceptionReason( event.getKickMessage() ) );
			
			site.getUserLookupAdapter().preLoginCheck( user );
			
			List<User> arraylist = new ArrayList<User>();
			User usera;
			
			for ( int i = 0; i < users.size(); ++i )
			{
				usera = (User) users.get( i );
				if ( usera.getUserId().equalsIgnoreCase( usera.getUserId() ) )
				{
					arraylist.add( usera );
				}
			}
			
			Iterator<User> iterator = arraylist.iterator();
			while ( iterator.hasNext() )
			{
				usera = (User) iterator.next();
				usera.kick( "You logged in from another location." );
				// TODO Make this message customizable from configs.
			}
			
			if ( !sess.isSet( "user" ) )
				sess.setArgument( "user", user.getUserId() );
			
			if ( !sess.isSet( "pass" ) )
				sess.setArgument( "pass", DigestUtils.md5Hex( user.getPassword() ) );
			
			site.getUserLookupAdapter().postLoginCheck( user );
			Loader.getInstance().onUserLogin( user );
			
			Object o = sess.getRequest().getAttribute( "remember" );
			boolean remember = ( o == null ) ? false : (boolean) o;
			
			if ( remember )
				sess.setCookieExpiry( 5 * 365 * 24 * 60 * 60 );
			else
				sess.setCookieExpiry( 604800 );
			
			user.setHandler( sess );
			users.add( user );
			return user;
		}
		catch ( LoginException l )
		{
			site.getUserLookupAdapter().failedLoginUpdate( user );
			throw l.setUser( user );
		}
	}
	
	/**
	 * Builtin User Lookup Adapter
	 */
	public UserLookupAdapter getBuiltinAdapter() throws SQLException
	{
		return new SqlAdapter( Loader.getPersistenceManager().getSql(), "users" );
	}
}
