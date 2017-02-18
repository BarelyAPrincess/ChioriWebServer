# IP & User Firewall

**Chiori-chan's Web Server** has a slew of features for dealing with user and/or ip addresses that should be banned from accessing the server.

## Manual Bans

Both IPs and Users can be manually banned using the built-in API or remote terminal commands.

Banning an IP using the API:
```java
    NetworkSecurity.banIp( "94.23.193.70" );
```

Banning an IP using remote terminal commands:
```bash
    # Sorry, not implemented yet!
```

Banning a User/Account using the API:
```java
    getAccount( "acctId" ).setBanned( true );
```

Banning a User/Account using the remote terminal commands:
```bash
    # Sorry, not implemented yet!
```

**Note**: Users are banned through the Permissions System, a user is considered banned when they have the `sys.banned` permission node assigned. As such, this also means that any `PermissibleEntity` can be subjected to banning.

## User Whitelisting

Depending on your situation, you might prefer to be able to whitelist a user for a particular site, this is where the Whitelist feature comes in. The way it function is essentially the opposite to how banning a user works. To enable set the configuration key `settings.whitelist` to `true`. All Guest, OP, and Admin accounts are automatically whitelisted to allow for logins and administration.

```java
    getAccount( "acctId" ).setWhitelisted( true );
```

**Note** The permission node `sys.whitelisted` is assigned to users who have been whitelisted on the server. Also, if the user has been banned, it will supersed the `isWhitelisted()` result.

**TODO** Allow whitelist to be enabled per site, instead of the entire server.

## Automatic IP Bans

**Developer Note**: Automatic Bans are handled by the `NetworkSecurity` class.

When the following rules are violated, the server will place the specified ban on the violating IP address:

* **Closed Early** If the remote connection closes the connection before the server has finished more than 3 times within a 1 second period; It will be banned for a total of three days.

* **HTTP 400 Error** If the remote connection caused more than 6x HTTP 400 Bad Requests within a 2 second period; It will be banned for a total of one day.

* **HTTP 500 Error** If the remote connection cause more than 24x HTTP 500 Internal Server Errors within a 1 second period, it will be banned for a total of one day.

***Note***: This feature is currently work in progress and the server currently has no built-in ip blocking ability. Banned IPs are output to a text file located in the server root named `banned-ips.txt`.