package com.chiorichan.plugin;

/**
 * Represents the order in which a plugin should be initialized and enabled
 */
public enum PluginLoadOrder
{
	/**
	 * Indicates the the server has just begun loading procedures
	 * NOT A RECOMMENDED PLUGIN LOAD ORDER
	 */
	INITIALIZATION,
	/**
	 * Indicates that the plugin will be loaded at startup
	 * It is recommended to register your plugin tcp packets at this point.
	 */
	STARTUP,
	/**
	 * Indicates that the plugin will be loaded after the server was started
	 */
	POSTSERVER,
	/**
	 * Indicates that the plugin will be loaded after the framework was initialized
	 */
	POSTFRAMEWORK,
	/**
	 * Indicates that the server has completed all required startup procedures.
	 * NOT A RECOMMENDED PLUGIN LOAD ORDER
	 */
	RUNNING
}
