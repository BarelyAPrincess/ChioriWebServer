# Introduction
**Chiori-chan's Web Server** is a HTTP/TCP Web Server allowing for both dynamic and static content delivered to both browsers and TCP clients. To provide flexibility, the server also includes a powerful Groovy Scripting Language. The Scripting Engine is also extendable using a provided API. Is the Groovy language not your thing, try our extensive Plugin API loosly based on the ever popular CraftBukkit Minecraft Server API. Chiori-chan's Web Server could be considered an Application Server as it gives you the power to create amazing web applications while taking less time and resources, while at the same time utilizing the power of the Java Virtual Machine.

**As of version 9.3 (Milky Polkadot), the following features are provided:**
* An extensive core API.
* Written in Java, (DUH!) making it cross-platform compatible.
* Apache like Virtual Hosts.
* The use of Convention over Configuration.
* YAML Configuration.
* ~~Terminal Prompt~~ *Temp Removed*
* ~~Administration Web Console~~ *Temp Removed*
* ~~Automatic Updater~~ *Broken*
* Easy to use request rewrite and routing config, similar to Apache's `mod_rewrite`.
* File Annotations, similar to CSS annotations.
* Builtin User Authorization System, never code your own auth system ever again.
* Builtin Session and Cookie Management.
* Supports GET, POST, HEAD requests.
* ~~.htaccess support.~~ *WIP*
* Scripting Language, Groovy (99% done) and Lua (10% done).
* Groovy Server Pages (GSP), which act much like PHP.
* Fast HTTP response times, benchmarks look great.
* Scripting Cache, *cached scripts keep the memory and cpu happy*
* Namespace based Permission System, e.g., `com.example.user.allowed` or `io.github.repository.canEdit`
* A rich plugin API inspired by the widely popular CraftBukkit Plugin API.
* Built-in Stacking Database Engine, e.g., `db.table("repositories").select().where("id").matches(repoId).map()`. Supports SQLite, MySql, and H2.
* Event API for an array of events. (75% done)
* Pre and Post Processors, which include:
  * JS Minimizer
  * CSS Minimizer
  * ~~SASS Processor~~ *WIP*
  * ~~Less Processor~~ *WIP*
  * ~~Coffee Script~~ *WIP*
  * Image Manipulator
* Official Plugins with the following additional features:
  * Templates Plugin
    * Reads file annotations to apply templates. `@theme com.chiorichan.theme.a`
    * Common and relative head file. `imclude.common` or `theme.a` -> `include.a`
    * Set page title. `@title Repositories`
    * Make exception pages pretty and easy to debug
    * And much more!
  * Dropbox Plugin
    * Implements the Dropbox API
  * ACME Plugin, i.e., Let's Encrypt
    * Issues and Renews SSL Certificates from the Let's Encrypt CA
  * Email Plugin
    * Allows easy emailing using the JavaSE Email API
  * ZXing Plugin
    * Implements the barcode rendering libraries by ZXing
* Released under the Mozilla Public License, version 2.0.
* And so much more!

**Planned Future Features, including but not limited to:**
* Server Clustering Abilities.
* Docker Deployable Image.
* Sandbox Mode, implement the Java SecurityManager.
* Remote Plugin Repository.
* Administration Web Interface.

## Seeking Help
Hello, my name is Chiori-chan and I'm currently the sole developer of **Chiori-chan's Web Server**. Recently my project has turned four years old, has just reached a little over 62,000 lines of code, and 700 commits, which has been a real personal accomplishment. Sadly, this means the project has also become a bit too much for me to handle, as many basic features have suffered and I find myself bug fixing and improving features that I only use personally. I find myself dedicating a part-time jobs worth of time trying to keep this project's development moving forward. This means development of **Chiori-chan's Web Server** is slow and very tedious. This also lack a whole lot of time to dedicate to my other projects, like the full-time business I run with my wife. The truth is, I need project contributors and beta testers. So I ask, if anyone reading this is interested in contributing, please contact me.

## Additional Resources

[Version History](/version-history.md)

## How To Build
You can either build Chiori-chan's Web Server with Eclipse IDE or using Gradle. It should be as simple as executing "./gradlew build" for linux users. Some plugins will also compile but you will have to execute "./gradlew :EmailPlugin:build" to build it. If built with Gradle, you will find the built files inside the "build/dest" directory.

## Coding
Our Gradle enviroment uses the CodeStyle plugin to check coding standards.

* Please attempt at making your code as easily understandable as possible.
* Leave comments whenever possible. Adding Javadoc is even more appreciated when possible.
* No spaces; use tabs. We like our tabs, sorry.
* No trailing whitespace.
* Brackets should always be on a new line.
* No 80 column limit or 'weird' midstatement newlines, try to keep your entire statement on one line.

## Pull Request Conventions
* The number of commits in a pull request should be kept to a minimum (squish them into one most of the time - use common sense!).
* No merges should be included in pull requests unless the pull request's purpose is a merge.
* Pull requests should be tested (does it compile? AND does it work?) before submission.
* Any major additions should have documentation ready and provided if applicable (this is usually the case).
* Most pull requests should be accompanied by a corresponding GitHub ticket so we can associate commits with GitHub issues (this is primarily for changelog generation).

## License

**Chiori-chan's Web Server** is licensed under the MIT License (MIT). While you're not obligated, if you decide to use our server or any part of our code (in part or whole), we would love to hear about it. We love to hear what people do with our works.

* Copyright \(c\) 2017 Penoaks Publishing LLC <development@chiorichan.com>
* Copyright \(c\) 2017 Chiori-chan, a.k.a., Joel Greene <me@chiorichan.com>

Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
