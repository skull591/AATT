rm -rf Output/*
adb logcat AndroidRuntime:E *:S >> Output/crash_log.txt &
if [ "$#">1 ]; then
	java -jar SO-DFS.jar $1 $2 $3 2>&1 | tee -a Output/execution_log.txt
else
	java -jar SO-DFS.jar $1 2>&1 | tee -a Output/execution_log.txt
fi
rm *.apk
exit 1
