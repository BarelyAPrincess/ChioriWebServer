/**
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 *
 * Copyright 2015 Chiori Greene a.k.a. Chiori-chan <me@chiorichan.com>
 * All Right Reserved.
 */
package com.chiorichan.session;

import java.io.File;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import com.chiorichan.Loader;
import com.chiorichan.account.AccountAttachment;
import com.chiorichan.account.AccountInstance;
import com.chiorichan.account.AccountMeta;
import com.chiorichan.account.AccountPermissible;
import com.chiorichan.factory.BindingProvider;
import com.chiorichan.factory.EvalBinding;
import com.chiorichan.factory.EvalFactory;
import com.chiorichan.http.HttpCookie;
import com.chiorichan.messaging.MessageSender;
import com.chiorichan.permission.PermissibleEntity;
import com.chiorichan.site.Site;
import com.chiorichan.site.SiteManager;
import com.chiorichan.util.StringFunc;

/**
 * Acts as a bridge between a Session and the User
 * TODO If Session is nullified, we need to start a new one
 */
public abstract class SessionWrapper implements BindingProvider, AccountAttachment
{
	/**
	 * The binding specific to this request
	 */
	private EvalBinding binding = new EvalBinding();
	
	/**
	 * The EvalFactory used to process scripts of this request
	 */
	private EvalFactory factory;
	
	/**
	 * The session associated with this request
	 */
	private Session session;
	
	/**
	 * Used to nullify a SessionWrapper and prepare it for collection by the GC
	 * something that should happen naturally but the simpler the better.
	 * 
	 * Sidenote: This is only for cleaning up a Session Wrapper, cleaning up an actual parent session is a whole different story.
	 */
	@SuppressWarnings( "unchecked" )
	public void finish()
	{
		Map<String, Object> bindings = session.globals;
		Map<String, Object> variables = binding.getVariables();
		List<String> disallow = Arrays.asList( new String[] {"out", "request", "response", "context"} );
		
		/**
		 * We transfer any global variables back into our parent session like so.
		 * We also check to make sure keys like [out, _request, _response, _FILES, _REQUEST, etc...] are excluded.
		 */
		if ( bindings != null && variables != null )
			for ( Entry<String, Object> e : variables.entrySet() )
				if ( !disallow.contains( e.getKey() ) && ! ( e.getKey().startsWith( "_" ) && StringFunc.isUppercase( e.getKey() ) ) )
					bindings.put( e.getKey(), e.getValue() );
		
		/**
		 * Session Wrappers use a WeakReference but by doing this we are making sure we are GC'ed sooner rather than later
		 */
		session.removeWrapper( this );
		
		/**
		 * Clearing references to these classes, again for easier GC cleanup.
		 */
		session = null;
		factory = null;
		binding = null;
		
		/**
		 * Active connections should be closed here
		 */
		finish0();
	}
	
	protected abstract void finish0();
	
	@Override
	public EvalBinding getBinding()
	{
		return binding;
	}
	
	public abstract HttpCookie getCookie( String key );
	
	public abstract Set<HttpCookie> getCookies();
	
	@Override
	public String getDisplayName()
	{
		return getSession().getDisplayName();
	}
	
	@Override
	public PermissibleEntity getEntity()
	{
		return getSession().getEntity();
	}
	
	@Override
	public EvalFactory getEvalFactory()
	{
		return factory;
	}
	
	public Object getGlobal( String key )
	{
		return binding.getVariable( key );
	}
	
	@Override
	public String getId()
	{
		return getSession().getId();
	}
	
	@Override
	public final AccountPermissible getPermissible()
	{
		return session;
	}
	
	protected abstract HttpCookie getServerCookie( String key );
	
	/**
	 * Gets the Session
	 * 
	 * @return
	 *         The session
	 */
	public final Session getSession()
	{
		if ( session == null )
			throw new IllegalStateException( "Detected an attempt to get session before startSession() was called" );
		
		return session;
	}
	
	/**
	 * Gets the Session but without throwing an exception on null
	 * Be sure to check if the session is null
	 * 
	 * @return
	 *         The session
	 */
	public final Session getSessionWithoutException()
	{
		return session;
	}
	
	@Override
	public abstract Site getSite();
	
	@Override
	public String getSiteId()
	{
		return null;// TODO New Empty Method
	}
	
	@Override
	public String getVariable( String key )
	{
		return getSession().getVariable( key );
	}
	
	@Override
	public String getVariable( String key, String def )
	{
		return getSession().getVariable( key, def );
	}
	
	public final boolean hasSession()
	{
		return session != null;
	}
	
	@Override
	public AccountInstance instance()
	{
		return session.instance();
	}
	
	@Override
	public boolean isInitialized()
	{
		return session.isInitialized();
	}
	
	@Override
	public AccountMeta meta()
	{
		return session.meta();
	}
	
	@Override
	public void sendMessage( MessageSender sender, Object... objs )
	{
		// Do Nothing
	}
	
	@Override
	public void sendMessage( Object... objs )
	{
		// Do Nothing
	}
	
	protected abstract void sessionStarted();
	
	public void setGlobal( String key, Object val )
	{
		binding.setVariable( key, val );
	}
	
	@Override
	public void setVariable( String key, String value )
	{
		getSession().setVariable( key, value );
	}
	
	/**
	 * Starts the session
	 * 
	 * @throws SessionException
	 */
	public void startSession() throws SessionException
	{
		session = SessionManager.INSTANCE.startSession( this );
		/*
		 * Create our Binding
		 */
		binding = new EvalBinding( new HashMap<String, Object>( session.getGlobals() ) );
		
		/*
		 * Create our EvalFactory
		 */
		factory = EvalFactory.create( this );
		
		/*
		 * Reference Session Variables
		 */
		binding.setVariable( "_SESSION", session.data.data );
		
		Site site = getSite();
		
		if ( site == null )
			site = SiteManager.INSTANCE.getDefaultSite();
		
		session.setSite( site );
		
		for ( HttpCookie cookie : getCookies() )
			session.putSessionCookie( cookie.getKey(), cookie );
		
		// Reference Context
		binding.setVariable( "context", this );
		
		// Reset __FILE__ Variable
		binding.setVariable( "__FILE__", new File( "" ) );
		
		if ( Loader.getConfig().getBoolean( "sessions.rearmTimeoutWithEachRequest" ) )
			session.rearmTimeout();
		
		sessionStarted();
	}
	
	// TODO: Future add of setDomain, setCookieName, setSecure (http verses https)
}
