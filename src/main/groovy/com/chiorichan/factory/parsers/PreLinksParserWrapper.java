/**
 * This software may be modified and distributed under the terms
 * of the MIT license.  See the LICENSE file for details.
 *
 * Copyright (c) 2017 Joel Greene <joel.greene@penoaks.com>
 * Copyright (c) 2017 Penoaks Publishing LLC <development@penoaks.com>
 *
 * All Rights Reserved.
 */
package com.chiorichan.factory.parsers;

import com.chiorichan.event.EventHandler;
import com.chiorichan.event.EventPriority;
import com.chiorichan.event.Listener;
import com.chiorichan.factory.event.PreEvalEvent;

/**
 * Wraps a {@link BasicParser} so it can be called at the lowest level before pre eval processing
 */
public class PreLinksParserWrapper implements Listener
{
	@EventHandler( priority = EventPriority.LOWEST )
	public void onEvent( PreEvalEvent event ) throws Exception
	{
		event.context().resetAndWrite( new LinksParser().runParser( event.context().readString(), event.context().request(), event.context().site() ) );
	}
}
