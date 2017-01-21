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

## Additional Site Configuration

* **site.title: [site-title]**: The site title. Typically used by the Templates Plugin. Default is pulled from `sites.defaultTitle` in the main server config file, `[server_root]/config.yaml`.
* **site.web-allowed-origin: '*'**: Sets the HTTP Header by the same name.
* **site.encryptionKey: [string]**: Specifies the encryption key used in encrypted values. [WIP]
* **site.envFile: [.env]**: Specify the location of the site environment file. Defaults to `.env` within the site webroot.

## Site SSL Configuration

The site's SSL certificate and key are expected to be located in the `[webroot]/[siteId]/ssl` directory and enabled with the following directives. When unconfigured, the site will use the default server certificate, configured via the main configuration.

* **site.sslCert: [ssl-certificate.crt]**: The SSL certificate file.
* **site.sslKey: [ssl-certificate.key]**: The SSL certificate key.
* **site.sslSecret: [ssl-secret]**: The SSL certificate secret. Leave blank for no secret.

**Security Note: ** Be sure not to keep your SSL private-key in the ssl directory. As it could be compromised via a rogue script or strategic server hack.

## Configure Site Domains

**Chiori-chan's Web Server** can share a multitude of domains and subdomains between sites and works very much like Apache's VirtualHost feature.

**Basic Domain Assignment:**
```yaml
site:
  domains:
    example_com: {}
```

**Advanced Domain Assignment:**
```yaml
site:
  domains:
    example_com:
      subdomain1: {}
      subdomain2: {}
      subdomain3: {}
    sitea_mydomain_local: {}
    subdomain4_example_com: {}
```

In the above example, only the domains with other subdomains or empty sections `{}` are assigned to the site, i.e., `mydomain.local` will not be assigned to this site but `example.com` will. Doing this you have the ability to assign subdomains to any number of sites, each either unique configuration and security. siteA can has `sitea.mydomain.local` and siteB could have `siteb.mydomain.local`, while `mydomain.local` can be assigned to siteZ.

**Developer Note: ** When using a full domain as a key, it's parative that all periods are replaced by underscores, by default YAML interprets periods as key separators. If your domain must contain an underscore, escape it with a backslash like so `some\_domain`.

**Developer Note: ** It's also important to note that sites can be accessed using the tilde `~` from only the default site, e.g., `http://127.0.0.1/~siteA`. This will only access to first specified domain, i.e., root domain.

### Public Webroot

By default each domain assigned to the site, is given it's own directory in the public directory, `[webroot]/[siteId]/public`. Each domain will follow the assignment of the assigned domain with underscores in place of periods and in reverse order, e.g., `[webroot]/[siteId]/public/com_example_subdomain1` or `[webroot]/[siteId]/public/local_mydomain_sitea`

### Domain Configuration

**Each section can be individually configured like so:**
```yaml
site:
  domains:
    example_com:
      __sslCert: bob.crt
      __sslKey: bob.key
      __sslSecret: 'secret'
      subdomain1:
        __directory: [relative to the public directory or absolute]
      subdomain2:
        __redirect: http://newsubdomain.example.com/
        __redirectCode: 301
```

Each configuration directive is appended with a double underscore `__` as to not confuse it with a subdomain. Unused directives are ignored but can be accessed using the Site Scripting API, e.g., `getSite().getDomains().get(0).getConfig("customDirective")` or `getSite().getDomain("sitea.mydomain.local").getConfig("directiveTwo")`.

See the `Site SSL Configuration` section for help configuring SSL on each subdomain using the `sslCert`, `sslKey`, and `sslSecret` directives. Unconfigured SSL will default to SSL on parent domains, the site, and then lastly the server.

* **redirect: [url]** Redirect visitors to the specified URL.
* **redirectCode: [code]** Use any of the available 3xx HTTP codes for the redirect.

**Developer Note: ** By default public directories are not allowed outside the webroot directory unless you change the `sites.allowPublicOutsideWebroot` directive to `true` in the main configuration `[server_root]/config.yaml`.

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

For more information about Datastores visit the [Datastore](/docs/configuration/datastore.md) page.

**First you must specify the database type:**

```yaml
database:
  type: [none, mysql, h2, sqlite]
  prefix: [table-prefix]
```

* **MySQL & H2 Directives**
```yaml
  database:
    host: localhost
    port: 3306
    database: [db-name]
    username: [db-username]
    password: [db-password]
```

* ** SQLite Directives **
```yaml
  database:
    file: db.sqlite
```

**Developer Note:** Environment variables are currently a work in progress and will be finalized very soon. You will be able to specify DB credentials using the `.env` properties file located in the site webroot.

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







