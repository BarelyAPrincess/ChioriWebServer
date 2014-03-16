package com.chiorichan.user;

import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.codec.digest.DigestUtils;

import com.chiorichan.Loader;
import com.chiorichan.database.SqlConnector;
import com.chiorichan.event.user.UserLoginEvent;
import com.chiorichan.event.user.UserLoginEvent.Result;
import com.chiorichan.http.PersistentSession;
import com.chiorichan.user.builtin.SqlAdapter;
import com.chiorichan.user.builtin.UserLookupAdapter;
import com.google.common.collect.Lists;

public class UserManager
{
	//private static final SimpleDateFormat d = new SimpleDateFormat( "yyyy-MM-dd \'at\' HH:mm:ss z" );
	private Loader server;
	private UserLookupAdapter userLookupAdapter;
	public final List<User> users = new java.util.concurrent.CopyOnWriteArrayList<User>();
	private final BanList banByName = new BanList( new File( "banned-users.txt" ) );
	private final BanList banByIP = new BanList( new File( "banned-ips.txt" ) );
	private Set<String> operators = new HashSet<String>();
	private Set<String> whitelist = new LinkedHashSet<String>();
	public boolean hasWhitelist;
	protected int maxUsers;
	protected int c;
	//private boolean m;
	private int n;
	
	public UserManager(Loader server0)
	{
		banByName.setEnabled( false );
		banByIP.setEnabled( false );
		maxUsers = Loader.getConfig().getInt( "server.maxLogins", -1 );
		
		server = server0;
		
		try
		{
			switch ( Loader.getConfig().getString( "users.lookupAdapter.type", "default" ) )
			{
				case "sql":
					String siteId = Loader.getConfig().getString( "users.lookupAdapter.siteId" );
					SqlConnector sql = ( siteId != null && !siteId.isEmpty() && Loader.getPersistenceManager().getSiteManager().getSiteById( siteId ) != null ) ? Loader.getPersistenceManager().getSiteManager().getSiteById( siteId ).getDatabase() : Loader.getPersistenceManager().getSql();
					
					userLookupAdapter = new SqlAdapter( sql, Loader.getConfig().getString( "users.lookupAdapter.table", "users" ), Loader.getConfig().getStringList( "users.lookupAdapter.fields" ) );
					Loader.getLogger().info( "Initiated Sql User Lookup Adapter `" + userLookupAdapter + "` with sql `" + sql + "`" );
					break;
				// case "file":
				// TODO Develop the file backend lookup adapter
				// break;
				default:
					userLookupAdapter = new SqlAdapter( Loader.getPersistenceManager().getSql(), "users" );
					Loader.getLogger().info( "Initiated Default User Lookup Adapter `" + userLookupAdapter + "`" );
					break;
			}
		}
		catch ( LookupAdapterException e )
		{
			Loader.getLogger().severe( "There was a severe error encoutered when attempting to create the User Lookup Adapter." );
			Loader.getLogger().panic( e.getMessage() );
		}
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
		Iterator<User> iterator = users.iterator();
		
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
	
	public Set<String> getWhitelisted()
	{
		return whitelist;
	}
	
	public Set<String> getOPs()
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
	
	public List<User> searchUsers( String s )
	{
		ArrayList<User> arraylist = Lists.newArrayList();
		Iterator<User> iterator = users.iterator();
		
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
	
	public User loadUser( String username ) throws LoginException
	{
		User user = null;
		
		for ( User u : users )
		{
			if ( userLookupAdapter.matchUser( u, username ) )
				user = u;
		}
		
		if ( user == null )
		{
			user = new User( username, userLookupAdapter );
			users.add( user );
		}
		
		return user;
	}
	
	public User attemptLogin( PersistentSession sess, String username, String password ) throws LoginException
	{
		if ( username == null || username.isEmpty() )
			throw new LoginException( LoginException.ExceptionReasons.emptyUsername );
		
		User user = loadUser( username );
		user.putHandler( sess );
		
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
			
			if ( event.getResult() != Result.ALLOWED )
				if ( event.getKickMessage().isEmpty() )
					throw new LoginException( LoginException.ExceptionReasons.incorrectLogin );
				else
					throw new LoginException( LoginException.customExceptionReason( event.getKickMessage() ) );
			
			userLookupAdapter.preLoginCheck( user );
			
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
			
			sess.setArgument( "user", user.getUserId() );
			sess.setArgument( "pass", DigestUtils.md5Hex( user.getPassword() ) );
			
			userLookupAdapter.postLoginCheck( user );
			Loader.getInstance().onUserLogin( user );
			
			return user;
		}
		catch ( LoginException l )
		{
			userLookupAdapter.failedLoginUpdate( user );
			throw l.setUser( user );
		}
	}
}