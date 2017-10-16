# AATT+
The implementation of AATT+, a tool for detecting concurrency bugs in Android apps. It executes on Linux only.

## ART Setup

In order to record execution traces in the pre-processing phase, we need to modify the ART of the Android device.
Directory mini-tracing-art6 contains the modified ART, which needs to be installed on the test Android device.

#### Get Source

Compiling the modified ART requries the whole Android Open Source Project (AOSP).
  1. Make a directory and refer to it as `$AOSP_HOME` for later usage.
  2. Download AOSP, check out tag `android-6.0.1_r77`. You can follow instructions at https://source.android.com/source/downloading.
  3. Move `$AOSP_HOME/art` out of `$AOSP_HOME` and copy `mini-tracing-art` into `$AOSP_HOME`, renaming it `art`

#### Build

You can follow instructions at https://source.android.com/source/building.
  1. Source build env
  
    $ . $AOSP_HOME/build/envsetup.sh
  
  2. Lunch configuration
    
    $ lunch aosp_arm-eng
    
  3. Make
  
    $ make libart libart-compiler -j 8
   
Known tool chain bug: https://bbs.archlinux.org/viewtopic.php?id=209698

#### Install

  1. Flash the factory image 6.0.1-M4B30Z to Nexus 5. You can download the image at https://developers.google.com/android/images#hammerhead.
  2. Connect the phone to your computer. The phone must be rooted first.
  3. Install the modified art
  
    $ $AOSP_HOME/art/install_libart.sh
    
  Note: if executing `install_libart.sh` reports errors such as "su: invalid uid/gid '-c'", switch all `-c` in the `install_libart.sh` to `0`. 
  
## Preparation

The project provides an example subject MultiPing, which is in folder `Subject`.

To test an App, its apk file is required. Put the apk file into the `Subject` folder. Make sure there is only one apk file in the `Subject` folder.

An `Config.txt` file is also requried. An example is in the project. There must be a space before and after each equation.

+ appPack: The package name of the app.
+ appLaunch: The launcher activity of the app.
+ uiautoDir: Default works just fine, need no to modify it.
+ APK_NAME: The name of the apk name.
+ SDK_Dir: The directory of the Android SDK.
+ android_version: the android platform version requried to compile the app.

## Execution

Connect the phone with modified ART to your computer. It must be the only Android device connect to your phone (the current implementation does not support emulator).

Execute `bash -x exec.sh` and wait for complementation.

The results will be in the `VMDriver\Output` folder. 

+ `crash_log` contains the stack information if there's a crash.
+ `execution_log` contains the execution log of the manifestation phase.
