# API Reference
## Variable and Type Related Methods
### `int count( map,collection,array,string )`
Counts all elements in a map, collection, array, or string

```php
print count( obj )
```

Output
* `[key1: "val1", key2: "val2"]` -> `2`
* `["obj1", "obj2", "obj3"]` -> `3`
* `"This is a test string!"` -> `22`
    
### `boolean is_array( object )`
Determines whether an object is an instance of array or collection

```php
def var = ["obj1", "obj2", "obj3"]
print is_array( var )
```

Output `True`

```php
def var = "This is a test string!"
print is_array( var )
```

Output `False`

### `boolean is_float( object )`
Determines whether an object is an instance of Float

* `print is_float( "abc" )` -> `False`
* `print is_float( 23 )` -> `False`
* `print is_float( 23.5 )` -> `True`
* `print is_float( True )` -> `False`

### `boolean is_int( object )`
Determines whether an object is an instance of Integer

* `print is_int( 23 )` -> `True`
* `print is_int( "23" )` -> `False`
* `print is_int( 23.5 )` -> `False`
* `print is_int( True )` -> `False`
* `print is_int( False )` -> `False`

### `String base64Encode( bytes,String )`
Encodes the specified argument into a String using the Base64 encoding scheme.

* `print base64Encode( "This is a test string!" )` -> `VGhpcyBpcyBhIHRlc3Qgc3RyaW5nIQ==`
* `print base64Encode( "Any type of byte array".getBytes() )` -> `Ynl0ZSBhcnJheQ==`

