
#!/usr/bin/env bash
set -euo pipefail

ENGINE="${ENGINE:-podman}"
IMAGE="docker.io/ublog/ublog:1.0.0"
CONTAINER="ublog"
BASE_DIR="${BASE_DIR:-$PWD/ublog-data}"

# 1. Create directories
mkdir -p \
    "$BASE_DIR/template" \
    "$BASE_DIR/blog" \
    "$BASE_DIR/image" \
    "$BASE_DIR/file" \
    "$BASE_DIR/user"

# 2. Pull the image
echo "Pulling uBlog..."
"$ENGINE" pull "$IMAGE"

# 3. Initialize templates from the container image
if [ -z "$(ls -A "$BASE_DIR/template")" ]; then
    echo "Installing default templates..."

    TEMP_CONTAINER=$("$ENGINE" create "$IMAGE")

    "$ENGINE" cp \
        "$TEMP_CONTAINER:/app/template/." \
        "$BASE_DIR/template/"

    "$ENGINE" rm "$TEMP_CONTAINER"
fi

# 4. Create an example administrator
if [ ! -f "$BASE_DIR/user/admin.properties" ]; then
    ADMIN_PASSWORD=$(openssl rand -hex 12)

    cat > "$BASE_DIR/user/admin.properties" <<EOF
name=Administrator
password=$ADMIN_PASSWORD
status=active
EOF

    echo "Administrator created."
    echo "Username: admin"
    echo "Password: $ADMIN_PASSWORD"
    echo "Save this password."
fi

# 5. Create example content
if [ ! -f "$BASE_DIR/blog/Index.md" ]; then
    cat > "$BASE_DIR/blog/Index.md" <<'EOF'
# Welcome to uBlog!

Your new blog is up and running.

Log in as the administrator to start creating
topics and writing Markdown entries.

For documentation, visit:
https://github.com/szabogabriel/ublog
EOF
fi

if [ ! -f "$BASE_DIR/blog/Error.md" ]; then
    cat > "$BASE_DIR/blog/Error.md" <<'EOF'
# Something went wrong

The requested page could not be found,
or an error occurred.

[Return to homepage](/)
EOF
fi

# 6. Start container
if "$ENGINE" container inspect "$CONTAINER" >/dev/null 2>&1; then
    echo "Container '$CONTAINER' already exists."
    echo "Remove it before recreating:"
    echo "  $ENGINE rm -f $CONTAINER"
    exit 1
fi

echo "Starting uBlog..."

"$ENGINE" run -d \
    --name "$CONTAINER" \
    --restart unless-stopped \
    -p 127.0.0.1:8080:8080 \
    -e BLOG_FOLDER=/app/blogFolder \
    -e USER_FOLDER=/app/userFolder \
    -e IMAGE_FOLDER=/app/imageFolder \
    -e FILE_FOLDER=/app/fileFolder \
    -e MUSTACHE_TEMPLATES_FOLDER=/app/template/personal \
    -v "$BASE_DIR/blog:/app/blogFolder:Z" \
    -v "$BASE_DIR/user:/app/userFolder:Z" \
    -v "$BASE_DIR/image:/app/imageFolder:Z" \
    -v "$BASE_DIR/file:/app/fileFolder:Z" \
    -v "$BASE_DIR/template:/app/template:Z" \
    "$IMAGE"

echo
echo "uBlog is starting!"
echo "Visit: http://localhost:8080"
echo "Data directory: $BASE_DIR"
