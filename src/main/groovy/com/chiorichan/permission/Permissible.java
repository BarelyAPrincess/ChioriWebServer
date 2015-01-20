package com.chiorichan.permission;

import java.util.Set;

public interface Permissible
{
	/**
	 * Web users id will be in the form of `siteId`_`acctId`.
	 * @return a unique identifier
	 */
	public String getId();
	
	/**
	 * Handler type
	 * @return PermissibleType
	 * 			the connection method
	 */
	public PermissibleType getType();
	
	/**
	 * Handler types
	 * @return Set<PermissibleType>
	 * 			a set of Connection Methods used
	 */
	public Set<PermissibleType> getTypes();
	
	/**
	 * If the entity is connected remotely then return the Remote Address.
	 * @return String
	 * 			an IPv4/IPv6 Address or null if no remote handlers
	 */
	public String getIpAddr();
	
	/**
	 * If the entity is connected remotely then return the Remote Addresses.
	 * @return Set<String>
	 * 			a set of IPv4/IPv6 Address or null if no remote handlers
	 */
	public Set<String> getIpAddrs();
	
	public boolean hasPermission( String req );
	public boolean hasPermission( Permission req );
	
	/**
	 * Is this permissible on the OP list.
	 * @return true if OP
	 */
	public boolean isOp();
}
