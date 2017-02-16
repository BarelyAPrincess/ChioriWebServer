# Routing File

Each Site has an independent routing file located in the site webroot named route and will either end with `.json` or `.yaml`.

**Developer Note**: As you can tell from the extension, the routing file can be in either YAML or JSON format. Also, the server has no preference over which format to use, both files will be combined when read with YAML coming second. This means rules in the JSON will get checked first when the server looks for a route.

## JSON Example

Each line contained within the JSON routing file, will be read as an individual rule, as such each line is it's own JSON Object, no need to wrap all the lines in a  JSON Array.

```json
{pattern: "/some/basic/url", file: "/the/file/to/read/location.html"}
{pattern: "/some/other/url", redirect: "http://google.com"}
```

## YAML Example

Rules contained within the YAML routing file are branched out like typical YAML format, also using the two space indent.

```yaml
rule1:
  pattern: "/some/basic/url"
  file: "/the/file/to/read/location.html"
rule2:
  pattern: "/some/other/url"
  redirect: "http://google.com"
```

Also note that the key names `rule1` or `rule2`, are what's considered rule ids and will be cover in depth here further down. The JSON routing file will need these specified with the key `id`, if you so choose to utilize this feature.

**Note**: For simplicity, examples will be given in the JSON format from here on.

## Routing Actions

As demonstrated in the examples above, a routing rule can have multiple outcomes and are as follows:

* `file` (a.k.a. rewrite) - Will produce a response using the specified file location.
* `redirect` - Will redirect the page request to the specified url using HTTP code 301. Urls not starting with `http` will become relative to the current full domain. If you wish to define the HTTP code used, also specify the key `status`, e.g., `{pattern: "/some/other/url", redirect: "http://google.com", status: 302}`.

## Pattern Arguments

As an added advanced feature, the pattern key can also be used to capture arguments from the url. Just replace any section of the specified url pattern with `[arg=]` and the server will do the rest. Whatever string between the brackets and equal symbol is your argument name.

```json
{pattern: "/projects/[projId=]/users/[userId=]/add", file: "scripts/projects/adduser.groovy"}
```

As you can see from the above example, the arguments `projId` and `userId` will be passed into the `adduser.groovy` script. Unlike GET and POST arguments, these argument will be made available through the `_REWRITE` and `_REQUEST` (if enabled) maps. Retrieving in Groovy script is as easy as `_REWRITE.projId` or `_REWRITE["userId"]`.

## Specify Rewrite Arguments

Let's say you have a groovy script that can handle actions and obviously you would be unable to capture the action argument from the pattern. This is where the `vargs` key comes in handy.

#### JSON
```json
{pattern: "/projects/[projId=]/users/[userId=]/add", file: "scripts/projects.controller.groovy", vargs: {"action": "addUser"}}
```

#### YAML
```yaml
rule1:
  pattern: "/projects/[projId=]/users/[userId=]/add"
  file: "scripts/projects.controller.groovy"
  vargs:
    action: "addUser"
```

You will notice the example above, the groovy script ends in `.controller.groovy`; While vargs can passed to any script, in this example the controller feature is optimal for this sort of thing.

For some more information on controllers, see [Controllers](/docs/scripting/controllers.md).

## Host Matches

For more refined rule matching, you can also match the host using regular expressions.

```json
{pattern: "/some/basic/url", host: "^site[0-9].example.com$", file: "/the/file/to/read/location.html"}
```

## Route URL

So far all this might look great but you could by thinking, "What if I want to retrieve the route and print it as a hyperlink". Well, we got you covered. This is where the rule id mentioned earlier comes in.

```json
{id: "project_rule1", ...}
```

From your groovy or gsp script, call the built-in method `route_id( "project_rule1" )` and the server will compute a URL from the route pattern.

**Developer Note**: Keep in mind that if your pattern contains rewrite arguments, the `route_id` method will throw a `SiteConfigurationException`, with the message `Route param [projId] went unspecified for id [project_rule1], pattern [{id: "project_rule1", ...}]`. You can specify the pattern arguments using the optional second parameter to `route_id` like so:

```groovy
route_id( "project_rule1", [projId: "bobsyouruncle", userId: "joe15"] )
```

By default, the server will attempt to use the `host` key as the domain name as long as it matches the regex `\^{0,1}[a-zA-Z0-9.]+\${0,1}`. If you find the host is considered invalid or is causing undesired outcome, you can specify the key `domain` to override, `{..., domain: "http://mysite1.example.com", host: "^mysite[0-9].example.com$", ...}`, leaving the domain value empty will force `route_id` to use the current full domain instead.

Lastly, let's say you'd like to print a route by id but have no need for the routing feature. To do so you can simply specify a blank route with the keys `id` and `url`, like so:

```json
{id: "route_rule1", url: "http://example.com/"}
```
