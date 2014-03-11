package ru.tehkode.permissions.backends;

import ru.tehkode.permissions.PermissionManager;

import com.chiorichan.configuration.Configuration;

public class SiteBackend extends SQLBackend
{
	public SiteBackend(PermissionManager manager, Configuration config)
	{
		super( manager, config );
	}
}
