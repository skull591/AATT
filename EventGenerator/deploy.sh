cd /home/alex-wang/Data1/AndroidSDK/platforms/android-23
android create uitest-project -n $1 -t 6 -p /home/alex-wang/aatt/${1}
cd /home/alex-wang/aatt/${1}
ant clean
ant build
adb push /home/alex-wang/aatt/${1}/bin/${1}.jar data/local/tmp
adb shell uiautomator runtest ${1}.jar -c ${2}
