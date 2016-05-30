# Routing

Also known as the route file. It is stored in the site webroot and is referenced by the server for determining URL rewrites. When a pattern directive matches the server will follow either a redirect or rewrite action.

**Rewrite action:** `pattern "/some/basic/url", file "/some/other/path/file.html"`.

**Redirect action:** `pattern "/old/site/url", redirect "/the/new/path"`.

When following a redirect action, you can define the status code used by also adding directive `status`. Like so: `pattern "/old/site/url", redirect "/the/new/path", status 301`.

### Subdomains

The normal pattern argument can only parse the url that follows the domain TLD. To better match, use the directive `subdomain` to explicitly state which subdomain this pattern is for.

`pattern "/accounts/my", file "/users/my.gsp", subdomain "users"`.

Leaving the subdomain directive empty will result in only matching the root domain.

### Capturing Arguments

The routing feature can also capture dynamic argument from within the url. To define just replace any section of the url with `[arg=]`. The wording between the brackets and equal symbol is your argument name.

`pattern "/projects/[projId=]/users/[userId=]/add", file "scripts/projects/adduser.groovy", subdomain ""`.

As you can see from the above example, the arguments `projId` and `userId` will pass into the `adduser.groovy` script as a list. To retrieve these values using the Groovy API, add the following: `_REWRITE.projId` or `_REWRITE["userId"]`.

### vargs

But say you have a script that can handle different actions but you are unable to capture this from the URL. Then this is what the `vargs` directive is intended for, defined as follows:

`pattern "/projects/[projId=]/users/[userId=]/add", file "scripts/projects.groovy", subdomain "", vargs [action: addUser]`.

For some more information on vargs, see [Controllers](/docs/scripting/controllers.md).