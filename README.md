# uBlog

This application is yet again a blog implementation from me. It's a simple application that is able to render markdown entries into HTML with a simple URL logic.

## Philosophy

This application has only a minimal amount of dependencies. A dependency injection framework, mustache template rendering, markdown template rendering and an H2 DB library. I wanted to try a really minimalistic application with some really low footprint. Hence I'm using Java Sockets to handle HTTP communication instead of a Web- or Application Server. The markdown and the H2 DB libraries are not mine, I didn't have the time to make one (yet). The other two I trust regarding memory usage and performance.

At the moment the app is running with an 8MB heap space setting and rendering markdown pages in about 1ms on my local machine, and about 70-100ms on my small cloud server. I'm satisfied.

## Features

There is not much going on (yet). You can list a folder with other folders and Markdown files. There is only one level of categorization possible. Settings work either via environment variables or via a property file. When nothing is set, there is a fallback to a default value. There is no magic going on. The only pages, which are not explicictly listed are the `Index.md` and `Error.md`. The Markdown files in the root folder are listed to the right side in the menu and the folders (categories) are listed to the left side.

Templates are in a separate folder and standard Mustache is supported. 

Database currently is not used.

An example of the configuration is present in the `run.sh` script. The `pom.xml` builds both a library JAR file and a fat JAR (containing every library as well). 

### Database settings (via environment variables)

`DB_USERNAME` sets the DB user.

`DB_PASSWORD` sets the DB password.

`DB_FILE_PATH` path to the DB file. It can be a relative path. We are using H2 in single file mode here.

`H2_WEB_PORT` port for the H2 web console. Not activated by default.

### Templating configuration

`MUSTACHE_TEMPLATES_CACHING_ENABLED` this feature when enabled, compiles the templates once and then uses them over and over, thus making the rendering faster.

`export MUSTACHE_TEMPLATES_FOLDER` the folder of where the templates are to be found.

### Technical settings

`SERVER_PORT` the port the server is listening to.

`SOCKET_HANDLER_THREAD_POOL_SIZE` the size of the thread pool for the request handlers. By default is set to 10 threads.

`TARGET_FOLDER` the folder where the markdown files are hosted. For example `/var/www/markdown`.