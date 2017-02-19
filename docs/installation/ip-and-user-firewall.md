# IP & User Firewall

**Chiori-chan's Web Server** has a slew of features for dealing with user and ip address banning.

## Manual Bans

Both IPs and Users can be manually banned using the built-in API or console commands.

Banning an IP via the API:
```java
    NetworkSecurity.banIp( "94.23.193.70" );
```

Banning an IP via console commands:
```bash
    # Sorry, not implemented yet!
```

Banning an account via the API:
```java
    getAccount( "acctId" ).setBanned( true );
```

Banning a account via console commands:
```bash
    # Sorry, not implemented yet!
```

**Note**: Users are banned via the Permissions API, a user is considered banned when they have the `sys.banned` permission node assigned. As such, this also means that any object extending the `PermissibleEntity` class can also be subjected to banning and whitelisting.

## User Whitelisting

Depending on your situation, you might prefer to use a whitelist over a blacklist, this is where the server whitelist feature comes in handy. As one would expect, the whitelist operates in a near opposite way of the blacklist. To enable, simple set the configuration key `settings.whitelist` to `true` in your server configuration file. All Guest, OP, and Admin accounts are permanently whitelisted to allow for logins and administration.

```java
    getAccount( "acctId" ).setWhitelisted( true );
```

**Note** The permission node `sys.whitelisted` is assigned to users who have been whitelisted. Keep in mind that if the user has been banned, the ban will supersede the `getAccount( "acctId" ).isWhitelisted()` method result.

**TODO** Allow whitelist to be enabled per site, instead of the entire server.

## Automatic IP Bans

**Developer Note**: Automatic bans are handled by the `NetworkSecurity` class.

When the one of the following violation occurs, the server will place the specified ban on the violating IP address:

* **Closed Early** If the remote connection closes the connection before the server has finished processing the request *(Exploit probing scripts like to close the connection as soon as the response is not what was expect.)* more than 3 times within a 1 second period; It will be banned for a total of three days.

* **HTTP 400 Error** If the remote connection caused more than 6x HTTP 400 Bad Requests within a 2 second period; It will be banned for a total of one day.

* **HTTP 500 Error** If the remote connection cause more than 24x HTTP 500 Internal Server Errors within a 1 second period, it will be banned for a total of one day.

*(Frequent HTTP 400 and HTTP 500 errors, could be related to an attempted DDOS/overload attack. We block IPs with these conditions in an attempt to prevent poor QOS to other connections.)*

**Note**: Automatic bans are currently a work in progress and the server does not yet have a built-in method of blocking as of yet. Banned IPs are output to a text file located in the server root named `banned-ipv4.txt` and `banned-ipv6.txt`. We recommend you use an automated cronjob to import these IPs into your server firewall for the most effective blocking. An IPTables import script is provided below for an example.

**DISCLAIMER**: We assume no fault for damages that may result by using this script. This script assumes you are using **NO** other methods for initializing your firewall.

```bash
	#!/bin/bash

	_input=/usr/share/chiori/banned-ipv4.txt
	_pub_if="eth0"
	IPT=/sbin/iptables
	
	# Die if file not found
	[ ! -f "$_input" ] && { echo "$0: File $_input not found."; exit 1; }
	
	# DROP and close everything
	$IPT -P INPUT DROP
	$IPT -P OUTPUT DROP
	$IPT -P FORWARD DROP
	
	# Unrestricted loopback
	$IPT -A INPUT -i lo -j ACCEPT
	$IPT -A OUTPUT -o lo -j ACCEPT
	
	Allow all outgoing connection but no incoming stuff by default
	$IPT -A OUTPUT -o ${_pub_if} -m state --state NEW,ESTABLISHED,RELATED -j ACCEPT
	$IPT -A INPUT -i ${_pub_if} -m state --state ESTABLISHED,RELATED -j ACCEPT
	
	# Blacklist Setup
	$IPT -N banlist

	egrep -v "^#|^$" x | while IFS= read -r ip
	do
		$IPT -A banlist -i ${_pub_if} -s $ip -j LOG --log-prefix "Banned IP"
		$IPT -A banlist -i ${_pub_if} -s $ip -j DROP
	done <"${_input}"
	
	$IPT -I INPUT -j banlist
	$IPT -I OUTPUT -j banlist
	$IPT -I FORWARD -j banlist
	
	# Insert additional IPTable rules here
	$IPT -A INPUT -i ${_pub_if} -p udp --dport 53 -j ACCEPT # DNS
	$IPT -A INPUT -i ${_pub_if} -p tcp --dport 53 -j ACCEPT # DNS
	$IPT -A INPUT -i ${_pub_if} -p tcp --dport 80 -j ACCEPT # HTTP
	$IPT -A INPUT -i ${_pub_if} -p tcp --dport 443 -j ACCEPT # HTTPS
	$IPT -A INPUT -i ${_pub_if} -p tcp --dport 22 -j ACCEPT # SSH
	
	# Uncomment if you're running the server from a non-privileged port
	# $IPT -A INPUT -i ${_pub_if} -p tcp --dport 8080 -j ACCEPT # HTTP-ALT
	# $IPT -A INPUT -i ${_pub_if} -p tcp --dport 8443 -j ACCEPT # HTTPS-ALT
	
	# Uncomment if you also wish to mascarade those non-privileged ports
	# $IPT -A PREROUTING -p tcp --dport 80 -j REDIRECT --to-ports 8080
	# $IPT -A PREROUTING -p tcp --dport 443 -j REDIRECT --to-ports 8443

	$IPT -A INPUT -i ${_pub_if} -p icmp --icmp-type 8 -m state --state NEW,ESTABLISHED,RELATED -m limit --limit 30/sec -j ACCEPT
	$IPT -A INPUT -i ${_pub_if} -p icmp -m icmp --icmp-type 3 -m limit --limit 30/sec -j ACCEPT
	$IPT -A INPUT -i ${_pub_if} -p icmp -m icmp --icmp-type 5 -m limit --limit 30/sec -j ACCEPT
	$IPT -A INPUT -i ${_pub_if} -p icmp -m icmp --icmp-type 11 -m limit --limit 30/sec -j ACCEPT

	$IPT -A INPUT -m limit --limit 5/m --limit-burst 7 -j LOG
	$IPT -A INPUT -j DROP
```