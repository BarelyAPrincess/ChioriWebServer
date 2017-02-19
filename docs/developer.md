# For Developers



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





