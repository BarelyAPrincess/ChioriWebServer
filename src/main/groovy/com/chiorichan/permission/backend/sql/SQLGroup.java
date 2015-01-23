package com.chiorichan.permission.backend.sql;

import com.chiorichan.permission.backend.PermissibleGroupProxy;
import com.chiorichan.permission.backend.SQLBackend;

/**
 * @author Chiori Greene
 */
public class SQLGroup extends PermissibleGroupProxy
{
	public SQLGroup( String name, SQLBackend sql )
	{
		super( name, sql );
	}
}
