# Controllers

Similar to controllers found in Model-View-Controller frameworks. Unlike most other frameworks, ***Chiori-chan's Web Server*** controllers can be anyplace in the url structure. Controllers are parsed lastly in the request handler chain, meaning the file `/directory/controller/ARealFile` will be parsed before `/directory/controller/action` is. Controllers are also compatible with Routes.

**Developer Note**: Controllers are initialized by the request handler chain but it's the job of the `ScriptingEngine` to make the proper action method calls. As such the only known scripting engine implementing controllers is the `GroovyEngine`, as such groovy controllers must end in `.groovy`.

## How to Create a Controller

1. First create a new file ending with the extension `.controller.groovy`. This is your controller file and can also be named `index.controller.groovy` to act as a directory based controller.

2. Actions can be several directories deep, as such to define an explicit method action, you must first convert the action to CamelCase (Uppercase character following each url separator.) and then append the word action.

```groovy
  def actionUserAdd() {
    // Handle Here
  }
  
  def actionUserRemove() {
    // Handle Here
  }

  def actionEdit() {
    // Handle Here
  }
  
  def actionDelete() {
    // Handle Here
  }
```

3. In cases where explicit actions you also have the option define a catch all method. Just remember to return `true` if the provided action was handled, so the server knows to not a HTTP 404.

```groovy
  def catchAll( action ) {
    /* As previously specified, controllers don't just handle single layer deep actions. So to avoid this, throw HttpError 404 or return `false` when the action contains a forward-slash (/) */
    if ( action.contains("/") )
      throw new HttpError( 404 );

    /* Actions are passed exactly as typed in the url and are case-sensitive */
    switch ( action.toLowerCase() )
    {
      case "deleteall":
        // Handle
        return true;
      case "destroy":
        // Sending a relative redirect will change the controller action.
        getResponse().sendRedirect( "delete" );
        return true;
      default:
        getResponse().sendRedirect( "index" );
        return true;
    }
  }
```

If your controller contains no action methods, (including no catch all) the server will always throw a `SiteConfigurationException`. If a catch all does exist and doesn't return `true`, a HTTP 404 will be thrown instead, that is unless your in development mode, which will report the action went unhandled.

### Using Route File

Controllers are also compatible with the Routes feature. You only need to manually capture the action argument or you can define the action using the `vargs` directive.

### Using vargs

```json
{pattern: "/admin/projects/[projId=]/edit", file "/scripts/projects.controller.groovy", vargs: [action:edit]}
```

### Using Capture

```json
{pattern: "/admin/projects/[projId=]/user/[action=]", file: "/scripts/projects.controller.groovy"}
```

See [Routing](/docs/configuration/routing.html) for more help with the Route File.