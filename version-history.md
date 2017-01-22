# Version History

**For the reasons of historical preservation, I am keeping the version history for the PHP port of Chiori-chan's Web Server (formerly known as Chiori Framework). Any version 5.2 and up is the Java Port**

## Version 1.0

Original Framework concept using object oriented programming. Each
page would call the framework in the beginning and end of each file which was later determined to be resource intensive and more work then desired.

## Version 2.0

First version introducing a loader that would be started using
Apache's mod\_rewrite. This version also introduced built-in WebDav support. The feature was later removed due to the issue of maintaining buggy code. A feature much like this might return one day.

## Version 3

Introduction of experimental administration panel. Removed in later versions. Limited version as an experimental jump between version 2 and version 4.

## Version 4

### Subversion 1 (Betarain)

#### Build 0101

Complete rewrite from version 3. Prior modules not supported in this version.

#### Build 0309

Bug fixes.

### Subversion 2 (Betadroid)

#### Build 0319

First release of component based system. Changed some function names to be more compliment with personal coding standards.
Rewrote the Database Component to use PDO instead of the previous mysql commands. Added a second level of containers called "Views" which allow for even less theme code.

#### Build 0326

Many more bug fixes. Ported around 98% of the user module code to a component.

#### Build 0606

Finished porting 99% of all outdated modules and code.

### Subversion 3 (Sentry)

#### Build 0712

First build safe for lite production use. Improved local file
loader, Framework will load the index file of a requested directory if it exists.

### Subversion 4 (Rainbow Dash)

#### Build 0901

First version to appear on the GitHub.

## Version 5

### Subversion 0 (Fluttershy)

#### Build 1106

More code rewrites to make the framework more streamlined and
easier to debug.

#### Build 1111

Imported some old code which broke the framework. Fixed in next push.

#### Build 1115/1116

Bug Fixes.

### Subversion 1 (Scootaloo)

#### Build 0106/0107

Introduced Hooks which are like event listeners.

### Subversion 2 (Lunar Dream) -- *Java Port*

#### Build 0825

First attempts to port framework to Java.

#### Build 0829

Major issues resolved. About 60%-70% of framework ported, 20% discarded.

## Version 6

### Subversion 0 (Sonic Dash)

#### Build 1004

Switched from Resin to Jetty. Removed Quercus and replaced it with our own Java/PHP hybrid language using the BeanShell libraries

### Subversion 1 (Sonic Dash)

#### Build 1012

Switched from BeanShell to GroovyShell. Now you can write your
scripting in uncompiled Java with the joys of Groovy.

### Subversion 2 (Sonic Doom)

#### Build 1212

Some framework instance/memory handling rewrites. Possibly broken
build.

#### Build 1222

Fixes to the log and console systems. Switched from Jetty to HttpServer (A builtin class of the JRE). Also implemented the TCP server side of the Chiori-chan's Web Server for use with Android and Standalone Apps.

#### Build 1227

Made structure layout changes to implement a TCP API and better support future changes.

### Subversion 3 (Flutter Bat)

#### Build 0104

Heavy code rewrites to move much code from the actual Framework into the new Request, Response and Session classes. All in preparation of a cleaner and much improved API. Expect this version to be broken until testing is performed.

#### Build 0105

Rebuilt the way page rendering is handled and implemented override
annotations. This wacky creation lets you override or set any directives like theme, view, title from within any file no matter if it was handled by the framework or interpreted directly from file.

#### Build 0106

As of this version, the framework was officially absorbed into the server as a whole. Scripts made for prior versions will most likely no longer work.

### Subversion 4 (Rarity Falls)

#### Build 0204

A whole new system for both users and permissions was implemented. If
you want to start using the new permissions system you will need to use the PermissionsEx plugin.

#### Build 0207

Major improvements to the way sessions are handled. Many session
configurations added.

#### Build 0313

Added a build.xml file so the project can be built with Apache Ant 1.8. Switch from YAML to Properties for project details/metadata file
(src/com/chiorichan/metadata.properties).

#### Release 6

-   Changed from using build numbers to release numbers since Jenkins implements this.
-   Optimize WebHandler code for improved performance.
-   Added new InputStream consume file util method.
-   Added privileged port check to NetworkManager.
-   Changed log formatting layout. Log messages now show the current thread and milliseconds.
-   Changed the shutdown procedure. System.exit was prematurely terminating plugins.
-   Added support for Multipart HTTP Sessions.
-   Added support for applications to embed the Web Server as a library. *(WIP)*
-   Moved Networking Code from Loader to it's own Class.
-   Made the base changes needed to run to application as a Client. (WIP)
-   Changed the way GSON loads Maps for Sessions.
-   Added auto updater that works with the Jenkins Build Server at <http://jenkins.chiorichan.com>. (WIP)
-   Added ant build.xml for use with the Jenkins Build Server.
-   Added Cartridge Return char support to the ConsoleLogFormatter. (You can now make Progress Bars in console. WOOT!)
-   Added Installation Id. Great for installation tracking.
-   Session cleanups and Update Checks are now on a Time Based Rotation.
-   Fixed bug with doubled output in log file.
-   Added option to disable chat colors.
-   Switch metadata file from YAML to Properties.

## Version 7 (Pony Feathers)

### Subversion 0

#### Release 0

-   Implemented the `FileUserAdapter`
-   Added a mech to prevent repetitive retries in the `SqlConnector.query()`.
-   Fixed the includes matcher loop bug. We had to reset the matcher since the source changes on each loop.
-   Commons can be indefinitely turned off from the config option config.noCommons.
-   You can now optionally force the Template Plugin to always render page requests. You can disable this with the override `@themeless`.
-   Made both `defaultDocType` and `defaultTag` a configurable option for the Template Plugin. See `plugins/Template/config.yml`
-   Implemented the ability to include packages inside html using " "
-   Bug fixes with the way file extension is parsed.
-   Bug fixes to Database Virtual Request Interpreter
-   Bug fixes to doInclude and added non-evaled html.
-   Bug fixes to subdomain load inside Site
-   Added `framework.sites.autoCreateSubdomains` and `framework.sites.subdomainsDefaultToRoot` options in server configs.
-   Moved event system to it's own class, plan to make events usable outside of plugins
-   Enhanced FileInterpreter, simplified the PluginManager and ServicesManager, Added Groovy Plugin Loader (Still needs reworking     to actually function.)

### Subversion 1

#### Release 0

-   Fixes to charset of both loading files and output to browser.
 `getResponse().setEncoding( "UTF-8" )` implemented.
-   Implemented Embedded Groovy Server Pages. See `EmbeddedShell.java`
-   `SQLAccountAdapter` now confirms if additional user field exists in table.
-   Major code restructuring to hopefully fix many lingering problems.
-   Undid a few of really silly implementations I did.

## Version 8 (Pony Toaster)

### Subversion 0

#### Release 0

-   Ant build script now creates simple filename jar files.
-   Evaling is now `CodeEvalFactory`.
-   Simplifying and improvements to `evalPackage()` and `evalFile()` methods inside `WebUtils` class.
-   Null and empty check for print method inside of `HttpResponse`.
-   Added `getLookupAdapter()` to Accounts and fixes to `ScriptingBaseGroovy`.
-   Templates plugin now listens for Http Exceptions and responds.
-   Updates to `PluginManager`, `getPluginByName()` method.
-   Updates to `SeaShells` and how they handle exceptions.
-   Added Exception and Error events for http requests.

### Subversion 1

#### Release 0

-   BUG FIX! Rewrites to `CodeEvalFactory` over a major design flaw
-   BIG FIX! Auto Updater was broken
-   BUG FIX! to Exceptions
-   BUG FIX! Timezone is now forced to UTC when sent to browser
-   BUG FIX! Proper exception is thrown if you forget to close a GSP
    Marker
-   BIG FIX! to common annotations
-   BUG FIX! to Templates Plugin with BaseTemplate.html
-   EXPANSION! to noCommons in the Templates plugin
-   EXPANSION! to date() method
-   EXPANSION! Added additional conditions to the GSP Interpreter
-   NEW FEATURE! CoffeeScripts can now be server side compiled with a
    PreProcessor
-   NEW FEATURE! Implemented File Uploaded a.k.a. Multipart Form
    Requests
-   NEW FEATURE! HTMLCommentParsers
-   NEW FEATURE! PreProcessors, Interpreters and PostProcessors which
    can manipulate requests server-side
-   NEW FEATURE! \[WIP\] Less PreProcessor, currently buggy
-   NEW FEATURE! Image PostProcessor, can resize images using params
-   NEW FEATURE! Implemented the site command which can create, delete
    and view sites
-   NEW METHOD! dateToEpoch(date), allows you to convert M/d/YYYY to
    epoch
-   NEW METHOD! castToLong() method in ObjectUtil
-   NEW METHOD! trim() to StringUtil
-   NEW METHOD! getAllTypes() to ContentTypes class
-   ADDED! url\_to(\[subdomain\]) to HTMLCommentParsers
-   ADDED! include(<package>) to HTMLCommentParsers
-   ADDED! url\_to\_\[login|logout\]() to HTMLCommentParsers
-   ADDED! optional param (altTableClass) to createTable method under
    WebUtils class
-   ADDED! crude cache system with command. Needs much more work
-   ADDED! Mozilla Rhino Library for non-OpenJDK compiles
-   ADDED! LICENSE file
-   ADDED! HEADER file
-   UPDATED! Guava library from v14.0.1 to v17.0.0
-   SWITCHED! to the Gradle Build System from Apache Ant

### Subversion 3

#### Release 0

-   SWITCHED to Netty for HTTP, HTTPS and TCP servers.

## Version 9!

### Subversion 0 (a.k.a. Milky Planet)

#### Build 0

-   ADDED! Directory listing feature \[WIP\]
-   ADDED! Ability to load sites from backend, using /fw/\~siteid
-   ADDED! Added CheckStyle plugin to Gradle
-   REWROTE! EventBus
-   REWROTE! PluginManager
-   REWROTE! Permissions System
-   REMOVED! Removed the Console. Will be reimplementing it later as a Plugin
-   REMOVED! Old org.json source code
-   UPDATED! Various log improvements
-   EXPANDED! PermissionsEx plugin is no longer needed as it funality is built in

#### Build 1

-   UPDATED! Changed default MIME type
-   UPDATED! Encoding UTF-8 used for text MIME, ISO-8859-1 used for all others

#### Build 2

-   ADDED! Gradle publish to our Maven Repository
-   ADDED! Several tables are now generated at load
-   FIXED! Finally finished SQLite support
-   UPDATED! Improved FileUploads
-   UPDATED! Major updates to the EvalFactory and associated classes
-   UPDATED! Switch from using byte\[\] to ByteBuf

#### Build 3

-   ADDED! Implemented a basic Query Server and Command System \[WIP\]
-   READDED! Websocket Support *[WIP]*
-   REWROTE! Rewriting of the Template Plugin
-   REWROTE! NetworkManager
-   UPDATED! `EvalFactory` now returns `EvalFactoryResult`

### Subversion 1 (a.k.a. Milky Way)

#### Build 0

-   ADDED: Dropbox Plugin
-   ADDED: Server delivers HTTP code 503 to client if server is not fully loaded up
-   NEW FEATURE: Added a Plugin Maven Library Downloader. See Dropbox plugin config for an example.

#### Build 1

-   ADDED: Route file can now contain comment lines, starting \#
-   CHANGE: Default source directory changed from pages to root of the site
-   ADDED: Groovy Script Timeout
-   TWEAK: Missing Rewrite File result
-   TWEAK: `ObjectFunc` class
-   TWEAK: SessionManager and AutoUpdater

### Subversion 2 (a.k.a. Milky Berry)

#### Build 0

-   REWROTE: Complete overhaul of Accounts and Sessions systems
-   CHANGE: Session Cookie params are not unchangeable

#### Build 1

-   REWRITE: Huge overhaul of ErrorReporting, Exceptions and Logging subsystems *[WIP](
-   ADDED: New --install argument
-   CHANGE: Re-enabled the Web UI extraction on install and tweaked the boot orders
-   REMOVED: ConfigurationManager and HttpUtils finally removed per deprecation
-   ADDED: Timing Constraints and Moved methods from `CommonFunc` to Timings class
-   CHANGE: Task Argument Orders
-   CHANGE: Removed "Chiori" from task class names
-   ADDED: Plugins and Dependencies can now load Native Libraries. WOOHOO! No more ClassPath crap.
-   ADDED: Steps taken to further implement automatic ban system *[WIP]*
-   CHANGE: Improvement to HttpError exception
-   CHANGE: Renamed ScheduleManager to `TaskManager`
-   ADDED: Plaintext passwords now stored in separate table
-   REWRITE: Plugin initialize subroutine
-   TWEAK: TemplatesPlugin per changes to server base

#### Build 2

-   DEPENDS: Officially updated Netty to 5.0.0.Alpha2 and Groovy to 2.4.3
-   TWEAK: Performance and Coding Standard Improvements per suggested by JARchitect

#### Build 3

-   REWROTE: `EvalFactory` Handling and Processing, noticeable speed improvement!!!
-   ADDED: `Cache`, `BWFilter`, `ARGBFilter` to `PostImageProcessor`

#### Build 4

-   FIXED: bug with build.properties not updating when gradle is ran
-   FIXED: `FileFunc.directoryHealthCheck()` writable bug
-   FIXED: Bug with Web UI not extracting into proper directory
-   FIXED: Bug with Web UI archive not being included in `shadowJar`, technically still not fixed.
-   CHANGE: Improved how permissions hold custom values
-   CHANGE: Improved Plugin Exceptions
-   CHANGE: `sendException()` now prints html and head tags to output
-   CHANGE: Exception catching improvements for plugins

#### Build 5

-   ADDED: Default permission type, similar to boolean type
-   ADDED/FIXED: `PermissionFile` backend
-   REMOVED: `sendError()` method
-   REMOVED: `help.yml` file
-   CHANGE: to how Permissions save and load
-   CHANGE: Source file license headers
-   CHANGE: `PermissionResult.setValue()` and `setDescription()` no longer auto-commit changes
-   CHANGE: Moved static methods from Permission to PermissionManager
-   TWEAK: Speed improvements to permission loading, saving, checking
 and logic in general

#### Build 6

-   REWROTE: Massive rewrites to how Permissions and Accounts relate with each other
-   REWROTE: Templates Plugin eval subroutines
-   CHANGE: Better implementations of Query Terminal
-   ADDED: Implemented `AdvancedCommand` class
-   ADDED: Virtual entity checks, prevents root and none from being saved to backend
-   ADDED: `successInit()` method to `AccountCreator` class, used to regulate root and none accounts
-   ADDED: New methods within utility classes
-   ADDED: New messaging subsystem system \[WIP\]
-   ADDED: Permission and Group references \[WIP\]
-   CHANGE: Command permission checks
-   CHANGE: Entity maps are now `ConcurrentHashMaps`
-   CHANGE: sys permission nodes, excluding sys.op, are now true for operators
-   CHANGE: Plugin Descriptions now use Yaml over a Map to load
-   CHANGE: Started to separate Groovy Evaluation code from `EvalFactory`. *[WIP]*
-   CHANGE: Eval is done directly thru `EvalContext`, removed `WebFunc` eval methods. Excludes `include()` and `require()` Groovy API
-   FIXED: `SyntaxException` not properly being caught

#### Build 7

-   CHANGE: Stacktraces now show their true filename versus their script name
-   CHANGE: Templates Plugin finds headers relative to theme package, i.e., `../../includes/common` and `../../includes/[themename]`
-   CHANGE: `PermissionNamespace` moved to utils for uses outside of `Permissions`
-   ADDED: Plugins can bundle libraries within the subdirectory `libraries`

#### Build 8

-   FIXED: Found a bug with not checking is WeakReference is unloaded
-   FIXED: Bug Fixes to database logging and prepared statements
-   FIXED: Huge bug with EventBus not creating new `EventHandlers` for each `AbstractEvent`
-   FIXED: Tasks not canceling on disable
-   FIXED: Token management was buggy
-   FIXED: TemplatesPlugin includes
-   UPDATED: EventBus.callEvent now returns the `AbstractEvent` for stacking
-   UPDATED: `EventBus` locks using an object instead of self
-   UPDATED: `EventHandlers` are now tracked from inside `EventBus` instead of each Event
-   UPDATED: Tweaks and improvements to Permission Commands
-   UPDATED: Several tweaks and improvements to permissions subsystem
-   UPDATED: various improvements to EventBus
-   UPDATED: Made updates to Dropbox Plugin
-   UPDATED: Changed maven download URL to JCenter Bintray, Maven Central used as a backup location
-   FEATURE: Added H2 Database support, files only
-   FEATURE: *[WIP]* Added datastores, currently only intended to replace SQL databases but will also implement file backends.

### Subversion 3 (a.k.a. Milky Polkadot)

#### Build 0
-   FIXED: HTTP log routine was reporting incorrect date and time
-   FIXED: Issues with Account SQL Save
-   UPDATED: Deprecated old unused log arguments and implemented the use of a logs directory
-   UPDATED: Changed how Account Subsystem returns results
-   UPDATED: Changed routing log level from FINE to FINER
-   UPDATED: `SQLQueryInsert` (SQL Datastore) now checks that all required columns have been provided before executing query

#### Build 1
- FIXED: Compatibility issues with SQL Datastores and SQLite
- UPDATED: Moved task ticks from Timings to new Ticks class for easier understanding
- UPDATED: Refactored much of the startup, shutdown, and restart subroutines, streamlined for efficiency
- UPDATED: AutoUpdater monitors server jar for modification and restarts to apply changes. (configurable)
- FEATURE: *[WIP]* Added new Server File Watcher
- FEATURE: Implemented optional Watchdog process that monitors a separate spawned JVM instance for crashes and restarts. (use --watchdog to enable) Only tested on Linux

#### Build 2/3
- FIXED: Permissions reload and loading issues
- FIXED: MySQL not auto-reconnecting
- FIXED: `ScriptingFactory` failing to create when `request.getBinding()` is null
- FIXED: Query kicking issue
- FIXED: `OutOfMemoryError` was not caught and was crashing the server
- FIXED: `ServerLogger.debug()` was throwing a NullPointer
- FIXED: SSL required java keystores. PEM and PKCS8 expected instead.
- FIXED: Login redirect was not using HTTP code 307
- ADDED: `generateAcctId()` to `AccountManager`
- CHANGE: Refactored several `TaskManager` methods
- CHANGE: Sessions are no longer cleaned up at shutdown
- FEATURE: CSRF implemented
- FEATURE: SSL SNI support
- FEATURE: Implemented SSL annotations: `REQUIRE` forces request over HTTPS, `DENY` forces request over HTTP (but why?), and `IGNORE` Duh. Throws error when SSL is unavailable.
- UPGRADE: Java 8 made the new defacto minimum
- DEPRECATED: `md5()`, `encodeBase64()`, and `decodeBase64()` `StringFunc` methods. Use `SecureFunc` methods instead.

#### Build 4

- CHANGE: Moved site configuration to `[webroot]/[site-id]/config.yaml`. Site directories also rely on conventions now
- CHANGE: If the `www` subdomain does not exist, visitors are redirected to root url.
- FEATURE: Implemented ACME plugin, auto maintains free Let's Encrypt certificates.
- DEPRECATED: SQL sites

#### Build 5

- FIXED: Broken routes
- FIXED: Session cleanup is now async
- FIXED: include() and require() methods were throwing `FileNotFoundException`
- FIXED: SSL exception catching
- FIXED: Site configuration not being saved on shutdown
- CHANGE: Site configuration files with mismatching siteId and directories names will be auto corrected
- CHANGE: Improved site domain and subdomain configuration, sites can now have multiple domains and be nested within each other, e.g., siteB.siteA.com within siteA.com
- CHANGE: Sites can be accessed thru the default site using the tilde character, e.g., http://localhost/~siteId
- CHANGE: Sites can have dedicated IPv4 and IPv6 addresses assigned to them, e.g., http://123.123.123.123/ -> SiteA and http://234.234.234.234/ -> SiteB
- CHANGE: More fleshing out of the new Acme Plugin for Let's Encrypt CA. Finished the domain verification function
- CHANGE: Changes to YamlConfiguration, added getAsList(), copy(), move(), and improved getList() methods
- CHANGE: Enabled about 22 additional cipher suites for SSL
- CHANGE: Moved com.chiorichan.https.* to com.chiorichan.http
- CHANGE: CSRF renamed to Nonce. Use Annotation `@nonce [Disabled, Flexible, PostOnly, GetOnly, Required]`. Nonce can now also be used to carry short lived strings between requests for extra CSRF protection
- CHANGE: New SSL annotation options, PostOnly and GetOnly. Each will REQUIRE SSL if request is being made over POST or GET.
- ADDED: SSL ciphers can be modified from the EnabledCipherSuites.txt file
- ADDED: Ticks class added to Groovy Imports list
- ADDED: Added methods prepend(), reverseOrder(), subNamespace(), and subNodes() to Namespace Class
- ADDED: regexCapture() method to StringFunc class, can be used to very easily capture a section of a string
- DEPRECATED: Site aliases and metatags

#### Build 6

- FEATURE: *[WIP]* Implemented Apache Configuration and .htaccess function. When finished, it will be able to read about 90% of all Apache directives and use them for controlling server behavior
- CHANGE: Cleaned up JAVADOC and code for HttpHandler class
- CHANGE: Slight changes to the `GroovyScriptingBase`, most builtin methods moved to the `Builtin` class
- CHANGE: Removed use of the Netty SNI Handler and replaced it with a built-in event driven SNI Handler, allows plugins to provide certificate mappings
- CHANGE: Majority of the work finished on Let's Encrypt (Acme) Plugin. It's about 95% work and only needs some bugfixes to proper certificate handling
- ADDED: `getDeveloperContact()` and `getHTMLFooter()` methods to `Versioning` class
- ADDED: `ObjectStacker` helper class, a utility class for stacking child and parent object nodes
- ADDED: Methods `FileFunc#buildFile(parts...), FileFunc#relPath(File)`, `Namespace#getFirst()`, `Namespace#getLast()`, `SecureFunc#rand(len, bool, bool, chars)`

### Subversion 4 (a.k.a. Milky Cross)

#### Build 0/1

- FIXED: Scripting exceptions were failing to be captured
- FEATURE: *[Experimental]* `DeployWrapper`, downloads required libraries from Maven for easy server deployment. 
- FEATURE: Groovy Script cache system
- FEATURE: Site archiving feature
- ADDED: *[WIP]* `NettyLogFactory`, redirect Netty logs to server log
- CHANGE: Load order tweak

#### Build 2

- FIXED: `var_dump()` NullPointer bug
- FIXED: `_REQUEST` map collisions
- FEATURE: New GSP tags (`<? ?>` -- PHP Style (Optional, off by default), `{{ }}` -- Print Escaped String, `{{! !}}` -- Print Unescaped String) and (`<%` `<?`) tags will not print without equal(=) in GSP files
- FEATURE: New `obStart()`, `obEnd()`, `obFlush()`, `section( key, [value] )`, and `yield( key )` to Groovy Scripting API
- ADDED: *[WIP]* DB table to model class
- CHANGE: Forced headers to always be lowercase
- CHANGE: License updated to the MIT LICENSE

### Subversion 5 (a.k.a. TBD)

#### Build 0

- CHANGE: License header and checkstyle cleanup

***(Last Updated 01/21/2017)***

