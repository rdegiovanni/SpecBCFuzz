#!/bin/bash
spin_dir=spin651_linux64/
model_file=$1
# spec=$2

cp $model_file $spin_dir/trap_formula.spin
pushd $spin_dir > /dev/null

# echo 'ltl {' >> trap_formula.spin
# echo $spec >> trap_formula.spin
# echo '}' >> trap_formula.spin

./spin651_linux64 -a trap_formula.spin
gcc -w -o pan -DNOREDUCE -DNXT pan.c
./pan -m1000000 -w27  

popd > /dev/null
