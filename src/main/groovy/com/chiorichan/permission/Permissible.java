package com.chiorichan.permission;

public interface Permissible extends PermissibleParent
{
	/**
	 * @return String
	 * 			a unique name
	 */
	public String getName();
	
	/**
	 * Web users id will be in the form of `siteId`_`acctId`.
	 * @return String
	 * 			a unique identifier
	 */
	public String getId();
	
	/**
	 * Handler type
	 * @return PermissibleType
	 * 			the connection method
	 */
	public PermissibleType getType();
	
	/**
	 * If the entity is connected remotely then return the Remote Address.
	 * @return String
	 * 			an IPv4/IPv6 Address or null if no remote handlers
	 */
	public String getIpAddr();
}
