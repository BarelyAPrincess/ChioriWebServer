package com.chiorichan.framework;

import com.chiorichan.http.PersistentSession;

/**
 * Provides methods that allow Groovy Scripts to more easily access server methods that would require imports.
 * ie. PluginManager, Server, Loader, Versioning
 * 
 * @author chiori
 */
@Deprecated
public class ServerUtilsWrapper
{
	protected final PersistentSession sess;
	
	public ServerUtilsWrapper(PersistentSession _sess)
	{
		sess = _sess;
	}
}