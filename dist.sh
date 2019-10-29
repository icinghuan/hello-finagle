#!/bin/bash
trap "exit" INT

SERVICE_FILE=deploy/services
if [ ! -s "$SERVICE_FILE" ]; then
    echo "services file not found at $SERVICE_FILE"
    exit 1
fi

ALL_MODULES=( $(cat $SERVICE_FILE) )

function usage() {
    echo "Usage: $0 [all ${ALL_MODULES[@]}]"
}

MODULE=$1
if [ "$MODULE" == "" ]; then
    MODULE="all"
fi
if [ "$MODULE" != "all" ] && ! [ -d $MODULE ]; then
    usage
    exit 1
fi

echo "
#######################################

clean and install all modules

#######################################
"
mvn -DskipTests=true clean install -U
ret=$?
if [ $ret != 0 ]; then
    echo ""
    echo "Maven install failed"
    echo ""
    exit 1
fi

if [ "$MODULE" == "all" ]; then
    MODULES=( "${ALL_MODULES[@]}" )
else
    MODULES=("$MODULE")
fi

for service in ${MODULES[@]}; do
    echo "
#######################################

packaging $service

#######################################
"
    cd $service
    ./dist.sh without_clean
    ret=$?
    if [ $ret != 0 ]; then
        echo ""
        echo "Package $service failed"
        echo ""
        exit 1
    fi
    cd ..
done



