/*
 * Xiujiang Li <njucslxj@gmail.com>
 * The codes are based on troyd
 * but we modify them to communicate with PC server via WIFI
 */

package umd.troyd;
import java.io.*;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.*;
import java.util.ArrayList;
import java.util.Deque;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;
import java.util.concurrent.Semaphore;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Instrumentation;
import android.content.Intent;
import android.os.*;
//import android.support.v4.widget.DrawerLayout;
import android.util.*;
import android.view.MenuItem;
import android.view.View;
import android.widget.*;

//import com.android.ddmlib.Log;
//import com.android.ddmlib.Log;
import com.robotium.solo.Solo;

/*
 * Troy is to start the targeting app
 * send the GUI elements and current activity to PC server
 * receive event actions from PC server and fire them
 */
public class Troy extends Instrumentation {
	final static String tag = Troy.class.getPackage().getName();
	static Deque<String> history = new LinkedList<String>();
	// App Under Test
	Intent intent = null;
	String actName = null;

	Socket socket = null;
	String serverIP = "114.212.82.81";//"114.212.87.227";//"172.19.116.6";//"114.212.84.23";//"172.28.35.93";//
	int port = -1;
	DataOutputStream out = null;
	DataInputStream in = null;

	@Override
	public void onCreate(Bundle arg) {
		super.onCreate(arg);
		Log.i("lqw", "onCreate");
		//String[] strs = arg.getString("AUT").split("_");
		//actName = strs[0];
		//	port = Integer.parseInt(strs[1]);
		port = Integer.parseInt(arg.getString("AUT"));
		Log.i("lqw", " "+port); //pkg的名字

		//	intent = new Intent();
		//	intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		//try {
		//	intent.setClass(getTargetContext(), Class.forName(actName));
		start();
		//	} catch (ClassNotFoundException e) {
		//		Log.e(tag, e.toString());
		//	}
		StrictMode.setThreadPolicy(new StrictMode.ThreadPolicy.Builder().detectDiskReads().detectDiskWrites().detectNetwork().penaltyLog().build());
	}


	static Solo solo;
	static Troy tempClass;
	static Activity activity;

	@Override
	public void onStart() {
		super.onStart();
		Log.i("lqw", "onStart");

		//	activity = startActivitySync(intent);  //启动act
		tempClass = this;
		solo = new Solo(this);
		//	solo.getCurrentActivity(); // to collect all opened activities

		/*
		KeyguardManager km = (KeyguardManager)
				getContext().getSystemService(Context.KEYGUARD_SERVICE);
		km.newKeyguardLock(tag).disableKeyguard();
		 */


		try {
			Log.i("lqw", "before connect");
			socket = new Socket(serverIP, port);
			in = new DataInputStream(socket.getInputStream());
			out = new DataOutputStream(socket.getOutputStream());
			//out.writeUTF("succ");
			//	out.writeUTF("troyd");
			//	out.flush();
			Log.i("lqw", "after connect");
			
			//if aardict sleep time should be 1200
			//should be 5000 for others
			try {
				Thread.sleep(5000);
				//Thread.sleep(1200);
			} catch (Exception e) {
				Log.i("lqw", "sleep exception connect" + e.toString());
			}
			Log.i("lqw", "get activity name");
			String actName = solo.getCurrentActivity().getClass().getName();
			Log.i("lqw", actName);
			if (actName.equals("aarddict.android.LookupActivity")) {
				Log.i("lqw", "in aarddict.android.LookupActivity");
				setDefaultText("a", false);
			}
			
			
			while (true) {
				Log.d("lqw", "read cmd");
				String cmd = in.readUTF();
				Log.d("lqw", "cmd is " + cmd);
				final String[] cmds = cmd.split("_");
				new Thread(new Runnable() {   //来了指令就开新的线程，这样保证主线程在执行指令时候不会阻塞后续指令的接收
					public void run() {
						float x,y;
						int index;
						Log.d("lqw", ""+Command.valueOf(cmds[0]));
						switch(Command.valueOf(cmds[0])) {
						case getviews: //defined in com.robotium.solo.Solo
							getViews();
							break;
						case getactivities:
							getActivities();
							break;
						case clickonscreen:
							//String clickCmd = CLICKONSCREEN+"_"+x+"_"+y;
							x = Float.parseFloat(cmds[1]);
							y = Float.parseFloat(cmds[2]);
							if ( y >= 0 ){
								history.offer("[Action] click at "+String.valueOf(x)+" "+String.valueOf(y));
								printLog();
								clickOnScreen(x, y);
							} else {
								y = -y;
								history.offer("[Action] double clicks at "+String.valueOf(x)+" "+String.valueOf(y));	
								clickOnScreen(x, y);clickOnScreen(x, y);
								printLog();

							}
							break;
						case doubleclickonscreen:
							x = Float.parseFloat(cmds[1]);
							y = Float.parseFloat(cmds[2]);
							history.offer("[Action] click at "+String.valueOf(x)+" "+String.valueOf(y));
							printLog();
							clickOnScreen(x, y);
							clickOnScreen(x, y);
							break;
						case longclickonscreen:
							x = Float.parseFloat(cmds[1]);
							y = Float.parseFloat(cmds[2]);
							history.offer("[Action] click at "+String.valueOf(x)+" "+String.valueOf(y));
							printLog();
							longClickOnScreen(x, y);
							break;
						case clickonview:
							history.offer("[Action] click on view: "+cmds[1]);
							printLog();
							clickOnView(cmds[1]);
							break;
						case longclickonview:
							history.offer("[Action] long click on view: "+cmds[1]);
							printLog();
							longClickOnView(cmds[1]);
							break;
						case pressbackkey:
							history.offer("[Action] press back");
							printLog();
							pressBack();
							break;
						case presssearchkey:
							history.offer("[Action] press search");
							printLog();
							pressSearch();
							break;
						case setbarvalue:
							int barId = Integer.parseInt(cmds[1]);
							int value = Integer.parseInt(cmds[2]);
							history.offer("[Action] set bat value: "+String.valueOf(barId)+" as "+String.valueOf(value));
							printLog();
							setBarValue(barId,value);
							break;
						case scrollup:
							history.offer("[Action] scroll up");
							printLog();
							scrollUp();
							break;
						case scrolldown:
							history.offer("[Action] scroll down");
							printLog();
							scrollDown();
							break;
						case scrollleft:
							history.offer("[Action] scroll left");
							printLog();
							scrollLeft();
							break;
						case scrollright:
							history.offer("[Action] scroll right");
							printLog();
							scrollRight();
							break;
						case rotate2landscape:
							history.offer("[Action] rotate to landscape");
							printLog();
							rotate2landscape();
							break;
						case rotate2portrait:
							history.offer("[Action] rotate to portrait");
							printLog();
							rotate2portrait();
							break;
						case appropauseresume:
							approPauseResume();
							break;
						case approstopstart:
							approStopStart();
							break;
						case approrelaunch:
							approRelaunch();
							break;
						case faithrelaunch:
							faithRelaunch();
							break;
						case settext:
							index = Integer.parseInt(cmds[1]);
							history.offer("[Action] set Text for "+String.valueOf(index)+" as "+cmds[2]);
							printLog();
							setText(index,cmds[2]);
							break;
						case settimepicker:
							index = Integer.parseInt(cmds[1]);
							int hour = Integer.parseInt(cmds[2]);
							int minute = Integer.parseInt(cmds[3]);
							setTimePicker(index,hour,minute);
							break;
						case setdatepicker:
							index = Integer.parseInt(cmds[1]);
							int year = Integer.parseInt(cmds[2]);
							int month = Integer.parseInt(cmds[3]);
							int day = Integer.parseInt(cmds[4]);
							setDatePicker(index,year,month,day);
							break;
						case scrolldownlist:
							index = Integer.parseInt(cmds[1]);
							history.offer("[Action] scroll down list for "+String.valueOf(index));
							printLog();
							scrollDownList(index);
							break;
						case setprogressbar:
							index = Integer.parseInt(cmds[1]);
							int barValue =  Integer.parseInt(cmds[2]);
							history.offer("[Action] set progress bar for "+String.valueOf(index)+" as "+String.valueOf(barValue));
							printLog();
							setProgressBar(index,barValue);
							break;
						case pressmenuitem:
							index = Integer.parseInt(cmds[1]);
							history.offer("[Action] press menu item");
							printLog();
							pressMenuItem(index);
							break;
						case unlock:
							history.offer("[Action] unlock");
							unLock();
							break;
						case scroll:
							float x1 = Float.parseFloat(cmds[1]);
							float y1 = Float.parseFloat(cmds[2]);
							float x2 = Float.parseFloat(cmds[3]);
							float y2 = Float.parseFloat(cmds[4]);
							history.offer("[Action] drag from ["+String.valueOf(x1)+","+String.valueOf(y1)+"] to ["+String.valueOf(x2)+","+String.valueOf(y2)+"]");
							printLog();
							drag(x1,y1,x2,y2);
							break;
						case clickactionbarhome:
							clickOnActionBarHomeButton();
							history.offer("[Action] click bar home");
							printLog();
							break;
						case clickactionbar:
							int actionBarId = Integer.parseInt(cmds[1]);
							history.offer("[Action] click action bar");
							printLog();
							clickOnActionBarItem(actionBarId);
							break;
						case clicklist:
							history.offer("[Action] click list "+cmds[1]+" at "+cmds[2]);
							printLog();
							clickInList(  Integer.parseInt(cmds[1]),  Integer.parseInt(cmds[2]));
							break;
						case clicklonglist:
							history.offer("[Action] long click list "+cmds[1]+" at "+cmds[2]);
							printLog();
							clickLongInList( Integer.parseInt(cmds[1]),  Integer.parseInt(cmds[2]));
							break;
						case setnavigationdrawer:
							setNavigationDrawer(Integer.parseInt(cmds[1]),  Integer.parseInt(cmds[2]));
							break;
						case finish:
							tearDown();
							break;
							
						//added by lqw at 2015-12-15
						case setdefaulttext:
							history.offer("[Action] set text "+cmds[1]);
							printLog();
							setDefaultText(cmds[1], true);
							break;
						case scrollandclick:
							history.offer("[Action] scroll "+cmds[1]+" and click at ["+cmds[2]+", "+cmds[3]+"]");
							printLog();
							scrollAndClick(Integer.parseInt(cmds[1]), Float.parseFloat(cmds[2]), Float.parseFloat(cmds[3]));
							break;
							
						//added at 2016-4-21
						case getBlcokingThreadsID:
							getBlcokingThreads();
							break;
						case unblockThread:
							unblockThread(Integer.parseInt(cmds[1]));
							break;
						case unblockThreadAll:
							unblockThreadAll0();
							break;
						case setSemInitValue:
							setSemInitValue0();
							break;
						default:
							Log.v("lqw", "unknow command");
							break;
						}
					}
				}).start();
			}
		}
		catch (Exception e) {
			Log.i("lqw", "connect: " + e.toString());
		}
	}
	@Override
	public void onDestroy() {
		try {
			socket.close();
		} catch (IOException e) {
			Log.i("lqw", "onDestroy: " + e.toString());
		}
	}

	enum Command {
		getviews, 
		getactivities, 
		clickonscreen,
		longclickonscreen,
		clickonview,
		doubleclickonscreen,
		longclickonview,
		finish,
		pressbackkey,
		presssearchkey,
		setbarvalue,
		scrollup,
		scrolldown,
		scrollleft,
		scrollright,
		rotate2landscape,
		rotate2portrait,
		appropauseresume,
		approstopstart,
		faithrelaunch,
		approrelaunch,
		settext,
		settimepicker,
		setdatepicker,
		scrolldownlist,
		setprogressbar,
		pressmenuitem,
		unlock,
		scroll,
		clickactionbarhome,
		clickactionbar,
		clicklist,
		clicklonglist,
		setnavigationdrawer,
		
		setdefaulttext,
		scrollandclick,
		
		//added at 2016-4-21
		getBlcokingThreadsID,
		unblockThread,
		unblockThreadAll,
		setSemInitValue
	};

	public static final String LISTENER_SIG_P = "LSP";
	public static final String VIEW_SIG_P = "LQWVSP";
	
	// obtain current objects on the screen
	private void getViews(){
		//added by lqw at 2015-12-10
		Class<?> viewClazz = null;
		Class<?> adpterViewClazz = null;
		Class<?> absListViewClazz = null;
		Class<?> listenerInfoClazz = null;
		Class<?> expListViewClazz = null;
		try {
			viewClazz = Class.forName("android.view.View");
			adpterViewClazz = Class.forName("android.widget.AdapterView");
			absListViewClazz = Class.forName("android.widget.AbsListView");
			expListViewClazz = Class.forName("android.widget.ExpandableListView");
			
			listenerInfoClazz = Class.forName("android.view.View$ListenerInfo");
		} catch (ClassNotFoundException e) {
			// TODO Auto-generated catch block
			Log.i("lqw", e.toString());
			
		}


		String result = "";
		ArrayList<View> views = solo.getCurrentViews();
		if(views==null||views.size()==0||views.isEmpty())
		{
			try {
				out.writeUTF("empty");
				Log.d("lqw","empty result");
				out.flush();
				return;
			} catch (IOException e) {
				Log.d("lqw", "empty getViews exception " + e.toString());
				//	e.printStackTrace();
			}
		}
		Log.d("getViews"," "+views.size());
		int timePickerNum = 0;
		int datePickerNum= 0;
		int absListViewNum = 0;
		int listViewNum = 0;
		int editTextNum = 0;
		int menuItemNum = 0;
		int progressBarNum = 0;
		int navigationDrawerNum = 0;
		int textViewNum=0;
		int othersNum=0;
		for (int i=0; i<views.size(); i++) {
			View viewNode = views.get(i);
			/*	Field listenerInfoField = null;
					listenerInfoField = Class.forName("android.view.View").getDeclaredField("mListenerInfo");
			    	if (listenerInfoField != null) {
					    listenerInfoField.setAccessible(true);
					}
					Object myLiObject = null;
					myLiObject = listenerInfoField.get(viewNode);

					// get the field mOnClickListener, that holds the listener and cast it to a listener
				//	Field listenerField = null;
					Field listenerField1 = Class.forName("android.view.View$ListenerInfo").getDeclaredField("mOnClickListener");
					Field listenerField2 = Class.forName("android.view.View$ListenerInfo").getDeclaredField("mOnLongClickListener");
					Field listenerField3 = Class.forName("android.view.View$ListenerInfo").getDeclaredField("mOnDragListener");
					int listener = 0;
					if(listenerField1!=null)
						listener += 1;
					if(listenerField2!=null)
						listener += 2;
					if(listenerField3!=null)
						listener += 4;*/
			int[] location = new int[2];
			viewNode.getLocationOnScreen(location);
			String ty = ""; 

			//added by lqw at 2015-12-10 to get the listener of a View
			//Log.i("lqw", "get the listenerInfo");
			String listenerSignature = "";
			try {
				//View
				{
					Field listenerInfoField = viewClazz.getDeclaredField("mListenerInfo");
	
					listenerInfoField.setAccessible(true);
					Object myListenerInfoObj = listenerInfoField.get(viewNode);
	
					if (myListenerInfoObj != null) {
						//onClick
						Field clickListenerField = listenerInfoClazz.getDeclaredField("mOnClickListener");
						View.OnClickListener myClickListener = (View.OnClickListener) clickListenerField.get(myListenerInfoObj);
						
						String signature1 = getListenerInfo(myClickListener, "onClick");
						listenerSignature = addListenerSignature(listenerSignature, signature1);
						
						//onLongClick
						Field longClickListenerField = listenerInfoClazz.getDeclaredField("mOnLongClickListener");
						longClickListenerField.setAccessible(true);
						View.OnClickListener myListener = (View.OnClickListener) longClickListenerField.get(myListenerInfoObj);
						
						String signature2 = getListenerInfo(myListener, "onLongClick");
						listenerSignature = addListenerSignature(listenerSignature, signature2);
					}
				}
			}	catch (Exception e) {
				// TODO Auto-generated catch block
				Log.i("lqw", "View: " + e.toString());
			}
				
				//AdapterView
			try{
				if (viewNode instanceof AdapterView) {
					//onItemClick
					Field itemClickListenerField = adpterViewClazz.getDeclaredField("mOnItemClickListener");
					itemClickListenerField.setAccessible(true);
					AdapterView.OnItemClickListener myItemClickListener = 
							(AdapterView.OnItemClickListener) itemClickListenerField.get(viewNode);
					
					String signature1 = getListenerInfo(myItemClickListener, "onItemClick");
					listenerSignature = addListenerSignature(listenerSignature, signature1);
					
					//onItemLongClick
					Field itemLongClickListenerField = adpterViewClazz.getDeclaredField("mOnItemLongClickListener");
					itemLongClickListenerField.setAccessible(true);
					AdapterView.OnItemClickListener myItemLongClickListener = 
							(AdapterView.OnItemClickListener) itemLongClickListenerField.get(viewNode);
					
					String signature2 = getListenerInfo(myItemLongClickListener, "onItemLongClick");
					listenerSignature = addListenerSignature(listenerSignature, signature2);
				}
				
				
			} catch (Exception e) {
				// TODO Auto-generated catch block
				Log.i("lqw", "AdapterView: " + e.toString());
			}
				
			try {
				//AbsListView
				if (viewNode instanceof AbsListView) {
					//onScroll
					Field listenerField = absListViewClazz.getDeclaredField("mOnScrollListener");
					listenerField.setAccessible(true);
					AbsListView.OnScrollListener myListener = (AbsListView.OnScrollListener) listenerField.get(viewNode);
					
					String signature = getListenerInfo(myListener, "onScroll");
					listenerSignature = addListenerSignature(listenerSignature, signature);
				}
			}  catch (Exception e) {
				// TODO Auto-generated catch block
				Log.i("lqw", "AbsListView: " + e.toString());
			}
			
			try {
				if (viewNode instanceof ExpandableListView) {
					//onChildClick
					Field listenerField = expListViewClazz.getDeclaredField("mOnChildClickListener");
					listenerField.setAccessible(true);
					ExpandableListView.OnChildClickListener myListener = 
							(ExpandableListView.OnChildClickListener) listenerField.get(viewNode);
					
					String signature = getListenerInfo(myListener, "onChildClick");
					listenerSignature = addListenerSignature(listenerSignature, signature);
				}
				//listenerSignature = listenerSignature.substring(0, listenerSignature.length() - LISTENER_SIG_P.length());

			} catch (Exception e) {
				// TODO Auto-generated catch block
				Log.i("lqw", "ExpandableListView: " + e.toString());
			}



			if(viewNode.isShown()){
				if(viewNode instanceof TimePicker){
					ty = viewNode.getClass().getName()+"___"+viewNode.getId()+"___"+timePickerNum+"___"+location[0]+"___"+location[1]+"___"+viewNode.getWidth()+"___"+viewNode.getHeight()+"___"+viewNode.isClickable()+"___"+viewNode.canScrollHorizontally(-1)+"___"+viewNode.canScrollHorizontally(1)+"___"+viewNode.canScrollVertically(-1)+"___"+viewNode.canScrollVertically(1)+"___"+viewNode.isInEditMode();
					result = result + ty + "___~~~";
					timePickerNum++;
				}
				else if(viewNode instanceof DatePicker){
					ty = viewNode.getClass().getName()+"___"+viewNode.getId()+"___"+datePickerNum+"___"+location[0]+"___"+location[1]+"___"+viewNode.getWidth()+"___"+viewNode.getHeight()+"___"+viewNode.isClickable()+"___"+viewNode.canScrollHorizontally(-1)+"___"+viewNode.canScrollHorizontally(1)+"___"+viewNode.canScrollVertically(-1)+"___"+viewNode.canScrollVertically(1)+"___"+viewNode.isInEditMode();
					result = result + ty + "___~~~";
					datePickerNum++;
				}
				else if(viewNode instanceof ListView){
					ty = viewNode.getClass().getName()+"___"+viewNode.getId()+"___"+listViewNum+"___"+location[0]+"___"+location[1]+"___"+viewNode.getWidth()+"___"+viewNode.getHeight()+"___"+viewNode.isClickable()+"___"+viewNode.canScrollHorizontally(-1)+"___"+viewNode.canScrollHorizontally(1)+"___"+viewNode.canScrollVertically(-1)+"___"+viewNode.canScrollVertically(1)+"___"+viewNode.isInEditMode();
					result = result + ty + "___~~~";
					listViewNum++;
				}
				else if(viewNode instanceof EditText){
					ty = viewNode.getClass().getName()+"___"+viewNode.getId()+"___"+editTextNum+"___"+location[0]+"___"+location[1]+"___"+viewNode.getWidth()+"___"+viewNode.getHeight()+"___"+viewNode.isClickable()+"___"+viewNode.canScrollHorizontally(-1)+"___"+viewNode.canScrollHorizontally(1)+"___"+viewNode.canScrollVertically(-1)+"___"+viewNode.canScrollVertically(1)+"___"+viewNode.isInEditMode();
					String text = ((EditText) viewNode).getText().toString();
					if (text.length() == 0)
						result = result + ty + "___~~~";
					else
						result = result + ty + "___" + text;
					editTextNum++;
				}
				else if(viewNode instanceof MenuItem ||viewNode.getClass().getName().toString().contains("MenuItem")){
					ty = viewNode.getClass().getName()+"___"+viewNode.getId()+"___"+menuItemNum+"___"+location[0]+"___"+location[1]+"___"+viewNode.getWidth()+"___"+viewNode.getHeight()+"___"+viewNode.isClickable()+"___"+viewNode.canScrollHorizontally(-1)+"___"+viewNode.canScrollHorizontally(1)+"___"+viewNode.canScrollVertically(-1)+"___"+viewNode.canScrollVertically(1)+"___"+viewNode.isInEditMode();
					result = result + ty + "___~~~";
					menuItemNum++;
				}
				else if(viewNode instanceof ProgressBar){  //不知道是哪种
					String type = "."+"ProgressBar";
					if(viewNode instanceof SeekBar)
						type = "."+"SeekBar";
					if(viewNode instanceof RatingBar)
						type = "."+"RatingBar";
					ty = viewNode.getClass().getName()+type+"___"+viewNode.getId()+"___"+progressBarNum+"___"+location[0]+"___"+location[1]+"___"+viewNode.getWidth()+"___"+viewNode.getHeight()+"___"+viewNode.isClickable()+"___"+viewNode.canScrollHorizontally(-1)+"___"+viewNode.canScrollHorizontally(1)+"___"+viewNode.canScrollVertically(-1)+"___"+viewNode.canScrollVertically(1)+"___"+viewNode.isInEditMode();
					result = result + ty + "___~~~";
					progressBarNum++;
				}
				/*else if(viewNode instanceof DrawerLayout){
							 ty = viewNode.getClass().getName()+"."+"NavigationDrawer"+"___"+viewNode.getId()+"___"+navigationDrawerNum+"___"+location[0]+"___"+location[1]+"___"+viewNode.getWidth()+"___"+viewNode.getHeight()+"___"+viewNode.isClickable()+"___"+viewNode.canScrollHorizontally(-1)+"___"+viewNode.canScrollHorizontally(1)+"___"+viewNode.canScrollVertically(-1)+"___"+viewNode.canScrollVertically(1)+"___"+viewNode.isInEditMode();
							 result = result + ty + "___~~~";
							 navigationDrawerNum++;
						}*/
				else if (viewNode instanceof TextView) {
					ty = viewNode.getClass().getName()+"___"+viewNode.getId()+"___"+textViewNum+"___"+location[0]+"___"+location[1]+"___"+viewNode.getWidth()+"___"+viewNode.getHeight()+"___"+viewNode.isClickable()+"___"+viewNode.canScrollHorizontally(-1)+"___"+viewNode.canScrollHorizontally(1)+"___"+viewNode.canScrollVertically(-1)+"___"+viewNode.canScrollVertically(1)+"___"+viewNode.isInEditMode();
					if(((TextView)viewNode).getText().equals(""))
						result = result + ty + "___" +"~~~";  //button
					else
						result = result + ty + "___" + ((TextView)viewNode).getText() ;
					//result = result + ty + "___" + "+++" ;
					textViewNum++;
				} 
				else{
					ty = viewNode.getClass().getName()+"___"+viewNode.getId()+"___"+othersNum+"___"+location[0]+"___"+location[1]+"___"+viewNode.getWidth()+"___"+viewNode.getHeight()+"___"+viewNode.isClickable()+"___"+viewNode.canScrollHorizontally(-1)+"___"+viewNode.canScrollHorizontally(1)+"___"+viewNode.canScrollVertically(-1)+"___"+viewNode.canScrollVertically(1)+"___"+viewNode.isInEditMode();
					result = result + ty + "___~~~";
					othersNum++;
				}

				//added by lqw at 2015-12-11, for get the listeners of view
				if (listenerSignature.length() == 0)
					result += "___none";
				else
					result += "___" + listenerSignature;
				
				
				//if it is not the last view, add a pivot, i.e. "*"
				//we modified pivot (*) as VIEW_SIG_P (i.e. LQWVSP). because some texts include * 
				if(i!=views.size()-1)
					result = result+VIEW_SIG_P;
			}
		}
		try {
			out.writeUTF(result);
			Log.d("lqw", "send result");
			out.flush();
		} catch (IOException e) {
			Log.d("lqw", "getResult: " + e.toString());
			//	e.printStackTrace();
		}


	}
	public void unLock(){
		solo.unlockScreen();
	}

	// obtain activities opened so far
	private void getActivities() {
		ArrayList<View> views = solo.getCurrentViews();
		String result = " ";
		if(views==null||views.size()==0||views.isEmpty())
		{
			result = "empty";
		}
		else{
			Activity currentActivity = solo.getCurrentActivity();
			result = currentActivity.getClass().getName();
			
			//added by lqw at 2015-12-12
			result += "___";    //because it is "___" in getView
			
			result += "none";
			String listenerSignature = ""; 
			
			Class<?> clazz = currentActivity.getClass();
			for (Method m : clazz.getMethods()) {
				if (m.getName().equals("onKeyDown")) {
					String tmp = m.toString();
					if (!tmp.equals("public boolean android.app.Activity.onKeyDown(int,android.view.KeyEvent)")) {
						String signature = convertSignature(clazz.getName(), tmp, m.getName());
						
						Log.i("lqw", tmp);
						Log.i("lqw", signature);
						
						listenerSignature = addListenerSignature(listenerSignature, signature);
					}
				} else if (m.getName().equals("onOptionsItemSelected")) {
					String tmp = m.toString();
					if (!tmp.equals("public boolean android.app.Activity.onOptionsItemSelected(android.view.MenuItem)")) {
						String signature = convertSignature(clazz.getName(), tmp, m.getName());
						
						Log.i("lqw", tmp);
						Log.i("lqw", signature);
						
						listenerSignature = addListenerSignature(listenerSignature, signature);
					}
				}
			}
		}
		try {
			out.writeUTF(result);
			out.flush();
		} catch (IOException e) {
			Log.d("lqw", "getActivity exception");
			//	e.printStackTrace();
		}
	}

	// stop the app under test
	private void tearDown() {
		try {
			out.writeUTF("finish");
			out.flush();
		} catch (IOException e) {
			//	e.printStackTrace();
			Log.i("lqw", "tear down: " + e.toString());
		}
		finish(0, new Bundle());
	}


	private void clickOnScreen(float x, float y){
		solo.clickOnScreen(x, y);
	}
	private void longClickOnScreen(float x, float y){
		solo.clickLongOnScreen(x, y);
	}
	private void clickOnView(String viewText){
		solo.clickOnText(viewText);
	}
	public void longClickOnView(String viewText) {
		solo.clickLongOnText(viewText);
	}
	public void pressBack(){
		solo.goBack();
	}
	public void pressSearch(){
		solo.pressSoftKeyboardSearchButton();
	}


	public void setBarValue(int id, int value){   //这样的发送和接收可以环环相扣收到吗，然后试试，不行就不返回东西了
		View viewNode = solo.getView(id);
		if(viewNode instanceof SeekBar){
			SeekBar seekBar = (SeekBar)viewNode;
			if(value<seekBar.getMax()){
				solo.setProgressBar(id, value);
			}

		}
		else if(viewNode instanceof RatingBar){
			RatingBar ratingBar = (RatingBar)viewNode;
			if(value<ratingBar.getMax()){
				solo.setProgressBar(id, value);
			}	
		}
		else{	
		}
	}
	public void scrollDown(){
		solo.scrollDown();
	}
	public void scrollUp(){
		solo.scrollUp();
	}
	public void scrollLeft(){
		solo.scrollToSide(solo.LEFT);


	}
	public void scrollRight(){
		solo.scrollToSide(solo.RIGHT);
	}
	public void rotate2landscape(){
		solo.setActivityOrientation(solo.LANDSCAPE);


	}
	public void rotate2portrait(){
		solo.setActivityOrientation(solo.PORTRAIT);
	}

	public void approPauseResume(){
		Log.d("剖色","PAUSERESUME");
		Activity currentActivity = solo.getCurrentActivity();
		this.callActivityOnPause(currentActivity);
		this.callActivityOnResume(currentActivity);
		Log.d("剖色","PAUSERESUME over");
	}
	public void approStopStart(){
		Activity currentActivity = solo.getCurrentActivity();
		Log.d("死党破","STOPSTART11");
		this.callActivityOnPause(currentActivity);
		Log.d("死党破","STOPSTART22");
		this.callActivityOnSaveInstanceState(currentActivity, null);
		Log.d("死党破","STOPSTART33");
		this.callActivityOnStop(currentActivity);
		Log.d("死党破","STOPSTART44");
		this.callActivityOnRestart(currentActivity);
		Log.d("死党破","STOPSTART55");
		this.callActivityOnStart(currentActivity);
		Log.d("死党破","STOPSTsART66");
		this.callActivityOnResume(currentActivity);
		Log.d("死党破","STOPSTART77");
		Log.d("死党破","STOPSTART over");
	}
	public void approRelaunch(){  //近似模式
		Log.d("approre老吃","Relaunch ");
		Activity currentActivity = solo.getCurrentActivity();
		currentActivity.recreate();
		Log.d("approrelaunch老吃","Relaunch over");
	}
	public void faithRelaunch(){
		Log.d("faithful老吃","Relaunch ");
		/*Activity currentActivity = solo.getCurrentActivity();
		Log.d("faithful老吃","Relaunch 11");
		//onPause() -> onStop() -> onDestroy() -> onCreate()
		this.callActivityOnPause(currentActivity);
		Log.d("faithful老吃","Relaunch 22");
		this.callActivityOnStop(currentActivity);
		Log.d("faithful老吃","Relaunch 33");
		this.callActivityOnDestroy(currentActivity);
		Log.d("faithful老吃","Relaunch 44");*/
		//this.callActivityOnCreate(currentActivity, null);  //这个 null可以吗
		rotate2landscape();
		rotate2portrait();
		Log.d("faithful老吃","Relaunch 55");
	}
	public void drag(float x1,float y1,float x2,float y2){
		solo.drag(x1,y1,x2,y2,1);
	}

	public void setText(int index,String text){
		solo.typeText(index, text);	
	}
	public void setTimePicker(int index,int hour,int minute){
		solo.setTimePicker(index, hour, minute);
	}
	public void setDatePicker(int index, int year,int month,int day){
		solo.setDatePicker(index, year, month, day);
	}
	public void scrollDownList(int index){
		//Log.i("lqw", "scrollDownList " + index);
		
		solo.scrollDownList(index);
	}
	public void setProgressBar(int index,int value){
		solo.setProgressBar(index,value);
	}

	public void pressMenuItem(int index){
		solo.pressMenuItem(index);
	}
	public void clickInList(int index, int line){
		solo.clickInList(line, index);
	}
	public void clickLongInList(int index, int line){
		solo.clickLongInList(line, index);
	} 
	public void clickOnActionBarHomeButton() {
		solo.clickOnActionBarHomeButton();
	}
	public void clickOnActionBarItem(int id){
		solo.clickOnActionBarItem(id);
	}
	public void setNavigationDrawer(int index,int isOpen){
		if(isOpen==1)
			solo.setNavigationDrawer(solo.OPENED);
		else
			solo.setNavigationDrawer(solo.CLOSED);
	}

	
	private String getListenerInfo(Object myListener, String methodName) throws Exception {
		
		if (myListener != null) {
			//Log.i("lqw", "listener class name: " + myListener.getClass().toString());

			Class<?> oclClazz = myListener.getClass();
			Method[] methods = oclClazz.getDeclaredMethods();

			//for (Method method : methods)
			//	Log.i("lqw", "method: " + method.getName());
			
			
			String signature = "";
			for (Method method : methods) {
				if (method.getName().equals(methodName)) {
					signature = convertSignature(oclClazz.getName(), method.toString(), methodName);
					
					//Log.i("lqw", signature);
					return signature;
				}
			}
		}
		
		return "";
	}
	
	private String convertSignature(String clazzName, String oldSignature, String methodName) {
		int index = oldSignature.indexOf(methodName);
		String sub = oldSignature.substring(index);
		return "<" + clazzName + ": void " + sub + ">";
	}
	
	public String addListenerSignature(String listenerSignature, String signature) {
		if (signature.length() > 0) {
			if (listenerSignature.length() == 0)
				return signature;
			else
				return listenerSignature + LISTENER_SIG_P + signature;
		} else
			return listenerSignature;
	}
	
	/*
	private String getListenerInfoFromAdapterView(Object myListener, String methodName) throws Exception {
	
		if (myListener != null) {
			Log.i("lqw", myListener.getClass().toString());

			Class<?> oclClazz = myListener.getClass();
			Method[] methods = oclClazz.getDeclaredMethods();

			for (Method method : methods)
				Log.i("lqw", method.getName());
			
			String signature = "";
			for (Method method : methods) {
				if (method.getName().equals(methodName)) {
					String tmp = method.toString();
					int index = tmp.indexOf(methodName);
					String sub = tmp.substring(index);
					signature = "<" + oclClazz.getName() + ": void " + sub + ">";
					
					Log.i("lqw", signature);
					return signature;
				}
			}
		}
		
		return "";
	}
	*/
	
	//added by lqw at 2015-12-15
	//if it is called by Troy, it should not send "OK"
	private void setDefaultText(String text, boolean cmd) {
		Log.i("lqw", "start set default text 1");
		ArrayList<View> views = solo.getCurrentViews();
		if (views == null || views.isEmpty()) {
			Log.i("lqw", "set default empty view");
			if (cmd == true) {
				try {
					out.writeUTF("OK");
					out.flush();
				} catch (IOException e) {
					Log.i("lqw", "empty default exception: " + e.toString());
				}
			}
			return;
		}
		Log.i("lqw", "start set default text 2");
		
		solo.hideSoftKeyboard();
		
		int editTextViewCnt = 0;
		for (int i = 0; i < views.size(); i++) {
			View view = views.get(i);
			if (view instanceof EditText) {
				EditText et = (EditText) view;
				String curText = et.getText().toString();
				Log.d("lqw", editTextViewCnt + ": " + curText);
				if (curText.length() == 0) {
					//Log.d("lqw", editTextViewCnt + "");
					solo.typeText(editTextViewCnt, text);
				}
				editTextViewCnt++;
			}
		}
		
		if (cmd == true) {
			try {
				out.writeUTF("OK");
				out.flush();
			} catch (IOException e) {
				Log.d("lqw", "setDefaultText exception");
			}
		}
	}
	
	private void scrollAndClick(int l, float x, float y) {
		try {
			Log.i("lqw", "scroll");
			scrollDownList(l);
			Thread.sleep(100);
			
			for (int i = 0; i < 4; ++i) {
				Log.i("lqw", "scroll");
				scrollDownList(l);
				//Thread.sleep(100);
				
				Log.i("lqw", "click");
				clickOnScreen(x, y);
				//Thread.sleep(100);
			}
			
		} catch (InterruptedException ie) {
			Log.i("lqw", ie.toString());
		}
	}
	
	
	//added at 2016-4-21
	Class<?> infoClazz;
	Map<?, ?> sems;
	private void getSems() {
		try {
			if (sems == null) {
				infoClazz = Class.forName("com.lqw.LQWInformationClazz");
			
				Field field = infoClazz.getField("__SEMS");
				sems = (Map<?, ?>)field.get(null);
			}
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		} catch (NoSuchFieldException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IllegalAccessException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IllegalArgumentException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	private void getBlcokingThreads() {
		String result = "ID:";
		
		try {
			getSems();
			
			for (Object id : sems.keySet()) {
				result += "_" + id;
			}
		} catch (IllegalArgumentException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		try {
			out.writeUTF(result);
			Log.d("lqw", "send result");
			out.flush();
		} catch (IOException e) {
			Log.d("lqw", "unblock all: " + e.toString());
			//	e.printStackTrace();
		}
	}
	private void unblockThread(int id) {
		if (sems == null)
			getSems();
		
		Semaphore sem = (Semaphore) sems.get(id);
		sem.release();
		sems.remove(id);

	}
	
	private void unblockThreadAll0() {
		if (sems == null)
			getSems();
		
		boolean finished = false;
		while (true) {
			finished = true;
			
			Iterator<?> it = sems.entrySet().iterator();
			while (it.hasNext()) {
				Semaphore sem = (Semaphore) ((Map.Entry<?, ?>) it.next()).getValue();
				sem.release();
				it.remove();
				finished = false;
			}
			if (finished)
				break;
			
			try {
				Thread.sleep(1000);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		try {
			out.writeUTF("OK");
			out.flush();
		} catch (IOException e) {
			Log.d("lqw", "unblockThreadAll0 exception");
		}
	}
	
	private void setSemInitValue0() {
		if (infoClazz == null)
			getSems();
		try {
			Field initValue = infoClazz.getField("__semInitValue");
			initValue.setInt(null, 1);
			
		} catch (NoSuchFieldException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IllegalAccessException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IllegalArgumentException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}
	
	/*
	 * added by alex 
	 */
	
	private void printLog() {
		for(String action: history){
			Log.v("lqw", action);
		}
	}
}

