/**
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 *
 * Copyright 2016 Chiori Greene a.k.a. Chiori-chan <me@chiorichan.com>
 * All Right Reserved.
 */
package com.chiorichan.terminal;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

import com.chiorichan.Loader;
import com.chiorichan.account.lang.AccountResult;
import com.chiorichan.factory.BindingProvider;
import com.chiorichan.factory.ScriptBinding;
import com.chiorichan.factory.ScriptingFactory;
import com.chiorichan.lang.EnumColor;
import com.chiorichan.util.FileFunc;
import com.chiorichan.util.Versioning;

public class QueryTerminalEntity extends TerminalEntity implements BindingProvider
{
	private ScriptBinding binding = new ScriptBinding();

	private ScriptingFactory factory;

	public QueryTerminalEntity( TerminalHandler handler )
	{
		super( handler );

		binding = new ScriptBinding();
		factory = ScriptingFactory.create( this );
		binding.setVariable( "context", this );
		binding.setVariable( "__FILE__", new File( "" ) );
	}

	@Override
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
					handler.println( EnumColor.GOLD + l );

				handler.println( String.format( "%s%sWelcome to %s Version %s!", EnumColor.NEGATIVE, EnumColor.GOLD, Versioning.getProduct(), Versioning.getVersion() ) );
				handler.println( String.format( "%s%s%s", EnumColor.NEGATIVE, EnumColor.GOLD, Versioning.getCopyright() ) );
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
		// Do Nothing!
	}

	@Override
	public void finish()
	{
		// Do Nothing!
	}

	@Override
	public ScriptBinding getBinding()
	{
		return binding;
	}

	@Override
	public ScriptingFactory getEvalFactory()
	{
		return factory;
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

		return ( String ) obj;
	}

	@Override
	public void setVariable( String key, String val )
	{
		// This is suppose to be persistent data, i.e., login. But we will use the metadata until something else can be made
		binding.setVariable( key, val );
	}
}
