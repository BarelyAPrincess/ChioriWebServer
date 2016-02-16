# Introduction
**Chiori-chan's Web Server** is a HTTP/TCP Web Server allowing for both dynamic and static content delivered to both browsers and TCP clients. To provide flexibility, the server also includes a powerful Groovy Scripting Language. The Scripting Engine is also extendable using a provided API. Is the Groovy language not your thing, try our extensive Plugin API loosly based on the ever popular CraftBukkit Minecraft Server API. Chiori-chan's Web Server could be considered an Application Server as it gives you the power to create amazing web applications while taking less time and resources, while at the same time utilizing the power of the Java Virtual Machine.

Please read our official documentation located at http://docs.chiorichan.com/. It contains great advanced information and tutorials on how to use Chiori-chan's Web Server.

# How To Build
You can either build Chiori-chan's Web Server using the Eclipse IDE or preferably by using the Gradle Build System. It should be as simple as executing "./gradlew build" for linux users. Gradle will output the built files to the `build/dest` directory.

# Coding
Our Gradle Build environment uses the CodeStyle plugin to check coding standards incase you make mistakes.

* Please attempt at making your code as easily understandable as possible.
* Leave comments whenever possible. Adding Javadoc is even more appreciated when possible.
* No spaces; use tabs. We like our tabs, sorry.
* No trailing whitespace.
* Brackets should always be on a new line.
* No 80 column limit or 'weird' midstatement newlines, try to keep your entire statement on one line.

# Pull Request Conventions
* The number of commits in a pull request should be kept to a minimum (squish them into one most of the time - use common sense!).
* No merges should be included in pull requests unless the pull request's purpose is a merge.
* Pull requests should be tested (does it compile? AND does it work?) before submission.
* Any major additions should have documentation ready and provided if applicable (this is usually the case).
* Most pull requests should be accompanied by a corresponding GitHub ticket so we can associate commits with GitHub issues (this is primarily for changelog generation).

# License
Chiori Web Server is licensed under the [Mozila Public License Version 2.0](https://www.mozilla.org/en-US/MPL/2.0/). If you decide to use our server or use any of our code (In part or whole), PLEASE, we would love to hear about it. We don't require this but it's generally cool to hear what others do with our stuff.

\(C) 2015 Greenetree LLC, Chiori-chan's Web Server.
