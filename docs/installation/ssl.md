# SSL Configuration

Secure Https is disabled by default. To enable either generate a self-signed certificate or obtain an offical one online. Using the ACME Plugin, you can obtain free valid certificates from the Let's Encrypt CA for each of your sites and it's subdomains, additional configuration will be needed.

Each site can have it's own certificate assigned using the configuration options sslCert and sslKey with the site configuration file, each certificate and key file must be in PEM format and located within webroot/[siteId]/ssl directory.