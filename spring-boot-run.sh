#!/usr/bin/env bash
ENV="$1"
if [[ -z $ENV ]]; then
    echo "Missing env parameter: spring-boot-run.sh dev|test|staging"
    exit 1
fi

CONFIG_FILE=config/$ENV.conf
if ! [[ -f $CONFIG_FILE ]]; then
    echo "file not found $CONFIG_FILE"
    exit 1
fi

LOG_FILE=config/logback.xml
LOG_FILE_CONF=""
if [ -f $LOG_FILE ]; then
    LOG_FILE_CONF="-Dlogging.config=$LOG_FILE"
fi

mvn -Drun.jvmArguments="-Dconfig.file=$CONFIG_FILE $LOG_FILE_CONF" spring-boot:run



