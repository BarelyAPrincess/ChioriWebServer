/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * Copyright 2014 Chiori-chan. All Right Reserved.
 *
 * @author Chiori Greene
 * @email chiorigreene@gmail.com
 */
package com.chiorichan.bus.events.server;

import com.chiorichan.plugin.RegisteredServiceProvider;

/**
 * An event relating to a registered service. This is called in a {@link org.bukkit.plugin.ServicesManager}
 */
public abstract class ServiceEvent extends ServerEvent
{
	private final RegisteredServiceProvider<?> provider;
	
	public ServiceEvent(final RegisteredServiceProvider<?> provider)
	{
		this.provider = provider;
	}
	
	public RegisteredServiceProvider<?> getProvider()
	{
		return provider;
	}
}
