#!/bin/sh

echo "The application will start in ${SLEEP_TIME}s..." && sleep ${SLEEP_TIME}
# JAVA_OPTS e.g. -Dspring.profiles.active=demo
exec java -jar quarkus-app/quarkus-run.jar "$@"