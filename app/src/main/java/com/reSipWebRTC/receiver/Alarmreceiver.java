package com.reSipWebRTC.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.reSipWebRTC.service.PhoneService;
import com.reSipWebRTC.util.Debug;

public class Alarmreceiver extends BroadcastReceiver {
	int t = 0;

	@Override
	public void onReceive(Context context, Intent intent) {
		if (intent.getAction().equals("meetplus.alarm.action")) {
			Debug.d("Alarmreceiver", "10 秒循环检测PhonService 是否可用...");
			if (t < 30) {
				if (!PhoneService.isready()) {
					Intent i = new Intent();
					i.setClass(context, PhoneService.class);
					Debug.i("Alarmreceiver", "启动phoneService服务");
					// 启动service
					// 多次调用startService并不会启动多个service 而是会多次调用onStart
					context.startService(i);
					t++;
				}
			}
		}
	}
}