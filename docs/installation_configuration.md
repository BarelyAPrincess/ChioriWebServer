# Configuration
**Chiori-chan's Web Server** stores configuration within the `server.yaml` file, located within the server root directory containing the server `jar` file. Default configuration values are save upon first run.

**Note** Regarding binding to ports below 1024 on Linux (e.g. 80) will require access to privileged ports. Typically you can workaround this issue by running as root or allowing privileged ports using the command `setcap 'cap_net_bind_service=+ep' /path/to/server.jar`. Our recommended solution is to listen on ports 8080 (http) and 8443 (https) and redirect the traffic using the IPTables firewall.

## About YAML
YAML(tm) is an international collaboration to make a data serialization language which is both human readable and computationally powerful. The founding members of YAML are Ingy döt Net (author of the Perl module `<Data::Denter>`), Clark Evans, and Oren Ben-Kiki. YAML emerged from the union of two efforts. The first was Ingy döt Net's need for a serialization format for Inline, this resulted in his `<Data::Denter` module. The second, was the joint work of Oren Ben-Kiki Clark Evans on simplifying XML within the sml-dev group. YAML was first publicized with a article on 12 May 2001. Oren and Clark's vision for YAML was very similar to Ingy's `<Data::Denter>`, and vice versa, thus a few days later they teamed up and YAML was born.

[Lean More](http://www.yaml.org/start.html)

## server.yaml
*Some of these config keys are buggy, deprecated or don't have full implementation yet. I try my best to update this document when I can but some might get overlooked.*

* server.httpHost: null

	The ip address to bind the web server to, null will bind to all.

* server.httpPort: 8080

	The port to start the http web server on.

* server.httpsPort: 0

	The port to start the https web server on. 0 = disabled

* server.tcpHost: null

	The ip address to bind the TCP server to.

* server.tcpPort: 1024

	The port to start the TCP server on.

* server.admin: chiorigreene@gmail.com

	This is the administrator e-mail address for the web server. Future plans to send exceptions and alerts to specified address.

* server.allowDirectoryListing: false

	If the web browser requests a directory and there is no index file should I list the directory contents?

* server.haltOnSevereError: false

	Should the console halt on severe errors which would require you to press 'Ctrl-c' to quit.

* server.enableWebServer: true

	Should the http and https web server start? Useful if you plan on this being strictly a TCP server.

* server.enableTcpServer: false

	Should the TCP server start? Useful is you plan on this being strictly a HTTP/HTTPS web server.

* server.database.database: chiorifw

	Server Database

* server.database.type: mysql

	What driver to use. Currently only supports mysql.

* server.database.host: localhost

	Where to find the database server. Be sure to allow the host if not running locally.

* server.database.port: 3306

	Database port number.

* server.database.username:

	Database Username

* server.database.password:

	Database Password

* settings.permissions-file: permissions.yml
	
	This is the file used to store permissions.

* settings.update-folder: update

	This is the folder used for both server and plugin automatic updates.

* settings.shutdown-message: Server Shutdown

	This is the message sent to devices and users that have active connections

* settings.whitelist: false

	This tells the server if its using a whitelisted user system.

* settings.webroot: webroot

	Defines the directory used to store web page data.

* settings.ping-packet-limit: 100

	Defines the max number of connections per minute. Perfect for DDOS prevention.

* auto-updater.enabled: true

	Is the server allowed to automaticly update ones self. Might want to disable this if you plan to run a custom build.

* auto-updater.on-broken

	Who to warn if the server or plugins detect that they are running broken builds.

* auto-updater.on-update

	Who to tell if an update is ready.

* auto-updater.perferred-channel: stable

	What channel of builds do you prefer. stable, beta, alpha, nightly

* auto-updater.host: dl.chiorichan.com

	What host address do we check and download updates from

* sessions.defaultSessionName: sessionId

	If no session name is set by the site then this is the name of the cookie used to make the session persistent.

* sessions.defaultTimeout: 3600

	Default timeout until the session is destroyed. 3600 = 1 hour

* sessions.defaultTimeoutWithLogin: 86400

	Default timeout until the session is destroyed if a user is present. 86400 = 24 hours

* sessions.defaultTimeoutRememberMe: 604800

	Default timeout until the session is destroyed if a user is present and they selected the remember me (HTTP Argument: remember = true/1). 604800 = 1 week

* sessions.allowNoTimeoutPermission: false

	Allows a logged in entity to have it's session never destroyed using the permission node: chiori.noTimeout. Be sure to manually destroy it if used.

* sessions.rearmTimeoutWithEachRequest: true

	Tells the server to recalculate the sessions timeout with each HTTP request made.

* sessions.maxSessionsPerIP: 6

	Tells the server what is the maximum allowed sessions per IP. If more exist the Persistence Manager will destroy the sessions with the soonest timeout.

* sessions.reuseVacantSessions: true

	Tells the Persistence Manager to reuse sessions that have no secure information (ie. A user login) that match the requesters IP. Great for those pesky requesters that ignore the session cookie.

* sessions.allowIPChange: false

	Sort of a prevention of session hiJacking. If a request has a different IP from the IP stored in the session should it be forced to use a new session?

* accounts.allowLoginTokens: true

	Allow accounts to become persistent using tokens.

* accounts.singleLogin: false

	One login per account.

* accounts.singleLoginMessage: You logged in from another location.

	Kick message when multiple logins are made.

* accounts.debug: false

	Enable account debug.

* accounts.sqlType.enabled: false

	Enable SQL account datastore.

* accounts.sqlType.default: false

	Make SQL datastore the default preferred method, used for creating new accounts.

* accounts.sqlType.table: accounts

	Which table should the SQL datastore use.

* accounts.sqlType.fields: [username, email, phone]

	Which fields can be used to login with.

* accounts.fileType.enabled: true

	Enable File account datastore

* accounts.fileType.default: true

	Make File SQL the default preferred method, used for creating new accounts.

* accounts.fileType.filebase: accounts

	Directory to store File accounts.

* accounts.fileType.fields: [username, email, phone]

	Which fields can be used to login with.