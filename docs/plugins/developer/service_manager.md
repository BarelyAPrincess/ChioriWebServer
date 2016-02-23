# Service Manager

Chiori-chan's Web Server features a service API that accessible through the AppController via `AppController.getService( PluginAPI.class )`. It allows for multiple plugins (including the core API) to register their support for cross-plugin services, intended to greatly reduce the amount APIs plugin developers have to write support for. The concept is based on the original Service Manager from Bukkit by sk89q.

The Services API depends heavily upon the concepts of services and providers. A service is usually an interface that providers can implement. Service are only registered with the `AppController` alongside providers. The Services API was written for multiple plugins implementing a single interface, though a plugin can register the same class as both a service and a provider.

The Site Manager (inherits from the Location Service found in the Chiori API) registers it self as a Service for the `com.chiorichan.account.AccountLocation` class with the following:
```Java
AppController.registerService( Site.class, (SiteManager) this, new ObjectContext( this ), ServicePriority.Normal );
```

* `com.chiorichan.site.Site` extends `com.chiorichan.account.AccountLocation`
* `com.chiorichan.site.SiteManager`, is the provider of `com.chiorichan.account.AccountLocation` instances
* As covered in another tutorial, `com.chiorichan.services.ObjectContext` provides context to an action, such as plugin, name, author, etc. In this case, it's reporting that `SiteManager` is the creating force behind this service registration. If a problem was to incur, the server would report that `Chiori-chan` was the code author.
* `com.chiorichan.services.ServicePriority` would be the level of priority to this registration. `ServicePriotity.Normal` is recommended unless you intentionally need to override another registration.