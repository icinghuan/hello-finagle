#!/bin/bash

function usage() {
    echo "Usage: "
    echo "  run.sh start [env]"
    echo "  run.sh restart [env]"
    echo "  run.sh status"
    echo "  run.sh stop"
}

PARAM_COUNT=$#
if [ $PARAM_COUNT -lt 3 ]; then
    usage
    exit 1
fi

NAME="$1"
APP="$2"
COMMAND="$3"
ENV="$4"

# ENV maybe empty
if [ "$ENV" != "" ]; then
    shift 4
    OTHER_OPTS="$@"  #other opts, like jvm opts
fi

PID_FILE=RUNNING_PID
STATUS_RUNNING="running"
STATUS_STOPPED="stopped"

if [ "$ENV" == "prod" ] || [[ "$ENV" = *-prod ]] || [ "$ENV" == "virtual" ]; then
    isProd=1
else
    isProd=0
fi

## after called, cstatus is set, pid is set if pid file exist
function status() {
    if [ -f $PID_FILE ]; then
        pid=$(< "$PID_FILE")
        if [ -z "`ps -A -o pid | grep -w ${pid}`" ]; then
            cstatus="${STATUS_STOPPED}"
        else
            cstatus="${STATUS_RUNNING}"
        fi
    else
        cstatus="$STATUS_STOPPED"
    fi
}

function start() {
    status
    if [ "$cstatus" = "$STATUS_RUNNING" ]; then
        echo "$APP is already running"
        exit 0
    fi

    if [ -z $ENV ]; then
        usage
        exit 1
    fi

    LOG_FILE=config/logback-$ENV.xml
    if [ ! -f $LOG_FILE ] && [ $isProd -eq 1 ]; then
        LOG_FILE=config/logback-prod.xml
    fi
    if [ ! -f $LOG_FILE ]; then
        LOG_FILE=config/logback.xml
    fi
    LOG_FILE_OPT=""
    if [ -f $LOG_FILE ]; then
        LOG_FILE_OPT="-Dlogging.config=$LOG_FILE"
    fi

    NEW_RELIC_OPT=""
    if [ -f newrelic.yml ] && [ -f newrelic/newrelic.jar ]; then
        NEW_RELIC_OPT="-Dnewrelic.environment=$ENV -Dnewrelic.config.file=newrelic.yml -javaagent:newrelic/newrelic.jar"
    fi

    CONFIG_FILE=config/$ENV.conf
    HEAP_DUMP_PATH="."
    GC_LOG_FILE="gcinfo"
    if [ $isProd -eq 1 ]; then
        LOGS_DIR=/data0/logs/app/$APP
        if ! [ -d $LOGS_DIR ]; then
            echo "creating logs dir $LOGS_DIR..."
            mkdir -p $LOGS_DIR
        fi
        CONFIG_FILE="/data0/app/alpha-config/$APP/$ENV.conf"
        HEAP_DUMP_PATH=$LOGS_DIR
        GC_LOG_FILE="$LOGS_DIR/gcinfo"
    fi
    if ! [ -f $CONFIG_FILE ]; then
        echo "config file not found: $CONFIG_FILE"
        exit 1
    fi

    CONFIG_FILE_OPT="-Dconfig.file=$CONFIG_FILE"
    OOM_OPT="-XX:+HeapDumpOnOutOfMemoryError -XX:HeapDumpPath=$HEAP_DUMP_PATH"
    GC_OPT="-verbose:gc -XX:+PrintGCDetails -XX:+PrintGCTimeStamps -XX:+PrintGCDateStamps -XX:+PrintTenuringDistribution -Xloggc:$GC_LOG_FILE"

    nohup java $OTHER_OPTS $GC_OPT $OOM_OPT $NEW_RELIC_OPT $LOG_FILE_OPT $CONFIG_FILE_OPT -jar $NAME.jar >> log 2>&1 &
    new_pid=$!
    echo $new_pid > RUNNING_PID
    echo "$APP is started, pid = $new_pid"
}

function stop() {
    for i in {0..30}
    do
        status
        if [ "$cstatus" = "$STATUS_RUNNING" ]; then
            echo "stopping $APP, pid = $pid"
            if [ $i -lt 15 ]; then
                kill $pid
            else
                kill -9 $pid
            fi
            sleep 1
        else
            if [ -f $PID_FILE ]; then
                rm $PID_FILE
            fi
            echo "$APP is stopped"
            return 0
        fi
    done
    echo "ERROR: $APP cannot be killed in $count seconds"
    exit 1
}

function restart() {
    stop && start
}

function find_log_file() {
    local _date=$(date +%Y-%m-%d)
    local _prefix="log."
    local _suffix=".log"
    local _logs_dir="../logs/"
    if [ $isProd -eq 1 ]; then
        _logs_dir="/data0/logs/app/$APP/"
    fi
    for i in {0..20}
    do
        log_file_count=$(find $_logs_dir -name "${_prefix}${_date}*${_suffix}" | wc -l )
        if [ $log_file_count -lt 1 ]; then
            echo "waiting for log files"
            sleep 1
        else
            local _last_num=$(( $log_file_count - 1 ))
            current_log_file="${_logs_dir}${_prefix}${_date}.${_last_num}${_suffix}"
            return 0
        fi
    done
    echo "cannot find log file"
    exit 1
}

function tailf() {
    find_log_file
    if [ "$current_log_file" == "" ]; then
        echo "cannot find today's log file"
        exit 1
    else
        tail -f $current_log_file
    fi
}

function check_start_status {
    find_log_file
    # 先把tailf的内容写到一个文件里面,然后定期扫描匹配. 匹配到或者超时之后,停止tailf
    rm -f RUNNING_START_LOG
    tail -f $current_log_file > RUNNING_START_LOG &
    tailproc=$!
    echo -n "application starting..."
    for i in {0..60}
    do
        matched_log=$(grep -E "(Started .*JVM running|Application startup failed)" RUNNING_START_LOG)
        if [[ $matched_log == *"JVM running"* ]]; then
            printf "\nApplication started: $matched_log\n"
            kill -9 $tailproc
            exit 0
        fi
        if [[ $matched_log == *"Application startup failed"* ]]; then
            printf "\nApplication Failed: $matched_log\n"
            kill -9 $tailproc
            exit 1
        fi
        echo -n "."
        sleep 1
    done
    printf "\nTimeout, unknown status! please check manually\n"
    kill -9 $tailproc
    exit 1
}

case "$COMMAND" in
    status)
        status
        if [ "$cstatus" = "$STATUS_RUNNING" ]; then
            echo "$APP is running"
        else
            echo "$APP is stopped"
        fi
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
        ENV=$COMMAND
        COMMAND="start"
        start
        ;;
esac