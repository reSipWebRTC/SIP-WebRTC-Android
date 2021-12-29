package com.reSipWebRTC.reSipWebRTCDemo;

import android.app.FragmentTransaction;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;

import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.SurfaceView;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.reSipWebRTC.sdk.CallLogBean;
import com.reSipWebRTC.sdk.CallStreamListener;
import com.reSipWebRTC.sdk.SipCallConnectedListener;
import com.reSipWebRTC.sdk.SipCallEndListener;
import com.reSipWebRTC.service.CallParams;
import com.reSipWebRTC.service.CallParamsImpl;
import com.reSipWebRTC.service.PhoneService;
import com.reSipWebRTC.util.Contacts;

public class CallActivity extends AppCompatActivity implements SipCallConnectedListener,
        SipCallEndListener, CallStreamListener, View.OnClickListener,
        KeypadFragment.OnFragmentInteractionListener {
    private static final String TAG = "CallActivity";
    private CallParams callParams = null;
    private boolean muteAudio = false;
    private boolean muteVideo = false;
    private boolean isVideo = false;
    // handler for the timer
    private Handler timerHandler = new Handler();
    int secondsElapsed = 0;
    private long timeConnected = 0;
    public static final String LIVE_CALL_PAUSE_TIME = "live-call-pause-time";
    private int callDirection = Contacts.RECEIVE_VIDEO_REQUEST;
    private String peer_number = null;
    private int call_id = -1;

    ImageButton btnMuteAudio, btnMuteVideo;
    ImageButton btnHangup;
    ImageButton btnAnswer, btnAnswerAudio;
    ImageButton btnKeypad;
    KeypadFragment keypadFragment;
    TextView lblCall, lblStatus, lblTimer;
    // Local preview screen position before call is connected.
    private static final int LOCAL_X_CONNECTING = 0;
    private static final int LOCAL_Y_CONNECTING = 0;
    private static final int LOCAL_WIDTH_CONNECTING = 100;
    private static final int LOCAL_HEIGHT_CONNECTING = 100;
    // Local preview screen position after call is connected.
    private static final int LOCAL_X_CONNECTED = 72;
    private static final int LOCAL_Y_CONNECTED = 2;
    private static final int LOCAL_WIDTH_CONNECTED = 25;
    private static final int LOCAL_HEIGHT_CONNECTED = 25;
    // Remote video screen position
    private static final int REMOTE_X = 0;
    private static final int REMOTE_Y = 0;
    private static final int REMOTE_WIDTH = 100;
    private static final int REMOTE_HEIGHT = 100;

    private FrameLayout localRenderLayout;
    private FrameLayout remoteRenderLayout;
    private SurfaceView remoteVideo;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Set window styles for fullscreen-window size. Needs to be done before
        // adding content.
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().addFlags(
                WindowManager.LayoutParams.FLAG_FULLSCREEN
                        | WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
                        | WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD
                        | WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED
                        | WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON);
        getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
        setContentView(R.layout.activity_call);

        // Initialize UI
        btnHangup = (ImageButton) findViewById(R.id.button_hangup);
        btnHangup.setOnClickListener(this);
        btnAnswer = (ImageButton) findViewById(R.id.button_answer);
        btnAnswer.setOnClickListener(this);
        btnAnswerAudio = (ImageButton) findViewById(R.id.button_answer_audio);
        btnAnswerAudio.setOnClickListener(this);
        btnMuteAudio = (ImageButton) findViewById(R.id.button_mute_audio);
        btnMuteAudio.setOnClickListener(this);
        btnMuteVideo = (ImageButton) findViewById(R.id.button_mute_video);
        btnMuteVideo.setOnClickListener(this);
        btnKeypad = (ImageButton) findViewById(R.id.button_keypad);
        btnKeypad.setOnClickListener(this);
        lblCall = (TextView) findViewById(R.id.label_call);
        lblStatus = (TextView) findViewById(R.id.label_status);
        lblTimer = (TextView) findViewById(R.id.label_timer);

        // Get Intent parameters.
        final Intent intent = getIntent();
        callDirection = intent.getIntExtra(Contacts.PHONESTATE, 0);
        peer_number = intent.getStringExtra(Contacts.PHONNUMBER);
        call_id = intent.getIntExtra(Contacts.PHONECALLID, -1);

        if (callDirection == Contacts.INVITE_VIDEO_REQUEST) {
            btnAnswer.setVisibility(View.INVISIBLE);
            btnAnswerAudio.setVisibility(View.INVISIBLE);
        } else {
            btnAnswer.setVisibility(View.VISIBLE);
            btnAnswerAudio.setVisibility(View.VISIBLE);
        }

        keypadFragment = new KeypadFragment();

        lblTimer.setVisibility(View.INVISIBLE);
        // these might need to be moved to Resume()
        btnMuteAudio.setVisibility(View.INVISIBLE);
        btnMuteVideo.setVisibility(View.INVISIBLE);
        btnKeypad.setVisibility(View.INVISIBLE);

        PhoneService.instance().setSipCallConnectedListener(this);
        PhoneService.instance().setSipCallEndListener(this);
        PhoneService.instance().setCallStreamListener(this);

        // open keypad
        FragmentTransaction ft = getFragmentManager().beginTransaction();
        ft.add(R.id.keypad_fragment_container, keypadFragment);
        ft.hide(keypadFragment);
        ft.commit();
        initializeVideo(true);
    }

    private void initializeVideo(boolean videoEnable)
    {
        this.localRenderLayout = (FrameLayout) findViewById(R.id.local_video_layout);
        this.remoteRenderLayout = (FrameLayout)findViewById(R.id.remote_video_layout);
        remoteVideo = PhoneService.instance().createRendererView(this);
        this.remoteRenderLayout.addView(remoteVideo);
        callParams = new CallParamsImpl();
    }

    @Override
    protected void onPause() {
        super.onPause();
        Log.i(TAG, "%% onPause");
    }

    @Override
    protected void onStart()
    {
       super.onStart();
       Log.i(TAG, "%% onStart");
       startCall();
    }

    @Override
    protected void onStop() {
        super.onStop();
        Log.i(TAG, "%% onStop");
    }

    @Override
    protected void onResume() {
        super.onResume();

        // The activity has become visible (it is now "resumed").
        Log.i(TAG, "%% onResume");
    }

    @Override
    protected void onDestroy()
    {
        super.onDestroy();
        // The activity is about to be destroyed.
        Log.i(TAG, "%% onDestroy");

        if (timerHandler != null) {
            timerHandler.removeCallbacksAndMessages(null);
        }
    }

    private void startCall()
    {
        isVideo = true;
        if (callDirection == Contacts.INVITE_VIDEO_REQUEST) {
            String text;
            if (isVideo) {
                text = "Video Calling ";
            }
            else {
                text = "Audio Calling ";
            }
            lblCall.setText(text + peer_number);
            lblStatus.setText("Initiating Call...");
            callParams.enableVideo(true);
            callParams.setAudioCodec("opus");
            callParams.setLocalDisplayName("david.xu");
            callParams.setVideoWidth(640);
            callParams.setVideoHeight(480);
            callParams.setVideoFps(15);
            callParams.setVideoMinBitrate(2000);
            callParams.setVideoMaxBitrate(2000);
            callParams.setUseCamera2(false);
            callParams.setEnableCpuOveruseDetection(true);
            callParams.setVideoCodecHwAcceleration(true);
            callParams.setLocalDeviceType("indoor");

            Log.e("callActivity", "========Initiating Call============:" +peer_number);
            call_id = PhoneService.instance().call(peer_number, callParams);
            //PhoneService.instance().setSpeakerphoneOn(call_id, true);
        }
        if (callDirection == Contacts.RECEIVE_VIDEO_REQUEST) {
            String text;
            if (isVideo) {
                text = "Video Call from ";
            }
            else {
                text = "Audio Call from ";
            }

            lblCall.setText(text + PhoneService.instance().getCallParams(call_id).remoteDisplayName());
            lblStatus.setText("Call Received...");

            //add by david.xu
            /*lblStatus.setText("Answering Call...");
            lblStatus.setText("Answering Call...");
            btnAnswer.setVisibility(View.INVISIBLE);
            btnAnswerAudio.setVisibility(View.INVISIBLE);
            CallParams callParams = PhoneService.instance().getCallParams(call_id);
            callParams.enableVideo(true);
            callParams.setAudioCodec("opus");
            callParams.setLocalDisplayName("david.xu");
            callParams.setVideoWidth(640);
            callParams.setVideoHeight(480);
            callParams.setVideoFps(15);
            callParams.setVideoMinBitrate(2000);
            callParams.setVideoMaxBitrate(2000);
            callParams.setUseCamera2(false);
            callParams.setEnableCpuOveruseDetection(true);
            callParams.setVideoCodecHwAcceleration(true);
            PhoneService.instance().answerCall(call_id, true);*/
            PhoneService.instance().answerCall(call_id, true);
        }
    }
    // UI Events
    public void onClick(View view) {
        if (view.getId() == R.id.button_hangup) {
            Log.e("callActivity", "====1==view.getId() == R.id.button_hangup====");
            if (call_id > 0) {
                Log.e("callActivity", "====2==view.getId() == R.id.button_hangup====");
                // incoming ringing
                lblStatus.setText("Rejecting Call...");
                PhoneService.instance().hangupCall(call_id);
            }
            finish();
        } else if (view.getId() == R.id.button_answer) {
            if (call_id > 0) {
               /* lblStatus.setText("Answering Call...");
                btnAnswer.setVisibility(View.INVISIBLE);
                btnAnswerAudio.setVisibility(View.INVISIBLE);
                PhoneService.instance().getCallParams(call_id).setAudioCodec("OPUS");
                PhoneService.instance().getCallParams(call_id).enableaudioProcessing(true);
                PhoneService.instance().getCallParams(call_id).setAgcControlLevel(1);
                PhoneService.instance().getCallParams(call_id).setAgcControlGain(20);
                PhoneService.instance().answerCall(call_id, true);*/

                //add by david.xu
                lblStatus.setText("Answering Call...");
                btnAnswer.setVisibility(View.INVISIBLE);
                btnAnswerAudio.setVisibility(View.INVISIBLE);
                CallParams callParams = PhoneService.instance().getCallParams(call_id);
                callParams.enableVideo(true);
                callParams.setAudioCodec("opus");
                callParams.setLocalDisplayName("david.xu");
                callParams.setVideoWidth(640);
                callParams.setVideoHeight(480);
                callParams.setVideoFps(15);
                callParams.setVideoMinBitrate(2000);
                callParams.setVideoMaxBitrate(2000);
                callParams.setUseCamera2(false);
                callParams.setEnableCpuOveruseDetection(true);
                callParams.setVideoCodecHwAcceleration(true);
                PhoneService.instance().answerCall(call_id, true);
            }
        } else if (view.getId() == R.id.button_answer_audio) {
            if (call_id > 0) {
                lblStatus.setText("Answering Call...");
                btnAnswer.setVisibility(View.INVISIBLE);
                btnAnswerAudio.setVisibility(View.INVISIBLE);
                PhoneService.instance().answerCall(call_id, false);
            }
        } else if (view.getId() == R.id.button_keypad) {
            keypadFragment.setCallId(call_id);

            View rootView = getWindow().getDecorView().findViewById(android.R.id.content);

            // show keypad
            FragmentTransaction ft = getFragmentManager().beginTransaction();
            ft.show(keypadFragment);
            ft.commit();
        } else if (view.getId() == R.id.button_mute_audio) {
            if (call_id > 0) {
                if (!muteAudio) {
                    btnMuteAudio.setImageResource(R.drawable.audio_muted);
                } else {
                    btnMuteAudio.setImageResource(R.drawable.audio_unmuted);
                }
                muteAudio = !muteAudio;
                if(muteAudio) {
                    //PhoneService.instance().stopVoiceChannel(call_id);
                    //PhoneService.instance().setSpeakerphoneOn(call_id, false);
                    //PhoneService.instance().stopVoiceReceiving(call_id);
                    //PhoneService.instance().sendMessage("sip:192.168.3.57", "dddddd");
                    PhoneService.instance().recordMedia(call_id, null, true, true);
                } else {
                    //PhoneService.instance().startVoiceChannel(call_id);
                    //PhoneService.instance().setSpeakerphoneOn(call_id, true);
                    PhoneService.instance().startVoiceReceiving(call_id);
                }
            }
        } else if (view.getId() == R.id.button_mute_video) {
            if (call_id > 0) {
                muteVideo = !muteVideo;
                if (muteVideo) {
                    btnMuteVideo.setImageResource(R.drawable.video_muted);
                    //PhoneService.instance().sendMessage("192.168.3.57", "dddddd");
                    //PhoneService.instance().stopVideoSending(call_id);
                    //PhoneService.instance().updateCall(call_id, false);
                } else {
                    btnMuteVideo.setImageResource(R.drawable.video_unmuted);
                    //PhoneService.instance().updateCall(call_id, true);
                    //PhoneService.instance().startVideoSending(call_id);
                }
            }

            /*if (call_id > 0) {
                muteVideo = !muteVideo;
                if (muteVideo) {
                    btnMuteVideo.setImageResource(R.drawable.video_muted);
                    //PhoneService.instance().stopVideoSending(call_id);
                    PhoneService.instance().updateCallByInfo(call_id, false);
                } else {
                    btnMuteVideo.setImageResource(R.drawable.video_unmuted);
                   // PhoneService.instance().startVideoSending(call_id);
                    PhoneService.instance().updateCallByInfo(call_id, true);
                }
            }*/
        }

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_call, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onFragmentInteraction(String action) {
        if (action.equals("cancel")) {
            FragmentTransaction ft = getFragmentManager().beginTransaction();
            ft.hide(keypadFragment);
            ft.commit();

        }
    }

    public void startTimer(int startSeconds) {
        secondsElapsed = startSeconds;
        String time = String.format("%02d:%02d:%02d", secondsElapsed / 3600, (secondsElapsed % 3600) / 60, secondsElapsed % 60);
        lblTimer.setText(time);
        secondsElapsed++;

        timerHandler.removeCallbacksAndMessages(null);
        // schedule a registration update after 'registrationRefresh' seconds
        Runnable timerRunnable = new Runnable() {
            @Override
            public void run() {
                startTimer(secondsElapsed);
            }
        };
        timerHandler.postDelayed(timerRunnable, 1000);
    }

    // Various conversion helpers
    /*private RCConnection.VideoResolution resolutionString2Enum(String resolution)
    {
        if (resolution.equals("160 x 120")) {
            return RCConnection.VideoResolution.RESOLUTION_QQVGA_160x120;
        }
        else if (resolution.equals("176 x 144")) {
            return RCConnection.VideoResolution.RESOLUTION_QCIF_176x144;
        }
        else if (resolution.equals("320 x 240")) {
            return RCConnection.VideoResolution.RESOLUTION_QVGA_320x240;
        }
        else if (resolution.equals("352 x 288")) {
            return RCConnection.VideoResolution.RESOLUTION_CIF_352x288;
        }
        else if (resolution.equals("640 x 360")) {
            return RCConnection.VideoResolution.RESOLUTION_nHD_640x360;
        }
        else if (resolution.equals("640 x 480")) {
            return RCConnection.VideoResolution.RESOLUTION_VGA_640x480;
        }
        else if (resolution.equals("800 x 600")) {
            return RCConnection.VideoResolution.RESOLUTION_SVGA_800x600;
        }
        else if (resolution.equals("1280 x 720")) {
            return RCConnection.VideoResolution.RESOLUTION_HD_1280x720;
        }
        else if (resolution.equals("1600 x 1200")) {
            return RCConnection.VideoResolution.RESOLUTION_UXGA_1600x1200;
        }
        else if (resolution.equals("1920 x 1080")) {
            return RCConnection.VideoResolution.RESOLUTION_FHD_1920x1080;
        }
        else if (resolution.equals("3840 x 2160")) {
            return RCConnection.VideoResolution.RESOLUTION_UHD_3840x2160;
        }
        else {
            return RCConnection.VideoResolution.RESOLUTION_DEFAULT;
        }
    }

    private RCConnection.VideoFrameRate frameRateString2Enum(String frameRate)
    {
        if (frameRate.equals("15 fps")) {
            return RCConnection.VideoFrameRate.FPS_15;
        }
        else if (frameRate.equals("30 fps")) {
            return RCConnection.VideoFrameRate.FPS_30;
        }
        else {
            return RCConnection.VideoFrameRate.FPS_DEFAULT;
        }
    }

    private RCConnection.AudioCodec audioCodecString2Enum(String audioCodec)
    {
        if (audioCodec.equals("OPUS")) {
            return RCConnection.AudioCodec.AUDIO_CODEC_OPUS;
        }
        else if (audioCodec.equals("ISAC")) {
            return RCConnection.AudioCodec.AUDIO_CODEC_ISAC;
        }
        else {
            return RCConnection.AudioCodec.AUDIO_CODEC_DEFAULT;
        }
    }

    private RCConnection.VideoCodec videoCodecString2Enum(String videoCodec)
    {
        if (videoCodec.equals("VP8")) {
            return RCConnection.VideoCodec.VIDEO_CODEC_VP8;
        }
        else if (videoCodec.equals("VP9")) {
            return RCConnection.VideoCodec.VIDEO_CODEC_VP9;
        }
        else if (videoCodec.equals("H264")) {
            return RCConnection.VideoCodec.VIDEO_CODEC_H264;
        }
        else {
            return RCConnection.VideoCodec.VIDEO_CODEC_DEFAULT;
        }
    }*/

    @Override
    public void onCallConnected(int call_id) {
        lblStatus.setText("Connected");
        this.call_id = call_id;
        btnMuteAudio.setVisibility(View.VISIBLE);
        if (!isVideo) {
            btnMuteVideo.setEnabled(false);
            btnMuteVideo.setColorFilter(Color.parseColor(getString(R.string.string_color_filter_video_disabled)));
        }
        btnMuteVideo.setVisibility(View.VISIBLE);
        btnKeypad.setVisibility(View.VISIBLE);

        lblTimer.setVisibility(View.VISIBLE);

        startTimer(0);

        // reset to no mute at beggining of new call
        muteAudio = false;
        muteVideo = false;

        //setVolumeControlStream(AudioManager.STREAM_VOICE_CALL);
    }

    @Override
    public void onCallEnd(int call_id, int status, CallLogBean mCallLogBean) {

        Log.i(TAG, "RCConnection disconnected");
        lblStatus.setText("Disconnected");

        btnMuteAudio.setVisibility(View.INVISIBLE);
        btnMuteVideo.setVisibility(View.INVISIBLE);

        this.call_id = -1;
        //setVolumeControlStream(AudioManager.STREAM_MUSIC);
        finish();
    }

    @Override
    public void onLocalVideoReady(int callId) {
        /*localRender.setVisibility(View.VISIBLE);
        localRenderLayout.setPosition(
                LOCAL_X_CONNECTING, LOCAL_Y_CONNECTING, LOCAL_WIDTH_CONNECTING, LOCAL_HEIGHT_CONNECTING);
        localRender.setScalingType(scalingType);
        localRender.setMirror(true);
        localRender.requestLayout();*/
        //PhoneService.instance().setLocalVideoRender(callId, localRenderLayout);
    }

    @Override
    public void onRemoteVideoReady(int callId) {
        /*remoteRender.setVisibility(View.VISIBLE);
        remoteRenderLayout.setPosition(REMOTE_X, REMOTE_Y, REMOTE_WIDTH, REMOTE_HEIGHT);
        remoteRender.setScalingType(scalingType);
        remoteRender.setMirror(false);
        localRender.setVisibility(View.VISIBLE);
        localRenderLayout.setPosition(
                LOCAL_X_CONNECTED, LOCAL_Y_CONNECTED, LOCAL_WIDTH_CONNECTED, LOCAL_HEIGHT_CONNECTED);
        localRender.setScalingType(RendererCommon.ScalingType.SCALE_ASPECT_FIT);
        localRender.setMirror(true);
        localRender.requestLayout();
        remoteRender.requestLayout();*/
        Log.e(TAG, "====================onRemoteVideoReady============");
        PhoneService.instance().setRemoteVideoRender(callId, remoteVideo);

        //PhoneService.instance().enableStatsEvents(call_id, true, 1000);

        //PhoneService.instance().setLocalVideoRender(callId, remoteRenderLayout);
    }

    @Override
    public void onUpdatedByRemote(int callId, boolean video) {

    }

    @Override
    public void onUpdatedByLocal(int callId, boolean video) {

    }
}
