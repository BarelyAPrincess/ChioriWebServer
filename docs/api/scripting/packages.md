# Packages
**Chiori-chan's Web Server** offers a strong package system available for webroots for easy file includes. Packages are files stored exclusively with the intend of being included within many various pages. Packages are heavily utilized by the Templates Plugin. Following the server directory structure conventions, the packages directory is named `resource` and located within the root level of each webroot. Sub-directory structure follows the JLS namespace conventions, e.g., `com.example.*` or `io.john.*`. Treating each period as a file separator, the package `net.organization.widgets.sidebar` would be located at `resource/net/organization/widgets/sidebar.*`. The final part of each namespace will be interpreted as the file name without the file extension, the server will do it's best to select the file based on a list of preferred extensions found in configuration. As of version 9.3 (Milky Polkadot) there is no way of explicitly specifying the extension.

Using the Scripting API, packages are included using the `Object include( String package )` and `Object require( String package )` methods. See [API Reference](api.md) for exact API.
Do note that `require` will halt script execution if a problem was encountered including the package, while `include` will ignore such problems. `require` and `include` will return the requested package as an object, e.g., `HTML` files will return as a String and Scripts will return the last object referenced or explicitly returned. If you desire to output to object directly to the buffer use `print`, `println`, `echo`, or `getResponse().write()` (preferred for writing byte arrays)

### Using a Package as an API
Each package script is executed as it's own Java Class, utilizing the ability to return an object, place 'return this' at the end of your script and implement each method you will need.

Package Script Example:
```groovy
def sendEmail( addr )
{
	// Code Here
}

def sayHello()
{
	return "Hello World";
}

return this;
```

Use Case:
```groovy
def api = require( "com.example.api.messaging" );
api.sendEmail( "norm@example.com" );
println api.sayHello();
```