package com.jsondemo.activity;

import java.util.ArrayList;
import java.util.List;

import android.app.ActionBar;
import android.app.Activity;
import android.app.LocalActivityManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.os.Bundle;
import android.os.Parcelable;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v4.view.ViewPager.OnPageChangeListener;
import android.util.DisplayMetrics;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.TranslateAnimation;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import android.view.KeyEvent;
import android.widget.TextView.OnEditorActionListener;

@SuppressWarnings("deprecation")
public class HeaderActivity extends Activity {
	// 用于标签控制的变量
	Context context = null;
	LocalActivityManager manager = null;
	private ViewPager pager = null; // 页卡内容
	private ImageView cursor;// 动画图片
	private TextView t1, t2;// 页卡头标
	private int offset = 0;// 动画图片偏移量
	private int currIndex = 0;// 当前页卡编号
	private int bmpW;// 动画图片宽度

	ActionBar actionBar;// 最上方那一行
	private EditText serverIP;
	private Button submitIP;
	private Menu menu;
	private String serverIp;

	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.header);

		context = HeaderActivity.this;
		manager = new LocalActivityManager(this, true);
		manager.dispatchCreate(savedInstanceState);

		// 初始化标签
		t1 = (TextView) findViewById(R.id.text1);
		t2 = (TextView) findViewById(R.id.text2);

		t1.setOnClickListener(new TagOnClickListener(0));
		t2.setOnClickListener(new TagOnClickListener(1));

		// 初始化ViewPager
		pager = (ViewPager) findViewById(R.id.vPager);
		final ArrayList<View> list = new ArrayList<View>(); // Tab页面列表
		Intent intent = new Intent(context, QueryActivity.class);
		list.add(getView("A", intent));
		Intent intent2 = new Intent(context, InputActivity.class);
		list.add(getView("B", intent2));
		pager.setAdapter(new MyPagerAdapter(list));
		pager.setCurrentItem(0);
		pager.setOnPageChangeListener(new MyOnPageChangeListener());

		// 初始化动画
		cursor = (ImageView) findViewById(R.id.cursor);
		bmpW = BitmapFactory.decodeResource(getResources(), R.drawable.bar)
				.getWidth();// 获取图片宽度
		DisplayMetrics dm = new DisplayMetrics();
		getWindowManager().getDefaultDisplay().getMetrics(dm);
		int screenW = dm.widthPixels;// 获取分辨率宽度
		offset = (screenW / 2 - bmpW) / 2;// 计算偏移量
		Matrix matrix = new Matrix();
		matrix.postTranslate(offset, 0);
		cursor.setImageMatrix(matrix);// 设置动画初始位置

		// menu
		actionBar = getActionBar();
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.main, menu);
		this.menu = menu;
		return true;
	}

	// 通过activity获取视图
	private View getView(String id, Intent intent) {
		return manager.startActivity(id, intent).getDecorView();
	}

	// 标签点击监听
	public class TagOnClickListener implements View.OnClickListener {
		private int index = 0;

		public TagOnClickListener(int i) {
			index = i;
		}

		@Override
		public void onClick(View v) {
			pager.setCurrentItem(index);
		}
	};

	// ViewPager适配器
	public class MyPagerAdapter extends PagerAdapter {
		public List<View> mListViews;

		public MyPagerAdapter(List<View> mListViews) {
			this.mListViews = mListViews;
		}

		@Override
		public void destroyItem(View arg0, int arg1, Object arg2) {
			((ViewPager) arg0).removeView(mListViews.get(arg1));
		}

		@Override
		public void finishUpdate(View arg0) {
		}

		@Override
		public int getCount() {
			return mListViews.size();
		}

		@Override
		public Object instantiateItem(View arg0, int arg1) {
			((ViewPager) arg0).addView(mListViews.get(arg1), 0);
			return mListViews.get(arg1);
		}

		@Override
		public boolean isViewFromObject(View arg0, Object arg1) {
			return arg0 == (arg1);
		}

		@Override
		public void restoreState(Parcelable arg0, ClassLoader arg1) {
		}

		@Override
		public Parcelable saveState() {
			return null;
		}

		@Override
		public void startUpdate(View arg0) {
		}
	}

	// 页卡切换监听
	public class MyOnPageChangeListener implements OnPageChangeListener {

		@Override
		public void onPageSelected(int arg0) {
			int one = offset * 2 + bmpW;// 页卡1 -> 页卡2 偏移量
			Animation animation = new TranslateAnimation(one * currIndex, one
					* arg0, 0, 0);
			currIndex = arg0;
			animation.setFillAfter(true);// True:图片停在动画结束位置
			animation.setDuration(300);
			cursor.startAnimation(animation);
		}

		@Override
		public void onPageScrolled(int arg0, float arg1, int arg2) {
		}

		@Override
		public void onPageScrollStateChanged(int arg0) {
		}
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		// action with ID action_settings was selected
		case R.id.action_settings:
			// Toast.makeText(getApplicationContext(), "setting selected",
			// Toast.LENGTH_SHORT).show();
			actionBar.setCustomView(R.layout.menu_ip_input);
			serverIP = (EditText) actionBar.getCustomView().findViewById(
					R.id.serverIP);
			serverIP.setText(ServerIP.getInstance().getServerIp());
			actionBar.setDisplayOptions(ActionBar.DISPLAY_SHOW_CUSTOM
					| ActionBar.DISPLAY_SHOW_HOME);
			item.setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER);
			(menu.findItem(R.id.action_submit))
					.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
			break;
		case R.id.action_submit:
			// Toast.makeText(getApplicationContext(), "submit selected",
			// Toast.LENGTH_SHORT).show();
			ServerIP.getInstance().setServerIp(serverIP.getText().toString());
			actionBar.setCustomView(null);
			actionBar.setDisplayOptions(ActionBar.DISPLAY_SHOW_CUSTOM
					| ActionBar.DISPLAY_SHOW_HOME);
			item.setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER);
			(menu.findItem(R.id.action_settings))
					.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
			break;
		default:
			break;
		}
		return true;
	}
}
