package com.reSipWebRTC.reSipWebRTCDemo;

import android.app.Fragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;

import com.reSipWebRTC.service.CallParams;
import com.reSipWebRTC.service.PhoneService;
import com.reSipWebRTC.util.Contacts;

public class VideoWaitFragment extends Fragment {
    private LinearLayout layoutAnswer;
    private LinearLayout layoutClose;
    private Button btnAnswer;
    private Button btnClose;
    private View vDivide;
    private int call_state;
    boolean inVideoTalking = false;
    VideoTalkFragment videoTalkFragment = null;
    private int call_id = -1;
    private String peer_number;
    private CallParams callParams;

    public boolean inVideoTalking() {
        return inVideoTalking;
    }

    public VideoTalkFragment getVideoTalkFragment() {
        return videoTalkFragment;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_videowait, null);
        init(view);
        return view;
    }

    private void init(View view) {
        Bundle bundle = getArguments();
        call_state = bundle.getInt(Contacts.PHONESTATE);
        call_id = bundle.getInt(Contacts.PHONECALLID);
        peer_number = bundle.getString(Contacts.PHONNUMBER);

        layoutAnswer = (LinearLayout) view.findViewById(R.id.layout_answer);
        layoutClose = (LinearLayout) view.findViewById(R.id.layout_close);
        layoutAnswer.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                goToVideoTalk();
            }
        });

        vDivide = view.findViewById(R.id.v_divide);

        btnAnswer = (Button) view.findViewById(R.id.btn_answer);
        btnClose = (Button) view.findViewById(R.id.btn_close);

        btnAnswer.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                // TODO Auto-generated method stub
                goToVideoTalk();
            }
        });

        btnClose.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                if(call_state == Contacts.INVITE_VIDEO_REQUEST) {
                    PhoneService.instance().hangupCall(call_id);
                } else if(call_state == Contacts.RECEIVE_VIDEO_REQUEST){
                    PhoneService.instance().rejectCall(call_id, 603, "reject");
                }
                getActivity().finish();
            }
        });
        layoutClose.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                if(call_state == Contacts.INVITE_VIDEO_REQUEST) {
                    PhoneService.instance().hangupCall(call_id);
                } else if(call_state == Contacts.RECEIVE_VIDEO_REQUEST){
                    PhoneService.instance().rejectCall(call_id, 603, "reject");
                }
                getActivity().finish();
            }
        });


        // 根据来电或去电显示不同的界面
        if (call_state == Contacts.RECEIVE_VIDEO_REQUEST) {
            layoutAnswer.setVisibility(View.VISIBLE);
            vDivide.setVisibility(View.VISIBLE);
            layoutClose.setVisibility(View.VISIBLE);
        } else if (call_state == Contacts.INVITE_VIDEO_REQUEST) {
            layoutAnswer.setVisibility(View.GONE);
            vDivide.setVisibility(View.GONE);
            layoutClose.setVisibility(View.VISIBLE);
        }
    }

    @Override
    public void onStop() {
        super.onStop();
    }

    /**
     * 显示视频电话
     */
    public void goToVideoTalk() {
        PhoneService.instance().answerCall(call_id, true);
    }

    /**
     * 打开视频通话界面
     */
    public void startVideoCallScreen() {
        FragmentManager fm = getFragmentManager();
        FragmentTransaction transaction = fm.beginTransaction();
        if (videoTalkFragment == null)
            videoTalkFragment = new VideoTalkFragment();
        Bundle bundle = new Bundle();
        bundle.putInt(Contacts.PHONECALLID, call_id);
        videoTalkFragment.setArguments(bundle);

        transaction.replace(R.id.layout_fl, videoTalkFragment);
        transaction.commit();
        inVideoTalking = true;
    }
}