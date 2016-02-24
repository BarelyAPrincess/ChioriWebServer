# Application Manager and it's API

Chiori-chan's Web Server features a manager API that accessible through the `AppManager` via `AppManager.manager( Manager.class )`. This allows application managers such as `SiteManager`, `PermissionManager`, `AccountManager`, etc. to maintain an instance of the manager with the service.

## Getting an Instance of a Registered Manager

Getting an instance is as easy as calling the `instance()` method of AppManager.

```java
AppManager.manager( YourManager.class ).instance();
```

The `instance()` method will throw a `IllegalStateException` if the manager has yet to be initialized. It is important that you don't call the `init()` method in these cases, this would be considered bad programming practice. Instead investigate why the `init()` method was never called, as it should be called by the `AppLoader` or if it's a custom manager, by the Plugin `onLoad()` method. See the following implementation section for more.

***Sidenote*** Built-in managers have a quick `instance()` method available so there is no need to call the `AppManager` directly, e.g., `PermissionManager.instance()`.

## Inplementation

To register your Manager with this API add the following to your Plugin `onEnable()` method:
```java
@Override
public void onLoad() throws PluginException
{
    AppManager.manager( YourManager.class ).init();
}
```

Then within your manager class implement the `ServiceManager` interface.

```java
public class YourManager implements ServiceManager
{
    @Override
    public void init()
    {
        // Called once your onLoad method initializes the manager
    }
}
```

To help make implementation easier, we suggest adding the following method to your manager, but relying directly on the `AppManager` or having other ways of caching an instance are both also acceptable.

```java
public static YourManager instance()
{
	return AppManager.manager( YourManager.class ).instance();
}
```

***Sidenote*** The Application Manager is a feature built into the Chiori API, so if implementing the Chiori API, you will need to add the manager initialization code snippet to an event listener method instead, typically within your Loader class:

```java
public class YourLoader extends AppLoader
{
    public static void main( String... args ) throws Exception
	{
		init( YourLoader.class, args );
	}

    @Override
    @EventHandler( priority = EventPriority.NORMAL )
    public void onRunlevelChange( RunlevelEvent event ) throws ApplicationException
    {
        if ( event.getRunLevel() == Runlevel.POSTSTARTUP )
            AppManager.manager( YourManager.class ).init();
    }
}
```