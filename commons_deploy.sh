#!/usr/bin/env bash

# 部署时的目录结构:
# /data0/app
#   - alpha-site
#       - deploy.sh
#       - 20170722-145502
#       - 20170724-100532
#       - 20170725-130143
#       - 20170725-130143.zip
#       - versions
#   - alpha-trade
#   - ...
#
#   versions 文件内容:
#       20170724-100532 COMMIT_B @部署时间B
#       20170725-130143 COMMIT_A @部署时间A
#

COMMAND=$1
ENV=$2

if [[ "$ENV" =~ ^.*prod$ ]]; then
    ENV=prod
fi

VERSION_FILE=versions
VERSION_MAX=5

function usage() {
    echo "Usage: $0 up|down dev|test|staging|prod"
    echo "       $0 start|restart|tailf dev|test|staging|prod"
    echo "       $0 cleanup"
    echo "       $0 stop|status"
}

function stop_version() {
    local _version=$1
    echo "stopping $_version"

    if [ -z $_version ] || [ ! -d $_version ]; then
        echo "invalid version $_version"
        exit 1
    fi
    cd $_version
    ./run.sh stop
    if [ $? -ne 0 ]; then
        echo "cannot stop app in $_version"
        exit 1
    fi
    cd ..
}

function start_version() {
    local _version=$1
    echo "starting $_version"
    if [ -z $_version ] || [ ! -d $_version ]; then
        echo "invalid version $_version"
        exit 1
    fi
    cd $_version
    ./run.sh start $ENV
    if [ $? -ne 0 ]; then
        echo "cannot start app in $_version"
        exit 1
    fi
    cd ..
}

function check_zips() {
    zips=$(ls *.zip | wc -l)
    if [ $zips -eq 0 ]; then
      echo "cannot deploy: no zip file"
      exit 1
    fi
    if [ $zips -ne 1 ]; then
      echo "cannot deploy: multiple zip files"
      exit 1
    fi
}

##
## after called, below parameter are set(but may be empty):
##      version_array: array of all version in version file
##      last: last version
##      current: current version
##
function get_versions() {
    if [ ! -f $VERSION_FILE ]; then
        echo "no version file"
    else
        local _versions="$(cat $VERSION_FILE | awk '{print $1}')"
        version_array=( $_versions )
        local _version_count=${#version_array[@]}
        if [ $_version_count -ge 1 ]; then
            local _current_index=$(( $_version_count - 1))
            current=${version_array[$_current_index]}
        fi
        if [ $_version_count -ge 2 ]; then
            local _last_index=$(( $_version_count - 2))
            last=${version_array[$_last_index]}
        fi
    fi
}

function cleanup() {
    # remove folders except last VERSION_MAX versions
    echo "cleaning up deploy folders"
    versions="$(tail -n$VERSION_MAX $VERSION_FILE | awk '{print $1}')"
    version_array=( $versions )
    folders=$(ls -d */)
    for folder in $folders; do
        v=${folder%%/}
        if [[ $v =~ ^[0-9]{8}-[0-9]{6}$ ]] && ! [[ ${version_array[*]} =~ "$v" ]]; then
            echo "delete $v"
            rm -rf $v
        fi
    done
}

function up() {
    # there should be only one zip file to deploy
    check_zips

    next_zip_file=$(ls *.zip)
    next=$(echo $next_zip_file | cut -d'.' -f 1)
    echo "next version = $next"
    if ! [ -d $next ]; then
        unzip -q $next_zip_file
    fi
    rm $next_zip_file

    next_commit=$(head -n1 $next/LAST_COMMIT | awk '{print $2}')
    echo "next commit = $next_commit"
    deploy_time=$(date +%Y%m%d-%H%M%S)
    echo "deploy time = $deploy_time"

    get_versions
    if [ -z $current ]; then
        echo "no current version"
    else
        echo "current version = $current"
        stop_version $current
    fi

    start_version $next

    echo "$next  $next_commit  @$deploy_time" >> $VERSION_FILE

    cleanup
}

function down() {
    get_versions
    if [ "$current" = "" ]; then
        echo "no current version"
        exit 1
    fi
    if [ "$last" = "" ]; then
        echo "no last version"
        exit 1
    fi

    stop_version $current
    start_version $last

    # remove version history
    count=$(cat $VERSION_FILE | wc -l)
    remaining=$(( $count - 1 ))
    head -n $remaining $VERSION_FILE > version.tmp
    mv version.tmp $VERSION_FILE
}

function status() {
    get_versions
    
    if [ -z "$current" ]; then
        echo "no current version"
        exit 1
    else
        echo "current version = $current"
    fi

    cd $current
    ./run.sh status
}

function stop() {
    get_versions
    if [ -z "$current" ]; then
        echo "no current version"
        exit 1
    else
        echo "current version = $current"
    fi
    cd $current
    ./run.sh stop
}

function start() {
    get_versions
    if [ -z "$current" ]; then
        echo "no current version"
        exit 1
    else
        echo "current version = $current"
    fi
    cd $current
    ./run.sh start $ENV
}

function restart() {
    get_versions
    if [ -z "$current" ]; then
        echo "no current version"
        exit 1
    else
        echo "current version = $current"
    fi
    cd $current
    ./run.sh restart $ENV
}

function tailf() {
    get_versions

    if [ -z "$current" ]; then
        echo "no current version"
        exit 1
    else
        echo "current version = $current"
    fi

    cd $current
    ./run.sh tailf $ENV
}

function check_start_status() {
    get_versions

    if [ -z "$current" ]; then
        echo "no current version"
        exit 1
    else
        echo "current version = $current"
    fi

    cd $current
    ./run.sh check_start_status $ENV
}

if [ "$COMMAND" != "cleanup" ] && [ "$COMMAND" != "status" ] && [ "$COMMAND" != "stop" ]; then
    if [ -z "$ENV" ]; then
        usage
        exit 1
    fi
fi

case "$COMMAND" in
    up)
        up
        ;;
    down)
        down
        ;;
    cleanup)
        cleanup
        ;;
    status)
        status
        ;;
    stop)
        stop
        ;;
    start)
        start
        ;;
    restart)
        restart
        ;;
    tailf)
        tailf
        ;;
    check_start_status)
        check_start_status
        ;;
    *)
        usage
        ;;
esac
