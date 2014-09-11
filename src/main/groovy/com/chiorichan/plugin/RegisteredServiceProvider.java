/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * Copyright 2014 Chiori-chan. All Right Reserved.
 *
 * @author Chiori Greene
 * @email chiorigreene@gmail.com
 */
package com.chiorichan.plugin;

/**
 * A registered service provider.
 * 
 * @param <T>
 *           Service
 */
public class RegisteredServiceProvider<T> implements Comparable<RegisteredServiceProvider<?>>
{
	
	private Class<T> service;
	private Plugin plugin;
	private T provider;
	private ServicePriority priority;
	
	public RegisteredServiceProvider(Class<T> service, T provider, ServicePriority priority, Plugin plugin)
	{
		
		this.service = service;
		this.plugin = plugin;
		this.provider = provider;
		this.priority = priority;
	}
	
	public Class<T> getService()
	{
		return service;
	}
	
	public Plugin getPlugin()
	{
		return plugin;
	}
	
	public T getProvider()
	{
		return provider;
	}
	
	public ServicePriority getPriority()
	{
		return priority;
	}
	
	public int compareTo( RegisteredServiceProvider<?> other )
	{
		if ( priority.ordinal() == other.getPriority().ordinal() )
		{
			return 0;
		}
		else
		{
			return priority.ordinal() < other.getPriority().ordinal() ? 1 : -1;
		}
	}
}
