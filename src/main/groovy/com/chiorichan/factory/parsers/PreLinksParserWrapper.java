/**
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 *
 * Copyright 2015 Chiori Greene a.k.a. Chiori-chan <me@chiorichan.com>
 * All Right Reserved.
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
