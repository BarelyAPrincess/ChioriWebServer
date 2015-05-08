/**
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * Copyright 2015 Chiori-chan. All Right Reserved.
 */
package com.chiorichan.net;

import groovy.lang.Binding;

import com.chiorichan.account.AccountHandler;
import com.chiorichan.account.system.SystemAccounts;
import com.chiorichan.factory.BindingProvider;
import com.chiorichan.factory.EvalFactory;
import com.chiorichan.net.query.QueryServerHandler;

/**
 * This class is used to make a connection between a TCP connection and it's Permissible.
 * 
 * @author Chiori Greene
 * @email chiorigreene@gmail.com
 */
public class NetworkPersistence extends AccountHandler implements BindingProvider
{
	protected final Binding binding = new Binding();
	protected EvalFactory factory = null;
	protected QueryServerHandler handler;
	
	public NetworkPersistence( QueryServerHandler handler )
	{
		this.handler = handler;
		attachAccount( SystemAccounts.noLogin );
	}
	
	protected Binding getBinding()
	{
		return binding;
	}
	
	@Override
	public void sendMessage( String... msgs )
	{
		handler.println( msgs );
	}
	
	@Override
	public boolean kick( String kickMessage )
	{
		handler.disconnect( kickMessage );
		return true;
	}
	
	@Override
	public boolean isRemote()
	{
		return true;
	}
	
	@Override
	public String getIpAddr()
	{
		return handler.getIpAddr();
	}
	
	@Override
	public EvalFactory getEvalFactory()
	{
		return getEvalFactory( true );
	}
	
	@Override
	public EvalFactory getEvalFactory( boolean createIfNull )
	{
		if ( factory == null && createIfNull )
			factory = EvalFactory.create( binding );
		
		return factory;
	}
	
	@Override
	public String toString()
	{
		return "NetworkPersistence{ipAddr=" + getIpAddr() + "}";
	}
}
