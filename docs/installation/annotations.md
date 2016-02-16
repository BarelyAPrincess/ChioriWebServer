# File Annotations

File Annotations allows you to fine tune the way a file is handled by the server. Annotaions are commonly applied by placing a key and value pair (@key value) within the very first lines of any file, including images. They can also be applied thru SQL routes or thru the Scripting API, e.g., getResponse().setAnnotation(key, value);, and vise-verse reading annotations with, e.g., getResponse().getAnnotation(key);. Keep in mind, not all annotations applied using the Scripting API will be detected as the time they are used by the server has already pasted.

* ssl \(required, ignore, deny\)

	Restricts the server to which protocols it may serve the file over. If say an end user requests a page with the annotaion '@ssl required' over an unsecure connection, i.e., http://, the server will automatically redirect the request to https. If such protocol is not enabled, the server will respond with a FORBIDDEN error. The same is true if '@ssl deny' is set and a request if made over a secure connection but this is only provided as yin and yang and probably should never be used.

### Template Plugin Annotaions
The following are excludely used by the Templates Plugin.

* theme \[package\]

	Sets the theme package to use for this file upon request, page content is placed
at the pagedata marker specified within plugin configuration.

* themeless

	Forces the Templates Plugin to NEVER render this page with a theme.

* noCommons

	Forces the Templates Plugin to not automatically add the common includes to the head tag.

* header \[string\]

	Includes this file after the beginning of the html tag of the page.

* footer \[string\]

	Includes this file at the end of the page before the end of the html tag.