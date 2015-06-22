/**
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 *
 * Copyright 2015 Chiori Greene a.k.a. Chiori-chan <me@chiorichan.com>
 * All Right Reserved.
 */
package com.chiorichan.permission;


public enum PermissionDefault
{
	ADMIN( "sys.admin" ), BANNED( "sys.banned" ), DEFAULT( "default" ), EVERYBODY( "" ), OP( "sys.op" ), WHITELISTED( "sys.whitelisted" );
	
	private String nameSpace = "";
	
	PermissionDefault( String nameSpace )
	{
		this.nameSpace = nameSpace;
	}
	
	public static boolean isDefault( Permission perm )
	{
		for ( PermissionDefault pd : PermissionDefault.values() )
			if ( pd.getNameSpace().equalsIgnoreCase( perm.getNamespace() ) )
				return true;
		
		return false;
	}
	
	public String getLocalName()
	{
		return ( nameSpace.contains( "." ) ) ? nameSpace.substring( nameSpace.indexOf( "." ) + 1 ) : nameSpace;
	}
	
	public String getNameSpace()
	{
		return nameSpace;
	}
	
	public Permission getNode()
	{
		Permission result = PermissionManager.INSTANCE.getNode( nameSpace, false );
		
		if ( result == null )
		{
			if ( this == EVERYBODY )
			{
				result = PermissionManager.INSTANCE.getNode( getNameSpace(), PermissionType.BOOL );
				result.getModel().setValue( true );
				result.getModel().setValueDefault( true );
			}
			else
				result = PermissionManager.INSTANCE.getNode( getNameSpace(), true );
			
			switch ( this )
			{
				case DEFAULT:
					result.getModel().setDescription( "Used as the default permission node if one does not exist. (DO NOT EDIT!)" );
					break;
				case EVERYBODY:
					result.getModel().setDescription( "This node is used for the 'everyone' permission. (DO NOT EDIT!)" );
					break;
				case OP:
					result.getModel().setDescription( "Indicates OP entities. (DO NOT EDIT!)" );
					break;
				case ADMIN:
					result.getModel().setDescription( "Indicates ADMIN entities. (DO NOT EDIT!)" );
					break;
				case BANNED:
					result.getModel().setDescription( "Indicates BANNED entities. (DO NOT EDIT!)" );
					break;
				case WHITELISTED:
					result.getModel().setDescription( "Indicates WHITELISTED entities. (DO NOT EDIT!)" );
					break;
			}
		}
		
		return result;
	}
	
	@Override
	public String toString()
	{
		return name() + "(nameSpace=" + nameSpace + ")";
	}
}
