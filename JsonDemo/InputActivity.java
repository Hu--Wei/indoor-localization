package com.jsondemo.activity;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.method.ScrollingMovementMethod;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.net.wifi.*;

public class InputActivity extends Activity {
	// server IP, for example 101.5.155.101:8080
	private EditText mEtServerIP;
	// position coordinate
	private EditText inX;
	private EditText inY;
	// position label
	private EditText inPos;
	// the angle between the map and north (i.e.: east = 90)
	private TextView mWifiResult;
	private Button mBtnWifi;
	private WifiTask wifiTask;

	// progress bar
	private ProgressBar bar2;
	private double expectedTime = 20000;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		// 显示app的界面
		setContentView(R.layout.input_main);

		// 获取app的组件信息
		mEtServerIP = (EditText) findViewById(R.id.et_serverip2);
		inX = (EditText) findViewById(R.id.editX);
		inY = (EditText) findViewById(R.id.editY);
		inPos = (EditText) findViewById(R.id.editPosition);

		mWifiResult = (TextView) findViewById(R.id.wifi_result);

		mBtnWifi = (Button) findViewById(R.id.btn_wifi);
		// progress bar
		bar2 = (ProgressBar) findViewById(R.id.bar2);

		mWifiResult.setMovementMethod(ScrollingMovementMethod.getInstance());

		mBtnWifi.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				bar2.setVisibility(View.VISIBLE);
				wifiTask = new WifiTask();
				wifiTask.execute();
			}
		});

	}

	/**
	 * 数据库输入线程的具体任务，包括： 1. 连接servlet 2. 将用户信息通过param用HttpPost发送给server
	 */

	class WifiTask extends AsyncTask<Void, Integer, String> {
		final public double LN10 = Math.log(10);
		final public int NUMBER_OF_TESTS = 5;
		private int count = 0;
		private WifiManager wifi;
		private String wifiResult = "";
		private Map<String, Double> map = new HashMap<String, Double>();
		private List<ScanResult> results = null;
		private int progress;
		private long startTime, duration;

		private void updateMap() {
			results = wifi.getScanResults();
			for (ScanResult result : results) {
				if (map.containsKey(result.BSSID)) {
					Double sum = map.get(result.BSSID);
					sum += Math.exp(result.level * LN10 / 10);
					map.put(result.BSSID, sum);
				} else
					map.put(result.BSSID, Math.exp(result.level * LN10 / 10));
			}
		}

		@Override
		protected String doInBackground(Void... params) {
			startTime = System.currentTimeMillis();
			progress = 0;
			publishProgress(0, 0);
			WifiReceiver receiver = new WifiReceiver();
			registerReceiver(receiver, new IntentFilter(
					WifiManager.SCAN_RESULTS_AVAILABLE_ACTION));
			count = 0;
			wifi = (WifiManager) getSystemService(Context.WIFI_SERVICE);
			map.clear();
			for (int i = 0; i < NUMBER_OF_TESTS; i++) {
				wifi.startScan();
				if (i > 0)
					updateMap();
				while (count <= i) {
					try {
						Thread.sleep(100);
						if (progress + 100 <= expectedTime * (i + 1) / NUMBER_OF_TESTS)
							progress += 100;
						publishProgress(progress, i);
					} catch (InterruptedException e) {
					}
				}
				if (i == NUMBER_OF_TESTS - 1)
					updateMap();
				progress = (int) (expectedTime * (i + 1) / NUMBER_OF_TESTS);
				publishProgress(progress, i + 1);
			}

			// 通过HttpPost连接servlet
			HttpClient hc = new DefaultHttpClient();
			String address = "http://" + (mEtServerIP).getText().toString()
					+ ":8080/ServerJsonDemo/servlet/JsonServlet";
			HttpPost hp = new HttpPost(address);
			String strPos = inPos.getText().toString();
			String strX = inX.getText().toString();
			String strY = inY.getText().toString();

			// 通过param发送参数给servlet
			List<NameValuePair> param = new ArrayList<NameValuePair>();
			param.add(new BasicNameValuePair("type", "input"));
			param.add(new BasicNameValuePair("pos", strPos));
			param.add(new BasicNameValuePair("x", strX));
			param.add(new BasicNameValuePair("y", strY));
			param.add(new BasicNameValuePair("num", map.size() + ""));
			int idx = 0;

			Iterator<Map.Entry<String, Double>> it = map.entrySet().iterator();
			while (it.hasNext()) {
				Map.Entry<String, Double> entry = (Map.Entry<String, Double>) it
						.next();
				param.add(new BasicNameValuePair((idx++) + "", entry.getKey()
						+ "&" + 10
						* Math.log10(entry.getValue() / NUMBER_OF_TESTS)));
			}

			try {
				hp.setEntity(new UrlEncodedFormEntity(param, "utf-8"));
				// 发送请求
				HttpResponse response = hc.execute(hp);
				// 返回200即请求成功
				if (response.getStatusLine().getStatusCode() == 200) {
					// 数据写入成功
					wifiResult += "上传成功";
				} else {
					wifiResult += "连接失败";
				}
			} catch (UnsupportedEncodingException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (ClientProtocolException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			bar2.setProgress(100);
			duration = System.currentTimeMillis() - startTime;
			expectedTime = duration;
			unregisterReceiver(receiver);	
			return wifiResult;
		}

		@Override
		protected void onPostExecute(String result) {
			// TODO Auto-generated method stub
			super.onPostExecute(result);
			// 在文本框中显示wifi信息
			mWifiResult.setText(result);
			bar2.setVisibility(View.GONE);
		}

		@Override
		protected void onProgressUpdate(Integer... values) {
			super.onProgressUpdate(values);
			mWifiResult.setText("已完成" + values[1] + "次扫描");
			bar2.setProgress((int) (values[0] / expectedTime * 100));
		}

		class WifiReceiver extends BroadcastReceiver {
			@Override
			public void onReceive(Context context, Intent intent) {
				int k = 1;
				wifiResult += "Scan #" + (count + 1) + "\n";
				final List<ScanResult> results = wifi.getScanResults();
				// list of access points from the last scan
				for (final ScanResult result : results) {
					wifiResult += (k++) + " : " + result.BSSID + " "
							+ result.SSID + " " + result.level + "\n";
				}
				count++;
			}
		}
	}
}
