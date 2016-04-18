package com.way.activity;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.database.ContentObserver;
import android.database.Cursor;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentTransaction;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.way.adapter.RosterAdapter;
import com.way.app.XXBroadcastReceiver;
import com.way.app.XXBroadcastReceiver.EventHandler;
import com.way.db.RosterProvider;
import com.way.db.RosterProvider.RosterConstants;
import com.way.fragment.ContactFragment;
import com.way.fragment.RecentChatFragment;
import com.way.fragment.SettingsFragment;
import com.way.service.IConnectionStatusCallback;
import com.way.service.IMService;
import com.way.ui.quickaction.ActionItem;
import com.way.ui.quickaction.QuickAction;
import com.way.ui.quickaction.QuickAction.OnActionItemClickListener;
import com.way.util.L;
import com.way.util.NetUtil;
import com.way.util.PreferenceConstants;
import com.way.util.PreferenceUtils;
import com.way.util.StatusMode;
import com.way.util.T;
import com.way.util.XMPPHelper;
import com.way.xx.R;

public class MainActivity extends FragmentActivity implements
		OnClickListener, IConnectionStatusCallback, EventHandler,
		FragmentCallBack {
	// 人物状态
	private static final int ID_CHAT = 0;
	private static final int ID_AVAILABLE = 1;
	private static final int ID_AWAY = 2;
	private static final int ID_XA = 3;
	private static final int ID_DND = 4;
	public static HashMap<String, Integer> mStatusMap;
	static { // 把人物状态放到Map集合中
		mStatusMap = new HashMap<String, Integer>();
		mStatusMap.put(PreferenceConstants.OFFLINE, -1);
		mStatusMap.put(PreferenceConstants.DND, R.drawable.status_shield);
		mStatusMap.put(PreferenceConstants.XA, R.drawable.status_invisible);
		mStatusMap.put(PreferenceConstants.AWAY, R.drawable.status_leave);
		mStatusMap.put(PreferenceConstants.AVAILABLE, R.drawable.status_online);
		mStatusMap.put(PreferenceConstants.CHAT, R.drawable.status_qme);
	}
	private Handler mainHandler = new Handler();
	private IMService mXxService;
	private View mNetErrorView;
	private TextView mTitleNameView;
	private ImageView mTitleStatusView;
	private ProgressBar mTitleProgressBar;
	private RosterAdapter mRosterAdapter;
	private ContentObserver mRosterObserver = new RosterObserver();


	private ImageView iv_tab_contact;
	private ImageView iv_tab_conversion;
	private ImageView iv_tab_dongtai;
	private TextView tv_title;
	private int currentSelectId;
	private SettingsFragment mFrag_right;
	private ContactFragment contactFragment;
	private RecentChatFragment mFrag_left;

	// 应该做的是跟三个fragment和相关的监听

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		startService(new Intent(MainActivity.this, IMService.class));

		setContentView(R.layout.main_center_layout);
		initViews();
		registerListAdapter();
		initData();
		
	}

	private void initViews() {
		// 网络状态
		mNetErrorView = findViewById(R.id.net_status_bar_top);

		// 状态
		mTitleNameView = (TextView) findViewById(R.id.ivTitleName);
		mTitleProgressBar = (ProgressBar) findViewById(R.id.ivTitleProgress);
		mTitleStatusView = (ImageView) findViewById(R.id.ivTitleStatus);
		mTitleNameView.setText(XMPPHelper.splitJidAndServer(PreferenceUtils
				.getPrefString(this, PreferenceConstants.ACCOUNT, "")));
		mTitleNameView.setOnClickListener(this);


		// 底部
		iv_tab_contact = (ImageView) findViewById(R.id.iv_tab_contact);
		iv_tab_conversion = (ImageView) findViewById(R.id.iv_tab_conversion);
		iv_tab_dongtai = (ImageView) findViewById(R.id.iv_tab_dongtai);
		tv_title = (TextView) findViewById(R.id.tv_title);

		iv_tab_contact.setOnClickListener(this);
		iv_tab_conversion.setOnClickListener(this);
		iv_tab_dongtai.setOnClickListener(this);

	}

	public void initData() {
		mFrag_left = new RecentChatFragment();
		mFrag_right = new SettingsFragment();
		contactFragment = new ContactFragment();
		FragmentTransaction mFragementTransaction = getSupportFragmentManager()
				.beginTransaction();
		mFragementTransaction.replace(R.id.fl_content, mFrag_left);
		mFragementTransaction.commit();

		// 设置iv_tab_dongtai选中状态
		iv_tab_conversion.setSelected(true);
		currentSelectId = R.id.iv_tab_conversion;
		// mFragementTransaction.replace(R.id.fl_content, mFrag_right);
	}

	@Override
	public void onClick(View v) {
		// 如果当前选中view的id是&&的话，什么都不操作（就是不让他每次点击的时候做刷新功能）
		if (v.getId() == currentSelectId) {
			return;
		}
		// 随着点击不同按钮，相应的值也要改变？？（道理同上）
		currentSelectId = v.getId();
		// 设置选中状态
		iv_tab_contact.setSelected(v.getId() == R.id.iv_tab_contact);
		iv_tab_conversion.setSelected(v.getId() == R.id.iv_tab_conversion);
		iv_tab_dongtai.setSelected(v.getId() == R.id.iv_tab_dongtai);
		FragmentTransaction mFragementTransaction = getSupportFragmentManager()
				.beginTransaction();
		switch (v.getId()) {
		case R.id.iv_tab_conversion:
			mFragementTransaction.replace(R.id.fl_content, mFrag_left);
			// 做头部的变化 TODO
			break;
		case R.id.iv_tab_contact:
			mFragementTransaction.replace(R.id.fl_content, contactFragment);
			break;
		case R.id.iv_tab_dongtai:
			mFragementTransaction.replace(R.id.fl_content, mFrag_right);
			break;
		case R.id.ivTitleName:
			if (isConnected())
				showStatusQuickAction(v);
			break;
		default:
			break;
		}
		mFragementTransaction.commit();
	}

	private void unbindXMPPService() {
		try {
			unbindService(mServiceConnection);
			L.i(LoginActivity.class, "[SERVICE] Unbind");
		} catch (IllegalArgumentException e) {
			L.e(LoginActivity.class, "Service wasn't bound!");
		}
	}

	private void bindXMPPService() {
		L.i(LoginActivity.class, "[SERVICE] Unbind");
		bindService(new Intent(MainActivity.this, IMService.class),
				mServiceConnection, Context.BIND_AUTO_CREATE
						+ Context.BIND_DEBUG_UNBIND);
	}


	private boolean isConnected() {
		return mXxService != null && mXxService.isAuthenticated();
	}



	private static final String[] GROUPS_QUERY = new String[] {
			RosterConstants._ID, RosterConstants.GROUP, };
	private static final String[] ROSTER_QUERY = new String[] {
			RosterConstants._ID, RosterConstants.JID, RosterConstants.ALIAS,
			RosterConstants.STATUS_MODE, RosterConstants.STATUS_MESSAGE, };

	protected void setViewImage(ImageView v, String value) {
		int presenceMode = Integer.parseInt(value);
		int statusDrawable = getIconForPresenceMode(presenceMode);
		v.setImageResource(statusDrawable);
		if (statusDrawable == R.drawable.status_busy)
			v.setVisibility(View.INVISIBLE);

	}

	private int getIconForPresenceMode(int presenceMode) {
		return StatusMode.values()[presenceMode].getDrawableId();
	}

	// 就是顶部拉出在线，离线等等状态的菜单，用的是自定义控件QuickAction
	@SuppressWarnings("deprecation")
	private void showStatusQuickAction(View v) {
		QuickAction quickAction = new QuickAction(this, QuickAction.VERTICAL);
		quickAction.addActionItem(new ActionItem(ID_CHAT,
				getString(R.string.status_chat), getResources().getDrawable(
						R.drawable.status_qme)));
		quickAction.addActionItem(new ActionItem(ID_AVAILABLE,
				getString(R.string.status_available), getResources()
						.getDrawable(R.drawable.status_online)));
		quickAction.addActionItem(new ActionItem(ID_AWAY,
				getString(R.string.status_away), getResources().getDrawable(
						R.drawable.status_leave)));
		quickAction.addActionItem(new ActionItem(ID_XA,
				getString(R.string.status_xa), getResources().getDrawable(
						R.drawable.status_invisible)));
		quickAction.addActionItem(new ActionItem(ID_DND, // 请勿打扰
				getString(R.string.status_dnd), getResources().getDrawable(
						R.drawable.status_shield)));
		quickAction
				.setOnActionItemClickListener(new OnActionItemClickListener() {

					@Override
					public void onItemClick(QuickAction source, int pos,
							int actionId) {
						if (!isConnected()) {
							T.showShort(MainActivity.this,
									R.string.conversation_net_error_label);
							return;
						}
						switch (actionId) {
						case ID_CHAT:
							mTitleStatusView
									.setImageResource(R.drawable.status_qme);
							// 记录相应的模式和状态
							PreferenceUtils.setPrefString(MainActivity.this,
									PreferenceConstants.STATUS_MODE,
									PreferenceConstants.CHAT);
							PreferenceUtils.setPrefString(MainActivity.this,
									PreferenceConstants.STATUS_MESSAGE,
									getString(R.string.status_chat));
							break;
						case ID_AVAILABLE:
							mTitleStatusView
									.setImageResource(R.drawable.status_online);
							PreferenceUtils.setPrefString(MainActivity.this,
									PreferenceConstants.STATUS_MODE,
									PreferenceConstants.AVAILABLE);
							PreferenceUtils.setPrefString(MainActivity.this,
									PreferenceConstants.STATUS_MESSAGE,
									getString(R.string.status_available));
							break;
						case ID_AWAY:
							mTitleStatusView
									.setImageResource(R.drawable.status_leave);
							PreferenceUtils.setPrefString(MainActivity.this,
									PreferenceConstants.STATUS_MODE,
									PreferenceConstants.AWAY);
							PreferenceUtils.setPrefString(MainActivity.this,
									PreferenceConstants.STATUS_MESSAGE,
									getString(R.string.status_away));
							break;
						case ID_XA:
							mTitleStatusView
									.setImageResource(R.drawable.status_invisible);
							PreferenceUtils.setPrefString(MainActivity.this,
									PreferenceConstants.STATUS_MODE,
									PreferenceConstants.XA);
							PreferenceUtils.setPrefString(MainActivity.this,
									PreferenceConstants.STATUS_MESSAGE,
									getString(R.string.status_xa));
							break;
						case ID_DND:
							mTitleStatusView
									.setImageResource(R.drawable.status_shield);
							PreferenceUtils.setPrefString(MainActivity.this,
									PreferenceConstants.STATUS_MODE,
									PreferenceConstants.DND);
							PreferenceUtils.setPrefString(MainActivity.this,
									PreferenceConstants.STATUS_MESSAGE,
									getString(R.string.status_dnd));
							break;
						default:
							break;
						}
						//
						mXxService.setStatusFromConfig();
						SettingsFragment fragment = (SettingsFragment) getSupportFragmentManager()
								.findFragmentById(R.id.main_right_fragment);
						fragment.readData();
					}
				});
		quickAction.show(v);
	}

	public abstract class EditOk {
		abstract public void ok(String result);
	}


	@Override
	public void onNetChange() {
		if (NetUtil.getNetworkState(this) == NetUtil.NETWORN_NONE) {
			T.showShort(this, R.string.net_error_tip);
			mNetErrorView.setVisibility(View.VISIBLE);
		} else {
			mNetErrorView.setVisibility(View.GONE);
		}
	}

	private void setStatusImage(boolean isConnected) {
		if (!isConnected) {
			mTitleStatusView.setVisibility(View.GONE);
			return;
		}
		String statusMode = PreferenceUtils.getPrefString(this,
				PreferenceConstants.STATUS_MODE, PreferenceConstants.AVAILABLE);
		int statusId = mStatusMap.get(statusMode);
		if (statusId == -1) {
			mTitleStatusView.setVisibility(View.GONE);
		} else {
			mTitleStatusView.setVisibility(View.VISIBLE);
			mTitleStatusView.setImageResource(statusId);
		}
	}

	@Override
	public void connectionStatusChanged(int connectedState, String reason) {
		switch (connectedState) {
		case IMService.CONNECTED:
			mTitleNameView.setText(XMPPHelper.splitJidAndServer(PreferenceUtils
					.getPrefString(MainActivity.this,
							PreferenceConstants.ACCOUNT, "")));
			mTitleProgressBar.setVisibility(View.GONE);
			// mTitleStatusView.setVisibility(View.GONE);
			setStatusImage(true);
			break;
		case IMService.CONNECTING:
			mTitleNameView.setText(R.string.login_prompt_msg);
			mTitleProgressBar.setVisibility(View.VISIBLE);
			mTitleStatusView.setVisibility(View.GONE);
			break;
		case IMService.DISCONNECTED:
			mTitleNameView.setText(R.string.login_prompt_no);
			mTitleProgressBar.setVisibility(View.GONE);
			mTitleStatusView.setVisibility(View.GONE);
			T.showLong(this, reason);
			break;

		default:
			break;
		}
	}

	@Override
	public IMService getService() {
		return mXxService;
	}

	@Override
	public MainActivity getMainActivity() {
		return this;
	}

	public void updateRoster() {
		mRosterAdapter.requery();
	}

	private class RosterObserver extends ContentObserver {
		public RosterObserver() {
			super(mainHandler);
		}

		public void onChange(boolean selfChange) {
			L.d(MainActivity.class, "RosterObserver.onChange: " + selfChange);
			if (mRosterAdapter != null)
				mainHandler.postDelayed(new Runnable() {
					public void run() {
						updateRoster();
					}
				}, 100);
		}
	}


	ServiceConnection mServiceConnection = new ServiceConnection() {

		@Override
		public void onServiceConnected(ComponentName name, IBinder service) {
			mXxService = ((IMService.XXBinder) service).getService();
			mXxService.registerConnectionStatusCallback(MainActivity.this);
			// 开始连接xmpp服务器
			// 如果连接到服务器上了，读取SP中的用户名和密码，登入到xmpp中
			if (!mXxService.isAuthenticated()) {
				String usr = PreferenceUtils.getPrefString(MainActivity.this,
						PreferenceConstants.ACCOUNT, "");
				String password = PreferenceUtils.getPrefString(
						MainActivity.this, PreferenceConstants.PASSWORD, "");
				mXxService.Login(usr, password);
				// mTitleNameView.setText(R.string.login_prompt_msg);
				// setStatusImage(false);
				// mTitleProgressBar.setVisibility(View.VISIBLE);
			} else {
				mTitleNameView.setText(XMPPHelper
						.splitJidAndServer(PreferenceUtils.getPrefString(
								MainActivity.this, PreferenceConstants.ACCOUNT,
								"")));
				setStatusImage(true);
				mTitleProgressBar.setVisibility(View.GONE);
			}
		}

		@Override
		public void onServiceDisconnected(ComponentName name) {
			mXxService.unRegisterConnectionStatusCallback();
			mXxService = null;
		}

	};
	
	private void registerListAdapter() {
		mRosterAdapter = new RosterAdapter(this);
		mRosterAdapter.requery();
	}
	/**
	 * 连续按两次返回键就退出
	 */
	private long firstTime;

	@Override
	public void onBackPressed() {
		if (System.currentTimeMillis() - firstTime < 3000) {
			finish();
		} else {
			firstTime = System.currentTimeMillis();
			T.showShort(this, R.string.press_again_backrun);
		}
	}

	@Override
	protected void onResume() {
		super.onResume();
		bindXMPPService();
		getContentResolver().registerContentObserver(
				RosterProvider.CONTENT_URI, true, mRosterObserver);
		setStatusImage(isConnected());
		mRosterAdapter.requery();
		// if (!isConnected())
		// mTitleNameView.setText(R.string.login_prompt_no);
		XXBroadcastReceiver.mListeners.add(this);
		if (NetUtil.getNetworkState(this) == NetUtil.NETWORN_NONE)
			mNetErrorView.setVisibility(View.VISIBLE);
		else
			mNetErrorView.setVisibility(View.GONE);
		/*ChangeLog cl = new ChangeLog(this);
		if (cl != null && cl.firstRun()) {
			cl.getFullLogDialog().show();
		}*/
	}

	@Override
	protected void onPause() {
		super.onPause();
		getContentResolver().unregisterContentObserver(mRosterObserver);
		unbindXMPPService();
		XXBroadcastReceiver.mListeners.remove(this);
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();

	} 
	
	//需要修改下 
	public List<String> getRosterGroups() {
		// we want all, online and offline
		List<String> list = new ArrayList<String>();
		Cursor cursor = getContentResolver().query(RosterProvider.GROUPS_URI,
				GROUPS_QUERY, null, null, RosterConstants.GROUP);
		int idx = cursor.getColumnIndex(RosterConstants.GROUP);
		cursor.moveToFirst();
		while (!cursor.isAfterLast()) {
			list.add(cursor.getString(idx));
			cursor.moveToNext();
		}
		cursor.close();
		return list;
	}

}
