# uBlog

This application is yet again a blog implementation from me. It's a simple application that is able to render markdown entries into HTML with a simple URL logic.

## Philosophy

This application has only a minimal amount of dependencies. A dependency injection framework, mustache template rendering, markdown template rendering and an H2 DB library. I wanted to try a really minimalistic application with some really low footprint. Hence I'm using Java Sockets to handle HTTP communication instead of a Web- or Application Server. The markdown and the H2 DB libraries are not mine, I didn't have the time to make one (yet). The other two I trust regarding memory usage and performance.

At the moment the app is running with an 8MB heap space setting and rendering markdown pages in about 1ms on my local machine, and about 70-100ms on my small cloud server. I'm satisfied.

## Features

There is not much going on (yet). You can list a folder with other folders and Markdown files. There is only one level of categorization possible. Settings work either via environment variables or via a property file. When nothing is set, there is a fallback to a default value. There is no magic going on. The only pages, which are not explicictly listed are the `Index.md` and `Error.md`. The Markdown files in the root folder are listed to the right side in the menu and the folders (categories) are listed to the left side.

Templates are in a separate folder and standard Mustache is supported. 

Database currently is not used.