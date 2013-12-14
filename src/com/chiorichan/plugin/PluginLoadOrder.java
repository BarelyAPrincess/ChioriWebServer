package com.chiorichan.plugin;

/**
 * Represents the order in which a plugin should be initialized and enabled
 */
public enum PluginLoadOrder
{
	/**
	 * Indicates that the plugin will be loaded at startup
	 */
	STARTUP,
	/**
	 * Indicates that the plugin will be loaded after the server was started
	 */
	POSTSERVER,
	/**
	 * Inidicates that the plugin will be loaded after the framework was initalized
	 */
	POSTFRAMEWORK
}
