# Controllers

Based on the popular, Model-View-Controllers. Controllers allow you to handle different actions from one a single Script, e.g., `http://example.com/site/[controllerNameWithoutExtension]/[action]`.

When a request fails to find an exact matching file, it will attempt to parse the last part as an action and the prior part as a controller. -- Considering the prior resolves to an actual controller.

1. To start, create a new groovy script under the desired directory with the extension `.controller.groovy`. 
2. To catch specific actions, just add a method like follows:
```groovy
  def actionEdit()
  {
    // Handle Here
  }
  
  def actionDelete()
  {
    // Handle Here
  }
```

3. If you prefer to leave the actions ambiguous, just create a catch all method and return `true` on a valid action, otherwise `false`.
```groovy
  def catchAll( action )
  {
    if ( action.contains("/") )
      // In some cases, actions could contain a subaction, it's your disgression to support them.
      throw new HttpError( 404 );

    // Action will be pass as typed
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

While the non-ambiguous methods are non-case sensitive, actions will pass to the catch all method as-is. When the action fails, i.e., no hard coded action method nor `catchAll` method returning true, the server will throw a 404 (Not Found) error. If you're in development mode, the error will also include a short description.

### Using Route File

Controllers are also compatible using the Route File, you will only need to capture the action argument using the Route formula or by explicitly defining it.

**Explicit**
`pattern "/admin/projects/[id=]/edit", file "/scripts/projects.controller.groovy", vargs [action:edit]`
**Normal**
`pattern "/admin/projects/[projId=]/user/[action=]", file "/scripts/.controller.groovy"`

See [Routes](Routes) for more help with the Route File.