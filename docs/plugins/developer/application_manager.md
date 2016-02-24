# Application Manager and it's API

Chiori-chan's Web Server features a manager API that accessible through the `AppManager` via `AppManager mgr = AppManager.manager( Manager.class )`. This allows application managers such as `SiteManager`, `PermissionManager`, `AccountManager`, etc. to maintain an instance of the manager with the service.

## Plugin Inplementation

To register your Manager with this API add the following to your Plugin `onEnable()` method:
```
@Override
public void onLoad() throws PluginException
{
    AppManager.manager( YourManager.class ).init();
}
```

Then within your manager class implement the `ServiceManager` interface. 