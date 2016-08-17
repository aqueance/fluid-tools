#!/usr/bin/env bash

# This is a wrapper for Maven, and all it does is set the "maven.api.version" property before calling
# Maven. The version is used by the packaging plugins to pull the required Maven API related dependencies.

function maven_version {
    local FOUND=false

    for arg in "$@"; do         # see http://stackoverflow.com/a/255913
        [[ $arg == -Dmaven.api.version=* ]] && local FOUND=true
    done

    ${FOUND} && echo "" || echo "-Dmaven.api.version=$(env mvn -version | head -1 | cut -d" " -f 3)"
}

env mvn $(maven_version "$@") "$@"