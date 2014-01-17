package ru.tehkode.permissions;

import java.util.List;
import java.util.Map;
import java.util.Set;

public interface PermissionsData {

	/**
	 * Returns all permissions for specified site
	 *
	 * @param siteName
	 * @return
	 */
	public List<String> getPermissions(String siteName);

	/**
	 * Set permissions for specified site
	 *
	 * @param permissions
	 * @param siteName
	 */
	public void setPermissions(List<String> permissions, String siteName);

	/**
	 * Returns ALL permissions for each site
	 *
	 * @return
	 */
	public Map<String, List<String>> getPermissionsMap();

	/**
	 * Returns sites where entity has permissions/options
	 *
	 * @return
	 */
	public Set<String> getSites();

	/**
	 * Returns prefix in specified site
	 *
	 * @param siteName
	 * @return
	 */
	@Deprecated
	public String getPrefix(String siteName);

	/**
	 * Sets prefix in specified site
	 *
	 * @param prefix
	 * @param siteName
	 */
	@Deprecated
	public void setPrefix(String prefix, String siteName);

	/**
	 * Returns suffix in specified site
	 *
	 * @param siteName
	 * @return
	 */
	@Deprecated
	public String getSuffix(String siteName);

	/**
	 * Set suffix in specified site
	 *
	 * @param prefix
	 * @param siteName
	 */
	@Deprecated
	public void setSuffix(String suffix, String siteName);

	/**
	 * Returns option value in specified sites.
	 * null if option is not defined in that site
	 *
	 * @param option
	 * @param siteName
	 * @return
	 */
	public String getOption(String option, String siteName);

	/**
	 * Sets option value in specified site
	 *
	 * @param option
	 * @param siteName
	 * @param value
	 */
	public void setOption(String option, String siteName, String value);

	/**
	 * Returns all options in specified site
	 *
	 * @param siteName
	 * @return
	 */
	public Map<String, String> getOptions(String siteName);

	/**
	 * Returns ALL options in each site
	 *
	 * @return
	 */
	public Map<String, Map<String, String>> getOptionsMap();


	/**
	 * Returns true if this User/Group exists only in server memory
	 *
	 * @return
	 */
	public boolean isVirtual();

	/**
	 * Commit data to backend
	 */
	public void save();

	/**
	 * Completely remove data from backend
	 */
	public void remove();
}
