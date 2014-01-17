package ru.tehkode.permissions;

import java.util.List;

public interface PermissionsUserData {

	/**
	 * Returns user groups in specified site
	 *
	 * @param siteName
	 */
	public List<String> getGroups(String siteName);

	/**
	 * Set groups in specified site
	 *
	 * @param groups
	 * @param siteName
	 */
	public void setGroups(List<PermissionGroup> groups, String siteName);
}
