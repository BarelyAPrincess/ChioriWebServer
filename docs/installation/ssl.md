# SSL

HTTPS is disabled by default. To enable change the main configuration directive `server.httpsPort` to `443`.

On next load, the server will generate a self-signed certificate if one does not already exist. We recommend you obtain a valid certificate from credible sources ASAP. You can use our ACME Plugin, which will auto maintain a free certificate from the Let's Encrypt CA for each of your site's domains, additional configuration will be needed.

**Developers Note: ** On Unix-like systems, using a port below 1024 requires privileged port access. A privilege normally given to the root user. So we recommend setting the port to something like `8443` and redirecting traffic using a firewall like IPTables. Running the server as the root user is highly not-recommended. See the main configuration page for more information.

See [Sites](/docs/configuration/sites.md "Sites") for help configuring SSL for each site.
