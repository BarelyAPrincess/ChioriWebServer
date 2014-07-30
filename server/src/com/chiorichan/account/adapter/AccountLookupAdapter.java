package com.chiorichan.account.adapter;

import java.util.List;

import com.chiorichan.account.bases.Account;
import com.chiorichan.account.helpers.AccountMetaData;
import com.chiorichan.account.helpers.LoginException;

/**
 * Provided so that any site can have custom places to store login information.
 * mySql, sqLite, file, etc.
 * 
 * @author Chiori Greene
 */
public interface AccountLookupAdapter
{
	public AccountLookupAdapter MEMORY_ADAPTER = new MemoryAdapter();

	/**
	 * Returns all accounts maintained by this adapter.
	 * @return
	 */
	public List<AccountMetaData> getAccounts();
	
	/**
	 * Attempt to serialize provided account.
	 * Use of the account instance may continue.
	 */
	public void saveAccount( AccountMetaData account );
	
	/**
	 * Attempt to reload details regarding this account.
	 * @return 
	 */
	public AccountMetaData reloadAccount( AccountMetaData account );
	
	/**
	 * Attempt to load a account.
	 * 
	 * @throws LoginException
	 */
	public AccountMetaData loadAccount( String account ) throws LoginException;
	
	/**
	 * Called before the AccountManager makes the login offical.
	 */
	public void preLoginCheck( Account account ) throws LoginException ;
	
	/**
	 * Called as the last line before account returned to scripts.
	 */
	public void postLoginCheck( Account account ) throws LoginException;
	
	/**
	 * Update any security mechs of failed login
	 */
	public void failedLoginUpdate( Account account );
	
	/**
	 * Called from AccountManager to determine if Account matches Accountname. Usually used to search the accounts array in a way the adapter sees fit. ex: email, phone, accountname, accountId
	 */
	public boolean matchAccount( Account account, String accountname );
}
