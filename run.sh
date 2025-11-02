#!/bin/bash

export DB_USERNAME=admin
export DB_PASSWORD=admin
export DB_FILE_PATH=./db/my.db

export H2_WEB_PORT=8082

export MUSTACHE_TEMPLATES_CACHING_ENABLED=true
export MUSTACHE_TEMPLATES_FOLDER=./template

export SERVER_PORT=8080

export SOCKET_HANDLER_THREAD_POOL_SIZE=10

export TARGET_FOLDER=./blogFolder

java -cp target/ublog*-fat.jar info.gabrielszabo.ublog.App