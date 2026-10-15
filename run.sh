#!/bin/bash

export MUSTACHE_TEMPLATES_CACHING_ENABLED=true
export MUSTACHE_TEMPLATES_FOLDER=./template/personal

export SERVER_PORT=8080

export SESSION_TIMEOUT=60
export SESSION_COOKIE_SECURE=false

export SOCKET_HANDLER_THREAD_POOL_SIZE=10
export SOCKET_HANDLER_QUEUE_CAPACITY=100

export TARGET_FOLDER=./testContent/blogFolder
export USER_FOLDER=./testContent/userFolder

mvn clean install

java -Xms4m -Xmx4m -cp target/ublog*-fat.jar info.gabrielszabo.ublog.App