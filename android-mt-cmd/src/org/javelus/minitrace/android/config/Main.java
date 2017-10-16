package org.javelus.minitrace.android.config;

import java.io.File;
import java.util.List;

import android.app.ActivityManagerNative;
import android.app.IActivityManager;
import android.content.pm.IPackageManager;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.UserHandle;

public class Main {

    static IActivityManager activityManager;

    static IActivityManager getActivityManager() {
        if (activityManager == null) {
            activityManager = ActivityManagerNative.getDefault();
            if (activityManager == null) {
                System.err.println("** Error: Unable to connect to activity manager; is the system "
                        + "running?");
                System.exit(1);
                return null;
            }
        }
        return activityManager;
    }

    static IPackageManager packageManager;

    static IPackageManager getPackageManager() {
        if (packageManager == null) {
            packageManager = IPackageManager.Stub.asInterface(ServiceManager.getService("package"));
            if (packageManager == null) {
                System.err.println("** Error: Unable to connect to package manager; is the system "
                        + "running?");
                System.exit(1);
                return null;
            }
        }

        return packageManager;
    }

    static int getUid(String pkg) {
        IPackageManager packageManager = getPackageManager();
        try {
            return packageManager.getPackageUid(pkg, UserHandle.myUserId());
        } catch (RemoteException e) {
            System.out.print("Cannot get uid for package " + pkg);
            e.printStackTrace();
            System.exit(1);
        }
        return 0;
    }

    static void stopPackage(String pkg) {

    }
    
    static void configure(String pkg, boolean enable, boolean harvest) {
        File outputDirectory = new File("/sdcard/");
        int uid = getUid(pkg);
        MiniTrace mt = new MiniTrace(getActivityManager(), pkg, uid, outputDirectory);
        if (enable) {
            int retryCount = 10;
            while (retryCount-->0) {
                List<Integer> pids = mt.checkPIDs();
                if (pids.isEmpty()) {
                    break;
                }
                System.out.println("Stop all packages, retry count " + retryCount);
                try {
                    System.out.println("Try to stop package " + pkg);
                    getActivityManager().forceStopPackage(pkg, 0);
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            mt.setup();
        } else {
            if (harvest) {
                mt.harvest();
            } else {
                mt.tearDown();
            }
        }
    }

    public static void main(String[] args) {
        if (args.length < 2) {
            printUsage();
            System.exit(1);
        }

        String cmd = args[0];
        String pkg = args[1];
        if (cmd.equals("enable")) {
            configure(pkg, true, false);
        } else if (cmd.equals("disable")) {
            configure(pkg, false, false);
        } else if (cmd.equals("harvest")) {
            configure(pkg, false, true);
        } else {
            System.out.print("Unsupported command: " + cmd);
            printUsage();
            System.exit(1);
        }
    }

    private static void printUsage() {
        System.out.println("run enable <package-name> or run disable <package-name>");
    }

}
