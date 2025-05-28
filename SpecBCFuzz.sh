#!/bin/bash
source init_env.sh
ARGS="$@"
mvn exec:java -Dexec.mainClass="main.Main" -Dexec.args="$ARGS"
