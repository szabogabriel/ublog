#!/bin/bash

FLAG_COMPILE=false
UI_VERSION=personal
DO_HELP=false
MEM_SIZE=8m

parseArgs() {
  while [[ $# -gt 0 ]]; do
    case $1 in
      -c|--compile)
        FLAG_COMPILE=true
        shift
        ;;
      -ui|--ui-version)
        UI_VERSION="$2"
        shift
        shift
        ;;
      -h|--help)
        DO_HELP=true
        shift
        ;;
      -m|--memory)
        MEM_SIZE="$2"
        shift # past argument
        shift
        ;;
      -*|--*)
        echo "Unknown option $1"
        exit 1
        ;;
    esac
  done
}

checkArgs() {
    if [[ "$DO_HELP" == true ]]; then
        echo "Usage: $0 [options]"
        echo "Options:"
        echo "  -c, --compile           Compile the project"
        echo "  -ui, --ui-version       Set the UI version: personal or plain (default: personal)"
        echo "  -h, --help              Display this help message"
        echo "  -m, --memory            Set the memory size (default: 8m)"
        exit 0
    fi
    if [[ "$UI_VERSION" != "personal" ]] && [[ "$UI_VERSION" != "plain" ]]; then
        echo "Invalid UI version: $UI_VERSION"
        echo "Valid options are: personal, plain"
        exit 1
    fi
}

setEnv() {
    export MUSTACHE_TEMPLATES_CACHING_ENABLED=true
    export MUSTACHE_TEMPLATES_FOLDER=./template/${UI_VERSION}
    export MUSTACHE_TEMPLATE_UPDATE=false

    export SERVER_PORT=8080

    export SESSION_TIMEOUT=60
    export SESSION_COOKIE_SECURE=false

    export SOCKET_HANDLER_THREAD_POOL_SIZE=10
    export SOCKET_HANDLER_QUEUE_CAPACITY=100

    export BLOG_FOLDER=./testContent/blogFolder
    export USER_FOLDER=./testContent/userFolder
    export IMAGE_FOLDER=./testContent/imageFolder
}

main() {
    parseArgs "$@"
    checkArgs
    setEnv
    if [[ "$FLAG_COMPILE" == true ]]; then
        mvn clean install || exit 1
    fi

    java -Xms${MEM_SIZE} -Xmx${MEM_SIZE} -cp target/ublog*-fat.jar info.gabrielszabo.ublog.App
}

main "$@"