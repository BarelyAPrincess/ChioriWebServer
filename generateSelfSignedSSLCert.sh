#!/bin/bash

DOMAIN="*.chiorichan.com"

SUBJ="
C=US
ST=[State]
O=[Organization]
localityName=[City]
commonName=$DOMAIN
organizationalUnitName=
emailAddress=
"

### DO NOT EDIT BELOW THIS LINE ###

echo "Generating RSA Key at 4096 bit"
openssl genrsa -out "secure.key" 4096
echo "Generating Signing Request"
openssl req -new -subj "$(echo -n "$SUBJ" | tr "\n" "/")" -key "secure.key" -out "server.csr" -passin pass:abcd1234
echo "Self Signing the Certificate"
openssl x509 -req -days 365 -in "server.csr" -signkey "secure.key" -out "server.crt"
echo "Removing Secret Key from RSA Key"
openssl rsa -in "secure.key" -out "secure.key" -passin pass:abcd1234
echo "Converting Secret Key to PKCS8 format"
openssl pkcs8 -topk8 -nocrypt -in "secure.key" -out "server.key"

echo "All done! You should only need to place the 'server.crt' and 'server.key' files withing the server root and enable the HTTPS server."

