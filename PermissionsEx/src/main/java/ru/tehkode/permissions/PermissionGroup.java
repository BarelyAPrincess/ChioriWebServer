/*
 * PermissionsEx - Permissions plugin for Loader
 * Copyright (C) 2011 t3hk0d3 http://www.tehkode.ru
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package ru.tehkode.permissions;

import ru.tehkode.permissions.events.PermissionEntityEvent;

import java.util.*;
import java.util.logging.Logger;

/**
 * @author t3hk0d3
 */
public abstract class PermissionGroup extends PermissionEntity implements Comparable<PermissionGroup> {

	protected final static String NON_INHERITABLE_PREFIX = "#";

	protected int weight = 0;
	protected boolean dirtyWeight = true;

	public PermissionGroup(String groupName, PermissionManager manager) {
		super(groupName, manager);
	}

	@Override
	public void initialize() {
		super.initialize();

		if (this.isDebug()) {
			Logger.getLogger("PermissionsEx").info("[PermissionsEx] Group " + this.getName() + " initialized");
		}
	}

	/**
	 * Return non-inherited group prefix.
	 * This means if a group don't have has own prefix
	 * then empty string or null would be returned
	 *
	 * @return prefix as string
	 */
	public String getOwnPrefix() {
		return this.getOwnPrefix(null);
	}

	public abstract String getOwnPrefix(String siteName);

	/**
	 * Return non-inherited suffix prefix.
	 * This means if a group don't has own suffix
	 * then empty string or null would be returned
	 *
	 * @return suffix as string
	 */
	public final String getOwnSuffix() {
		return this.getOwnSuffix(null);
	}

	public abstract String getOwnSuffix(String siteName);

	/**
	 * Returns own (without inheritance) permissions of group for site
	 *
	 * @param site site's site name
	 * @return Array of permissions for site
	 */
	public abstract String[] getOwnPermissions(String site);

	/**
	 * Returns option value in specified site without inheritance
	 * This mean option value wouldn't be inherited from parent groups
	 *
	 * @param option
	 * @param site
	 * @param defaultValue
	 * @return option value or defaultValue if option was not found in own options
	 */
	public abstract String getOwnOption(String option, String site, String defaultValue);

	public String getOwnOption(String option) {
		return this.getOwnOption(option, null, null);
	}

	public String getOwnOption(String option, String site) {
		return this.getOwnOption(option, site, null);
	}

	public boolean getOwnOptionBoolean(String optionName, String site, boolean defaultValue) {
		String option = this.getOwnOption(optionName, site, Boolean.toString(defaultValue));

		if ("false".equalsIgnoreCase(option)) {
			return false;
		} else if ("true".equalsIgnoreCase(option)) {
			return true;
		}

		return defaultValue;
	}

	public int getOwnOptionInteger(String optionName, String site, int defaultValue) {
		String option = this.getOwnOption(optionName, site, Integer.toString(defaultValue));

		try {
			return Integer.parseInt(option);
		} catch (NumberFormatException e) {
		}

		return defaultValue;
	}

	public double getOwnOptionDouble(String optionName, String site, double defaultValue) {
		String option = this.getOwnOption(optionName, site, Double.toString(defaultValue));

		try {
			return Double.parseDouble(option);
		} catch (NumberFormatException e) {
		}

		return defaultValue;
	}

	public int getWeight() {
		if (this.dirtyWeight) {
			this.weight = this.getOptionInteger("weight", null, 0);
			this.dirtyWeight = false;
		}

		return this.weight;
	}

	public void setWeight(int weight) {
		this.setOption("weight", Integer.toString(weight));

		this.dirtyWeight = true;

		this.clearMembersCache();
		this.callEvent(PermissionEntityEvent.Action.WEIGHT_CHANGED);
	}

	/**
	 * Checks if group is participating in ranking system
	 *
	 * @return
	 */
	public boolean isRanked() {
		return (this.getRank() > 0);
	}

	/**
	 * Returns rank in ranking system. 0 if group is not ranked
	 *
	 * @return
	 */
	public int getRank() {
		return this.getOwnOptionInteger("rank", null, 0);
	}

	/**
	 * Set rank for this group
	 *
	 * @param rank Rank for group. Specify 0 to remove group from ranking
	 */
	public void setRank(int rank) {
		if (rank > 0) {
			this.setOption("rank", Integer.toString(rank));
		} else {
			this.setOption("rank", null);
		}

		this.callEvent(PermissionEntityEvent.Action.RANK_CHANGED);
	}

	/**
	 * Returns ranking ladder where this group is participating in
	 *
	 * @return Name of rank ladder as String
	 */
	public String getRankLadder() {
		return this.getOption("rank-ladder", "", "default");
	}

	/**
	 * Set rank ladder for this group
	 *
	 * @param rankLadder Name of rank ladder
	 */
	public void setRankLadder(String rankLadder) {
		if (rankLadder.isEmpty() || rankLadder.equals("default")) {
			rankLadder = null;
		}

		this.setOption("rank-ladder", rankLadder);

		this.callEvent(PermissionEntityEvent.Action.RANK_CHANGED);
	}

	protected abstract String[] getParentGroupsNamesImpl(String siteName);

	/**
	 * Returns array of parent groups objects
	 *
	 * @return array of groups objects
	 */
	public PermissionGroup[] getParentGroups(String siteName) {
		List<PermissionGroup> parentGroups = new LinkedList<PermissionGroup>();

		for (String parentGroup : this.getParentGroupsNamesImpl(siteName)) {

			// Yeah horrible thing, i know, that just safety from invoking empty named groups
			parentGroup = parentGroup.trim();
			if (parentGroup.isEmpty()) {
				continue;
			}

			if (parentGroup.equals(this.getName())) {
				continue;
			}

			PermissionGroup group = this.manager.getGroup(parentGroup);
			if (!parentGroups.contains(group) && !group.isChildOf(this, siteName, true)) { // To prevent cyclic inheritance
				parentGroups.add(group);
			}
		}

		if (siteName != null) {
			// Site Inheritance
			for (String parentSite : this.manager.getSiteInheritance(siteName)) {
				parentGroups.addAll(Arrays.asList(getParentGroups(parentSite)));
			}

			parentGroups.addAll(Arrays.asList(getParentGroups(null)));
		}

		Collections.sort(parentGroups);

		return parentGroups.toArray(new PermissionGroup[0]);
	}

	public PermissionGroup[] getParentGroups() {
		return this.getParentGroups(null);
	}

	public Map<String, PermissionGroup[]> getAllParentGroups() {
		Map<String, PermissionGroup[]> allGroups = new HashMap<String, PermissionGroup[]>();

		for (String siteName : this.getSites()) {
			allGroups.put(siteName, this.getSiteGroups(siteName));
		}

		allGroups.put(null, this.getSiteGroups(null));

		return allGroups;
	}

	protected PermissionGroup[] getSiteGroups(String siteName) {
		List<PermissionGroup> groups = new LinkedList<PermissionGroup>();

		for (String groupName : this.getParentGroupsNamesImpl(siteName)) {
			if (groupName == null || groupName.isEmpty() || groupName.equalsIgnoreCase(this.getName())) {
				continue;
			}

			PermissionGroup group = this.manager.getGroup(groupName);

			if (!groups.contains(group)) {
				groups.add(group);
			}
		}

		Collections.sort(groups);

		return groups.toArray(new PermissionGroup[0]);
	}

	/**
	 * Returns direct parents names of this group
	 *
	 * @return array of parents group names
	 */
	public String[] getParentGroupsNames(String siteName) {
		List<String> groups = new LinkedList<String>();
		for (PermissionGroup group : this.getParentGroups(siteName)) {
			groups.add(group.getName());
		}

		return groups.toArray(new String[0]);
	}

	public String[] getParentGroupsNames() {
		return this.getParentGroupsNames(null);
	}

	/**
	 * Set parent groups
	 *
	 * @param parentGroups Array of parent groups names to set
	 */
	public abstract void setParentGroups(String[] parentGroups, String siteName);

	public void setParentGroups(String[] parentGroups) {
		this.setParentGroups(parentGroups, null);
	}

	/**
	 * Set parent groups
	 *
	 * @param parentGroups Array of parent groups objects to set
	 */
	public void setParentGroups(PermissionGroup[] parentGroups, String siteName) {
		List<String> groups = new LinkedList<String>();

		for (PermissionGroup group : parentGroups) {
			groups.add(group.getName());
		}

		this.setParentGroups(groups.toArray(new String[0]), siteName);

		this.callEvent(PermissionEntityEvent.Action.INHERITANCE_CHANGED);
	}

	public void setParentGroups(PermissionGroup[] parentGroups) {
		this.setParentGroups(parentGroups, null);
	}

	protected abstract void removeGroup();

	/**
	 * Check if this group is descendant of specified group
	 *
	 * @param group            group object of parent
	 * @param checkInheritance set to false to check only the direct inheritance
	 * @return true if this group is descendant or direct parent of specified group
	 */
	public boolean isChildOf(PermissionGroup group, String siteName, boolean checkInheritance) {
		if (group == null) {
			return false;
		}

		for (PermissionGroup parentGroup : this.getParentGroups(siteName)) {
			if (group.equals(parentGroup)) {
				return true;
			}

			if (checkInheritance && parentGroup.isChildOf(group, siteName, checkInheritance)) {
				return true;
			}
		}

		return false;
	}

	public boolean isChildOf(PermissionGroup group, boolean checkInheritance) {
		for (String siteName : this.getSites()) {
			if (this.isChildOf(group, siteName, checkInheritance)) {
				return true;
			}
		}

		return this.isChildOf(group, null, checkInheritance);
	}

	public boolean isChildOf(PermissionGroup group, String siteName) {
		return isChildOf(group, siteName, false);
	}

	public boolean isChildOf(PermissionGroup group) {
		return isChildOf(group, false);
	}

	/**
	 * Check if this group is descendant of specified group
	 *
	 * @param groupName        name of group to check against
	 * @param checkInheritance set to false to check only the direct inheritance
	 * @return
	 */
	public boolean isChildOf(String groupName, String siteName, boolean checkInheritance) {
		return isChildOf(this.manager.getGroup(groupName), siteName, checkInheritance);
	}

	public boolean isChildOf(String groupName, boolean checkInheritance) {
		return isChildOf(this.manager.getGroup(groupName), checkInheritance);
	}

	/**
	 * Check if specified group is direct parent of this group
	 *
	 * @param groupName to check against
	 * @return
	 */
	public boolean isChildOf(String groupName, String siteName) {
		return this.isChildOf(groupName, siteName, false);
	}

	public boolean isChildOf(String groupName) {
		return this.isChildOf(groupName, false);
	}

	/**
	 * Return array of direct child group objects
	 *
	 * @return
	 */
	public PermissionGroup[] getChildGroups(String siteName) {
		return this.manager.getGroups(this.getName(), siteName, false);
	}

	public PermissionGroup[] getChildGroups() {
		return this.manager.getGroups(this.getName(), false);
	}

	/**
	 * Return array of descendant group objects
	 *
	 * @return
	 */
	public PermissionGroup[] getDescendantGroups(String siteName) {
		return this.manager.getGroups(this.getName(), siteName, true);
	}

	public PermissionGroup[] getDescendantGroups() {
		return this.manager.getGroups(this.getName(), true);
	}

	/**
	 * Return array of direct members (users) of this group
	 *
	 * @return
	 */
	public PermissionUser[] getUsers(String siteName) {
		return this.manager.getUsers(this.getName(), siteName, false);
	}

	public PermissionUser[] getUsers() {
		return this.manager.getUsers(this.getName());
	}

	public boolean isDefault(String siteName) {
		return this.equals(this.manager.getDefaultGroup(siteName));
	}

	/**
	 * Overriden methods
	 */
	@Override
	public String getPrefix(String siteName) {
		// @TODO This method should be refactored

		String localPrefix = this.getOwnPrefix(siteName);

		if (siteName != null && (localPrefix == null || localPrefix.isEmpty())) {
			// Site-inheritance
			for (String parentSite : this.manager.getSiteInheritance(siteName)) {
				String prefix = this.getOwnPrefix(parentSite);
				if (prefix != null && !prefix.isEmpty()) {
					localPrefix = prefix;
					break;
				}
			}

			// Common space
			if (localPrefix == null || localPrefix.isEmpty()) {
				localPrefix = this.getOwnPrefix(null);
			}
		}

		if (localPrefix == null || localPrefix.isEmpty()) {
			for (PermissionGroup group : this.getParentGroups(siteName)) {
				localPrefix = group.getPrefix(siteName);
				if (localPrefix != null && !localPrefix.isEmpty()) {
					break;
				}
			}
		}

		if (localPrefix == null) { // NPE safety
			localPrefix = "";
		}

		return localPrefix;
	}

	@Override
	public String getSuffix(String siteName) {
		// @TODO This method should be refactored

		String localSuffix = this.getOwnSuffix(siteName);

		if (siteName != null && (localSuffix == null || localSuffix.isEmpty())) {
			// Site-inheritance
			for (String parentSite : this.manager.getSiteInheritance(siteName)) {
				String suffix = this.getOwnSuffix(parentSite);
				if (suffix != null && !suffix.isEmpty()) {
					localSuffix = suffix;
					break;
				}
			}

			// Common space
			if (localSuffix == null || localSuffix.isEmpty()) {
				localSuffix = this.getOwnSuffix(null);
			}
		}

		if (localSuffix == null || localSuffix.isEmpty()) {
			for (PermissionGroup group : this.getParentGroups(siteName)) {
				localSuffix = group.getSuffix(siteName);
				if (localSuffix != null && !localSuffix.isEmpty()) {
					break;
				}
			}
		}

		if (localSuffix == null) { // NPE safety
			localSuffix = "";
		}

		return localSuffix;
	}

	@Override
	public String[] getPermissions(String site) {
		List<String> permissions = new LinkedList<String>();
		this.getInheritedPermissions(site, permissions, true, false, new HashSet<PermissionGroup>());
		return permissions.toArray(new String[0]);
	}

	@Override
	public void addPermission(String permission, String siteName) {
		List<String> permissions = new LinkedList<String>(Arrays.asList(this.getOwnPermissions(siteName)));

		if (permissions.contains(permission)) {
			permissions.remove(permission);
		}

		permissions.add(0, permission);

		this.setPermissions(permissions.toArray(new String[0]), siteName);
	}

	@Override
	public void removePermission(String permission, String siteName) {
		List<String> permissions = new LinkedList<String>(Arrays.asList(this.getOwnPermissions(siteName)));

		permissions.remove(permission);

		this.setPermissions(permissions.toArray(new String[0]), siteName);
	}

	protected void getInheritedPermissions(String siteName, List<String> permissions, boolean groupInheritance, boolean siteInheritance, Set<PermissionGroup> visitedGroups) {
		if (visitedGroups.size() == 0) {
			permissions.addAll(Arrays.asList(this.getTimedPermissions(siteName)));
			permissions.addAll(Arrays.asList(this.getOwnPermissions(siteName)));
		} else { // filter permissions for ancestors groups
			this.copyFilterPermissions(NON_INHERITABLE_PREFIX, permissions, this.getTimedPermissions(siteName));
			this.copyFilterPermissions(NON_INHERITABLE_PREFIX, permissions, this.getOwnPermissions(siteName));
		}

		if (siteName != null) {
			// Site inheritance
			for (String parentSite : this.manager.getSiteInheritance(siteName)) {
				getInheritedPermissions(parentSite, permissions, false, true, visitedGroups);
			}
			// Common permission
			if (!siteInheritance) {
				this.getInheritedPermissions(null, permissions, false, true, visitedGroups);
			}
		}

		// Group inhertance
		if (groupInheritance && !visitedGroups.contains(this)) {
			visitedGroups.add(this);

			for (PermissionGroup group : this.getParentGroups(siteName)) {
				group.getInheritedPermissions(siteName, permissions, true, false, visitedGroups);
			}
		}
	}

	protected void copyFilterPermissions(String filterPrefix, List<String> to, String[] from) {
		for (String permission : from) {
			if (permission.startsWith(filterPrefix)) {
				continue;
			}
			to.add(permission);
		}
	}

	@Override
	public void addTimedPermission(String permission, String site, int lifeTime) {
		super.addTimedPermission(permission, site, lifeTime);

		this.clearMembersCache();
	}

	@Override
	public void removeTimedPermission(String permission, String site) {
		super.removeTimedPermission(permission, site);

		this.clearMembersCache();
	}

	protected void clearMembersCache() {
		for (PermissionUser user : this.getUsers()) {
			user.clearCache();
		}
	}

	@Override
	public final void remove() {
		for (String site : this.getSites()) {
			this.clearChildren(site);
		}

		this.clearChildren(null);

		this.removeGroup();

		this.callEvent(PermissionEntityEvent.Action.REMOVED);
	}

	private void clearChildren(String siteName) {
		for (PermissionGroup group : this.getChildGroups(siteName)) {
			List<PermissionGroup> parentGroups = new LinkedList<PermissionGroup>(Arrays.asList(group.getParentGroups(siteName)));
			parentGroups.remove(this);

			group.setParentGroups(parentGroups.toArray(new PermissionGroup[0]), siteName);
		}

		for (PermissionUser user : this.getUsers(siteName)) {
			user.removeGroup(this, siteName);
		}
	}

	@Override
	public String getOption(String optionName, String siteName, String defaultValue) {
		return getOption(optionName, siteName, defaultValue, new HashSet<PermissionGroup>());
	}

	protected String getOption(String optionName, String siteName, String defaultValue, Set<PermissionGroup> alreadyVisited) {
		String value = this.getOwnOption(optionName, siteName, null);
		if (value != null) {
			return value;
		}

		if (siteName != null) { // site inheritance
			for (String site : manager.getSiteInheritance(siteName)) {
				value = this.getOption(optionName, site, null);
				if (value != null) {
					return value;
				}
			}

			// Check common space
			value = this.getOption(optionName, null, null);
			if (value != null) {
				return value;
			}
		}

		// Inheritance
		if (!alreadyVisited.contains(this)) {
			alreadyVisited.add(this);
			for (PermissionGroup group : this.getParentGroups(siteName)) {
				value = group.getOption(optionName, siteName, null, alreadyVisited);
				if (value != null) {
					return value;
				}
			}
		}

		// Nothing found
		return defaultValue;
	}


	@Override
	public int compareTo(PermissionGroup o) {
		return this.getWeight() - o.getWeight();
	}
}
