# Site Configuration

**NOTICE: **_As of version 9.4, there exists a somewhat unintensional bug that overrides configuration files upon server shutdown. Until this bug is fixed, it's recommended not to make configuration changes unless the server is not running._

## Creating a New Site

**Chiori-chan's Web Server** uses conventions to load sites and it's configuration. Each configuration file is kept under a unique directory within the webroot. The webroot unless overridden, is normally found under the server root directory, e.g., `[server_root]/webroot`.

To create a new site with the site id "foobar", you would create the configuration file at `[webroot]/foobar/config.yaml`. It's generally good practice to name the unique site directory the same as the site id, otherwise, the server will attempt to move the site to the correct directory. Each configuration file is written in YAML format and should at least contain the directive `site.id: [siteId]`.

## Configure Site Logins

```yaml
accounts:
  loginForm: /~wisp/login
  loginPost: /
```

* `accounts.loginForm`: The location of the login form. Visitors are redirected to this location when they encounter a page that requires a login, normally via the page annotation `@reqlogin`. An error message is also pasted via the `_NONCE` global; `_NONCE.level`, `_NONCE.msg`, and `_NONCE.target`. **Note:** `/~wisp/` is a special URL for internal server resources. It's experimental and should not be relied upon for future use.
* `accounts.loginPost`: The location to redirect after a successful login.

## Configure Site Sessions

Session are configured with the following configuration directives

```yaml
sessions:
  persistenceMethod: cookie
  cookie-name: SessionId
  default-life: 604800
  remember-life: 157680000
```

* `sessions.persistenceMethod: [cookie or param]`: How sessions are kept persistent, either with a browser cookie or as a URL param.
* `sessions.cookie-name: [string]`: The cookie name used if persistence is using COOKIE. To protect internal server cookies from scripts, the session cookie name is prefixed with `_ws`.
* `sessions.default-life: [seconds]`: The amount of time in seconds before disposing the session. Session validity is renewed with each request.
* `sessions.remember-life: [seconds]`: Overrides default-life when `remember = 1` is pasted during a successful account login.

## Configure Site Database

**Chiori-chan's Web Server** uses the internal Datastore feature to access MySQL, H2, and SQLite databases. The server has a global one that is configured via the main `[server_root]/config.yaml` file, additional databases can be configured via the following directives.

For more information about Datastores visit the  [Datastore](/Datastore "Datastore") page.

```yaml
database:

```

## Enabling Automated Site Archiving

Adding the directive `archive.enable: true` to the site configuration will enable automated site archiving.  Archives are kept in the directory `[server_root]/archive/[siteId]`. By default, only the last 3 archives are kept and a new archive is created every 24 hours, tracked by the `archive.lastRun: [epoch]` directive. To increase \(or decrease\) the number of archives kept, add the directive `archive.keep: [int]`. To change the archive interval, add the directive `archive.interval: [ticks]`, appending S, M, H, or D will specify the ticks in the intervals of: Seconds, Minutes, Hours, and Days. The server when not overloaded runs at 20 ticks per second.

**YAML Config Preview**

```yaml
archive:  
  enable: true  
  lastRun: 0  
  interval: 24h  
  keep: 3
```







