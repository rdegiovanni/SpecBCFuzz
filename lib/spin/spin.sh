#!/bin/bash
spin_dir=spin651_linux64/
#echo "Running Spin!"
model_file=$1
#cat $model_file
# spec=$2
 
DIR=$(dirname "$model_file")
 
cp $model_file $DIR/$spin_dir/trap_formula.spin
 
pushd $DIR/$spin_dir
#cd $spin_dir
 
# echo 'ltl {' >> trap_formula.spin
# echo $spec >> trap_formula.spin
# echo '}' >> trap_formula.spin
 
echo '--------->>>>Script Start SPIN'
 
$DIR/$spin_dir/spin651_linux64 -a $DIR/$spin_dir/trap_formula.spin
gcc  -w -o pan -DNOREDUCE -DNXT $DIR/$spin_dir/pan.c
 
 
$DIR/$spin_dir/pan -a -m1000000 -w27
echo '-------->>>>>Script End SPIN'
popd
exit 0
