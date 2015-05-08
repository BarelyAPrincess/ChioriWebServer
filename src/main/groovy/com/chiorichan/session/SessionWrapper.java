/**
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * Copyright 2015 Chiori-chan. All Right Reserved.
 */
package com.chiorichan.session;

import groovy.lang.Binding;

import java.io.File;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import com.chiorichan.Loader;
import com.chiorichan.account.Account;
import com.chiorichan.configuration.file.YamlConfiguration;
import com.chiorichan.factory.BindingProvider;
import com.chiorichan.factory.EvalBinding;
import com.chiorichan.factory.EvalFactory;
import com.chiorichan.framework.ConfigurationManagerWrapper;
import com.chiorichan.http.HttpCookie;
import com.chiorichan.site.Site;

/**
 * Acts as a bridge between a Session and the User
 * TODO If Session is nullified, we need to start a new one
 * 
 * @author Chiori Greene
 * @email chiorigreene@gmail.com
 */
@SuppressWarnings( "deprecation" )
public abstract class SessionWrapper implements BindingProvider
{
	// XXX user, pass, remember - Revisit
	private static final List<String> disallowedKeys = Arrays.asList( new String[] {"user", "pass", "remember", "out", "context", "_REQUEST", "__FILE__", "_SESSION", "_REWRITE", "_GET", "_POST", "_SERVER", "_FILES"} );
	
	/**
	 * The binding specific to this request
	 */
	private EvalBinding binding;
	
	/**
	 * The EvalFactory used to process scripts of this request
	 */
	private EvalFactory factory;
	
	/**
	 * The session associated with this request
	 */
	private Session session;
	
	/**
	 * Starts the session
	 * 
	 * @throws SessionException
	 */
	public void startSession() throws SessionException
	{
		session = Loader.getSessionManager().startSession( this );
		
		sessionStarted();
		
		/*
		 * Register with Wrapped Session
		 */
		session.registerWrapper( this );
		
		/*
		 * Create our Binding
		 */
		binding = new EvalBinding( new HashMap<String, Object>( session.getGlobals() ) ); // Referenced or cloned?
		
		/**
		 * Create EvalFacory
		 */
		factory = EvalFactory.create( this );
		
		/*
		 * Reference Session Variables
		 */
		binding.setVariable( "_SESSION", session.variables );
		
		/*
		 * Create our EvalFactory
		 */
		factory = EvalFactory.create( binding );
		
		/*
		 * Update our site
		 * XXX
		 */
		session.site = getSite();
		
		for ( HttpCookie cookie : getCookies() )
		{
			session.sessionCookies.put( cookie.getKey(), cookie );
		}
		
		String sessionKey = "sessionId";
		YamlConfiguration yaml = session.site.getYaml();
		if ( yaml != null )
			sessionKey = yaml.getString( "sessions.cookie-name", sessionKey );
		
		if ( !sessionKey.equals( session.sessionKey ) && session.sessionCookies.containsKey( session.sessionKey ) )
		{
			session.sessionCookies.put( sessionKey, session.sessionCookies.get( session.sessionKey ) );
			session.sessionCookies.remove( session.sessionKey );
		}
		
		session.sessionKey = sessionKey;
		session.sessionCookie = session.sessionCookies.get( sessionKey );
		session.processSessionCookie();
		
		// Reference Context
		binding.setVariable( "context", this );
		
		// Reset __FILE__ Variable
		binding.setVariable( "__FILE__", new File( "" ) );
	}
	
	/**
	 * Gets the Session
	 * 
	 * @return
	 *         The session
	 */
	public final Session getSession()
	{
		if ( session == null )
			throw new IllegalStateException( "getSession() was called before startSession()" );
		
		return session;
	}
	
	public void setGlobal( String key, Object val )
	{
		binding.setVariable( key, val );
	}
	
	public Object getGlobal( String key )
	{
		return binding.getVariable( key );
	}
	
	public void setVariable( String key, String value )
	{
		session.setVariable( key, value );
	}
	
	public String getVariable( String key )
	{
		return session.getVariable( key );
	}
	
	protected Binding getBinding()
	{
		return binding;
	}
	
	public ConfigurationManagerWrapper getConfigurationManager()
	{
		return new ConfigurationManagerWrapper( this );
	}
	
	@Override
	public EvalFactory getEvalFactory()
	{
		return factory;
	}
	
	public String getSessionCookieName()
	{
		return getSite().getYaml().getString( "sessions.cookie-name", SessionManager.getDefaultCookieName() );
	}
	
	public abstract String getIpAddr();
	
	public abstract HttpCookie getCookie( String key );
	
	public abstract Set<HttpCookie> getCookies();
	
	protected abstract void finish0();
	
	@SuppressWarnings( "unchecked" )
	public void finish()
	{
		Map<String, Object> bindings = session.globals;
		Map<String, Object> variables = binding.getVariables();
		
		if ( bindings != null && variables != null )
		{
			// Copy all keys besides those in disallowedKeys list into the parent binding map
			for ( Entry<String, Object> e : variables.entrySet() )
				if ( !disallowedKeys.contains( e.getKey() ) )
					bindings.put( e.getKey(), e.getValue() );
		}
		
		// Make sure we are GC'ed sooner rather than later
		session.wrappers.remove( this );
	}
	
	protected abstract void sessionStarted();
	
	protected abstract Site getSite();
	
	public abstract void send( Object obj );
	
	public abstract void send( Account sender, Object obj );
	
	// TODO: Future add of setDomain, setCookieName, setSecure (http verses https)
}
