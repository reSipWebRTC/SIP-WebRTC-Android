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

import com.reSipWebRTC.reSipWebRTCDemo.R;
import com.reSipWebRTC.sdk.CallMediaStatsReport;
import com.reSipWebRTC.service.PhoneService;
import com.reSipWebRTC.util.Contacts;

import org.webrtc.EglBase;
import org.webrtc.Logging;
import org.webrtc.StatsReport;
import org.webrtc.SurfaceViewRenderer;
import org.webrtc.VideoFrame;
import org.webrtc.VideoSink;

import java.util.HashMap;
import java.util.Map;

public class VideoTalkFragment extends Fragment implements CallMediaStatsReport {
    private boolean usingFrontCamera = true;
    private Button mHangup, mSwapCam, mMute, mSpeaker;
    int currentOrientation = OrientationEventListener.ORIENTATION_UNKNOWN;
    int currentCameraOrientation = 0;
    int numCamera = -1;
    public Handler mHandler = null;
    private GLSurfaceView videoView;
    private EglBase rootEglBase;
    private SurfaceViewRenderer localRender;
    private SurfaceViewRenderer remoteRender;
    private boolean hidden = false;
    private LinearLayout mLocalLayout = null;
    private LinearLayout mRemoteLayout = null;
    private int call_id = -1;
    private final ProxyVideoSink remoteProxyRenderer = new ProxyVideoSink();

    private static class ProxyVideoSink implements VideoSink {
        private VideoSink target;

        @Override
        synchronized public void onFrame(VideoFrame frame) {
            if (target == null) {
                //Logging.d(TAG, "Dropping frame in proxy because target is null.");
                return;
            }

            target.onFrame(frame);
        }

        synchronized public void setTarget(VideoSink target) {
            this.target = target;
        }
    }

    /*private class ProxyRenderer implements VideoRenderer.Callbacks {
        private VideoRenderer.Callbacks target;

        synchronized public void renderFrame(VideoRenderer.I420Frame frame) {
            if (target == null) {
                //Logging.d(TAG, "Dropping frame in proxy because target is null.");
                VideoRenderer.renderFrameDone(frame);
                return;
            }

            target.renderFrame(frame);
        }

        synchronized public void setTarget(VideoRenderer.Callbacks target) {
            this.target = target;
        }
    }*/

    //private SurfaceViewRenderer remoteProxyRenderer;
    private SurfaceViewRenderer localProxyRenderer;

    /**
     * 是否静音
     */
    private boolean isMute = false;

    /**
     * 是否为免提
     */
    private boolean isSpeaker = false;

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
        PhoneService.instance().setCallMediaStatsReport(this);
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
                PhoneService.instance().hangUpCall(call_id);
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

        if (remoteRender == null) {
            remoteRender = new SurfaceViewRenderer(getActivity());
            LinearLayout r = new LinearLayout(getActivity());
            //r.setBackgroundResource(R.drawable.boader);
            r.addView(remoteRender);
            mRemoteLayout.addView(r);
        }

        if (localRender == null) {
            localRender = new SurfaceViewRenderer(getActivity());
            LinearLayout l = new LinearLayout(getActivity());
            //l.setBackgroundResource(R.drawable.boader);
            l.addView(localRender);
            mLocalLayout.addView(l);
        }

        rootEglBase = EglBase.create();
        localRender.init(rootEglBase.getEglBaseContext(), null);
        remoteRender.init(rootEglBase.getEglBaseContext(), null);
        localRender.setZOrderMediaOverlay(true);
        remoteRender.setMirror(true);

        //localProxyRenderer.setTarget(localRender);
        remoteProxyRenderer.setTarget(remoteRender);

        System.out.println("===============startVideoRender============");
        PhoneService.instance().startVideoRender(call_id, localProxyRenderer, this.remoteProxyRenderer);
        PhoneService.instance().setSpeakerphoneOn(call_id, isSpeaker);
        PhoneService.instance().setVideoMaxBitrate(call_id, 500);
}

    public void stopVideoChannel() {

        //PhoneService.instance().StopVideoChannel();
    }

    @Override
    public void onDestroy() {
        if (rootEglBase != null) {
            rootEglBase.release();
        }
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

    @Override
    public void onCallMediaStatsReady(final StatsReport[] reports) {
        // TODO Auto-generated method stub

        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                updateEncoderStatistics(reports);
            }
        });
    }

    private void updateEncoderStatistics(final StatsReport[] reports) {
        StringBuilder encoderStat = new StringBuilder(128);
        StringBuilder bweStat = new StringBuilder();
        StringBuilder connectionStat = new StringBuilder();
        StringBuilder videoSendStat = new StringBuilder();
        StringBuilder videoRecvStat = new StringBuilder();
        String fps = null;
        String targetBitrate = null;
        String actualBitrate = null;

        for (StatsReport report : reports) {
            if (report.type.equals("ssrc") && report.id.contains("ssrc")
                    && report.id.contains("send")) {
                // Send video statistics.
                Map<String, String> reportMap = getReportMap(report);
                String trackId = reportMap.get("googTrackId");
                if (trackId != null && trackId.contains("ARDAMSv0")) {
                    fps = reportMap.get("googFrameRateSent");
                    //System.out.println("==========SendVideoFps======:" +fps);
                    videoSendStat.append(report.id).append("\n");
                    for (StatsReport.Value value : report.values) {
                        // System.out.print("===========name========:" +value.name +"\n");
                        //System.out.print("===========value========:" +value.value +"\n");
                        String name = value.name.replace("goog", "");
                        videoSendStat.append(name).append("=").append(value.value).append("\n");
                    }
                }
            } else if (report.type.equals("ssrc") && report.id.contains("ssrc")
                    && report.id.contains("recv")) {
                // Receive video statistics.
                Map<String, String> reportMap = getReportMap(report);
                // Check if this stat is for video track.
                String frameWidth = reportMap.get("googFrameWidthReceived");
                if (frameWidth != null) {
                    videoRecvStat.append(report.id).append("\n");
                    for (StatsReport.Value value : report.values) {
                        String name = value.name.replace("goog", "");
                        //System.out.print("===========name========:" +value.name +"\n");
                        //System.out.print("===========value========:" +value.value +"\n");
                        videoRecvStat.append(name).append("=").append(value.value).append("\n");
                    }
                }
            } else if (report.id.equals("bweforvideo")) {
                // BWE statistics.
                Map<String, String> reportMap = getReportMap(report);
                targetBitrate = reportMap.get("googTargetEncBitrate");
                actualBitrate = reportMap.get("googActualEncBitrate");
                int TB = Integer.valueOf(targetBitrate).intValue();
                int AB = Integer.valueOf(actualBitrate).intValue();
                TB = TB / 1000;
                AB = AB / 1000;
                targetBitrate = String.valueOf(TB);
                actualBitrate = String.valueOf(AB);

                bweStat.append(report.id).append("\n");
                for (StatsReport.Value value : report.values) {
                    //System.out.print("===========name========:" +value.name +"\n");
                    //System.out.print("===========value========:" +value.value +"\n");
                    String name = value.name.replace("goog", "").replace("Available", "");
                    //bweStat.append(name).append("=").append(value.value).append("\n");
                }
            } else if (report.type.equals("googCandidatePair")) {
                // Connection statistics.
                Map<String, String> reportMap = getReportMap(report);
                String activeConnection = reportMap.get("googActiveConnection");
                if (activeConnection != null && activeConnection.equals("true")) {
                    connectionStat.append(report.id).append("\n");
                    for (StatsReport.Value value : report.values) {
                        String name = value.name.replace("goog", "");
                        //(name.equals("packetsDiscardedOnSend"))
                        //System.out.print("===========name========:" +value.name +"\n");
                        // System.out.print("===========packetsDiscardedOnSend========:" +value.value +"\n");
                        // connectionStat.append(name).append("=").append(value.value).append("\n");
                    }
                }
            }
        }
        //hudViewBwe.setText(bweStat.toString());
        //hudViewConnection.setText(connectionStat.toString());
        //hudViewVideoSend.setText(videoSendStat.toString());
        //hudViewVideoRecv.setText(videoRecvStat.toString());

        if (true) {
            if (fps != null) {
                encoderStat.append("Fps:  ").append(fps).append("\n");
            }
            if (targetBitrate != null) {
                encoderStat.append("Target BR: ").append(targetBitrate).append("\n");
            }
            if (actualBitrate != null) {
                encoderStat.append("Actual BR: ").append(actualBitrate).append("\n");
            }
        }

       /* if (cpuMonitor.sampleCpuUtilization()) {
            encoderStat.append("CPU%: ")
                    .append(cpuMonitor.getCpuCurrent()).append("/")
                    .append(cpuMonitor.getCpuAvg3()).append("/")
                    .append(cpuMonitor.getCpuAvgAll());
        }*/
        //encoderStatView.setText(encoderStat.toString());
    }

    private Map<String, String> getReportMap(StatsReport report) {
        Map<String, String> reportMap = new HashMap<String, String>();
        for (StatsReport.Value value : report.values) {
            reportMap.put(value.name, value.value);
        }
        return reportMap;
    }
}
