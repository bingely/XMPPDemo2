package com.way.activity;

import android.os.Bundle;
import android.text.util.Linkify;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.RequestManager;
import com.way.ui.glide.GlideCircleTransform;
import com.way.ui.glide.GlideRoundTransform;
import com.way.ui.swipeback.SwipeBackActivity;
import com.way.xx.R;

public class AboutActivity extends SwipeBackActivity {
	private RequestManager glideRequest;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.about);
		TextView tv = (TextView) findViewById(R.id.app_information);
		// android.text.util.Linkify是一个辅助类，通过RegEx样式匹配，自动地在TextView类（和继承的类）中创建超链接
		Linkify.addLinks(tv, Linkify.ALL);
		
		
		ImageView imageview = (ImageView) findViewById(R.id.brand);
		
		glideRequest = Glide.with(this);
		//glideRequest.load("https://www.baidu.com/img/bdlogo.png").transform(new GlideCircleTransform(this)).into(imageview);
		
		glideRequest.load("http://www.chinagif.com/gif/animal/fox/0018.gif").transform(new GlideRoundTransform(this)).into(imageview);
	}
	
	// 点击头像弹出更新信息 TODO
	public void showChangeLog(View view) {
		//new ChangeLog(this).getFullLogDialog().show();
	}
}
