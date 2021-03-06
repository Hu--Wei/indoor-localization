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
import org.json.JSONException;
import org.json.JSONObject;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.AsyncTask;
import android.os.Bundle;

import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextView;
import android.net.wifi.*;

public class QueryActivity extends Activity implements SensorEventListener {
	// server IP, for example 101.5.155.101:8080
	//private TextView mEtServerIP;

	private TextView mTvResult;

	private String mStrResult;

	private Button mBtnLogin;

	private ScrollView mScrollView;
	private ImageView imageView;

	private QueryTask mTask;
	private float orientOfDevice;

	// orientation sensor
	private SensorManager mSensorManager;
	private Sensor mOrientation;

	// progress bar
	private ProgressBar bar1;
	private double expectedTime = 2500;

	// position (left-down corner is 0, 0)
	float posX;// 0 ~ 1
	float posY;// 0 ~ 1

	// wifi manager
	private WifiManager wifi;
	WifiReceiver receiver;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		// 显示app的界面
		setContentView(R.layout.query_main);

		// 获取app的组件信息
		//mEtServerIP = (TextView) findViewById(R.id.et_serverip);
		mTvResult = (TextView) findViewById(R.id.tv_result);
		mBtnLogin = (Button) findViewById(R.id.btn_login);
		mScrollView = (ScrollView) findViewById(R.id.scrollView);
		imageView = (ImageView) findViewById(R.id.img_map);

		// orientation sensor
		mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
		mOrientation = mSensorManager.getDefaultSensor(Sensor.TYPE_ORIENTATION);
		mSensorManager.registerListener(this, mOrientation,
				SensorManager.SENSOR_DELAY_NORMAL);

		// progress bar
		bar1 = (ProgressBar) findViewById(R.id.bar1);

		// position initialization
		posX = 0.5f;
		posY = 0.5f;

		wifi = (WifiManager) getSystemService(Context.WIFI_SERVICE);
		receiver = new WifiReceiver();

		/**
		 * 处理btn_login的响应任务，启动一个MyTask，处理数据库的查询
		 */
		mBtnLogin.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				// TODO Auto-generated method stub
				// mStrName = (mEtName).getText().toString();
				// AsyncTask异步任务开始
				bar1.setVisibility(View.VISIBLE);
				mTask = new QueryTask();
				mTask.execute();
			}
		});

	}

	class QueryTask extends AsyncTask<Void, Integer, String> {
		private List<ScanResult> results = null;
		public boolean finished = false;
		private int progress;
		private long startTime, duration;

		@Override
		protected String doInBackground(Void... params) {
			startTime = System.currentTimeMillis();
			progress = 0;
			publishProgress(0, 0);
			registerReceiver(receiver, new IntentFilter(
					WifiManager.SCAN_RESULTS_AVAILABLE_ACTION));
			wifi.startScan();

			while (!finished) {
				try {
					Thread.sleep(100);
					progress += 100;
					publishProgress(progress, 0);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
			publishProgress(progress, 1);
			// TODO Auto-generated method stub
			HttpClient hc = new DefaultHttpClient();
			// 这里是服务器的IP，不要写成localhost了，即使在本机测试也要写上本机的IP地址，localhost会被当成模拟器自身的
			String address = "http://" + ServerIP.getInstance().getServerIp()
					+ ":8080/ServerJsonDemo/servlet/JsonServlet";
			HttpPost hp = new HttpPost(address);
			List<NameValuePair> param = new ArrayList<NameValuePair>();
			param.add(new BasicNameValuePair("type", "search"));
			results = wifi.getScanResults();
			param.add(new BasicNameValuePair("num", results.size() + ""));
			int idx = 0;
			for (ScanResult result : results) {
				param.add(new BasicNameValuePair((idx++) + "", result.BSSID
						+ "&" + result.level));
			}
			String str = null;
			try {
				hp.setEntity(new UrlEncodedFormEntity(param, "utf-8")); // jsonObj.toString()));
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
			bar1.setProgress(100);
			duration = System.currentTimeMillis() - startTime;
			expectedTime = duration;
			// 返回包好user信息的str
			unregisterReceiver(receiver);
			return str;
		}

		@Override
		protected void onPostExecute(String result) {
			// TODO Auto-generated method stub
			super.onPostExecute(result);
			// 在文本框中显示user信息
			mTvResult.setText(result);
			bar1.setVisibility(View.GONE);
			
			int scrollTarget = (int) (imageView.getTop() * posY + imageView
					.getBottom() * (1 - posY));
			int screenHeight = mScrollView.getBottom() - mScrollView.getTop();
			scrollTarget -= screenHeight / 2;
			int totalHeight = imageView.getBottom() - mBtnLogin.getTop();
			if (scrollTarget < 0)
				scrollTarget = 0;
			if (scrollTarget > totalHeight - screenHeight)
				scrollTarget = totalHeight - screenHeight;
			mScrollView.smoothScrollTo(0, scrollTarget);
		}

		@Override
		protected void onProgressUpdate(Integer... values) {
			super.onProgressUpdate(values);
			switch (values[1]) {
			case 0:
				mTvResult.setText("Scanning Wifi...");
				break;
			case 1:
				mTvResult.setText("Locating...");
				break;
			}
			bar1.setProgress((int) (values[0] / expectedTime * 100));
		}

		/**
		 * 处理HttpResponse返回的response信息，从体重提取name, sex, age等信息
		 * 
		 * @param response
		 * @return
		 * @throws ParseException
		 * @throws IOException
		 * @throws JSONException
		 */
		String PhaseJson(HttpResponse response) throws ParseException,
				IOException, JSONException {
			String pos = null, x = null, y = null, str = null;
			mStrResult = EntityUtils.toString(response.getEntity());
			// 将返回结果生成JSON对象，返回的格式首先是user数组
			JSONObject result = new JSONObject(mStrResult);
			// 从user数组中提取出user的信息
			// 对于同名用户，只获取最后一条信息 //改成了只获取第一条
			pos = result.getString("pos");
			x = result.getString("x");
			y = result.getString("y");
			str = "Position: " + pos;
			System.out.println(str);

			posX = Float.parseFloat(x) / (float) 6.5;
			posY = Float.parseFloat(y) / (float) 16.5;

			return str;
		}
	}

	class WifiReceiver extends BroadcastReceiver {
		@Override
		public void onReceive(Context context, Intent intent) {
			mTask.finished = true;
		}
	}

	public void displayMap() {
		BitmapFactory.Options myOptions = new BitmapFactory.Options();
		myOptions.inDither = true;
		myOptions.inScaled = false;
		myOptions.inPreferredConfig = Bitmap.Config.ARGB_8888;// important
		myOptions.inPurgeable = true;

		Bitmap bitmap = BitmapFactory.decodeResource(getResources(),
				R.drawable.map, myOptions);
		Paint paint = new Paint();
		paint.setAntiAlias(true);
		paint.setColor(Color.RED);
		paint.setStyle(Paint.Style.FILL);

		Bitmap workingBitmap = Bitmap.createBitmap(bitmap);
		Bitmap mutableBitmap = workingBitmap
				.copy(Bitmap.Config.ARGB_8888, true);

		Canvas canvas = new Canvas(mutableBitmap);

		Matrix matrix = new Matrix();
		matrix.setRotate(orientOfDevice + 90);

		Path path = new Path();
		path.setFillType(Path.FillType.EVEN_ODD);
		path.moveTo(0, -35);
		path.lineTo(10, 0);
		path.lineTo(-10, 0);
		path.close();
		path.transform(matrix);
		path.offset(canvas.getWidth() * (float) posX, canvas.getHeight()
				* (1 - (float) posY));

		canvas.drawPath(path, paint);

		// offset is cumulative
		// next draw displaces 50,100 from previous
		imageView.setAdjustViewBounds(true);
		imageView.setImageBitmap(mutableBitmap);

	}

	@Override
	public void onSensorChanged(SensorEvent event) {
		// TODO Auto-generated method stub
		float azimuth_angle = event.values[0];
		orientOfDevice = azimuth_angle;
		displayMap();
	}

	@Override
	public void onAccuracyChanged(Sensor sensor, int accuracy) {
		// TODO Auto-generated method stub
	}

	@Override
	protected void onResume() {
		super.onResume();
		mSensorManager.registerListener(this, mOrientation,
				SensorManager.SENSOR_DELAY_NORMAL);
	}

	@Override
	protected void onPause() {
		super.onPause();
		mSensorManager.unregisterListener(this);
	}
}
