package com.chiorichan;

public enum RunLevel
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
	 * Indicates that the plugin will be loaded after the server was started.
	 * If your plugin has any specific server side tasks, put them here.
	 * NOTE: POSTCLIENT will never happen on server side.
	 */
	POSTSERVER,
	/**
	 * Indicates that the plugin will be loader after the client was started.
	 * If your plugin has any specific client side tasks, put them here.
	 * Note: POSTSERVER will never happen on client side.
	 */
	POSTCLIENT,
	/**
	 * Indicates that the plugin will be loaded after the framework was initialized
	 */
	INITIALIZED,
	/**
	 * Indicates that the server has completed all required startup procedures.
	 * NOT A RECOMMENDED PLUGIN LOAD ORDER
	 */
	RUNNING,
	/**
	 * Indicates that the server is able to reload or currently is reloading.
	 * NOT A RECOMMENDED PLUGIN LOAD ORDER - BUT GOOD FOR IDENIFYING CURRENT SERVER STATE
	 */
	RELOAD;
}
