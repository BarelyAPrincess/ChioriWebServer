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

import java.util.Map;

import ru.tehkode.permissions.events.PermissionEntityEvent;

public abstract class ProxyPermissionUser extends PermissionUser {

	protected PermissionEntity backendEntity;

	public ProxyPermissionUser(PermissionEntity backendEntity) {
		super(backendEntity.getName(), backendEntity.manager);

		this.backendEntity = backendEntity;

		this.setName(backendEntity.getName());

		this.virtual = backendEntity.isVirtual();

	}

	@Override
	public void initialize() {
		super.initialize();
		this.backendEntity.initialize();
	}

	@Override
	public String[] getSites() {
		return backendEntity.getSites();
	}

	@Override
	public String getOwnPrefix(String siteName) {
		return backendEntity.getPrefix(siteName);
	}

	@Override
	public String getOwnSuffix(String siteName) {
		return backendEntity.getSuffix(siteName);
	}

	@Override
	public void setPrefix(String prefix, String siteName) {
		this.backendEntity.setPrefix(prefix, siteName);

		this.clearCache();
	}

	@Override
	public void setSuffix(String suffix, String siteName) {
		this.backendEntity.setSuffix(suffix, siteName);

		this.clearCache();
	}

	@Override
	public boolean isVirtual() {
		return backendEntity.isVirtual();
	}

	@Override
	public String[] getOwnPermissions(String site) {
		return this.backendEntity.getPermissions(site);
	}

	@Override
	public Map<String, String[]> getAllPermissions() {
		return this.backendEntity.getAllPermissions();
	}

	@Override
	public void setPermissions(String[] permissions, String site) {
		this.backendEntity.setPermissions(permissions, site);

		this.clearCache();

		this.callEvent(PermissionEntityEvent.Action.PERMISSIONS_CHANGED);
	}

	@Override
	public Map<String, Map<String, String>> getAllOptions() {
		return this.backendEntity.getAllOptions();
	}

	@Override
	public String getOwnOption(String option, String site, String defaultValue) {
		return this.backendEntity.getOption(option, site, defaultValue);
	}

	@Override
	public Map<String, String> getOptions(String site) {
		return this.backendEntity.getOptions(site);
	}

	@Override
	public void setOption(String permission, String value, String site) {
		this.backendEntity.setOption(permission, value, site);

		this.clearCache();

		this.callEvent(PermissionEntityEvent.Action.OPTIONS_CHANGED);
	}

	@Override
	public void save() {
		this.backendEntity.save();
		super.save();
	}

	@Override
	public void remove() {
		this.backendEntity.remove();
		super.remove();
	}
}
