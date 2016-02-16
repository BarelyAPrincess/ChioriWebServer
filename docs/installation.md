# Installation

To install Chiori-chan's Web Server you need to grab the latest release from our [GitHub Releases](https://github.com/ChioriGreene/ChioriWebServer/releases) page, if you'd like to try the bleeding edge build visit our [Jenkin's Build Server](http://jenkins.chiorichan.com/job/ChioriWebServer/).

## Automated Build Server

[![Build Status](http://jenkins.chiorichan.com/buildStatus/icon?job=ChioriWebServer)](http://jenkins.chiorichan.com/job/ChioriWebServer/)
[![Build Status](https://travis-ci.org/ChioriGreene/ChioriWebServer.svg?branch=master)](https://travis-ci.org/ChioriGreene/ChioriWebServer)

Do you like running the latest bleeding edge builds? Give our automated build servers a try. We utilize both a [Jenkin's Build Server](http://jenkins.chiorichan.com/job/ChioriWebServer/) and [Travis](https://travis-ci.org/ChioriGreene/ChioriWebServer) which pushes each build to our [Artifactory Maven Server](http://jenkins.chiorichan.com:8081/artifactory/snapshots/com/chiorichan/ChioriWebServer/). Each build is triggered by way of a git push.

Chiori-chan's Web Server is equipped with an auto updater which checks our Jenkin Build Server for newer versions. We highly recommend turning the updater off for production environments as bleed edge build could have irreversible damaging effects. We can't be held responsible for damage or unexpected behavior when using bleeding-edge builds.