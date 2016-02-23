# Services Manager and it's API

Chiori-chan's Web Server features a service API that accessible through the AppController via `AppController.getService( PluginAPI.class )`. It allows for multiple plugins (including the core API) to register their support for cross-plugin services, intended to greatly reduce the amount APIs plugin developers have to write support for. The concept is based on the original Service Manager from Bukkit by sk89q.

The Services API depends heavily upon the concepts of services and providers. A service is usually an interface that providers can implement. Service are only registered with the `AppController` alongside providers. The Services API was written for multiple plugins implementing a single interface, though a plugin can register the same class as both a service and a provider.

The Site Manager (inherits from the Location Service found in the Chiori API) registers it self as a Service for the `com.chiorichan.account.AccountLocation` class with the following:
```Java
AppController.registerService( AccountLocation.class, (SiteManager) this, new ObjectContext( this ), ServicePriority.Normal );
```

* `com.chiorichan.site.Site` extends `com.chiorichan.account.AccountLocation`
* `com.chiorichan.site.SiteManager`, is the provider of `com.chiorichan.account.AccountLocation` instances
* As covered in another tutorial, `com.chiorichan.services.ObjectContext` provides context to an action, such as plugin, name, author, etc. In this case, it's reporting that `SiteManager` is the creating force behind this service registration. If a problem was to incur, the server would report that `Chiori-chan` was the code author.
* `com.chiorichan.services.ServicePriority` would be the level of priority to this registration. `ServicePriotity.Normal` is recommended unless you need to override another registration.

In the above example the `SiteManager` is informing the server that it can provide `AccountLocation` instances. If you as a plugin developer wish to override `SiteManager`'s ability to provide `AccountLocation` instances, you must set your `ServicePriorty` to either `High` or `Highest`.

Likewise, if you need to query for an `AccountLocation` instance, you would do the following:
```Java
public AccountLocation getLocation()
{
	LocationService mgr = AppController.getService( AccountLocation.class ); // Query for Service Provider
	if ( mgr == null ) // No Service Provider is registered!
		return null;
	return mgr.getLocation( locId ); // Make query for AccountLocation from the LocationService API
}
```

***Sidenote*** `AppController.getService( Service )` will throw the `ClassCastException` if the registered service is not an instance of the `LocationService` class for obvious reasons. Also remember that the `AppController.getService( Class )` will return the highest registered Service, you must call `AppController.getServiceList( Class )` or `AppController.getService( Class, ServicePriority )` if you wish to see other registered services.



