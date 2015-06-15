/**
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * Copyright 2015 Chiori-chan. All Right Reserved.
 */
package com.chiorichan.factory;

import groovy.lang.Binding;

import java.util.Collections;
import java.util.Map;
import java.util.Set;

import com.google.common.collect.Sets;

/**
 * Our own binding extended so we can better track if and when a binding variable is changed
 * 
 * @author Chiori Greene, a.k.a. Chiori-chan {@literal <me@chiorichan.com>}
 */
public class EvalBinding extends Binding
{
	private final Set<String> history = Sets.newHashSet();
	
	public EvalBinding()
	{
		
	}
	
	public EvalBinding( Map<String, Object> map )
	{
		super( map );
	}
	
	/**
	 * Sets the value of the given variable
	 *
	 * @param name
	 *            the name of the variable to set
	 * @param value
	 *            the new value for the given variable
	 */
	@Override
	public void setVariable( String name, Object value )
	{
		super.setVariable( name, value );
		history.add( name );
	}
	
	public Set<String> getUpdateHistory()
	{
		return Collections.unmodifiableSet( history );
	}
	
	public void clearHistory()
	{
		history.clear();
	}
}
