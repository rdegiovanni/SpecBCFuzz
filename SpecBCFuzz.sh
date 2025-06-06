#!/bin/bash
source init_env.sh
ARGS="$@"
#mvn exec:java -Dexec.mainClass="main.Main" -Dexec.args="$ARGS"

JOB_ID=common

#export LD_PRELOAD=$BASEDIR/lib_tmp/lib_$JOB_ID/black_linux/libblack.so:$LD_PRELOAD
#echo $LD_PRELOAD

#export LD_PRELOAD=$BASEDIR/lib_tmp/lib_$JOB_ID/black_linux/libblack.so:$LD_PRELOAD
#echo $LD_PRELOAD

#ORIGINAL
#export LD_LIBRARY_PATH=$BASEDIR/lib_tmp/lib_$JOB_ID/pltl/:$BASEDIR/lib_tmp/lib_$JOB_ID/nusmv/:$BASEDIR/lib_tmp/lib_$JOB_ID/black_linux/libblack.so:$BASEDIR/lib_tmp/lib_$JOB_ID/black_linux/lib:$BASEDIR/lib_tmp/lib_$JOB_ID/black_linux/black:$BASEDIR/lib_tmp/lib_$JOB_ID/black_linux/external/cryptominisat5/lib:$BASEDIR/lib_tmp/lib_$JOB_ID/black_linux/external/minisat/lib:$BASEDIR/lib_tmp/lib_$JOB_ID/black_linux/external/mathsat5-Linux-x86_64/lib:$BASEDIR/lib_tmp/lib_$JOB_ID/black_linux/external/CVC5/bin:$LD_LIBRARY_PATH
#echo $LD_LIBRARY_PATH

#-Xss512M
#-Xms2G
#-Xmx5G

#java -Xmx15g -Djava.library.path=/usr/local/lib -cp target/classes/.:lib_tmp/lib_$JOB_ID/* -jar target/fuzzing-ltl-solvers-0.0.1-SNAPSHOT.jar "$@"
java -Xmx8g -Djava.library.path=/usr/local/lib -cp target/classes/.:lib_tmp/lib_$JOB_ID/* -jar target/fuzzing-ltl-solvers-0.0.1-SNAPSHOT.jar "$@"