package com.chiorichan.permission.backend.sql;

import com.chiorichan.permission.backend.PermissibleProxy;
import com.chiorichan.permission.backend.SQLBackend;

/**
 * @author Chiori Greene
 */
public class SQLEntity extends PermissibleProxy
{
	public SQLEntity( String name, SQLBackend sql )
	{
		super( name, sql );
	}
}
