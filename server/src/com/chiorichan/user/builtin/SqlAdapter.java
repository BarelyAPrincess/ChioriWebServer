package com.chiorichan.user.builtin;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

import org.json.JSONException;

import com.chiorichan.database.SqlConnector;
import com.chiorichan.framework.Site;
import com.chiorichan.user.LoginException;
import com.chiorichan.user.User;
import com.chiorichan.user.UserMetaData;
import com.chiorichan.util.Common;
import com.google.common.collect.Lists;

public class SqlAdapter implements UserLookupAdapter
{
	SqlConnector sql;
	String table;
	List<String> userFields;
	
	// site.getYaml().getList( "logins.additionalFields" )
	
	public SqlAdapter(SqlConnector _sql, String _table, String... _userFields) throws SQLException
	{
		this( _sql, _table, Lists.newArrayList( _userFields ) );
	}
	
	public SqlAdapter(SqlConnector _sql, String _table, List<String> _userFields)
	{
		sql = _sql;
		table = _table;
		userFields = _userFields;
	}

	@Override
	public boolean isAdapterValid( Site site )
	{
		return sql != null && sql.isConnected();
	}
	
	public ResultSet getResultSet( String uid ) throws SQLException
	{
		if ( uid == null || uid.isEmpty() )
			return null;
		
		ResultSet rs = sql.query( "SELECT * FROM `users` WHERE `userID` = '" + uid + "' LIMIT 1;" );
		
		if ( rs == null || sql.getRowCount( rs ) < 1 )
			return null;
		
		return rs;
	}
	
	@Override
	public void saveUser( UserMetaData user )
	{
		
	}
	
	@Override
	public UserMetaData reloadUser( UserMetaData user )
	{
		return null;
	}
	
	@Override
	public UserMetaData loadUser( String username ) throws LoginException
	{
		try
		{
			UserMetaData meta = new UserMetaData();
			
			if ( username == null || username.isEmpty() )
				throw new LoginException( LoginException.ExceptionReasons.emptyUsername );
			
			String additionalUserFields = "";
			for ( String f : userFields )
			{
				additionalUserFields += " OR `" + f + "` = '" + username + "'";
			}
			
			ResultSet rs = sql.query( "SELECT * FROM `users` WHERE `username` = '" + username + "' OR `userID` = '" + username + "'" + additionalUserFields + ";" );
			
			if ( rs == null || sql.getRowCount( rs ) < 1 )
				throw new LoginException( LoginException.ExceptionReasons.incorrectLogin );
			
			meta.setAll( SqlConnector.convertRow( rs ) );
			
			meta.set( "displayName", ( rs.getString( "fname" ).isEmpty() ) ? rs.getString( "name" ) : rs.getString( "fname" ) + " " + rs.getString( "name" ) );
			
			return meta;
		}
		catch ( SQLException | JSONException e )
		{
			throw new LoginException( e );
		}
	}
	
	@Override
	public void preLoginCheck( User user ) throws LoginException
	{
		UserMetaData meta = user.getMetaData();
		
		if ( meta.getInteger( "numloginfail" ) > 5 )
			if ( meta.getInteger( "lastloginfail" ) > ( Common.getEpoch() - 1800 ) )
				throw new LoginException( LoginException.ExceptionReasons.underAttackPleaseWait );
		
		if ( !meta.getString( "actnum" ).equals( "0" ) )
			throw new LoginException( LoginException.ExceptionReasons.accountNotActivated );
	}
	
	@Override
	public void postLoginCheck( User user ) throws LoginException
	{
		sql.queryUpdate( "UPDATE `users` SET `lastactive` = '" + Common.getEpoch() + "', `lastlogin` = '" + Common.getEpoch() + "', `lastloginfail` = 0, `numloginfail` = 0 WHERE `userID` = '" + user.getUserId() + "'" );
	}
	
	@Override
	public void failedLoginUpdate( User user )
	{
		// TODO Update use as top reflect this failure.
		// sql.queryUpdate( "UPDATE `users` SET `lastactive` = '" + Common.getEpoch() + "', `lastloginfail` = 0, `numloginfail` = 0 WHERE `userID` = '" + user.getUserId() + "'" );
	}
}
