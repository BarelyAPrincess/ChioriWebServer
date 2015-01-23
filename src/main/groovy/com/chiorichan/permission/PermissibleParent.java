package com.chiorichan.permission;

import com.chiorichan.permission.structure.Permission;

interface PermissibleParent
{
	/**
	 * 
	 * @param req
	 * 			The permissible to check against
	 * @return Boolean
	 * 			Is this entity permitted
	 */
	public boolean isPermissionSet( String req );
	public boolean isPermissionSet( Permission req );
	
	/**
	 * 
	 * @param req
	 * 			The permissible to check against
	 * @return Boolean
	 * 			Is this entity permitted
	 */
	public boolean hasPermission( String req );
	public boolean hasPermission( Permission req );
	
	/**
	 * Is this permissible on the OP list.
	 * @return true if OP
	 */
	public boolean isOp();
}
