/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * Copyright 2014 Chiori-chan. All Right Reserved.
 *
 * @author Chiori Greene
 * @email chiorigreene@gmail.com
 */
package com.chiorichan.plugin.messaging;

/**
 * Thrown if a plugin attempts to register for a reserved channel (such as "REGISTER")
 */
@SuppressWarnings( "serial" )
public class ReservedChannelException extends RuntimeException
{
	public ReservedChannelException()
	{
		this( "Attempted to register for a reserved channel name." );
	}
	
	public ReservedChannelException(String name)
	{
		super( "Attempted to register for a reserved channel name ('" + name + "')" );
	}
}
