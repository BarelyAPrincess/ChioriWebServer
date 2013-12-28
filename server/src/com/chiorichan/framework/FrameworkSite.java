package com.chiorichan.framework;

import java.sql.ResultSet;
import java.sql.SQLException;

import com.chiorichan.database.SqlConnector;

public class FrameworkSite extends Site
{
	public FrameworkSite(ResultSet rs) throws SQLException
	{
		super( rs );
	}
	
	void setDatabase( SqlConnector sql )
	{
		this.sql = sql;
	}
}
