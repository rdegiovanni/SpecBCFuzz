#!/bin/bash -l

source init_env.sh

SLURM_DIR=$BASEDIR/outcomes/slurm
LOG_DIR=$BASEDIR/outcomes/log
#LTL-SOLVERS
LIB_DIR=$BASEDIR/lib

#N_RUN=10000
N_RUN=1
SAT_TIMEOUT=300
#EXPERIMENT=mutation-format:benchmarks
#EXPERIMENT=mutation-format:gore
EXPERIMENT=searchbased:SemSyn-format:gore

#if [$EXPERIMENT = "mutation-format:benchmarks"] 
#then
#  N_RUN=-100 #Flag to run all cases.
#fi

#Benchmark folders.
#SPECS[0]=acacia
#SPECS[1]=alaska
#SPECS[2]=anzu
#SPECS[3]=forobots
#SPECS[4]=rozier/counter
#SPECS[5]=rozier/pattern
#SPECS[6]=rozier/formulas/n1
#SPECS[7]=rozier/formulas/n2
#SPECS[8]=rozier/formulas/n3
#SPECS[9]=rozier/formulas/n4
#SPECS[10]=rozier/formulas/n5
#SPECS[11]=schuppan
#SPECS[12]=trp/N5x
#SPECS[13]=trp/N5y
#SPECS[14]=trp/N12x
#SPECS[15]=trp/N12y

#START=0
#END=16

#benchmark
#SPECS=( $(ls -R $BENCHMARKS_PATH | grep ":" | sed -e "s/:$//") )
#END=#SPECS[@]

#all  cases per job
#SPEC_FOLDER=$BASEDIR/specs/
#SPECS=( $(ls -R $SPEC_FOLDER | grep ":" | sed -e "s/:$//") )
#END=#SPECS[@]
#START=0

#one case per job
SPEC_FOLDER=$BASEDIR/datasets/specs/
SPECS=( $(ls -R $SPEC_FOLDER | grep ":" | sed -e "s/:$//") )
START=1
END=${#SPECS[@]}

pushd ${BASEDIR}
echo $JAVA_HOME
echo $PATH

echo $(java -version)
echo $(javac -version)

#mvn assembly:assembly

mkdir -p $SLURM_DIR
mkdir -p $LOG_DIR
mkdir -p $LIB_DIR

# LIB_COMMON=lib_common
# mkdir -p $BASEDIR/lib_tmp/$LIB_COMMON
# cp -R $BASEDIR/lib/* $BASEDIR/lib_tmp/$LIB_COMMON

START_REPET_JOBS=0
END_REPET_JOBS=1
K="$START"

while [ "$K" -lt "$END" ]
do
    echo "Running $K..."
    #echo "benchmark version"
    #sbatch --job-name=$K-ltl-fuzzer --output=$SLURM_DIR/$K-ltl-fuzzer.out $SLURM_CONFIG ./fuzzing-ltl-solvers.sh -log=$LOG_DIR-$K -lib=$LIB_DIR -id=$K -run=$N_RUN -sattimeout=$SAT_TIMEOUT -experiment=$EXPERIMENT -specs=$BASEDIR/benchmarks/${SPECS[$K]} $K
    #echo "GORE parameter - all  cases per job"
    #sbatch --job-name=$K-ltl-fuzzer --output=$SLURM_DIR/$K-ltl-fuzzer.out $SLURM_CONFIG ./fuzzing-ltl-solvers.sh -log=$LOG_DIR-$K -lib=$LIB_DIR -id=$K -run=$N_RUN -sattimeout=$SAT_TIMEOUT -experiment=$EXPERIMENT -specs=$SPEC_FOLDER/${SPECS[$K]} $K
    echo "GORE parameter - one case per job"
    echo "${SPECS[$K]}"
    ID_JOB="$START_REPET_JOBS"
    END_TMP=`expr $END - 1`
    ID_JOB=`expr $ID_JOB \\* $END_TMP`
    ID_JOB=`expr $ID_JOB + $K`
    echo "ID: $ID_JOB"
    nohup ./SpecBCFuzz.sh -log=$LOG_DIR-$ID_JOB -lib=$LIB_DIR -id=$ID_JOB -run=$N_RUN -sattimeout=$SAT_TIMEOUT -experiment=$EXPERIMENT -enableThreads -popSize=100 -maxNumOfInd=1000 -specs=${SPECS[$K]} 1> $SLURM_DIR/$ID_JOB-ltl-fuzzer.out 2> $SLURM_DIR/$ID_JOB-ltl-fuzzer.err
    K=`expr $K + 1`
    if [ "$K" == "$END" ] && [ "$START_REPET_JOBS" -lt "$END_REPET_JOBS" ]
    then
       K="$START"
       START_REPET_JOBS=`expr $START_REPET_JOBS + 1`
    fi
done

echo "Finished."

