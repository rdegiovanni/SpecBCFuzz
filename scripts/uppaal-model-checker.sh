#!/bin/bash
uppal_dir=spin-6.5.1/
model_file=$1
spec=$2

VERIFIER=uppaal-5.0.0/UPPAAL-5.0.0.app/Contents/Resources/uppaal/bin/verifyta

# cp $model_file $spin_dir/specs/trap_formula.spin
# pushd $spin_dir/specs/ > /dev/null

# echo 'ltl {' >> trap_formula.spin
# echo $spec >> trap_formula.spin
# echo '}' >> trap_formula.spin

# spin -a  trap_formula.spin
# gcc -w -o pan -DNOREDUCE -DNXT pan.c
# ./pan -m1000000 -w27  

# popd > /dev/null