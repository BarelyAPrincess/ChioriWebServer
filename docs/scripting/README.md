# Scripting Languages
**Chiori-chan's Web Server** utilizes a proprietary API heavily based on the PHP API for ease of use. Under-the-hood, we utilize the Groovy Shell interpreter, with added support for Groovy Server Pages (GSP). Much like PHP, GSP allows groovy code to be placed within code blocks starting  with `<%` and ending with `%>`.

    You are running <% getProduct() %> <% getVersion() %>
Output `You are running Chiori-chan's Web Server 9.3.6 (Milky Polkadot)`

While Groovy is the default and preferred scripting language, additional scripting languages can be implemented using the provided Plugins API. See [Lua Plugin](plugins/LuaPlugin.md) for more information. Implementing the server API will depend of how each language is implemented, i.e., scripts need to extend the `com.chiorichan.factory.api.Builtin` class.

**Note**: while it is attempted to replicate the PHP API, the PHP syntax was not, so following the Groovy syntax will be a must.

[Groovy Language Syntax](http://groovy-lang.org/syntax.html)

PHP Examples

```php

    $vars = array( "val1", "val2", "val3" )

    foreach( $vars as $var ) { echo $var }

    $var = array("key1" => val1", "key2" => "val2", "key3" => "val3")
    
    foreach( $vars as $key => $var ) {}
```

Those same examples written in Groovy

```groovy
    def vars = ["val1", "val2", "val3"]
    
    vars.each { var -> println var }

    def vars = [key1: "val1", key2: "val2", key3: "val3"]
    
    vars.each { key, var -> println key + ": " + var }
```
