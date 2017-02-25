# Annotations

Annotations allow you to fine tune the way any file or request is handled via a simple key and value placed in the first few lines of any file, including images. \(That is if you don't care to read the images locally.\) Annotations are based on the same annotations that can be found in CSS files, which shouldn't be a problem since CSS doesn't share any similar annotations. If annotations need to be dynamic or placing them statically in each file is not convenient, they can also be set \(and get\) via the scripting API:

```groovy
/* Set Annotation Value */
getResponse().setAnnotation("key", "value");
/* Get Annotation Value */
getResponse().getAnnotation("key");
```

**Note** Keep in mind that there is always a risk that setting annotations via the scripting API, would go undetected by relevant server subsystems. Acceptable examples to this are the `ssl`, `nonce`, and `trailingSlash` annotations.

## Server Annotations

`@ssl` - Restricts the server to which protocol it can serve the file over.

* `Preferred` - If the server is running HTTPS, the server will make the redirect.
* `PostOnly` - Will force SSL if the request made is POST.
* `GetOnly` - Will force SSL if the request made is GET.
* `Required` - Will force SSL always and will throw FORBIDDEN if HTTPS is unavailable.
* `Deny` - Will force NON-SSL always.
* `Ignore` - Default

`@trailingSlash` - Defines if a request URL should or should-not should not end with a trailing slash.

* `Always` - If the url has no trailing slash, a 301 redirect to with a slash is made.
* `Never` - If the url has a trailing slash, a 301 redirect to remove the trailing slash is made.
* `Ignore` - Default

**Note** If the request is made on a directory, index file, or controller \(without action\), `trailingSlash` defaults to `Always`.

`@nonce` - Defines if the server should be checking the NONCE. See NONCE section for more information.

* `Required` - Check NONCE and fail if NONCE was not provided.
* `GetOnly` - Check NONCE on GET requests only.
* `PostOnly` - Check NONCE on POST requests only.
* `Flexible` - Only check NONCE if NONCE was provided.
* `Disabled` - Default

**Note** NONCE is always required by the login subroutine and does not effect the value set in the annotation.

`@reqlogin` - Forces the user to have a valid login, violators are redirecting to the site login page.

* `Boolean` - True/False

`@reqperm` - Forces the user to have a specified permissions before the page is rendered.

* `String` - Period Separated Permission Namespace

`@shell` - Forces which interpreter to use, ignores the file extension.

* `String` - Interpreter Name
  * `embedded` - Embedded Groovy Engine
  * `groovy` - Groovy Engine
  * `less` - LESS to CSS engine
  * `sass` - SASS to CSS engine
  * `html` or `text` - Default

`@encoding` - Forces which encoding to delivery the file in. Defaults to `UTF-8` for files and `ISO-8859-1` for images.

* `String` - Standard Encoding Name

`@contentType` - Forces the returned content type.

* `String` - Standard MIME Type

### Template Plugin Annotations

The following are exclusively used by the Templates Plugin and normally can be set by the Scripting API without issue.

`@title` - The page title, prepended to the site title, e.g., `Index Page - Example Corporation`

* `String` - Standard String

`@theme` - The page theme. See Templates section for more information.

* `String` - Period Separated Resource Namespace

`@view` - The page view. See Templates section for more information.

* `String` - Period Separated Resource Namespace

`@themeless` - Prevent the Templates Plugin from rendering the page theme. Overrides the plugin option `config.alwaysRender`.

* `Boolean` - True/False

`@noCommons` - Prevent the page from placing the commons resource in the HTML head.

* `Boolean` - True/False

`@header` - Places this file in the HTML head.

* `String` - Period Separated Resource Namespace

`@footer` - Places this file before the final HTML body.

* `String` - Period Separated Resource Namespace





