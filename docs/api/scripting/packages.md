# Packages
**Chiori-chan's Web Server** offers a strong package system available for webroots for easy file includes. Packages are files stored exclusively with the intend of being included within many various pages. Packages are heavily utilized by the Templates Plugin. Following the server directory structure conventions, the packages directory is named `resource` and located within the root level of each webroot. Sub-directory structure follows the JLS namespace conventions, e.g., `com.example.*` or `io.john.*`. Treat each period as a file separator, so 'com.example.' would be under 'com/example'. The last part of each package would be the file name without the file extension, the server does the work of selecting the best file based on a list of preferred extensions found in configuration. So 'com.example.widgets.menu' could be translated to file 'com/example/widgets/menu.groovy'. Unforchantly there is currently no way of specifying the extension you are expecting.

Thru the Scripting API, you can include and/or require a package path, e.g., require("com.example.widgets.menu");. Using include\(\) will ignore exceptions thrown, while require\(\) will stop execution in the same event, including FileNotFoundException. Please keep in mind that require and include methods will return the packages as objects, html files to return as a String, while scripts will return the last object used or returned. Output directly to buffer using the standard print\(\) and println\(\) methods.

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