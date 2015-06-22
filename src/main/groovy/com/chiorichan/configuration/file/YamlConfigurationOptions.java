/**
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 *
 * Copyright 2015 Chiori Greene a.k.a. Chiori-chan <me@chiorichan.com>
 * All Right Reserved.
 */
package com.chiorichan.configuration.file;

import org.apache.commons.lang3.Validate;

/**
 * Various settings for controlling the input and output of a {@link YamlConfiguration}
 */
public class YamlConfigurationOptions extends FileConfigurationOptions
{
	private int indent = 2;
	
	protected YamlConfigurationOptions( YamlConfiguration configuration )
	{
		super( configuration );
	}
	
	@Override
	public YamlConfiguration configuration()
	{
		return ( YamlConfiguration ) super.configuration();
	}
	
	@Override
	public YamlConfigurationOptions copyDefaults( boolean value )
	{
		super.copyDefaults( value );
		return this;
	}
	
	@Override
	public YamlConfigurationOptions pathSeparator( char value )
	{
		super.pathSeparator( value );
		return this;
	}
	
	@Override
	public YamlConfigurationOptions header( String value )
	{
		super.header( value );
		return this;
	}
	
	@Override
	public YamlConfigurationOptions copyHeader( boolean value )
	{
		super.copyHeader( value );
		return this;
	}
	
	/**
	 * Gets how much spaces should be used to indent each line.
	 * <p>
	 * The minimum value this may be is 2, and the maximum is 9.
	 * 
	 * @return How much to indent by
	 */
	public int indent()
	{
		return indent;
	}
	
	/**
	 * Sets how much spaces should be used to indent each line.
	 * <p>
	 * The minimum value this may be is 2, and the maximum is 9.
	 * 
	 * @param value
	 *            New indent
	 * @return This object, for chaining
	 */
	public YamlConfigurationOptions indent( int value )
	{
		Validate.isTrue( value >= 2, "Indent must be at least 2 characters" );
		Validate.isTrue( value <= 9, "Indent cannot be greater than 9 characters" );
		
		this.indent = value;
		return this;
	}
}
