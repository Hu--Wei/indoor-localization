package com.jsondemo.activity;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;

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
import android.widget.Toast;
import android.net.wifi.*;

public class MainActivity extends Activity {
	private EditText mEtServerIP;
	private EditText inX;
	private EditText inY;
	private EditText inZ;
	private EditText inPos;
	private EditText mEtName;
	
	private TextView mWifiResult;
	private TextView mTvResult;
	
	private String mStrName, mStrResult;
	
	private Button mBtnLogin;
	private Button mBtnWifi;
	
	private WifiTask wifiTask;
	private MyTask mTask;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		//显示app的界面
		setContentView(R.layout.activity_main);
		
		//获取app的组件信息
		mEtServerIP = (EditText) findViewById(R.id.et_serverip);
		inX = (EditText) findViewById(R.id.editX);
		inY = (EditText) findViewById(R.id.editY);
		inZ = (EditText) findViewById(R.id.editZ);
		inPos = (EditText) findViewById(R.id.editPosition);
		mEtName = (EditText) findViewById(R.id.et_hello);
		
		mWifiResult = (TextView) findViewById(R.id.wifi_result);
		mTvResult = (TextView) findViewById(R.id.tv_result);
		
		mBtnWifi = (Button) findViewById(R.id.btn_wifi);
		mBtnLogin = (Button) findViewById(R.id.btn_login);
		
		mWifiResult.setMovementMethod(ScrollingMovementMethod.getInstance());

		/**
		 *  处理btn_login的响应任务，启动一个MyTask，处理数据库的查询
		 */
		mBtnLogin.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				// TODO Auto-generated method stub
				mStrName = (mEtName).getText().toString();
				// AsyncTask异步任务开始
				mTask = new MyTask();
				mTask.execute(mStrName);
			}
		});
		
		/**
		 *  处理btn_submit的响应任务，启动一个InputTask，处理数据库输入
		 */
		/*inSubmit.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v){
				// TODO Auto-generated method stub
				// AsyncTask异步任务开始
				inTask = new InputTask();
				inTask.execute(mStrName);
			}
		});*/
		
		/**
		 * 处理btn_wifi的响应任务，启动一个WifiTask
		 */
		mBtnWifi.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v){
				wifiTask = new WifiTask();
				wifiTask.execute();
			}	
		});
	}
	
	
	/**
	 *  数据库输入线程的具体任务，包括：
	 *  1. 连接servlet
	 *  2. 将用户信息通过param用HttpPost发送给server
	 */
	/*private class InputTask extends AsyncTask<String, Void, String> {

			@Override
			protected String doInBackground(String... params) {	
				//通过HttpPost连接servlet
				HttpClient hc = new DefaultHttpClient();
				String address = "http://" + (mEtServerIP).getText().toString() + "/servlet/JsonServlet";
				HttpPost hp = new HttpPost(address);
				String name = (inName).getText().toString();
				String age = (inAge).getText().toString();
				String sex = (inSex).getText().toString();
				
				//通过param发送参数给servlet
				List<NameValuePair>param = new ArrayList<NameValuePair>();
				param.add(new BasicNameValuePair("type", "input"));
				param.add(new BasicNameValuePair("name", name));
				param.add(new BasicNameValuePair("age", age));
				param.add(new BasicNameValuePair("sex", sex));
				try{
					hp.setEntity(new UrlEncodedFormEntity(param, "utf-8")); 
					// 发送请求
					HttpResponse response = hc.execute(hp);
					// 返回200即请求成功
					if (response.getStatusLine().getStatusCode() == 200) {
						//数据写入成功
						System.out.println("上传成功");
					} else {
						System.out.println("连接失败");
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
				return name;
			}
		}*/
			
	class WifiTask extends AsyncTask<Void, Integer, String> {
		
		@Override
		protected String doInBackground(Void... params) {
			publishProgress(0);
			String wifiResult = "";
			List<ScanResult> results = null;
			for (int i = 0; i < 5; i++) {
				try {
					Thread.sleep(1000);
				}
				catch (InterruptedException e) {
				}
				//wifiResult = "";
				WifiManager wifi = (WifiManager) getSystemService(Context.WIFI_SERVICE);
				wifi.startScan();
				results = wifi.getScanResults();
				if (results == null)
				{
					return "Wifi 未打开";
				}
				int k = 0;
				for(ScanResult result: results)
				{
					System.out.println(result.toString());
					wifiResult += (++k) + ": " + result.SSID + " " + result.BSSID + " " + result.level + "\n";
				}
				publishProgress(i + 1);
			}
			//mWifiResult.setText(wifiResult);

			//通过HttpPost连接servlet
			HttpClient hc = new DefaultHttpClient();
			String address = "http://" + (mEtServerIP).getText().toString() + "/servlet/JsonServlet";
			HttpPost hp = new HttpPost(address);
			String strPos = inPos.getText().toString();
			String strX = inX.getText().toString();
			String strY = inY.getText().toString();
			String strZ = inZ.getText().toString();
			
			//通过param发送参数给servlet
			List<NameValuePair> param = new ArrayList<NameValuePair>();
			param.add(new BasicNameValuePair("type", "input"));
			param.add(new BasicNameValuePair("pos", strPos));
			param.add(new BasicNameValuePair("x", strX));
			param.add(new BasicNameValuePair("y", strY));
			param.add(new BasicNameValuePair("z", strZ));
			for(ScanResult result: results) {
				param.add(new BasicNameValuePair(result.BSSID, result.level + ""));
			}
			try{
				hp.setEntity(new UrlEncodedFormEntity(param, "utf-8")); 
				// 发送请求
				HttpResponse response = hc.execute(hp);
				// 返回200即请求成功
				if (response.getStatusLine().getStatusCode() == 200) {
					//数据写入成功
					System.out.println("上传成功");
				} else {
					System.out.println("连接失败");
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
			mWifiResult.setText(values[0] + " scan(s) finished!");
		}
	}

	class MyTask extends AsyncTask<String, Void, String> {

		@Override
		protected String doInBackground(String... params) {
			// TODO Auto-generated method stub
			HttpClient hc = new DefaultHttpClient();
			// 这里是服务器的IP，不要写成localhost了，即使在本机测试也要写上本机的IP地址，localhost会被当成模拟器自身的
			String address = "http://" + (mEtServerIP).getText().toString() + "/servlet/JsonServlet";
			HttpPost hp = new HttpPost(address);
			List<NameValuePair>param = new ArrayList<NameValuePair>();
			param.add(new BasicNameValuePair("type", "search"));
			param.add(new BasicNameValuePair("name", params[0]));
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
					System.out.println("连接失败");
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
			String age = null, id = null, name=null, sex=null, str=null;
			mStrResult = EntityUtils.toString(response.getEntity());
			// 将返回结果生成JSON对象，返回的格式首先是user数组
			JSONArray userarray = new JSONObject(mStrResult).getJSONArray("users" );
			//从user数组中提取出user的信息
			//对于同名用户，只获取最后一条信息 //改成了只获取第一条
            for(int i=userarray.length()-1; i>=0;i--) {
            	JSONObject userInfo = userarray.getJSONObject(i);
                id = userInfo.getString("id" );
                name = userInfo.getString("name");
                age = userInfo.getString("age" );
                sex = userInfo.getString("sex");
                str = "Id: " + id + " Name: " + name + " Age: " + age + " Sex: " + sex;
                System.out.println(str);
            }
            
            return str;
			//return wifiResult;
		}
	}
}