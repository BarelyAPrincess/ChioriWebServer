/**
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 *
 * Copyright 2015 Chiori Greene a.k.a. Chiori-chan <me@chiorichan.com>
 * All Right Reserved.
 */
package com.chiorichan.terminal;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.Collection;

import com.chiorichan.LogColor;
import com.chiorichan.Loader;
import com.chiorichan.account.AccountManager;
import com.chiorichan.account.AccountPermissible;
import com.chiorichan.account.AccountType;
import com.chiorichan.account.auth.AccountAuthenticator;
import com.chiorichan.account.lang.AccountException;
import com.chiorichan.account.lang.AccountResult;
import com.chiorichan.factory.BindingProvider;
import com.chiorichan.factory.ScriptBinding;
import com.chiorichan.factory.ScriptingFactory;
import com.chiorichan.messaging.MessageSender;
import com.chiorichan.permission.PermissibleEntity;
import com.chiorichan.site.Site;
import com.chiorichan.site.SiteManager;
import com.chiorichan.util.FileFunc;
import com.chiorichan.util.ObjectFunc;
import com.chiorichan.util.Versioning;

/**
 * Used to interact with commands and logs
 */
public class TerminalEntity extends AccountPermissible implements Terminal, BindingProvider
{
	private TerminalHandler handler;
	private String prompt = "";
	
	private ScriptBinding binding = new ScriptBinding();
	private ScriptingFactory factory;
	
	public TerminalEntity( TerminalHandler handler )
	{
		this.handler = handler;
		
		binding = new ScriptBinding();
		factory = ScriptingFactory.create( this );
		binding.setVariable( "context", this );
		binding.setVariable( "__FILE__", new File( "" ) );
		
		try
		{
			loginWithException( AccountAuthenticator.NULL, AccountType.ACCOUNT_NONE.getId() );
		}
		catch ( AccountException e )
		{
			if ( e.getResult().hasCause() )
				e.getResult().getCause().printStackTrace();
			AccountManager.getLogger().severe( e.getMessage() );
			
			handler.kick( "Internal Server Error: " + e.getMessage() );
		}
	}
	
	public void displayWelcomeMessage()
	{
		try
		{
			InputStream is = null;
			try
			{
				is = Loader.class.getClassLoader().getResourceAsStream( "com/chiorichan/banner.txt" );
				
				String[] banner = new String( FileFunc.inputStream2Bytes( is ) ).split( "\\n" );
				
				for ( String l : banner )
					handler.println( LogColor.GOLD + l );
				
				handler.println( String.format( "%s%sWelcome to %s Version %s!", LogColor.NEGATIVE, LogColor.GOLD, Versioning.getProduct(), Versioning.getVersion() ) );
				handler.println( String.format( "%s%s%s", LogColor.NEGATIVE, LogColor.GOLD, Versioning.getCopyright() ) );
			}
			finally
			{
				if ( is != null )
					is.close();
			}
		}
		catch ( IOException e )
		{
			
		}
	}
	
	@Override
	protected void failedLogin( AccountResult result )
	{
		// TODO New Empty Method
	}
	
	public void finish()
	{
		// TODO New Empty Method
	}
	
	@Override
	public ScriptBinding getBinding()
	{
		return binding;
	}
	
	@Override
	public PermissibleEntity getEntity()
	{
		return meta().getEntity();
	}
	
	@Override
	public ScriptingFactory getEvalFactory()
	{
		return factory;
	}
	
	public TerminalHandler getHandler()
	{
		return handler;
	}
	
	@Override
	public String getIpAddr()
	{
		return handler.getIpAddr();
	}
	
	@Override
	public Collection<String> getIpAddresses()
	{
		return Arrays.asList( getIpAddr() );
	}
	
	@Override
	public AccountPermissible getPermissible()
	{
		return this;
	}
	
	@Override
	public Site getSite()
	{
		return SiteManager.INSTANCE.getDefaultSite();
	}
	
	@Override
	public String getVariable( String key )
	{
		return getVariable( key, null );
	}
	
	@Override
	public String getVariable( String key, String def )
	{
		if ( !binding.hasVariable( key ) )
			return def;
		
		// This is suppose to be persistent data, i.e., login. But we will use the metadata until something else can be made
		Object obj = binding.getVariable( key );
		
		if ( obj == null || ! ( obj instanceof String ) )
			return def;
		
		return ( ( String ) obj );
	}
	
	@Override
	public AccountResult kick( String reason )
	{
		return handler.kick( LogColor.AQUA + "You are being kicked for reason: " + LogColor.RESET + reason );
	}
	
	@Override
	public void prompt()
	{
		handler.print( "\r" + prompt );
	}
	
	@Override
	public void resetPrompt()
	{
		try
		{
			prompt = LogColor.GREEN + getId() + "@" + InetAddress.getLocalHost().getHostName() + LogColor.RESET + ":" + LogColor.BLUE + "~" + LogColor.RESET + "$ ";
		}
		catch ( UnknownHostException e )
		{
			prompt = LogColor.GREEN + getId() + "@localhost ~$ ";
		}
		
		prompt();
	}
	
	@Override
	public void sendMessage( MessageSender sender, Object... objs )
	{
		for ( Object obj : objs )
			try
			{
				handler.println( sender.getDisplayName() + ": " + ObjectFunc.castToStringWithException( obj ) );
			}
			catch ( ClassCastException e )
			{
				handler.println( sender.getDisplayName() + " sent object " + obj.getClass().getName() + " but we had no idea how to properly output it to your terminal." );
			}
	}
	
	@Override
	public void sendMessage( Object... objs )
	{
		for ( Object obj : objs )
			try
			{
				handler.println( ObjectFunc.castToStringWithException( obj ) );
			}
			catch ( ClassCastException e )
			{
				handler.println( "Received object " + obj.getClass().getName() + " but we had no idea how to properly output it to your terminal." );
			}
	}
	
	@Override
	public void setPrompt( String prompt )
	{
		if ( prompt != null )
			this.prompt = prompt;
		
		prompt();
	}
	
	@Override
	public void setVariable( String key, String val )
	{
		// This is suppose to be persistent data, i.e., login. But we will use the metadata until something else can be made
		binding.setVariable( key, val );
	}
	
	@Override
	protected void successfulLogin()
	{
		// TODO Unregister from old login, i.e., NONE Account
		// Temp Fix until a better way is found, i.e., logout!
		AccountType.ACCOUNT_NONE.instance().unregisterAttachment( this );
		
		registerAttachment( this );
	}
	
	@Override
	public String toString()
	{
		return String.format( "TerminalEntity{AcctId=%s,ip=%s}", meta().getId(), getIpAddr() );
	}
}
