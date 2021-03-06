#!/bin/bash

alias runSPF-tcas='LD_LIBRARY_PATH=$LD_LIBRARY_PATH:/export/scratch2/vaibhav/java-ranger/lib TARGET_CLASSPATH_WALA=/export/scratch2/vaibhav/java-ranger/build/examples/ java -Djava.library.path=/export/scratch2/vaibhav/java-ranger/lib -Xmx12288m -ea -Dfile.encoding=UTF-8 -jar /export/scratch/vaibhav/jpf-core-veritesting/build/RunJPF.jar '
shopt -s expand_aliases
VERIDIR=/export/scratch2/vaibhav/java-ranger

TIMEOUT_MINS=1440 && export TIMEOUT_MINS
LD_LIBRARY_PATH=$LD_LIBRARY_PATH:/export/scratch2/vaibhav/java-ranger/lib && export LD_LIBRARY_PATH
TARGET_CLASSPATH_WALA=/export/scratch2/vaibhav/java-ranger/build/examples/ && export TARGET_CLASSPATH_WALA


echo "Running 3 step - mode 1"
MAX_STEPS=3 && export MAX_STEPS 
timeout $(($TIMEOUT_MINS))m  java -Djava.library.path=/export/scratch2/vaibhav/java-ranger/lib -Xmx12288m -ea -Dfile.encoding=UTF-8 -jar /export/scratch/vaibhav/jpf-core-veritesting/build/RunJPF.jar $VERIDIR/src/examples/veritesting/tcas/tcas.mode1.jpf >& $VERIDIR/logs/tcas.mode1.$(($MAX_STEPS))step.log
# tcas mode1 3 steps already times out in 24 hours
# echo "Running 4 step - mode 1"
# MAX_STEPS=4 && export MAX_STEPS 
# timeout $(($TIMEOUT_MINS))m  java -Djava.library.path=/export/scratch2/vaibhav/java-ranger/lib -Xmx12288m -ea -Dfile.encoding=UTF-8 -jar /export/scratch/vaibhav/jpf-core-veritesting/build/RunJPF.jar $VERIDIR/src/examples/veritesting/tcas/tcas.mode1.jpf >& $VERIDIR/logs/tcas.mode1.$(($MAX_STEPS))step.log
