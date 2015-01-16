package com.chiorichan;

public enum RunLevel
{
	/**
	 * Indicates the server has not done anything YET!
	 */
	INITIALIZATION,
	/**
	 * Indicates the server has begun startup procedures.
	 */
	STARTUP,
	/**
	 * Indicates the server has started the HTTP, HTTPS and TCP listeners.
	 */
	POSTSERVER,
	/**
	 * Indicates the server has started the DatabaseEngine, SitesManager, AccountManager, and SessionManager.
	 */
	INITIALIZED,
	/**
	 * Indicates the server has completed all required startup procedures and started the main thread tick.
	 */
	RUNNING,
	/**
	 * Indicates the server is reloading.
	 */
	RELOAD,
	/**
	 * Indicates the server is preparing to shutdown.
	 */
	SHUTDOWN;
}
