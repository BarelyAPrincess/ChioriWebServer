package com.chiorichan.account.bases;

import java.util.Set;

import org.apache.commons.codec.digest.DigestUtils;

import com.chiorichan.ChatColor;
import com.chiorichan.Loader;
import com.chiorichan.account.adapter.AccountLookupAdapter;
import com.chiorichan.account.helpers.AccountMetaData;
import com.chiorichan.account.helpers.LoginException;
import com.chiorichan.account.helpers.LoginExceptionReasons;
import com.chiorichan.permissions.PermissibleBase;
import com.chiorichan.permissions.Permission;
import com.chiorichan.permissions.PermissionAttachment;
import com.chiorichan.permissions.PermissionAttachmentInfo;
import com.chiorichan.plugin.Plugin;

public class Account extends Sentient
{
	protected final PermissibleBase perm;
	protected AccountMetaData metaData = new AccountMetaData();
	protected String acctId;
	
	protected AccountLookupAdapter _cachedAdapter;
	
	public Account(String userId, AccountLookupAdapter adapter) throws LoginException
	{
		if ( userId.isEmpty() )
			throw new LoginException( LoginExceptionReasons.emptyUsername );
		
		if ( adapter == null )
			throw new LoginException( LoginExceptionReasons.unknownError );
		
		_cachedAdapter = adapter;
		
		metaData = adapter.loadAccount( userId );
		acctId = metaData.getAccountId();
		
		perm = new PermissibleBase( this );
	}
	
	public Account(AccountMetaData meta, AccountLookupAdapter adapter)
	{
		_cachedAdapter = adapter;
		
		metaData = meta;
		acctId = meta.getAccountId();
		
		perm = new PermissibleBase( this );
	}
	
	public AccountMetaData getMetaData()
	{
		return metaData;
	}
	
	public boolean validatePassword( String _password )
	{
		String password = metaData.getPassword();
		return ( password.equals( _password ) || password.equals( DigestUtils.md5Hex( _password ) ) || DigestUtils.md5Hex( password ).equals( _password ) );
	}
	
	public boolean isBanned()
	{
		return Loader.getAccountsManager().isBanned( acctId );
	}
	
	public boolean isWhitelisted()
	{
		return Loader.getAccountsManager().isWhitelisted( acctId );
	}
	
	public boolean isOp()
	{
		return Loader.getAccountsManager().isOp( acctId );
	}
	
	public String getPassword()
	{
		return metaData.getPassword();
	}
	
	public String getDisplayName()
	{
		return metaData.getString( "displayName" );
	}
	
	public String getUsername()
	{
		return metaData.getUsername();
	}
	
	public String getName()
	{
		return metaData.getAccountId();
	}
	
	public void kick( String kickMessage )
	{
		for ( SentientHandler handler : handlers )
			handler.kick( kickMessage );
	}
	
	public void save()
	{
		_cachedAdapter.saveAccount( metaData );
	}
	
	public boolean canSee( Account user )
	{
		return false;
	}
	
	@Override
	public String getId()
	{
		return getAccountId();
	}
	
	public String getAccountId()
	{
		String uid = metaData.getString( "acctId" );
		
		if ( uid == null )
			uid = metaData.getString( "accountId" );
		
		/** TEMP START - MAYBE **/
		if ( uid == null )
			uid = metaData.getString( "userId" );
		
		if ( uid == null )
			uid = metaData.getString( "userID" );
		
		if ( uid == null )
			uid = metaData.getString( "id" );
		/** TEMP END **/
		
		return uid;
	}
	
	public String toString()
	{
		return "User{" + metaData.toString() + ",Handlers{" + handlers.toString() + "}}";
	}
	
	public String getString( String key )
	{
		return getString( key, "" );
	}
	
	public String getString( String key, String def )
	{
		if ( !metaData.containsKey( key ) )
			return def;
		
		return metaData.getString( key );
	}
	
	public boolean isPermissionSet( String name )
	{
		return perm.isPermissionSet( name );
	}
	
	public boolean isPermissionSet( Permission perm )
	{
		return this.perm.isPermissionSet( perm );
	}
	
	public boolean hasPermission( String req )
	{
		Loader.getLogger().info( ChatColor.GREEN + "Checking `" + getAccountId() + "` for permission `" + req + "` with result `" + perm.hasPermission( req ) + "`" );
		
		// Everyone
		if ( req.equals( "-1" ) || req.isEmpty() )
			return true;
		
		// OP Only
		if ( req.equals( "0" ) || req.equalsIgnoreCase( "op" ) || req.equalsIgnoreCase( "admin" ) || req.equalsIgnoreCase( "root" ) )
			return isOp();
		
		return perm.hasPermission( req );
	}
	
	public boolean hasPermission( Permission perm )
	{
		return this.perm.hasPermission( perm );
	}
	
	public PermissionAttachment addAttachment( Plugin plugin, String name, boolean value )
	{
		return perm.addAttachment( plugin, name, value );
	}
	
	public PermissionAttachment addAttachment( Plugin plugin )
	{
		return perm.addAttachment( plugin );
	}
	
	public PermissionAttachment addAttachment( Plugin plugin, String name, boolean value, int ticks )
	{
		return perm.addAttachment( plugin, name, value, ticks );
	}
	
	public PermissionAttachment addAttachment( Plugin plugin, int ticks )
	{
		return perm.addAttachment( plugin, ticks );
	}
	
	public void removeAttachment( PermissionAttachment attachment )
	{
		perm.removeAttachment( attachment );
	}
	
	public void recalculatePermissions()
	{
		perm.recalculatePermissions();
	}
	
	public Set<PermissionAttachmentInfo> getEffectivePermissions()
	{
		return perm.getEffectivePermissions();
	}
	
	public void disconnect( String reason )
	{
		perm.clearPermissions();
	}
	
	public void reloadAndValidate() throws LoginException
	{
		metaData = _cachedAdapter.loadAccount( acctId );
	}
	
	
}
