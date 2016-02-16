# API Reference

As of version 9.3 (Milky Polkadot) plugins can only be implemented using Java and packaged in `.jar` archives. There are plans to implement any given scripting language, such as `groovy`, as a plugin and packaged within a `.zip` file.

Until the Plugin Development Guide is finished you can loosely follow the [CraftBukkit Plugin Guide](https://forums.bukkit.org/threads/basic-bukkit-plugin-tutorial.1339/)

See [Plugins](../README.md) on how to install them.

## How To Write a Java Server Plugin

**Note** Never use the namespace `com.chiorichan.*` within your plugins, it is exclusively reserved and will kick an error when loading your Plugin(s).

Writting a Java Plugin for Chiori-chan's Web Server is easy and is even easier when using the Eclipse IDE. *Keep in mind these steps might require a particular eclipse setup, so your mileage might vary.*

1. First add a new Java Project to the Eclipse Workspace. `File -> New -> Java Project`
  * Project Name: Freely Choose, Example Plugin will suffice for this guide.
  * Java JRE: I recommend 1.8 but anything lower should also work.
2. *While it's not required for this guide, you can use our Maven repository under Gradle and Ant Built Scripts for ease of use*

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