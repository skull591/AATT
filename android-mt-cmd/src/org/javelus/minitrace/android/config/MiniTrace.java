package org.javelus.minitrace.android.config;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

import android.app.ActivityManager.RunningAppProcessInfo;
import android.app.IActivityManager;
import android.os.RemoteException;

public class MiniTrace {

    private IActivityManager mAm;

    private String packageName;
    private int uid;
    

    private File outputDirectory;
    
    public MiniTrace(IActivityManager mAm, String packageName, int uid, File outputDirectory) {
        this.mAm = mAm;
        this.uid = uid;
        this.outputDirectory = outputDirectory;
        this.packageName = packageName;
    }
    
    static String[] SETENFORCE_0 = "su 0 setenforce 0".split(" ");

    static String[] CONFIG_FILES = new String[] {
            "mini_trace_%d_config.in",
            "mini_trace_%d_coverage.dat",
            "mini_trace_%d_data.bin",
            "mini_trace_%d_info.log",
    };
    
    static String getCoverageDataPath(int uid) {
        return String.format("/data/mini_trace_%d_coverage.dat", uid);
    }

    static boolean clearFile(String fileName) {
        try {
            PrintWriter pw = new PrintWriter(fileName);
            pw.close();
            return true;
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        return false;
    }

    public void setup() {
        int ret = -1;
        try {
            ret = executeCommandAndWaitFor(SETENFORCE_0);
            if (ret != 0) {
                throw new RuntimeException("Cannot setup mini trace");
            }
            for (String config : CONFIG_FILES) {
                String fileName = String.format(config, uid);
                String sdcard = "/sdcard/" + fileName;
                String data = "/data/" + fileName;
                if (clearFile(sdcard)) {
                    ret = moveData(sdcard, data);
                    if (ret != 0) {
                        throw new RuntimeException("Cannot move " + sdcard + " to " + data);
                    }
                    ret = chownToUser(data, uid - 10000);
                    if (ret != 0) {
                        throw new RuntimeException("Cannot chown for " + data);
                    }
                } else {
                    throw new RuntimeException("Cannot create file " + sdcard);
                }
            }
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            e.printStackTrace();
        }

        if (ret != 0) {
            throw new RuntimeException("Cannot setup mini trace");
        }
    }

    public void tearDown() {
        // do a 
        harvest();

        int ret = -1;
        try {
            ret = executeCommandAndWaitFor(SETENFORCE_0);
            if (ret != 0) {
                throw new RuntimeException("Cannot stop mini trace");
            }
            for (String config : CONFIG_FILES) {
                System.out.println("Save data to " + outputDirectory);
                String fileName = String.format(config, uid);
                String sdcard = new File(outputDirectory, fileName).getAbsolutePath();
                String data = "/data/" + fileName;
                ret = moveData(data, sdcard);
                if (ret != 0) {
                    throw new RuntimeException("Cannot move " + data + " to " + sdcard);
                }
            }
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            e.printStackTrace();
        }

        if (ret != 0) {
            throw new RuntimeException("Cannot stop mini trace");
        }

    }

    private int moveData(String src, String dest) {
        System.out.println("Move data from " + src + " to " + dest);
        int ret = -1;
        try {
            ret = executeCommandAndWaitFor(new String[] {
                    "su", "0", "cp", src, dest
            });
            ret = executeCommandAndWaitFor(new String[] {
                    "su", "0", "rm", src
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
        return ret;
    }

    private int chownToUser(String fileName, int uid) {
        String userGroup = "u0_a" + uid + ':' + "u0_a" + uid;
        int ret = -1;
        try {
            ret = executeCommandAndWaitFor(new String[] {
                    "su", "0", "chown", userGroup, fileName
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
        return ret;
    }


    private String[] harvestCmdArray = "su 0 kill -USR2 100".split(" ");

    public boolean harvest() {
        List<Integer> pids = checkPIDs();
        if (pids.isEmpty()) {
            return false;
        }
        File dataFile = new File(getCoverageDataPath(uid));
        for (int pid : pids) {
            long before = dataFile.lastModified();
            System.out.println("Before timestamp " + before);
            harvest(pid);
            final int retry = 5;
            int count = 1;
            while (true) {
                long after = dataFile.lastModified();
                System.out.println("After timestamp " + after);
                if (after != before) {
                    System.out.println("File updated");
                    break;
                }
                if (count > retry) {
                    System.out.println("Retry timeout");
                    break;
                }
                System.out.println("Retry " + count++);
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
        return true;
    }

    public static int executeCommandAndWaitFor(String[] cmd) throws InterruptedException, IOException {
        return Runtime.getRuntime().exec(cmd).waitFor();
    }

    public boolean harvest(int pid) {
        try {
            harvestCmdArray[harvestCmdArray.length - 1] = String.valueOf(pid);
            System.out.println("Run command " + join(" ", harvestCmdArray));
            int ret = executeCommandAndWaitFor(harvestCmdArray);
            if (ret != 0) {
                System.out.println("Fail to run command " + join(" ", harvestCmdArray));
                return false;
            }
        } catch (IOException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return true;
    }


    private String join(String string, String[] array) {
        if (array == null) {
            return "null";
        }
        if (array.length == 0) {
            return "";
        }
        if (array.length == 1) {
            return array[0];
        }
        StringBuilder sb = new StringBuilder();
        sb.append(array[0]);
        for (int i = 1; i < array.length; i++) {
            sb.append(' ');
            sb.append(array[i]);
        }
        return sb.toString();
    }

    public List<Integer> checkPIDs() {
        List<Integer> pids = new ArrayList<Integer>(3);
        try {
            List<RunningAppProcessInfo> processes = mAm.getRunningAppProcesses();
            for (RunningAppProcessInfo proc : processes) {
                for (String pkg : proc.pkgList) {
                    if (packageName.equals(pkg)) {
                        pids.add(proc.pid);
                        break;
                    }
                } 
            }
        } catch (RemoteException e) {
            e.printStackTrace();
        }
        return pids;
    }




}
