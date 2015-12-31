/**
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 *
 * Copyright 2015 Chiori Greene a.k.a. Chiori-chan <me@chiorichan.com>
 * All Right Reserved.
 */
package com.chiorichan;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.apache.commons.lang3.ClassUtils;

/**
 * Produces contexts that reference the source of actions, e.g., EventRegistration and TaskCreation
 * This class also takes into account known subsystem interfaces, e.g., Plugins
 */
public class SourceContext
{
	public enum SourceFlags
	{

	}

	public static SourceContext produce( Object source, SourceFlags... flags )
	{
		return new SourceContext( source, flags );
	}
	private Object source;
	private String name;

	private List<SourceFlags> flags;

	private SourceContext( Object source, SourceFlags... flags )
	{
		this.source = source;
		this.flags = Arrays.asList( flags );

		name = ClassUtils.getSimpleName( source.getClass() );

		/*
		 * if ( source instanceof EventRegistrar )
		 * if ( !creator.isEnabled() )
		 * throw new IllegalCreatorAccessException( "EventCreator attempted to register " + listener + " while not enabled" );
		 *
		 * if ( source instanceof TaskRegistrar )
		 * {
		 *
		 * }
		 *
		 * if ( source instanceof Plugin )
		 * {
		 *
		 * }
		 */
	}

	public List<String> getAuthors()
	{
		// TODO Implement this
		return Arrays.asList( new String[] {"Chiori-chan"} );
	}

	public List<SourceFlags> getFlags()
	{
		return Collections.unmodifiableList( flags );
	}

	public String getFullName()
	{
		return getName();
	}

	public String getName()
	{
		return name;
	}

	public Object getSource()
	{
		return source;
	}

	public boolean isEnabled()
	{
		// Implement this!

		return true;
	}
}
