#!/bin/bash
#
# 这个脚本和remote.sh 的差异只有一点:
#   remote.sh 一般在人工部署时使用, 会在启动服务后直接打开tail -f展示日志, 人工可以继续查看日志,进行请求测试等确认没问题后再关闭tail -f
#
#   iremote.sh 在启动服务后自动监控文件的内容,根据打印出来的日志判断是否成功启动.
#   存在超时等无法判断的情况,或者启动后有异常也无法发现,主要是给自动部署系统使用.
#

command=$1
service=$2
env=$3
index=$4

if [ "$env" == "prod" ] || [[ "$env" = *-prod ]] || [ "$env" == "virtual" ]; then
    isProd=1
else
    isProd=0
fi

function usage() {
    echo "Usage: $0 command service env index"
    echo "       commands: up|down|status|restart|start|stop|tailf|cp "
}

function smart_copy() {
    local _source=$1
    local _target_host=$2
    local _target_path=$3
    local _hostname=`hostname`
    if [ "$_hostname" == "$_target_host" ] || [ "$_target_host" == "localhost" ]; then
        echo "cp $_source to $_target_path"
        cp $_source $_target_path
    else
        echo "scp $_source to $_target_host:$_target_path"
        scp $_source $_target_host:$_target_path
    fi
}

## 调用后获得变量 remote_path, hosts, host_count, host
function parse_locations() {
    local _service=$1
    local _env=$2
    if [ "$_service" = "" ] || [ "$_env" == "" ]; then
        usage
        exit 1
    fi
    local _location_file=deploy/${_env}/locations
    if [ ! -s $_location_file ]; then
        echo "location file not found at $_location_file"
        usage
        exit 1
    fi

    local _fields=( $(awk "/^$_service:/" $_location_file) )
    remote_path=${_fields[1]}
    hosts=(${_fields[@]:2})
    host_count=${#hosts[*]}

    if [[ "$index" =~ ^[0-9]+$ ]]; then
        if [[ $index -lt $host_count ]]; then
            host=${hosts[$index]}
        else
            echo "invalid host index $index, should be less than $host_count"
            exit 1
        fi
    elif [[ ${hosts[*]} =~ "$index" ]]; then
        host=$index
    else
        echo "invalid hostname $index"
        exit 1
    fi
}

function scp_zip() {
    smart_copy $service/target/*.zip $host $remote_path
}

function smart_run() {
    local _command=$1
    local _hostname=`hostname`
    if [ "$_hostname" == "$host" ] || [ "$host" == "localhost" ] ; then
        eval "$_command"
    else
        ssh app@$host /bin/bash <<EOF
        $_command
EOF
    fi
}

function up() {
    scp_zip

    smart_run "
        cd $remote_path &&
        ./commons_deploy.sh up $env  &&
        ./commons_deploy.sh check_start_status $env
    "
}

function down() {
    smart_run "
        cd $remote_path &&
        ./commons_deploy.sh down $env &&
        ./commons_deploy.sh check_start_status $env
    "
}

function tailf() {
    smart_run "
        cd $remote_path &&
        ./commons_deploy.sh tailf $env
    "
}

function status() {
    smart_run "
        cd $remote_path &&
        ./commons_deploy.sh status
    "
}

function restart() {
    smart_run "
        cd $remote_path &&
        ./commons_deploy.sh restart $env &&
        ./commons_deploy.sh check_start_status $env
    "
}

function start() {
    smart_run "
        cd $remote_path &&
        ./commons_deploy.sh start $env
        ./commons_deploy.sh check_start_status $env
    "
}

function stop() {
    smart_run "
        cd $remote_path &&
        ./commons_deploy.sh stop $env
    "
}

function copy_deploy_sh() {
    if [ $isProd -eq 0 ] ; then
        smart_run "
            mkdir -p $remote_path
        "
    fi
    smart_copy commons_deploy.sh $host $remote_path
}

parse_locations $service $env $index
copy_deploy_sh

case "$command" in
    up)
        up
        ;;
    down)
        down
        ;;
    status)
        status
        ;;
    tailf)
        tailf
        ;;
    restart)
        restart
        ;;
    stop)
        stop
        ;;
    start)
        start
        ;;
    cp)
        scp_zip
        ;;
    *)
        usage
        ;;
esac