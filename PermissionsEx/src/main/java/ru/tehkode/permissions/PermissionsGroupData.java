package ru.tehkode.permissions;

import java.util.List;

public interface PermissionsGroupData extends PermissionsData {


	public List<String> getParents(String siteName);

	public void setParents(String siteName, List<String> parents);
}
