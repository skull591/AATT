#!/bin/sh

#preprocessing
adb shell 'ls /data/mini_trace*' | tr -d '\r' | sed -e 's/^\///' | xargs -n1 adb shell rm
pack=$(head -1 Config.txt | awk 'BEGIN {FS=" "} {print $3}')
cp Config.txt EventGenerator/
cp Config.txt Instrumentor/
cp Config.txt VMDriver/
rm EventGenerator/apks/*
rm Instrumentor/apks/*
cp Subject/*.apk EventGenerator/apks/
cp Subject/*.apk Instrumentor/apks/

# activates trace collection
cd android-mt-cmd
python install.py
python minitrace.py enable $pack

# begin event sequence generation
cd ../
cp Subject/*.apk EventGenerator/apks/
cp Config.txt EventGenerator/
cd EventGenerator
timeout 1m java -jar DynamicController.jar
adb shell ps | grep uiautomator | adb shell kill

# stop trace collection and obtain traces
cd ../android-mt-cmd && python minitrace.py disable $pack
cd ../Instrumentor/execution_traces
rm *
adb shell 'ls /data/mini_trace*' | tr -d '\r' | sed -e 's/^\///' | xargs -n1 adb pull
adb shell 'ls /sdcard/mini_trace*' | tr -d '\r' | sed -e 's/^\///' | xargs -n1 adb pull

# analyze traces and instrument apk
log=$(ls *.log)
trace=$(ls *.bin)
cd ../ && java -jar Instrumentor.jar $log $trace

# manifest concurrency bugs
cd analysis_result
cp *.apk ../../VMDriver/subjects/apks/
cp *.txt ../../VMDriver/subjects/suspicious/
cd ../../VMDriver
apk=$(ls subjects/apks/*)
sus=$(ls subjects/suspicious/*)
./run.sh 5 $apk $sus
