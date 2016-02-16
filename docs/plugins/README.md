# Plugins
## Official Plugins

### Let's Encrypt (Acme) Plugin
Allows the Web Server to auto manage SSL certificates issued by the Let's Encrypt Certificate Authority. [Let's Encrypt](https://letsencrypt.org/)

### Dropbox Plugin


### Templates Plugin

### Email Plugin

### ZXing Plugin
Implements the barcode rendering libraries by ZXing into the Server API.

    if ( getPluginByName( "ZXing Plugin" ) == null )
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
Implements the Interactive Console found in older versions of Chiori-chan's Web Server. Originally intended to serve as an example how to load Native Libraries using the built-in plugin feature found in `config.yaml`.

### [WIP] Lua Plugin
Implements the Lua Programming Language as a Server Scripting Language. Also associates the `.lua` file extension with the new Scripting Engine.