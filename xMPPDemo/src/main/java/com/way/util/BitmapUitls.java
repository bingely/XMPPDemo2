package com.way.util;

import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PorterDuffXfermode;
import android.graphics.RectF;

import com.way.ui.pulltorefresh.PullToRefreshBase.Mode;

public class BitmapUitls {
	/**
	 * 把bitmap转成圆形
	 * */
	public Bitmap toRoundBitmap(Bitmap bitmap) {
		int width = bitmap.getWidth();
		int height = bitmap.getHeight();
		int r = 0;
		// 取最短边做边长
		if (width < height) {
			r = width;
		} else {
			r = height;
		}
		// 构建一个bitmap
		Bitmap backgroundBm = Bitmap.createBitmap(width, height,
				Config.ARGB_8888);
		// new一个Canvas，在backgroundBmp上画图
		Canvas canvas = new Canvas(backgroundBm);
		Paint p = new Paint();
		// 设置边缘光滑，去掉锯齿
		p.setAntiAlias(true);
		RectF rect = new RectF(0, 0, r, r);
		// 通过制定的rect画一个圆角矩形，当圆角X轴方向的半径等于Y轴方向的半径时，
		// 且都等于r/2时，画出来的圆角矩形就是圆形
		canvas.drawRoundRect(rect, r / 2, r / 2, p);
		// 设置当两个图形相交时的模式，SRC_IN为取SRC图形相交的部分，多余的将被去掉
		// TODO
		//p.setXfermode(new PorterDuffXfermode(Mode.SRC_IN));
		// canvas将bitmap画在backgroundBmp上
		canvas.drawBitmap(bitmap, null, rect, p);
		return backgroundBm;
	}
}
