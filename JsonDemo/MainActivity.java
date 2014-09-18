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
import org.apache.http.ParseException;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

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
import android.widget.TextView;
import android.net.wifi.*;

public class MainActivity extends Activity {
	//server IP, for example 101.5.155.101:8080
	private EditText mEtServerIP;
	//position coordinate
	private EditText inX;
	private EditText inY;
	//private EditText inZ;
	//position label
	private EditText inPos;
	
	private TextView mWifiResult;
	private TextView mTvResult;
	
	private String mStrResult;
	
	private Button mBtnLogin;
	private Button mBtnWifi;
	private Button mBtnTrain;
	
	private WifiTask wifiTask;
	private MyTask mTask;
	private TrainTask trainTask;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		//显示app的界面
		setContentView(R.layout.activity_main);
		
		//获取app的组件信息
		mEtServerIP = (EditText) findViewById(R.id.et_serverip);
		inX = (EditText) findViewById(R.id.editX);
		inY = (EditText) findViewById(R.id.editY);
		//inZ = (EditText) findViewById(R.id.editZ);
		inPos = (EditText) findViewById(R.id.editPosition);
		
		mWifiResult = (TextView) findViewById(R.id.wifi_result);
		mTvResult = (TextView) findViewById(R.id.tv_result);
		
		mBtnWifi = (Button) findViewById(R.id.btn_wifi);
		mBtnLogin = (Button) findViewById(R.id.btn_login);
		mBtnTrain = (Button) findViewById(R.id.btn_train);
		
		mWifiResult.setMovementMethod(ScrollingMovementMethod.getInstance());

		/**
		 *  处理btn_login的响应任务，启动一个MyTask，处理数据库的查询
		 */
		mBtnLogin.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				// TODO Auto-generated method stub
				//mStrName = (mEtName).getText().toString();
				// AsyncTask异步任务开始
				mTask = new MyTask();
				mTask.execute("");
			}
		});
		
		mBtnWifi.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v){
				wifiTask = new WifiTask();
				wifiTask.execute();
			}	
		});
		
		mBtnTrain.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v){
				trainTask = new TrainTask();
				trainTask.execute();
			}	
		});
	}
	
	
	/**
	 *  数据库输入线程的具体任务，包括：
	 *  1. 连接servlet
	 *  2. 将用户信息通过param用HttpPost发送给server
	 */
	
	class WifiTask extends AsyncTask<Void, Integer, String> 
	{
		final public double LN10 = Math. log(10);
		final public int NUMBER_OF_TESTS = 5;
		private int count = 0;
		private WifiManager wifi;
		private String wifiResult = "";
		private Map<String, Double> map = new HashMap<String, Double>();
		private List<ScanResult> results = null;
		private void updateMap() {
			results = wifi.getScanResults();
			for (ScanResult result : results) {
				if (map.containsKey(result.BSSID)) {
					Double sum = map.get(result.BSSID);
					sum += Math.exp(result.level * LN10 / 10);
					map.put(result.BSSID, sum);
				}
				else 
					map.put(result.BSSID, Math.exp(result.level * LN10 / 10));
			}								
		}
		@Override
		protected String doInBackground(Void... params) {
			publishProgress(0);
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
					}
					catch (InterruptedException e) {
					}
				}
				if (i == NUMBER_OF_TESTS - 1)
					updateMap();
				publishProgress(i + 1);
			}

			//通过HttpPost连接servlet
			HttpClient hc = new DefaultHttpClient();
			String address = "http://" + (mEtServerIP).getText().toString() + ":8080/ServerJsonDemo/servlet/JsonServlet";
			HttpPost hp = new HttpPost(address);
			String strPos = inPos.getText().toString();
			String strX = inX.getText().toString();
			String strY = inY.getText().toString();
			//String strZ = inZ.getText().toString();
			
			//通过param发送参数给servlet
			List<NameValuePair> param = new ArrayList<NameValuePair>();
			param.add(new BasicNameValuePair("type", "input"));
			param.add(new BasicNameValuePair("pos", strPos));
			param.add(new BasicNameValuePair("x", strX));
			param.add(new BasicNameValuePair("y", strY));
			//param.add(new BasicNameValuePair("z", strZ));
			param.add(new BasicNameValuePair("num", map.size() + ""));
			int idx = 0;

			Iterator<Map.Entry<String, Double>> it = map.entrySet().iterator();
			while (it.hasNext()) {
				Map.Entry<String, Double> entry = (Map.Entry<String, Double>) it.next();
				param.add(new BasicNameValuePair((idx++) + "", entry.getKey() + "&" + 
						10 * Math.log10(entry.getValue() / NUMBER_OF_TESTS)));
			}

			try{
				hp.setEntity(new UrlEncodedFormEntity(param, "utf-8")); 
				// 发送请求
				HttpResponse response = hc.execute(hp);
				// 返回200即请求成功
				if (response.getStatusLine().getStatusCode() == 200) {
					//数据写入成功
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
			return wifiResult;
		}
		
		@Override
		protected void onPostExecute(String result) {
			// TODO Auto-generated method stub
			super.onPostExecute(result);
			//在文本框中显示wifi信息
			mWifiResult.setText(result);
		}
		
		@Override
		protected void onProgressUpdate(Integer ... values) {
			super.onProgressUpdate(values);
			String s = values[0] > 1? "s": "";
			mWifiResult.setText(values[0] + " scan" + s + " finished!");
		}
		
		class WifiReceiver extends BroadcastReceiver {
	        @Override
	        public void onReceive(Context context, Intent intent) {             
	            //WifiManager wifiManager = (WifiManager) getSystemService(Context.WIFI_SERVICE);
	            //wifiManager.startScan();//request a scan for access points
	        	int k = 1;
	        	wifiResult += "Scan #" + (count + 1) + "\n";
	            final List<ScanResult> results= wifi.getScanResults();//list of access points from the last scan
	                for(final ScanResult result : results){
	                	wifiResult += (k++) + " : " + result.BSSID + " " 
	                			+ result.SSID + " " + result.level + "\n";
	            }
	            count++;
	        }
	    }
	}
	
	class TrainTask extends AsyncTask<String, Void, String>
	{

		@Override
		protected String doInBackground(String... params) 
		{
			// TODO Auto-generated method stub
			HttpClient hc = new DefaultHttpClient();
			// 这里是服务器的IP，不要写成localhost了，即使在本机测试也要写上本机的IP地址，localhost会被当成模拟器自身的
			String address = "http://" + (mEtServerIP).getText().toString() + ":8080/ServerJsonDemo/servlet/JsonServlet";
			HttpPost hp = new HttpPost(address);
			List<NameValuePair> param = new ArrayList<NameValuePair>();
			param.add(new BasicNameValuePair("type", "test"));
			// String str=null;
			try
			{
				hp.setEntity(new UrlEncodedFormEntity(param, "utf-8")); 
				// 发送请求
				HttpResponse response = hc.execute(hp);
				// 返回200即请求成功
				if (response.getStatusLine().getStatusCode() == 200) 
				{	
					System.out.println("训练成功");
				} 
				else 
				{
					System.out.println("连接失败");
				}
			}
			catch (UnsupportedEncodingException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (ClientProtocolException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			//返回包好user信息的str
			return null;
		}
	}

	class MyTask extends AsyncTask<String, Integer, String> {
		private WifiManager wifi;
		private String wifiResult = "";
		private List<ScanResult> results = null;
		private int progress;
		boolean finished = false;

		@Override
		protected String doInBackground(String... params) {
			publishProgress(0);
			WifiReceiver receiver = new WifiReceiver();
			registerReceiver(receiver, new IntentFilter(
					WifiManager.SCAN_RESULTS_AVAILABLE_ACTION));
			wifi = (WifiManager) getSystemService(Context.WIFI_SERVICE);
			wifi.startScan();
			
			while (!finished) {
				try {
					Thread.sleep(100);
				} 
				catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
			publishProgress(1);
			// TODO Auto-generated method stub
			HttpClient hc = new DefaultHttpClient();
			// 这里是服务器的IP，不要写成localhost了，即使在本机测试也要写上本机的IP地址，localhost会被当成模拟器自身的
			String address = "http://" + (mEtServerIP).getText().toString() + ":8080/ServerJsonDemo/servlet/JsonServlet";
			HttpPost hp = new HttpPost(address);
			List<NameValuePair>param = new ArrayList<NameValuePair>();
			param.add(new BasicNameValuePair("type", "search"));
			results = wifi.getScanResults();
			param.add(new BasicNameValuePair("num", results.size() + ""));
			int idx = 0;
			for (ScanResult result : results) {
				param.add(new BasicNameValuePair((idx++) + "", result.BSSID + result.level));
			}
			String str=null;
			try {
				hp.setEntity(new UrlEncodedFormEntity(param, "utf-8")); //jsonObj.toString()));
				// 发送请求
				HttpResponse response = hc.execute(hp);
				// 返回200即请求成功
				if (response.getStatusLine().getStatusCode() == 200) {
					// 获取响应中的数据，这也是一个JSON格式的数据
					str = PhaseJson(response);
				} else {
					return "连接失败";
				}
			} catch (UnsupportedEncodingException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (JSONException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (ClientProtocolException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			//返回包好user信息的str
			return str;
		}

		@Override
		protected void onPostExecute(String result) {
			// TODO Auto-generated method stub
			super.onPostExecute(result);
			//在文本框中显示user信息
			mTvResult.setText(result);
		}

		@Override
		protected void onProgressUpdate(Integer ... values) {
			super.onProgressUpdate(values);
			switch (values[0]) {
				case 0: 
					mWifiResult.setText("正在扫描Wifi");
					break;
				case 1:
					mWifiResult.setText("扫描成功，正在查询");
					break;
			}
		}

		/**
		 * 处理HttpResponse返回的response信息，从体重提取name, sex, age等信息
		 * @param response
		 * @return
		 * @throws ParseException
		 * @throws IOException
		 * @throws JSONException
		 */
		String PhaseJson(HttpResponse response) throws ParseException, IOException, JSONException
		{
			String pos = null, x = null, y = null, str = null;
			mStrResult = EntityUtils.toString(response.getEntity());
			// 将返回结果生成JSON对象，返回的格式首先是user数组
			JSONArray userarray = new JSONObject(mStrResult).getJSONArray("users" );
			//从user数组中提取出user的信息
			//对于同名用户，只获取最后一条信息 //改成了只获取第一条
            for(int i=userarray.length()-1; i>=0;i--) {
            	JSONObject userInfo = userarray.getJSONObject(i);
                pos = userInfo.getString("pos" );
                x = userInfo.getString("x");
                y = userInfo.getString("y" );
                //z = userInfo.getString("z");
                str = "房间: " + pos + " X: " + x + " Y: " + y;
                System.out.println(str);
            }
            
            return str;
			//return wifiResult;
		}
		class WifiReceiver extends BroadcastReceiver {
	        @Override
	        public void onReceive(Context context, Intent intent) {             
	            //WifiManager wifiManager = (WifiManager) getSystemService(Context.WIFI_SERVICE);
	            //wifiManager.startScan();//request a scan for access points
	        	/*int k = 1;
	        	//wifiResult += "Scan #" + (count + 1) + "\n";
	            final List<ScanResult> results= wifi.getScanResults();//list of access points from the last scan
	                for(final ScanResult result : results){
	                	wifiResult += (k++) + " : " + result.BSSID + " " 
	                			+ result.SSID + " " + result.level + "\n";
	            }*/
	            finished = true;
	        }
	    }
	}
}
