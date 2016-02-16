# Plugins

The Chiori-chan's Web Server features a rich plugin system inspired by the plugin system found in the beloved Minecraft Server, Craftbukkit.

Plugins are located within the `plugins` directory by default and end with file extension `.jar`.

[How to Install Plugins Tutorial by Chiori-chan](https://www.youtube.com/watch?v=HQj2vu0BfI0)

## Official Plugins

### Let's Encrypt (Acme) Plugin
Allows the Web Server to auto manage SSL certificates issued by the Let's Encrypt Certificate Authority. [Let's Encrypt](https://letsencrypt.org/)

### Templates Plugin
Being one of the most well developed Plugins of Chiori-chan's Web Server. Implements an easy to use template formatter subsystem. Using the Server Event Bus, the plugin listens to the page rendering event and wrappers the output with a template and container file.

[Templates Plugin - Part 1 of 3](https://www.youtube.com/watch?v=WDX7gnSVQkg&index=8&list=PL5W-gdSkWP6TOBoL-YDEPZaadwBGXGOyO)
[Templates Plugin - Part 2 of 3](https://www.youtube.com/watch?v=Bbfzw28Vgvk&index=9&list=PL5W-gdSkWP6TOBoL-YDEPZaadwBGXGOyO)
[Templates Plugin - Part 3 of 3](https://www.youtube.com/watch?v=ZkUK2QNAlQw&index=10&list=PL5W-gdSkWP6TOBoL-YDEPZaadwBGXGOyO)

### Dropbox Plugin


### Email Plugin

### ZXing Plugin
Implements the barcode rendering libraries by ZXing into the Server API.

    if ( getPluginByNameWithoutException( "ZXing Plugin" ) == null )
    {
    	throw new HttpError( 500, "It would appear the ZXing Plugin is not loaded." );
    }
    else if ( empty( _REQUEST.barcode ) )
    {
        throw new HttpError( 500, "The argument 'barcode' is missing." );
    }
    else
    {
    	getResponse().setContentType( "image/png" );
    	getResponse().setEncoding( "ISO-8859-1" );
    	getResponse().print( getPluginByName( "ZXing Plugin" ).createQRCodeSimple( _REQUEST.barcode ?: "NULL" ) );
    }


### [WIP] Interactive Console Plugin
Implements the Interactive Console found in older versions of Chiori-chan's Web Server. Originally intended to serve as an example how to load Native Libraries using the built-in plugin feature that can be enabled within the `config.yaml` file.

### [WIP] Lua Plugin
Implements the Lua Programming Language as a Server Scripting Language. Also associates the `.lua` file extension with the new Scripting Engine.

[Lua Plugin Tutorial by Chiori-chan](https://www.youtube.com/watch?v=_VJoMV77GHU)