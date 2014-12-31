#!/bin/bash

keytool -genkey -keyalg RSA -alias selfsigned -keystore server.keystore -storetype JKS -storepass abcd1234 -validity 360 -keysize 2048
keytool -export -alias selfsigned -keystore server.keystore -rfc -file public.crt
