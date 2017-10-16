#! /usr/bin/python

import os, sys, traceback, subprocess

DIR=os.path.abspath(os.path.dirname(__file__))

ADB=os.getenv('ADB', 'adb')

APE_ROOT='/sdcard/'

def run_cmd(*args):
    print('Run cmd: ' + (' '.join(*args)))
    subprocess.check_call(*args)


if __name__ == '__main__':
    try:
        run_cmd([ADB, 'push', os.path.join(DIR, 'minitrace-cmd.jar'), APE_ROOT])
    except:
        traceback.print_exc()

