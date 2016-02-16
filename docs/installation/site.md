# Site

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