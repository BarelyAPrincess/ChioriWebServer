/**
 * This software may be modified and distributed under the terms
 * of the MIT license.  See the LICENSE file for details.
 * <p>
 * Copyright (c) 2017 Chiori Greene a.k.a. Chiori-chan <me@chiorichan.com>
 * Copyright (c) 2017 Penoaks Publishing LLC <development@penoaks.com>
 * <p>
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
public class PreIncludesParserWrapper implements Listener
{
	@EventHandler( priority = EventPriority.LOWEST )
	public void onEvent( PreEvalEvent event ) throws Exception
	{
		event.context().resetAndWrite( new IncludesParser().runParser( event.context().readString(), event.context().site(), event.context(), event.context().factory() ) );
		event.context().resetAndWrite( new RequiresParser().runParser( event.context().readString(), event.context().site(), event.context(), event.context().factory() ) );
		event.context().resetAndWrite( new LocaleParser().runParser( event.context().readString(), event.context().site() ) );
		event.context().resetAndWrite( new YieldParser().runParser( event.context().readString(), event.context().factory() ) );
		event.context().resetAndWrite( new SectionParser().runParser( event.context().readString(), event.context().factory() ) );
	}
}
