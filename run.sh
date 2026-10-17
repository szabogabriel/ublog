#!/bin/bash

export MUSTACHE_TEMPLATES_CACHING_ENABLED=true
export MUSTACHE_TEMPLATES_FOLDER=./template/personal
export MUSTACHE_TEMPLATE_UPDATE=false

export SERVER_PORT=8080

export SESSION_TIMEOUT=60
export SESSION_COOKIE_SECURE=false

export SOCKET_HANDLER_THREAD_POOL_SIZE=10
export SOCKET_HANDLER_QUEUE_CAPACITY=100

export TARGET_FOLDER=./testContent/blogFolder
export USER_FOLDER=./testContent/userFolder
export IMAGE_FOLDER=./testContent/imageFolder

mvn clean install

java -Xms8m -Xmx8m -cp target/ublog*-fat.jar info.gabrielszabo.ublog.App