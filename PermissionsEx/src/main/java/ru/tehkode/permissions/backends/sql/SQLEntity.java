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
package ru.tehkode.permissions.backends.sql;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;

import ru.tehkode.permissions.PermissionEntity;
import ru.tehkode.permissions.PermissionManager;
import ru.tehkode.permissions.backends.SQLBackend;

/**
 * @author code
 */
public class SQLEntity extends PermissionEntity {

	public enum Type {

		GROUP, USER
	}

	protected SQLBackend backend;
	protected Map<String, List<String>> sitesPermissions = null;
	protected Map<String, Map<String, String>> sitesOptions = null;
	protected List<String> commonPermissions = null;
	protected Map<String, String> commonOptions = null;
	protected Map<String, Set<String>> parents = null;
	protected Type type;
	protected String prefix;
	protected String suffix;

	public SQLEntity(String name, PermissionManager manager, SQLEntity.Type type, SQLBackend backend) {
		super(name, manager);
		this.backend = backend;
		this.type = type;

		this.fetchInfo();
		this.fetchPermissions();
		this.fetchInheritance();
	}

	public static Set<String> getEntitiesNames(SQLConnection sql, Type type, boolean defaultOnly) {
		try {
			Set<String> entities = new HashSet<String>();

			ResultSet result = sql.prepAndBind("SELECT name FROM `{permissions_entity}` WHERE `type` = ? " + (defaultOnly ? " AND `default` = 1" : ""), type.ordinal()).executeQuery();

			while (result.next()) {
				entities.add(result.getString("name"));
			}

			result.close();

			return Collections.unmodifiableSet(entities);
		} catch (SQLException e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public String[] getSites() {
		Set<String> sites = new HashSet<String>();

		sites.addAll(sitesOptions.keySet());
		sites.addAll(sitesPermissions.keySet());
		sites.addAll(parents.keySet());

		return sites.toArray(new String[0]);
	}

	@Override
	public String getPrefix(String siteName) {
		return (siteName == null || siteName.isEmpty()) ? this.prefix : this.getOption("prefix", siteName);
	}

	@Override
	public String getSuffix(String siteName) {
		return (siteName == null || siteName.isEmpty()) ? this.suffix : this.getOption("suffix", siteName);
	}

	@Override
	public void setPrefix(String prefix, String siteName) {
		if (siteName == null || siteName.isEmpty()) {
			this.prefix = prefix;
			this.updateInfo();
		} else {
			this.setOption("prefix", prefix, siteName);
		}
	}

	@Override
	public void setSuffix(String suffix, String siteName) {
		if (siteName == null || siteName.isEmpty()) {
			this.suffix = suffix;
			this.updateInfo();
		} else {
			this.setOption("suffix", suffix, siteName);
		}
	}

	public String[] getParentNames(String siteName) {
		if (this.parents == null) {
			this.fetchInheritance();
		}

		if (this.parents.containsKey(siteName)) {
			return this.parents.get(siteName).toArray(new String[0]);
		}

		return new String[0];
	}

	@Override
	public String[] getPermissions(String site) {
		List<String> permissions = new LinkedList<String>();

		if (commonPermissions == null) {
			this.fetchPermissions();
		}

		if (site != null && !site.isEmpty()) {
			List<String> sitePermissions = this.sitesPermissions.get(site);
			if (sitePermissions != null) {
				permissions.addAll(sitePermissions);
			}
		} else {
			permissions = commonPermissions;
		}

		return permissions.toArray(new String[0]);
	}

	@Override
	public String getOption(String option, String site, String defaultValue) {
		if (site != null && !site.isEmpty() && this.sitesOptions.containsKey(site)) {
			if (this.sitesOptions.get(site).containsKey(option)) {
				return this.sitesOptions.get(site).get(option);
			}
		}

		if ((site == null || site.isEmpty()) && this.commonOptions.containsKey(option)) {
			return this.commonOptions.get(option);
		}

		return defaultValue;
	}

	@Override
	public void setOption(String option, String value, String site) {
		if (option == null || option.isEmpty()) {
			return;
		}

		if (site == null) {
			site = "";
		}

		if (value == null || value.isEmpty()) {
			try {
				this.backend.getSQL().prepAndBind("DELETE FROM `{permissions}` WHERE `name` = ? AND `permission` = ? AND `type` = ? AND `site` = ?", this.getName(), option, this.type.ordinal(), site).execute();
			} catch (SQLException e) {
				throw new RuntimeException(e);
			}

			if (!site.isEmpty() && this.sitesOptions.containsKey(site)) {
				this.sitesOptions.get(site).remove(option);
			} else {
				this.commonOptions.remove(option);
			}

			return;
		}

		Boolean newOption = true;
		if (this.commonOptions == null) {
			this.fetchPermissions();
		}

		if (!site.isEmpty() && sitesOptions.containsKey(site) && sitesOptions.get(site).containsKey(option)) {
			newOption = false;
		} else if (site.isEmpty() && commonOptions.containsKey(option)) {
			newOption = false;
		}


		try {
			if (newOption) {
				this.backend.getSQL().prepAndBind("INSERT INTO `{permissions}` (`name`, `permission`, `value`, `site`, `type`) VALUES (?, ?, ?, ?, ?)", this.getName(), option, value, site, this.type.ordinal()).execute();
			} else {
				this.backend.getSQL().prepAndBind("UPDATE `{permissions}` SET `value` = ? WHERE `name` = ? AND `type` = ? AND `permission` = ?", value, this.getName(), this.type.ordinal(), option).execute();
			}
		} catch (SQLException e) {
			throw new RuntimeException(e);
		}

		if (this.isVirtual()) {
			this.save();
		}

		// Refetch options
		this.fetchPermissions();
	}

	public void setParents(String[] parentGroups, String siteName) {
		try {
			// Clean out existing records
			if (siteName != null) { // damn NULL
				this.backend.getSQL().prepAndBind("DELETE FROM `{permissions_inheritance}` WHERE `child` = ? AND `type` = ? AND `site` = ?", this.getName(), this.type.ordinal(), siteName).execute();
			} else {
				this.backend.getSQL().prepAndBind("DELETE FROM `{permissions_inheritance}` WHERE `child` = ? AND `type` = ? AND IFNULL(`site`, 1)", this.getName(), this.type.ordinal()).execute();
			}


			PreparedStatement statement = this.backend.getSQL().prepAndBind("INSERT INTO `{permissions_inheritance}` (`child`, `parent`, `type`, `site`) VALUES (?, ?, ?, ?)", this.getName(), "toset", this.type.ordinal(), siteName);
			for (String group : parentGroups) {
				if (group == null || group.isEmpty()) {
					continue;
				}
				statement.setString(2, group);
				statement.addBatch();
			}
			statement.executeBatch();
		} catch (SQLException e) {
			throw new RuntimeException(e);
		}

		if (this.isVirtual()) {
			this.save();
		}

		//reload inherirance
		this.parents = null;
		this.fetchInheritance();
	}

	@Override
	public Map<String, String> getOptions(String site) {
		Map<String, String> options = site == null ? this.commonOptions : this.sitesOptions.get(site);

		return options != null ? options : new HashMap<String, String>();
	}

	@Override
	public Map<String, String[]> getAllPermissions() {
		Map<String, String[]> allPermissions = new HashMap<String, String[]>();

		allPermissions.put(null, this.commonPermissions.toArray(new String[0]));

		for (Map.Entry<String, List<String>> entry : this.sitesPermissions.entrySet()) {
			allPermissions.put(entry.getKey(), entry.getValue().toArray(new String[0]));
		}

		return allPermissions;
	}

	@Override
	public Map<String, Map<String, String>> getAllOptions() {
		Map<String, Map<String, String>> allOptions = new HashMap<String, Map<String, String>>();

		allOptions.put(null, this.commonOptions);

		for (Map.Entry<String, Map<String, String>> entry : this.sitesOptions.entrySet()) {
			allOptions.put(entry.getKey(), entry.getValue());
		}

		return allOptions;
	}

	@Override
	public void setPermissions(String[] permissions, String site) {
		if (site == null) {
			site = "";
		}

		try {
			this.backend.getSQL().prepAndBind("DELETE FROM `{permissions}` WHERE `name` = ? AND `type` = ? AND `site` = ? AND `value` = ''", this.getName(), this.type.ordinal(), site).execute();

			PreparedStatement statement = this.backend.getSQL().prepAndBind("INSERT INTO `{permissions}` (`name`, `permission`, `value`, `site`, `type`) VALUES (?, ?, '', ?, ?)", this.getName(), "toset", site, this.type.ordinal());
			for (int i = permissions.length - 1; i >= 0; i--) { // insert in reverse order
				statement.setString(2, permissions[i]);
				statement.addBatch();
			}
			statement.executeBatch();
		} catch (SQLException e) {
			throw new RuntimeException(e);
		}

		if (this.isVirtual()) {
			this.save();
		}

		this.fetchPermissions();
	}

	@Override
	public void save() {
		this.updateInfo();
	}

	@Override
	public void remove() {
		try {
			// clear inheritance info
			this.backend.getSQL().prepAndBind("DELETE FROM `{permissions_inheritance}` WHERE `child` = ? AND `type` = ?", this.getName(), this.type.ordinal()).execute();
			// clear permissions
			this.backend.getSQL().prepAndBind("DELETE FROM `{permissions}` WHERE `name` = ? AND `type` = ?", this.getName(), this.type.ordinal()).execute();
			// clear info
			this.backend.getSQL().prepAndBind("DELETE FROM `{permissions_entity}` WHERE `name` = ? AND `type` = ?", this.getName(), this.type.ordinal()).execute();
		} catch (SQLException e) {
			throw new RuntimeException(e);
		}

		this.virtual = true;
		this.commonOptions.clear();
		this.commonPermissions.clear();
		this.sitesOptions.clear();
		this.sitesPermissions.clear();
		this.parents.clear();
	}

	protected void updateInfo() {
		String sql;
		if (this.isVirtual()) { // This section are suspicious, here was problem which are resolved mysticaly. Keep eye on it.
			sql = "INSERT INTO `{permissions_entity}` (`prefix`, `suffix`, `name`, `type`) VALUES (?, ?, ?, ?)";
		} else {
			sql = "UPDATE `{permissions_entity}` SET `prefix` = ?, `suffix` = ? WHERE `name` = ? AND `type` = ?";
		}

		try {
			this.backend.getSQL().prepAndBind(sql, this.prefix, this.suffix, this.getName(), this.type.ordinal()).execute();
		} catch (SQLException e) {
			if (this.isVirtual()) {
				this.virtual = false;
				this.updateInfo(); // try again
			}

			throw new RuntimeException(e);
		}

		this.virtual = false;
	}

	protected final void fetchPermissions() {
		this.sitesOptions = new HashMap<String, Map<String, String>>();
		this.sitesPermissions = new HashMap<String, List<String>>();
		this.commonOptions = new HashMap<String, String>();
		this.commonPermissions = new LinkedList<String>();

		try {
			ResultSet results = this.backend.getSQL().prepAndBind("SELECT `permission`, `site`, `value` FROM `{permissions}` WHERE `name` = ? AND `type` = ? ORDER BY `id` DESC", this.getName(), this.type.ordinal()).executeQuery();
			while (results.next()) {
				String permission = results.getString("permission").trim();
				String site = results.getString("site").trim();
				String value = results.getString("value");

				// @TODO: to this in more optimal way
				if (value.isEmpty()) {
					if (!site.isEmpty()) {
						List<String> sitePermissions = this.sitesPermissions.get(site);
						if (sitePermissions == null) {
							sitePermissions = new LinkedList<String>();
							this.sitesPermissions.put(site, sitePermissions);
						}

						sitePermissions.add(permission);
					} else {
						this.commonPermissions.add(permission);
					}
				} else {
					if (!site.isEmpty()) {
						Map<String, String> siteOptions = this.sitesOptions.get(site);
						if (siteOptions == null) {
							siteOptions = new HashMap<String, String>();
							sitesOptions.put(site, siteOptions);
						}

						siteOptions.put(permission, value);
					} else {
						commonOptions.put(permission, value);
					}
				}
			}
		} catch (SQLException e) {
			throw new RuntimeException(e);
		}
	}

	protected final void fetchInheritance() {
		try {
			this.parents = new HashMap<String, Set<String>>();

			ResultSet results = this.backend.getSQL().prepAndBind("SELECT `parent`, `site` FROM `{permissions_inheritance}` WHERE `child` = ? AND `type` = ? ORDER BY `id` DESC", this.getName(), this.type.ordinal()).executeQuery();

			while (results.next()) {
				String parentName = results.getString(1);
				String siteName = results.getString(2);

				if (!this.parents.containsKey(siteName)) {
					this.parents.put(siteName, new HashSet<String>());
				}

				this.parents.get(siteName).add(parentName);
			}
		} catch (SQLException e) {
			throw new RuntimeException(e);
		}
	}

	protected final void fetchInfo() {
		try {
			ResultSet result = this.backend.getSQL().prepAndBind("SELECT `name`, `prefix`, `suffix` FROM `{permissions_entity}` WHERE `name` = ? AND `type` = ? LIMIT 1", this.getName(), this.type.ordinal()).executeQuery();

			if (result.next()) {
				this.prefix = result.getString("prefix");
				this.suffix = result.getString("suffix");

				// For teh case-insensetivity
				this.setName(result.getString("name"));

				this.virtual = false;
			} else {
				this.prefix = "";
				this.suffix = "";
				this.virtual = true;
			}
		} catch (SQLException e) {
			throw new RuntimeException(e);
		}
	}
}
