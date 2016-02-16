# API Reference

As of version 9.3 (Milky Polkadot) plugins can only be implemented using Java and packaged in `.jar` archives. There are plans to implement any given scripting language, such as `groovy`, as a plugin and packaged within a `.zip` file.

Until the Plugin Development Guide is finished you can loosely follow the [CraftBukkit Plugin Guide](https://forums.bukkit.org/threads/basic-bukkit-plugin-tutorial.1339/)

See [Plugins](../README.md) on how to install them.

## Java Server Plugin Getting Started

**Note** Never use the namespace `com.chiorichan.*` within your plugins, it is exclusively reserved and will kick an error when loading your Plugin(s).

Writting a Java Plugin for Chiori-chan's Web Server is easy and is even easier when using the Eclipse IDE. *Keep in mind these steps might require a particular eclipse setup, so your mileage might vary.*

First add a new Java Project to the Eclipse Workspace. `File -> New -> Java Project`
* Project Name: Freely Choose, Example Plugin will suffice for this guide.
* Java JRE: We recommend 1.8 but anything lower should also work.

*While it's not required for this guide, you can use our Maven repository within a Gradle and Ant Build Script to make compiling even easier*

Example Gradle Build Script
```gradle
apply plugin: 'eclipse'
apply plugin: 'java'

repositories
{
	jcenter()
	maven
	{ url "http://jenkins.chiorichan.com:8081/artifactory/" }
	mavenCentral()
}

buildscript
{
	repositories
	{
		jcenter()
		maven
		{ url "http://jenkins.chiorichan.com:8081/artifactory/" }
		mavenCentral()
	}
}

sourceCompatibility = '1.8'
targetCompatibility = '1.8'

group = 'com.example.plugin'
description = 'One dummy plugin to rule them all!'

dependencies {
    compile group: 'com.chiorichan', name: 'ChioriWebServer', version: '9.3.6-d2f5ed3-travis-185'

    // Include other dependencies here, the following are examples.
	compile group: 'com.google.guava', name: 'guava', version: '18.0'
	compile group: 'org.apache.commons', name: 'commons-lang3', version: '3.3.2'
}
```

*If you decide to skip the Build Script, you will need to obtain the latest version of our project from either our [GitHub Releases](https://github.com/ChioriGreene/ChioriWebServer/releases), [Jenkins Build Server](http://jenkins.chiorichan.com/job/ChioriWebServer/), or Directly from our [Artifactory Repository](http://jenkins.chiorichan.com:8081/artifactory/snapshots-maven/com/chiorichan/ChioriWebServer/) and add it to your eclipse project build path.*

Next you will need to add the source directories `src/main/java` and `src/main/resources`. Under the resources directory create the file `plugin.yaml`. Following the YAML syntax, derive from the following example:
```yaml
name: Example Plugin # Your Plugin Name
author: John Smith # You The Author
main: com.example.plugin.MainClassFile # The Main Class. A different name is recommended.
version: '1.0' # Plugin Version
depends: [] # Dependencies on Other Plugins, e.g., Templates Plugin or Dropbox Plugin
```

Unless the dependency is already loaded by the server, add each required dependency to the following list and append the list to the end of `plugin.yaml`. These dependencies will be downloaded and loaded automatically by the server at runtime.

```yaml
libraries:
  - "com.baulsupp.kolja:jcurses:0.9.5.3" # Example - Please Remove
  - "package:project:version" # Example - Please Remove
```

Lastly add the Main Class File you just specified under the `java` directory, e.g., `com.example.plugin.MainClassFile`. Add derive from the following example:
```java
package com.example.plugin;

import com.chiorichan.plugin.lang.PluginException;
import com.chiorichan.plugin.loader.Plugin;

/**
 * Main initializing Class for Example Plugin
 */
public class MainClassFile extends Plugin
{
	@Override
	public void onDisable() throws PluginException
	{
		// Called when your plugin is disabled by the server.
		// It is recommended you deconstruct your plugin here.
		// Note that onEnable() will get called if the server is only reloading.
	}
	
	@Override
	public void onEnable() throws PluginException
	{
		// Called when your plugin is enabled by the server.
		// It is recommended you do most of your plugin loading logic here.
	}
	
	@Override
	public void onLoad() throws PluginException
	{
		// Called when your plugin is loaded by the server.
	}
}
```