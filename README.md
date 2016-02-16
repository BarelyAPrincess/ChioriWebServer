# Introduction
**Chiori-chan's Web Server** is a HTTP/TCP Web Server allowing for both dynamic and static content delivered to both browsers and TCP clients. To provide flexibility, the server also includes a powerful Groovy Scripting Language. The Scripting Engine is also extendable using a provided API. Is the Groovy language not your thing, try our extensive Plugin API loosly based on the ever popular CraftBukkit Minecraft Server API. Chiori-chan's Web Server could be considered an Application Server as it gives you the power to create amazing web applications while taking less time and resources, while at the same time utilizing the power of the Java Virtual Machine.

As of version 9.3 (Milky Polkadot), the following features are provided:
* An extensive core API.
* Written in Java, (DUH!) making it crossplatform compatible.
* Apache like Virtual Host with excellent subdomain support.
* The use of Convention over Configuration.
* ~~Terminal Prompt~~ *Temp Removed*
* ~~Administration Web Console~~ *Temp Removed*
* Automatic Updater
* Easy to use request rewrite and routing tables, similar to Apache's `mod_rewrite`.
* File Annotations, based on CSS annotations and controls request handling.
* Builtin User Authorization System, never code your own auth system ever again.
* Builtin Session and Cookie Management.
* Supports GET, POST, HEAD requests.
* Scripting Language, currently builtin and preferred Groovy Shell.
* Groovy Server Pages (GSP), which act much like PHP.
* Namespace based Permission System, e.g., `com.example.user.allowed` or `io.github.repository.canEdit`
* A rich plugin API inspired by the widely popular CraftBukkit Plugin API, a Minecraft Server
* Builtin Stacking Database Engine, e.g., `db.table("repositories").select().where("id").matches(repoId).map()`. Supports SQLite, mySQL, and H2.
* Event API for an array of events. *Script based event listeners are planned*
* Pre and Post Processors, which include:
  * JS Minimizer
  * CSS Minimizer
  * ~~Less Processor~~ *WIP*
  * ~~Coffee Script~~ *WIP*
  * Image Manipulator
* Official Plugins with the following additional features:
  * Templates Plugin
    * Reads file annotations to apply templates. `@theme com.chiorichan.theme.a`
    * Common and relative head file. `imclude.common` or `theme.a` -> `include.a`
    * Set page title. `@title Repositories`
    * Make exception pages pretty and easy to debug
    * And much more!
  * ZXing Plugin
    * Implements the barcode rendering libraries by ZXing into the Server API
* Released under the Mozilla Public License, version 2.0.
* And much much more, all to be covered in this documentation.

# Seeking Help
Hello, my name is Chiori-chan and I'm currently the sole developer of Chiori-chan's Web Server since it's incarnation over three years now. Recently my project has just reached a little over 62,000 lines of code and 700 commits, which has been a real accomplishment. Sadly, this means the project has also become a bit too much for me to handle alone and which means I need help. I find myself dedicating a part-time jobs worth of time trying to keep this project's development moving forward and this is not something I can keep up very much longer. This means not only do I have a limit of time to deicate to my other projects but also a limit of time to finding project contributors and beta testers. So I ask, if anyone reading this is interested in contributing, please contact me.

# To Do
-   Implement a Server Administration Web Interface.
-   Add better error handling and syntax debugging.
-   Create a Sandbox Mode, i.e., SecurityManager
-   Start a Plugin repository
-   Write Plugin API Documentation. (You can use Bukkit Plugin API JavaDocs for the time being)
-   Better the ChangeLogs and Javadocs.
-   Allow certain events to be thrown on site Groovy files not just Plugins, i.e., webroot/resource/com/chiorichan/events/UserLoginEvent.groovy or NotFoundEvent.groovy
-   Improve built-in file cache system
-   Implement SASS Preprocessor
-   Finish htaccess implementation

## SSL

Secure Https is disabled by default. To enable either generate a self-signed certificate or obtain an offical one online. Using the ACME Plugin, you can obtain free valid certificates from the Let's Encrypt CA for each of your sites and it's subdomains, additional configuration will be needed.

Each site can have it's own certificate assigned using the configuration options sslCert and sslKey with the site configuration file, each certificate and key file must be in PEM format and located within webroot/[siteId]/ssl directory.

## File Annotations
File Annotations allows you to fine tune the way a file is handled by the server. Annotaions are commonly applied by placing a key and value pair (@key value) within the very first lines of any file, including images. They can also be applied thru SQL routes or thru the Scripting API, e.g., getResponse().setAnnotation(key, value);, and vise-verse reading annotations with, e.g., getResponse().getAnnotation(key);. Keep in mind, not all annotations applied using the Scripting API will be detected as the time they are used by the server has already pasted.

* ssl \(required, ignore, deny\)

	Restricts the server to which protocols it may serve the file over. If say an end user requests a page with the annotaion '@ssl required' over an unsecure connection, i.e., http://, the server will automatically redirect the request to https. If such protocol is not enabled, the server will respond with a FORBIDDEN error. The same is true if '@ssl deny' is set and a request if made over a secure connection but this is only provided as yin and yang and probably should never be used.

### Template Plugin Annotaions
The following are excludely used by the Templates Plugin.

* theme \[package\]

	Sets the theme package to use for this file upon request, page content is placed
at the pagedata marker specified within plugin configuration.

* themeless

	Forces the Templates Plugin to NEVER render this page with a theme.

* noCommons

	Forces the Templates Plugin to not automatically add the common includes to the head tag.

* header \[string\]

	Includes this file after the beginning of the html tag of the page.

* footer \[string\]

	Includes this file at the end of the page before the end of the html tag.

## Run as a Service
It is possible to run Chiori-chan's Web Server as a system service thru many methods that exceed the scope of this readme. Search online for solutions about running a Java instance as a service.

## File Placement
As long as the release jar "server.jar" file is in a directory that is writable, All required subdirectories and files will be created on first run.

## Database
Most features of the server can utilize either file or database based configuration. On first run, the server will create a SQLite database within the server root as a placeholder. You can easily switch to a MySql or H2 database thru configuration. You can use sql file 'frameworkdb.sql' to create a new database.

## Sites
Sites are the equivalent of VirtualHosts on Apache Web Server. To create a new site, create the directory path webroot/[siteId]/config.yaml and place the following content within, of course modifying the contents to your needs:
```yaml
site:
  id: SiteId
  title: Site Title
  domain: example.com
subdomains: []
web:
  allowed-origin: '*'
sessions:
  cookie-name: SessionId
  default-life: 604800
  remember-life: 157680000
scripts:
  login-form: 'signin.html'
  login-post: '/'
database:
  type: none
  host: null
  port: 3306
  database: null
  username: null
  password: null
  prefix: ''
  connectionString: ''
```

## Plugins
Because this web server has a Plugin System loosly based on CraftBukkit you can develop Plugins almost the same way, with the only differences being the API. You can find a nice beginners tutorial for CraftBukkit at <https://forums.bukkit.org/threads/basic-bukkit-plugin-tutorial.1339/>. To install a plugin just place it within the 'plugins' directory located within the server root.

# Version History
For the reasons of preservation, I will keep the version history for the
PHP Framework here but any version 5.2 and up will be the Java Port

Version 1.0
-----------
Original Framework concept first using object oriented programming. Each
page would call framework in the beginning and end of each file which
was later determined to be resource intensive and more work then
desired.

Version 2.0
-----------
First version introducing a loader that would be started using
mod\_rewrite. This version also introduced built-in WebDav support,
Feature later removed due to the issue of maintaining the buggy code.

Version 3
---------
### Subversion 1
First introduction of experimental administration panel. Panel removed
in later versions but there are plans to introduce it.

### Subversion 2
Limited Version, Used as an experimental version to Version 4.0.

Version 4
---------
### Subversion 1 (Betarain)
#### Build 0101
Fourth time completely rewriting source code from scratch. Version 3 or
prior modules not supported in this version.
#### Build 0309
Nothing more then bug fixes.
### Subversion 2 (Betadroid)
#### Build 0319
First introduction release of component based system. Changed some
function names to be more compliment with personal coding standards.
Rewrote the Database Component to use the new PDO instead of the
previous mysql commands. This change allows multiple db connections,
file based db using SQLite and other db types like oracle. Also added a
second level of containers called "Views" which allow for even less
theme code.
#### Build 0326

Many more bug fixes. Finished porting 98% of the user module code to a
component.

#### Build 0606

Finished porting 99% of all outdated modules and code. Fixed many more
bugs.

### Subversion 3 (Sentry)

#### Build 0712

First build safe for lite production use. Also improved local file
loader, Framework will load the index file of a requested folder if it
exists. Some core framework panel code was added to possibly introduce
the framework panel again but later decided to scrap the idea and wait
till later release.

### Subversion 4 (Rainbow Dash)

#### Build 0901

First version to appear on the GitHub. Made some bug fixes.

Version 5
---------

### Subversion 0 (Fluttershy)

#### Build 1106

Again more code rewrites to make the framework more streamlined and
easier to debug.

#### Build 1111

Inported some old code which broke the framework. Fixed in next push.

#### Build 1115/1116

Bug Fixes.

### Subversion 1 (Scootaloo)

#### Build 0106/0107

Introduced Hooks which are like event listeners. Bug Fixes.

### Subversion 2 (Lunar Dream)

#### Build 0825

First attempts to port framework to the Chiori Web Server.

#### Build 0829

Major issues resolved. About 60%-70% of framework ported, 20% discarded
(to be replaced).

Version 6
---------

### Subversion 0 (Sonic Dash)

#### Build 1004

Switched from Resin to Jetty. Removed Quercus and replaced it with our
own Java/PHP hybrid language using the BeanShell libraries

### Subversion 1 (Sonic Dash)

#### Build 1012

Switched from BeanShell to GroovyShell. Now you can write your web
script in uncompiled Java with the joys of Groovy.

### Subversion 2 (Sonic Doom)

#### Build 1212

Some framework instance/memory handling rewrites. Possiblely broken
build.

#### Build 1222

Fixes to the log and console systems. Switched from Jetty to HttpServer
(A builtin class of the JRE). Also implemented the TCP server side of
the Chiori Web Server for use with Android and Standalone Apps.

#### Build 1227

Made structure layout changes to implement a TCP API and better support
future changes.

### Subversion 3 (Flutter Bat)

#### Build 0104

Heavy code rewrites to move much code from the actual Framework into the
new Request, Response and Session classes. All in preperation of a
cleaner and much improved API. Expect this version to be broken until
testing is performed.

#### Build 0105

Rebuilt the way page rendering is handled and implemented Override
Annotations. This wacky creation lets you override or set any varible
like theme, view, title from within any file no matter if it was
redirected by the framework or interpeted directly from file.

#### Build 0106

As of this version, The framework was offical absorbed into the server
as a whole. Scripts made for prior versions will most likely no longer
work.

### Subversion 4 (Rarity Falls)

#### Build 0204

A whole new system for both users and permissions was implemented. If
you want to start using the new permissions system you will need to use
the PermissionsEx plugin.

#### Build 0207

Major improvements to the way sessions are handled. Many session
configurations added.

#### Build 0313

Added a build.xml file so the project can not be built with Apache Ant
1.8. Switch from YAML to Properties for project details/metadata file
(server/src/com/chiorichan/metadata.properties) so Ant/build.xml could
now make versioned binary files.

#### Release 6

-   Changed from using build numbers to release numbers since Jenkins
    implements this.
-   Optimize WebHandler code for improved performance.
-   Added new InputStream consume file util method.
-   Added privilaged port check to NetworkManager.
-   Changed log formating layout. Log messages now show the current
    thread and milliseconds.
-   Changed the shutdown proceedure. System.exit was prematurly
    terminating plugins.
-   Added support for Multipart HTTP Sessions.
-   Added support for applications to embed the Web Server as a library.
    (WIP)
-   Moved Networking Code from Loader to it's own Class.
-   Made the base changes needed to run to application as a Client.
    (WIP)
-   Changed the way GSON loads Maps for Sessions.
-   Added auto updater that works with the Jenkins Build Server at
    <http://jenkins.chiorichan.com>. (WIP)
-   Added ant build.xml for use with the Jenkins Build Server.
-   Added Cartridge Return char support to the ConsoleLogFormatter. (You
    can now make Progress Bars in console. WOOT!)
-   Added InstallationId. Great for installation tracking.
-   Session cleanups and Update Checks are now on a Time Based Rotation.
-   Fixed bug with doubled output in log file.
-   Added option to disable chat colors.
-   Switch metadata file from YAML to Properties.

Version 7 (Pony Feathers)
--------------------------------------

### Subversion 0

#### Release 0

-   Implemented the FileUserAdapter
-   Added a mech to prevent repetitive retrys in
    the SqlConnector.query().
-   Fixed the includes matcher loop bug. We had to reset the matcher
    since the source changes on each loop.
-   Commons can be indefinitely turned off from the config
    option config.noCommons.
-   You can now optionally force the Template Plugin to always render
    page requests. You can disable this with the override @themeless.
-   Made both defaultDocType and defaultTag a configurable option for
    the Template Plugin. See plugins/Template/config.yml
-   Implemented the ability to include packages inside html using " "
-   Bug fix with the way file extension is parsed.
-   Bug fixes to Database Virtual Request Interpreter
-   Bug fixes to doInclude and added non-evaled htm/html.
-   Bug fixes to subdomain load inside Site
-   Added 'framework.sites.autoCreateSubdomains' and
    'framework.sites.subdomainsDefaultToRoot' options in server configs.
-   Moved event system to it's own class, plan to make events usable
    outside of plugins
-   Enhanced FileInterpreter, simplified the PluginManager and
    ServicesManager, Added Groovy Plugin Loader (Still needs reworking
    to actually function.)

### Subversion 1

#### Release 0

-   Fixes to charset of both loading files and output to browser.
    getResponse().setEncoding( "UTF-8" ) implemented.
-   Implemented Embedded Groovy Server Pages. See EmbeddedShell.java
-   SQLAccountAdapter now confirms if additional user field exists in
    table
-   Major code restructuring to hopefully fix many lingering problems.
-   Undid a few of really silly implementations I did.

Version 8 (Pony Toaster)
------------------------

### Subversion 0

#### Release 0

-   Ant build script now creates simple filename jar files.
-   Evaling is now CodeEvalFactory.
-   Simplifying and improvments to EvalPackage and EvalFile methods
    inside WebUtils class.
-   Null and empty check for print method inside of HttpResponse.
-   Added GetLookupAdapter to Accounts and fixes to ScriptingBaseGroovy.
-   Templates plugin now listens for Http Exceptions and responds.
-   Updates to PluginManager, getPluginByName method.
-   Updates to SeaShells and how they handle exceptions.
-   Added Exception and Error events for http requests.
-   Offical release of Version 8.0.0

### Subversion 1

#### Release 0

-   BUG FIX! Rewrites to CodeEvalFactory over a major design flaw
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

Version 9!
----------

### Subversion 0 (a.k.a. Milky Planet)

#### Build 0

-   ADDED! Directory listing feature \[WIP\]
-   ADDED! Ability to load sites from backend, using /fw/\~siteid
-   ADDED! Added CheckStyle plugin to Gradle
-   REWROTE! EventBus
-   REWROTE! PluginManager
-   REWROTE! Permissions System
-   REMOVED! Removed the Console. Will be reimplementing it later as a
    Plugin
-   REMOVED! Old org.json source code
-   UPDATED! Various log improvements
-   EXPANDED! PermissionsEx plugin is no longer needed as it funality is
    built in

#### Build 1

-   UPDATED! Changed default MIME type
-   UPDATED! Encoding UTF-8 used for text MIME, ISO-8859-1 used for all
    others

#### Build 2

-   ADDED! Gradle publish to our Maven Repository
-   ADDED! Several tables are now generated at load
-   FIXED! Finally finished SQLite support
-   UPDATED! Improved FileUploads
-   UPDATED! Major updates to the EvalFactory and associated classes
-   UPDATED! Switch from using byte\[\] to ByteBuf

#### Build 3

-   ADDED! Implemented a basic Query Server and Command System \[WIP\]
-   READDED! Websocket Support \[WIP\]
-   REWROTE! Rewriting of the Template Plugin
-   REWROTE! NetworkManager
-   UPDATED! EvalFactory now returns EvalFactoryResult

### Subversion 1 (a.k.a. Milky Way)

#### Build 0

-   ADDED: Dropbox Plugin
-   ADDED: Server delivers HttpCode 503 to client if server is not fully
    loaded up
-   NEW FEATURE: Added a Plugin Maven Library Downloader. See Dropbox
    plugin config for an example.

#### Build 1

-   ADDED: Route file can now contain comment lines, starting \#
-   CHANGE: Default source directory changed from pages to root of the
    site
-   ADDED: Groovy Script Timeout
-   TWEAK: Missing Rewrite File result
-   TWEAK: ObjectFunc class
-   TWEAK: SessionManager and AutoUpdater

### Subversion 2 (a.k.a. Milky Berry)

#### Build 0

-   REWROTE: Complete overhaul of Accounts and Sessions systems
-   CHANGE: Session Cookie params are not unchangable

#### Build 1

-   REWRITE: Huge overhaul of ErrorReporting, Exceptions and Logging
    subsystems \[WIP\]
-   ADDED: New --install argument
-   CHANGE: Reenabled the Web UI extraction on install and tweaked the
    boot orders
-   REMOVED: ConfigurationManager and HttpUtils finally removed per
    deprecation
-   ADDED: Timing Constraints and Moved methods from CommonFunc to
    Timings class
-   CHANGE: Task Argument Orders
-   CHANGE: Removed "Chiori" from task class names
-   ADDED: Plugins and Dependencies can now load Native Libraries.
    WOOHOO! No more ClassPath crap. \[WIP\]
-   ADDED: Steps taken to further implement automatic ban system \[WIP\]
-   CHANGE: Improvement to HttpError exception
-   CHANGE: Renamed ScheduleManager to TaskManager
-   ADDED: Plaintext passwords now stored in seperate table
-   REWRITE: Plugin initalize subroutine
-   TWEAK: TemplatesPlugin per changes to server base

#### Build 2

-   DEPENDS: Offically updated Netty to 5.0.0.Alpha2 and Groovy to 2.4.3
-   TWEAK: Performance and Coding Standard Improvments per suggested by
    JARchitect

#### Build 3

-   REWROTE: EvalFactory Handling and Processing, Noticable Speed
    Improvment!!!
-   ADDED: Cache, BWFilter, ARGBFilter to PostImageProcessor

#### Build 4

-   ADDED: start.sh created on first runs for unix-like systems,
    debug\_start.sh also created when in development mode
-   FIXED: bug with build.properties not updating when gradle is ran
-   FIXED: FileFunc.directoryHealthCheck() writable bug
-   FIXED: Bug with Web UI not extracting into proper directory
-   FIXED: Bug with Web UI archive not being included in shadowJar,
    technically still not fixed.
-   CHANGE: Improved how permissions hold custom values
-   CHANGE: Improved Plugin Exceptions
-   CHANGE: sendException() now prints html and head tags to output
-   CHANGE: Exception catching improvements for plugins

#### Build 5

-   ADDED: Default permission type, similar to boolean type
-   ADDED/FIXED: PermissionFile backend
-   REMOVED: sendError() method
-   REMOVED: help.yml file
-   CHANGE: to how Permissions save and load
-   CHANGE: Source file license headers
-   CHANGE: PermissionResult.setValue() and setDescription() no longer
    auto-commit changes
-   CHANGE: Moved static methods from Permission to PermissionManager
-   TWEAK: Speed improvements to permission loading, saving, checking
    and logic in general

#### Build 6

-   REWROTE: Massive rewrites to how Permissions and Accounts relate
    with each other
-   REWROTE: Templates Plugin eval subroutines
-   CHANGE: Better implementations of Query Terminal
-   ADDED: Imeplemented AdvancedCommand class
-   ADDED: Virtual entity checks, prevents root and none from being
    saved to backend
-   ADDED: successInit() method to AccountCreator class, used to
    regulate root and none accounts
-   ADDED: New methods within utility classes
-   ADDED: New messaging subsystem system \[WIP\]
-   ADDED: Permission and Group references \[WIP\]
-   CHANGE: Command permission checks
-   CHANGE: Entity maps are now ConcurrentHashMaps
-   CHANGE: sys permission nodes, excluding sys.op, are now true for
    operators
-   CHANGE: Plugin Descriptions now use Yaml over a Map to load
-   CHANGE: Started to seperate Groovy Evaluation code from EvalFactory.
    \[WIP\]
-   CHANGE: Eval is done directly thru EvalContext, removed WebFunc
    eval methods. Excludes include() and require() groovy api
-   FIXED: SyntaxException not properly being caught

#### Build 7

-   CHANGE: Stacktraces now show their true filename verses their script
    name
-   CHANGE: Templates Plugin finds headers relative to theme package,
    i.e., ../../includes/common and ../../includes/<themename>
-   CHANGE: PermissionNamespace moved to utils for uses outside of
    Permissions
-   ADDED: Plugins can bundle libraries within the subdirectory
    "libraries"

#### Build 8

-   FIXED: Found a bug with not checking is WeakReference is unloaded
-   FIXED: Bug Fixes to database logging and prepared statements
-   FIXED: Huge bug with EventBus not creating new EventHandlers for
    each AbstractEvent
-   FIXED: Tasks not canceling on disable
-   FIXED: Token management was buggy
-   FIXED: TemplatesPlugin includes
-   UPDATED: EventBus.callEvent now returns the AbstractEvent for
    stacking
-   UPDATED: EventBus locks using an object instead of self
-   UPDATED: EventHandlers are now tracked from inside EventBus instead
    of each Event
-   UPDATED: Tweaks and improvements to Permission Commands
-   UPDATED: Several tweaks and improvements to permissions subsystem
-   UPDATED: various improvements to EventBus
-   UPDATED: Made updates to Dropbox Plugin
-   UPDATED: Changed maven download URL to JCenter Bintray, Maven
    Central used as a backup location
-   FEATURE: Added H2 Database support, files only
-   FEATURE: \[WIP\] Added datastores, currently only intended to
    replace SQL databases but will also implement file backends.

### Subversion 3 (a.k.a. Milky Polkadot)

#### Build 0
-   FIXED: HTTP log routine was reporting incorrect date and time
-   FIXED: Issues with Account SQL Save
-   UPDATED: Deprecated old unused log arguments and implmented the use
    of a logs directory
-   UPDATED: Changed how Account Subsystem returns results
-   UPDATED: Changed routing log level from FINE to FINER
-   UPDATED: SQLQueryInsert (SQL Datastore) now checks that all required
    columns have been provided before executing query

#### Build 1
-   FIXED: Compatibility issues with SQL Datastores and SQLite
-   UPDATED: Moved task ticks from Timings to new Ticks class for easier
    understanding
-   UPDATED: Refactored much of the startup, shutdown, and restart
    subroutines, streamlined for efficiency
-   UPDATED: AutoUpdater monitors server jar for modification and
    restarts to apply changes. (configurable)
-   FEATURE: \[WIP\] Added new Server File Watcher
-   FEATURE: Implemented optional Watchdog process that monitors a
    seperate spawned JVM instance for crashes and restarts. (use
    --watchdog to enable) Only tested on Linux

# How To Build
You can either build Chiori-chan's Web Server with Eclipse IDE or using Gradle. It should be as simple as executing "./gradlew build" for linux users. Some plugins will also compile but you will have to execute "./gradlew :EmailPlugin:build" to build it. If built with Gradle, you will find the built files inside the "build/dest" directory.

# Coding
Our Gradle enviroment uses the CodeStyle plugin to check coding standards.

* Please attempt at making your code as easily understandable as possible.
* Leave comments whenever possible. Adding Javadoc is even more appreciated when possible.
* No spaces; use tabs. We like our tabs, sorry.
* No trailing whitespace.
* Brackets should always be on a new line.
* No 80 column limit or 'weird' midstatement newlines, try to keep your entire statement on one line.

# Pull Request Conventions
* The number of commits in a pull request should be kept to a minimum (squish them into one most of the time - use common sense!).
* No merges should be included in pull requests unless the pull request's purpose is a merge.
* Pull requests should be tested (does it compile? AND does it work?) before submission.
* Any major additions should have documentation ready and provided if applicable (this is usually the case).
* Most pull requests should be accompanied by a corresponding GitHub ticket so we can associate commits with GitHub issues (this is primarily for changelog generation).

# License
Chiori Web Server is licensed under the Mozila Public License Version 2.0. If you decide to use our server or use any of our code (In part or whole), PLEASE, we would love to hear about it, It's not required but it's generally cool to hear what others do with our stuff.

\(C) 2015 Greenetree LLC, Chiori-chan's Web Server.
