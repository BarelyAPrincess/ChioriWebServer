/**
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * Copyright 2015 Chiori-chan. All Right Reserved.
 */
package com.chiorichan.factory.event;

import com.chiorichan.event.EventHandler;
import com.chiorichan.event.EventPriority;
import com.chiorichan.event.Listener;
import com.chiorichan.factory.parsers.BasicParser;

/**
 * Wraps a {@link BasicParser} so it can be called at the lowest level before pre eval processing
 * 
 * @author Chiori Greene, a.k.a. Chiori-chan {@literal <me@chiorichan.com>}
 */
public class PreParseWrapper implements Listener
{
	private final BasicParser[] parsers;
	
	public PreParseWrapper( BasicParser... parsers )
	{
		this.parsers = parsers;
	}
	
	@EventHandler( priority = EventPriority.LOWEST )
	public void onEvent( PreEvalEvent event ) throws Exception
	{
		for ( BasicParser parser : parsers )
			event.context().resetAndWrite( parser.runParser( event.context().readString(), event.context().site() ) );
	}
}
