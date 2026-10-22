# uBlog

uBlog is a small Java application that serves a folder of Markdown files as a blog. It uses a simple URL scheme, renders pages to HTML, and keeps content, templates, user properties, and images in filesystem folders.

## Philosophy

The project favors a small footprint and few dependencies over a conventional web stack. HTTP is handled directly with Java sockets rather than a web or application server. Markdown rendering and Mustache templates provide the presentation layer; the remaining application code is intentionally straightforward. Configuration has explicit defaults, and there is no database in the current runtime path.

The container starts the JVM with an 8 MB initial and maximum heap. That is a deliberately small starting point, not a performance guarantee: actual memory needs depend on the content, traffic, and runtime environment.

## Current State

The application reads Markdown files from a root folder and its immediate category folders. Root-level entries appear in the menu, categories appear separately, and `Index.md` and `Error.md` are used for their special pages. The Mustache templates used to render the UI live under `template/`. Currently the final template must be set. Templates can be customized by a `theme.properties` file. If present, the properties are editable for the publisher via the user settings.

Users are loaded from property files in the user folder. Authenticated editing can create categories and entries and update existing entries, so the content and image folders must be writable for those operations. Password updates can also write to the user folder. The name of the users property file is used as the login username. The password can be set as plaintext initially and is encrpyted upon first authentication by the user. The password can be changed by the authenticated user via the user settings.

The Maven build produces a regular JAR and a shaded `fat` JAR. The `Containerfile` builds and runs the shaded JAR. The database-related environment variables documented previously are not used by the current application configuration.

## Configuration

Environment variables take precedence over values in `app.properties` for settings handled by the application configuration. The corresponding property keys use lowercase dotted names (for example, `SERVER_PORT` maps to `server.port`). `SESSION_TIMEOUT` is read directly from the environment and is not read from `app.properties`. Paths are relative to the process working directory unless absolute paths are used.

The following are the runtime environment variables used by the application. Defaults below are the values set by the `Containerfile` (the application may use the alternate code default when run outside the container).

| Variable | Container default | Purpose |
| --- | --- | --- |
| `SERVER_PORT` | `8080` | TCP port the HTTP server listens on inside the container. |
| `BLOG_FOLDER` | `/app/blogFolder` | Markdown content root. The app may create categories and entries here. |
| `USER_FOLDER` | `/app/userFolder` | User `.properties` files. Password updates write to this folder. |
| `IMAGE_FOLDER` | `/app/imageFolder` | Folder for files served under `/image/`. |
| `MUSTACHE_TEMPLATES_FOLDER` | `/app/template/personal` | Mustache templates and optional `strings.properties`. |
| `MUSTACHE_TEMPLATES_CACHING_ENABLED` | `true` | Cache compiled templates after first use. |
| `MUSTACHE_TEMPLATE_UPDATE` | `false` | Pass template-update mode to the Mustache template loader. |
| `SESSION_TIMEOUT` | `60` | Session idle timeout in minutes. |
| `SESSION_COOKIE_SECURE` | `false` | Set the session cookie's `Secure` attribute. Enable when HTTPS is provided by a reverse proxy. |
| `SOCKET_HANDLER_THREAD_POOL_SIZE` | `10` | Number of request-handler threads. |
| `SOCKET_HANDLER_QUEUE_CAPACITY` | `100` | Maximum queued connections; excess connections receive HTTP 503. |

For a local run, `./run.sh` sets the template and data folders to the repository's `template/` and `testContent/` directories. Use `./run.sh --help` to see its options.

User management is done via property files placed in the `USER_FOLDER`. The name of the file is the username. Content is straight forward:

```properties
password=mypassword
name=John Doe
status=active
```

The password can be entered plain (as in the example). Upon first login the password is encrypted. `status=active` is used to check if the user is allowed to log in. 

## Container

Build the image from the repository root:

```sh
docker build -f Containerfile -t ublog:local .
```

The image includes default templates and sample blog and user files. Run it with the HTTP port published:

```sh
docker run --rm --name ublog -p 8080:8080 ublog:local
```

To use host-managed folders instead, create the folders and seed the content and user files as needed. The content and user mounts below are writable because editing and password updates can modify them; the image mount is read-only because images are only served:

```sh
mkdir -p content users images
cp -a testContent/blogFolder/. content/
cp -a testContent/userFolder/. users/

docker run --rm --name ublog -p 8080:8080 \
	-e BLOG_FOLDER=/data/content \
	-e USER_FOLDER=/data/users \
	-e IMAGE_FOLDER=/data/images \
	-v "$PWD/content:/data/content:rw" \
	-v "$PWD/users:/data/users:rw" \
	-v "$PWD/images:/data/images:ro" \
	ublog:local
```

Open `http://localhost:8080`. Put Markdown files and category directories in `content/`, user property files in `users/`, and image files in `images/`. If serving through an HTTPS reverse proxy, set `SESSION_COOKIE_SECURE=true`; the proxy should forward requests to the container's HTTP port. For a non-default server port, set `SERVER_PORT` and publish that same container port.

## Template development

Third party templates are supported. Mustache is used to render the given templates.