/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * Copyright 2014 Chiori-chan. All Right Reserved.
 *
 * @author Chiori Greene
 * @email chiorigreene@gmail.com
 */
package com.chiorichan.conversations;

import com.chiorichan.ChatColor;
import com.chiorichan.plugin.Plugin;

/**
 * PluginNameConversationPrefix is a {@link ConversationPrefix} implementation that displays the plugin name in front of
 * conversation output.
 */
public class PluginNameConversationPrefix implements ConversationPrefix
{
	
	protected String separator;
	protected ChatColor prefixColor;
	protected Plugin plugin;
	
	private String cachedPrefix;
	
	public PluginNameConversationPrefix(Plugin plugin)
	{
		this( plugin, " > ", ChatColor.LIGHT_PURPLE );
	}
	
	public PluginNameConversationPrefix(Plugin plugin, String separator, ChatColor prefixColor)
	{
		this.separator = separator;
		this.prefixColor = prefixColor;
		this.plugin = plugin;
		
		cachedPrefix = prefixColor + plugin.getDescription().getName() + separator + ChatColor.WHITE;
	}
	
	/**
	 * Prepends each conversation message with the plugin name.
	 * 
	 * @param context
	 *           Context information about the conversation.
	 * @return An empty string.
	 */
	public String getPrefix( ConversationContext context )
	{
		return cachedPrefix;
	}
}
