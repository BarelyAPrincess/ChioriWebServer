package com.chiorichan.permission;

public interface PermissionMatcher
{
	public boolean isMatches( String expression, String permission );
}
