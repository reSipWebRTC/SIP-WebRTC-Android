package com.reSipWebRTC.reSipWebRTCDemo;

import android.app.Fragment;
import android.content.SharedPreferences;
import android.opengl.GLSurfaceView;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.OrientationEventListener;
import android.view.Surface;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;

import com.reSipWebRTC.sdk.CallMediaStatsReport;
import com.reSipWebRTC.service.PhoneService;
import com.reSipWebRTC.util.Contacts;

import java.util.HashMap;
import java.util.Map;

public class VideoTalkFragment extends Fragment {
    private boolean usingFrontCamera = true;
    private Button mHangup, mSwapCam, mMute, mSpeaker;
    int currentOrientation = OrientationEventListener.ORIENTATION_UNKNOWN;
    int currentCameraOrientation = 0;
    int numCamera = -1;
    public Handler mHandler = null;
    private GLSurfaceView videoView;
    private boolean hidden = false;
    private LinearLayout mLocalLayout = null;
    private LinearLayout mRemoteLayout = null;
    private int call_id = -1;
    /**
     * 是否静音
     */
    private boolean isMute = false;

    /**
     * 是否为免提
     */
    private boolean isSpeaker = true;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_video_screen, null);
        init(view);
        return view;
    }

    private void init(View view) {
        SharedPreferences sp = PreferenceManager
                .getDefaultSharedPreferences(this.getActivity());
        Bundle bundle = getArguments();
        call_id = bundle.getInt(Contacts.PHONECALLID);

        mHandler = new Handler();
        PhoneService.instance().enableStatsEvents(call_id, true, 1000);

        mLocalLayout = (LinearLayout) view.findViewById(R.id.llLocalView);
        mRemoteLayout = (LinearLayout) view.findViewById(R.id.llRRemoteView);

        mSwapCam = (Button) view.findViewById(R.id.swap_cam);
        mSwapCam.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                usingFrontCamera = !usingFrontCamera;
                PhoneService.instance().switchCamera(call_id);
            }
        });

        mHangup = (Button) view.findViewById(R.id.hangup);
        mHangup.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                PhoneService.instance().hangupCall(call_id);
                getActivity().finish();
            }
        });

        mMute = (Button) view.findViewById(R.id.mute);
        mMute.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!isMute) {
                    PhoneService.instance().setMicrophoneMute(call_id, true);
                    isMute = !isMute;
                } else {
                    PhoneService.instance().setMicrophoneMute(call_id, false);
                    isMute = !isMute;
                }
            }
        });

        mSpeaker = (Button) view.findViewById(R.id.Speaker);
        mSpeaker.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!isSpeaker) {
                    PhoneService.instance().setSpeakerphoneOn(call_id, true);
                    isSpeaker = !isSpeaker;
                } else {
                    PhoneService.instance().setSpeakerphoneOn(call_id, false);
                    isSpeaker = !isSpeaker;
                }
            }
        });

        startVideoRender();
    }

    /**
     * 开始视频渲染
     */
    private void startVideoRender() {

    }

    public void stopVideoChannel() {

        //PhoneService.instance().StopVideoChannel();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        //PjSipService.instance().StopAudioManager();
        //stopVideoChannel();
    }

    public int getCameraOrientation(int cameraOrientation) {

        Display display = getActivity().getWindowManager().getDefaultDisplay();
        int displatyRotation = display.getRotation();
        int degrees = 0;
        switch (displatyRotation) {
            case Surface.ROTATION_0:
                degrees = 0;
                break;
            case Surface.ROTATION_90:
                degrees = 90;
                break;
            case Surface.ROTATION_180:
                degrees = 180;
                break;
            case Surface.ROTATION_270:
                degrees = 270;
                break;
        }

        int result = 0;
        if (cameraOrientation > 180) {
            result = (cameraOrientation + degrees) % 360;
        } else {
            result = (cameraOrientation - degrees + 360) % 360;
        }
        return result;
    }

    public void SetupCameraRotation() {
        //int cameraOrientation = PhoneService.instance().GetCameraOrientation(usingFrontCamera ? 1 : 0);
        //PhoneService.instance().SetCameraOutputRotation(getCameraOrientation(cameraOrientation));
    }

    public void setVideoRenderViewOrientation(int orientation) {
        hidden = false;
        mHangup.setVisibility(View.VISIBLE);
    }

}
