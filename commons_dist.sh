#!/bin/bash
NAME=$1
WITHOUT_CLEAN=$2
FOLDER_NAME=`date +%Y%m%d-%H%M%S`

if [ "$WITHOUT_CLEAN" == "without_clean" ]; then
    mvn -DskipTests=true package
else
    mvn clean package -DskipTests=true -U
fi
package_status=$?
if [ $package_status != 0 ]; then
    echo ""
    echo "Packaging failed, please check errors in logs ($NAME)"
    echo ""
    exit 1
fi

mkdir -p target/$FOLDER_NAME
cd target
git log -1 > $FOLDER_NAME/LAST_COMMIT
mv $NAME.jar $FOLDER_NAME
cp ../run.sh $FOLDER_NAME
if [ -f ../commons_run.sh ]; then
    cp ../commons_run.sh $FOLDER_NAME
else
    cp ../../commons_run.sh $FOLDER_NAME
fi

if [ -f ../newrelic.yml ]; then
    cp ../newrelic.yml $FOLDER_NAME
    mv newrelic $FOLDER_NAME
fi

cp -r ../config $FOLDER_NAME
zip -r $FOLDER_NAME.zip $FOLDER_NAME
rm -rf $FOLDER_NAME
