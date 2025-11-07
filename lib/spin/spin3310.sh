#!/bin/bash
spin_dir=spin3310_linux64/
#echo "Running Spin!"
model_file=$1
#cat $model_file
# spec=$2
 
DIR=$(dirname "$model_file")
 
cp $model_file $DIR/$spin_dir/trap_formula.spin
cp $model_file'_formula' $DIR/$spin_dir/formula.spin
 
pushd $DIR/$spin_dir
#cd $spin_dir
 
# echo 'ltl {' >> trap_formula.spin
# echo $spec >> trap_formula.spin
# echo '}' >> trap_formula.spin
 
echo '--------->>>>Script Start SPIN'
 
./spin3310_linux64 -a trap_formula.spin -F formula.spin
gcc  -w -o pan -DNOREDUCE -DNXT pan.c
 
 
./pan -e -m1000000 -w24
echo '-------->>>>>Script End SPIN'
popd
exit 0
