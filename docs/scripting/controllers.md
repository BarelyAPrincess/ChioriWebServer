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

While the non-ambiguous methods are non-case sensitive, actions will pass to the catch all method as-is.


### Using Route File

One positive benefit of using controllers is the ability to route requests using the built-in Route File. Much like routing to a normal file, controllers work the same way.

Add the following line to your Routes file in the site root.
`pattern "/admin/projects/[action=]", file "/scripts/projects.controller.groovy", subdomain ""`