package umd.troyd;


import android.app.IntentService;
import android.content.ComponentName;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;


public class Ignite extends IntentService {
	
	public Ignite() {
		super(Ignite.class.getName());
	}

	// to start this server,
	// adb shell am startservice -n umd.redexer/.Ignite -e AUT <pkg>
	// <pkg> should be equal to the target package name mentioned at manifest
	@Override
	protected void onHandleIntent(Intent argv) {
		String actNamePort = argv.getExtras().getString("AUT"); //pkg的名字和
	//	String[] strs = actNamePort.split("_"); 
	//	String actName = strs[0];
	//	int port = Integer.parseInt(strs[1]);
		ComponentName instr = new ComponentName(this, Troy.class);
		Bundle arg = new Bundle();
		arg.putString("AUT", actNamePort);
		startInstrumentation(instr, null, arg);//启动Troy.class
	}
}

