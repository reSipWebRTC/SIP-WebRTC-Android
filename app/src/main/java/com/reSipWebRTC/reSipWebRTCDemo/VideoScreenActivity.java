package com.reSipWebRTC.reSipWebRTCDemo;

import android.annotation.SuppressLint;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Configuration;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.PowerManager;
import android.view.KeyEvent;
import android.view.OrientationEventListener;
import android.view.Window;
import android.view.WindowManager;
import android.widget.LinearLayout;

import androidx.appcompat.app.AppCompatActivity;

import com.reSipWebRTC.sdk.CallLogBean;
import com.reSipWebRTC.sdk.SipCallConnectedListener;
import com.reSipWebRTC.sdk.SipCallEndListener;
import com.reSipWebRTC.service.PhoneService;
import com.reSipWebRTC.util.Contacts;
import com.reSipWebRTC.util.Debug;
import com.reSipWebRTC.util.Direction;

public class VideoScreenActivity extends AppCompatActivity implements SipCallConnectedListener, SipCallEndListener {
	private final String TAG = VideoScreenActivity.class.getSimpleName();
	static public VideoScreenActivity the_vid_ui = null;
	private OrientationEventListener orientationListener;
	int currentOrientation = OrientationEventListener.ORIENTATION_UNKNOWN;
	int currentCameraOrientation = 0;
	private Handler handler = new Handler();
	public static VideoScreenActivity instance() {
		return the_vid_ui;
	}
    private int call_id = -1;

	String number;
	VideoWaitFragment videoWaitFragment = null;

	@SuppressLint("InvalidWakeLockTag")
    @Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		the_vid_ui = this;
		Debug.i(TAG, "onCreate ");

		if (this.getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE) {
			Debug.i(TAG, "landscape");
		} else if (this.getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT) {
			Debug.i(TAG, "portrait");
		}

		//getWindow().addFlags(
				//WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED
						//| WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
						//| WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON);

		final Window win = getWindow();
		win.addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED
				| WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD
				| WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
				| WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON);

		wakeLock = ((PowerManager) getSystemService(POWER_SERVICE))
				.newWakeLock(PowerManager.SCREEN_BRIGHT_WAKE_LOCK
						| PowerManager.ON_AFTER_RELEASE, "WakeLockActivity");

		setContentView(R.layout.activity_video);

        init();
	}

    private void init() {
        IntentFilter intentFilter = new IntentFilter(Contacts.BROADCAST_CATION);
        Intent intent = getIntent();
        int state = intent.getIntExtra(Contacts.PHONESTATE, 0);
        call_id = intent.getIntExtra(Contacts.PHONECALLID, 0);

        boolean isFront = intent.getBooleanExtra(Contacts.PHONEFRONT, false);
        FragmentManager fm = getFragmentManager();
        if (isFront) {
            VideoTalkFragment videoTalkFragment = new VideoTalkFragment();
            FragmentTransaction ft = fm.beginTransaction();
            ft.add(R.id.layout_fl, videoTalkFragment, VIDEOWAITE);
            ft.commit();
        } else {
            videoWaitFragment = new VideoWaitFragment();
            Bundle bundle = new Bundle();
            bundle.putInt(Contacts.PHONESTATE, state);
            bundle.putInt(Contacts.PHONECALLID, call_id);
            String pushType = intent.getStringExtra(Contacts.NOTIFACTION_TYPE);
            number = intent.getStringExtra(Contacts.PHONNUMBER);
            bundle.putString(Contacts.PHONNUMBER, number);
            bundle.putString(Contacts.PHONNUMBER, number);
            bundle.putString(Contacts.NOTIFACTION_TYPE, pushType);
            bundle.putBoolean(Contacts.PHONEFRONT, isFront);
            videoWaitFragment.setArguments(bundle);
            FragmentTransaction ft = fm.beginTransaction();
            ft.add(R.id.layout_fl, videoWaitFragment, VIDEOWAITE);
            ft.commit();
        }

        orientationListener = new OrientationEventListener(this,
                SensorManager.SENSOR_DELAY_UI) {
            public void onOrientationChanged(int orientation) {
                if (orientation != ORIENTATION_UNKNOWN) {
                    currentOrientation = orientation;
                    //Debug.d(TAG, "onOrientationChanged "+orientation);
                }
            }
        };

        orientationListener.enable();

        PhoneService.instance().setSipCallConnectedListener(this);
        PhoneService.instance().setSipCallEndListener(this);
    }

    PowerManager.WakeLock wakeLock;
	private final String VIDEOWAITE = "VIDEOWAITE";

	@Override
	protected void onDestroy() {
		super.onDestroy();
		the_vid_ui = null;
	}

	@Override
	protected void onPause() {
		super.onPause();
		if (wakeLock != null) {
			wakeLock.release();
		}

		//Debug.i(TAG, "视频通话 界面被暂停");

		if (videoWaitFragment != null && videoWaitFragment.inVideoTalking()) {
			/**
			 * 正在视频通话，切换出去时需要关闭摄像头
			 */
			//Debug.i(TAG, "视频通话=========界面被暂停");
			//videoWaitFragment.getVideoTalkFragment().stopVideoChannel();
			orientationListener.disable();
		}
	}

	@Override
	protected void onResume() {
		super.onResume();
		if (wakeLock != null) {
			wakeLock.acquire();
		}
		//Debug.i(TAG, "视频通话 界面被恢复");

		if (videoWaitFragment != null && videoWaitFragment.inVideoTalking()) {
			/**
			 * 视频界面恢复时需要重新启动摄像头
			 */
			//videoWaitFragment.getVideoTalkFragment().startVideoChannel();
			orientationListener.enable();
			/*
			 * int delay = 1000;
			 */
		}
	}

	/**
	 * 开始视频界面
	 */
	private void startVideoCallScreen(Direction dir) {
		FragmentManager fm = ((VideoScreenActivity) the_vid_ui)
				.getFragmentManager();
		VideoWaitFragment videoWaitFragment = (VideoWaitFragment) fm
				.findFragmentByTag(VIDEOWAITE);
		if (videoWaitFragment != null) {
			videoWaitFragment.startVideoCallScreen();
			orientationListener.enable();
		}
	}

	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event)
	{
		if (keyCode == event.KEYCODE_BACK) {
			return true;
		}
		return super.onKeyDown(keyCode, event);
	}
	
	@Override
	public void onConfigurationChanged(Configuration newConfig) {

		if (videoWaitFragment != null
				&& videoWaitFragment.getVideoTalkFragment() != null) {

			VideoTalkFragment vid_talk = videoWaitFragment
					.getVideoTalkFragment();

			if (newConfig.orientation == Configuration.ORIENTATION_PORTRAIT) {
				vid_talk.setVideoRenderViewOrientation(LinearLayout.VERTICAL);
			}

			if (newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE) {
				vid_talk.setVideoRenderViewOrientation(LinearLayout.HORIZONTAL);
			}
			vid_talk.SetupCameraRotation();
		}

		Debug.d(TAG, "onConfigurationChanged");
		super.onConfigurationChanged(newConfig);
	}

	@Override
	public void onCallEnd(int call_id, int status, CallLogBean mCallLogBean) {
		// TODO Auto-generated method stub
		// TODO Auto-generated method stub
		handler.postDelayed(new Runnable() {
			
			@Override
			public void run() {
				finish();
			}
		}, 1500);
	
	}


	@Override
	public void onCallConnected(int call_id) {
		this.startVideoCallScreen(PhoneService.instance().getCallDirection(call_id));
	}
}
