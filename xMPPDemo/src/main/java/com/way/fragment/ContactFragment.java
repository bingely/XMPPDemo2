package com.way.fragment;
import java.util.HashMap;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.ExpandableListView;
import android.widget.ExpandableListView.OnChildClickListener;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextView;
import com.way.activity.ChatActivity;
import com.way.adapter.RosterAdapter;
import com.way.app.XXBroadcastReceiver.EventHandler;
import com.way.service.IConnectionStatusCallback;
import com.way.service.IMService;
import com.way.ui.iphonetreeview.IphoneTreeView;
import com.way.ui.pulltorefresh.PullToRefreshBase;
import com.way.ui.pulltorefresh.PullToRefreshBase.OnRefreshListener;
import com.way.ui.pulltorefresh.PullToRefreshScrollView;
import com.way.ui.slidingmenu.SlidingMenu;
import com.way.util.NetUtil;
import com.way.util.PreferenceConstants;
import com.way.util.PreferenceUtils;
import com.way.util.T;
import com.way.util.XMPPHelper;
import com.way.xx.R;
public class ContactFragment extends Fragment implements OnClickListener,
		IConnectionStatusCallback, EventHandler {
	private static final int ID_CHAT = 0;
	private static final int ID_AVAILABLE = 1;
	private static final int ID_AWAY = 2;
	private static final int ID_XA = 3;
	private static final int ID_DND = 4;
	public static HashMap<String, Integer> mStatusMap;
	static {
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
	private SlidingMenu mSlidingMenu;
	private View mNetErrorView;
	private TextView mTitleNameView;
	private ImageView mTitleStatusView;
	private ProgressBar mTitleProgressBar;
	private PullToRefreshScrollView mPullRefreshScrollView;
	private IphoneTreeView mIphoneTreeView;
	private RosterAdapter mRosterAdapter;
	// TODO
	//private ContentObserver mRosterObserver = new RosterObserver();
	private int mLongPressGroupId, mLongPressChildId;
	
	private Context mContext;
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		mContext = getActivity();
		// TODO 放在这边合适吗
		//registerListAdapter();
	}
	
	@Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        return inflater.inflate(R.layout.contact_fragment, container, false);
    }
    
    @Override
	public void onViewCreated(View view, Bundle savedInstanceState) {
		super.onViewCreated(view, savedInstanceState);
		initView(view);
		registerListAdapter();
	}
    
    private void initView(View view) {
		mNetErrorView = view.findViewById(R.id.net_status_bar_top);
		mTitleNameView = (TextView) view.findViewById(R.id.ivTitleName);
		mTitleProgressBar = (ProgressBar) view.findViewById(R.id.ivTitleProgress);
		mTitleStatusView = (ImageView) view.findViewById(R.id.ivTitleStatus);
		mPullRefreshScrollView = (PullToRefreshScrollView) view.findViewById(R.id.pull_refresh_scrollview);
		mIphoneTreeView = (IphoneTreeView) view.findViewById(R.id.iphone_tree_view);
		mIphoneTreeView.setHeaderView(getActivity().getLayoutInflater().inflate(R.layout.contact_buddy_list_group, mIphoneTreeView, false));
		mIphoneTreeView.setEmptyView(view.findViewById(R.id.empty));
		mTitleNameView.setOnClickListener(this);
		
		handleView();
	}
    
    private void registerListAdapter() {
		mRosterAdapter = new RosterAdapter(mContext, mIphoneTreeView,
				mPullRefreshScrollView);
		mIphoneTreeView.setAdapter(mRosterAdapter);
		mRosterAdapter.requery();
	}
	private void handleView() {
		// 改成getActivity()
		String prefString = PreferenceUtils
				.getPrefString(getActivity(), PreferenceConstants.ACCOUNT, "");
		String splitJidAndServer = XMPPHelper.splitJidAndServer(prefString);
		mTitleNameView.setText(splitJidAndServer);
		
		mPullRefreshScrollView.setOnRefreshListener(new OnRefreshListener<ScrollView>() {
			@Override
			public void onRefresh(
					PullToRefreshBase<ScrollView> refreshView) {
				//new GetDataTask().execute();
			}
		});
		
		// 长按监听
		mIphoneTreeView.setOnItemLongClickListener(new OnItemLongClickListener() {
			@Override
			public boolean onItemLongClick(AdapterView<?> parent,
					View view, int position, long id) {
				itemLongClick(view);
				return false;
			}
		});
		mIphoneTreeView.setOnChildClickListener(new OnChildClickListener() {
			@Override
			public boolean onChildClick(ExpandableListView parent, View v,
					int groupPosition, int childPosition, long id) {
				// TODO
				String userJid = mRosterAdapter.getChild(groupPosition,
						childPosition).getJid();
				String userName = mRosterAdapter.getChild(groupPosition,
						childPosition).getAlias();
				startChatActivity(userJid, userName);
				return false;
			}
		});
	}
	
	private void startChatActivity(String userJid, String userName) {
		Intent chatIntent = new Intent(getActivity(), ChatActivity.class);
		Uri userNameUri = Uri.parse(userJid);
		chatIntent.setData(userNameUri);
		chatIntent.putExtra(ChatActivity.INTENT_EXTRA_USERNAME, userName);
		startActivity(chatIntent);
	}
    
    private void itemLongClick(View view) {
		int groupPos = (Integer) view.getTag(R.id.xxx01); // 参数值是在setTag时使用的对应资源id号
		int childPos = (Integer) view.getTag(R.id.xxx02);
		mLongPressGroupId = groupPos;
		mLongPressChildId = childPos;
		if (childPos == -1) {// 长按的是父项
			// 根据groupPos判断你长按的是哪个父项，做相应处理（弹框等）
			// TODO
			/*showGroupQuickActionBar(view
					.findViewById(R.id.group_name));*/
		} else {
			// 根据groupPos及childPos判断你长按的是哪个父项下的哪个子项，然后做相应处理。
			// "onClick child position = " + groupPos
			// + ":" + childPos);
			// TODO
			/*showChildQuickActionBar(view
					.findViewById(R.id.icon));*/
		}
	}
    @Override
	public void onNetChange() {
		if (NetUtil.getNetworkState(getActivity()) == NetUtil.NETWORN_NONE) {
			T.showShort(getActivity(), R.string.net_error_tip);
			mNetErrorView.setVisibility(View.VISIBLE);
		} else {
			mNetErrorView.setVisibility(View.GONE);
		}
	}
    @Override
	public void connectionStatusChanged(int connectedState, String reason) {
		switch (connectedState) {
		case IMService.CONNECTED:
			mTitleNameView.setText(XMPPHelper.splitJidAndServer(PreferenceUtils
					.getPrefString(mContext,
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
			T.showLong(mContext, reason);
			break;
		default:
			break;
		}
	}
	@Override
	public void onClick(View arg0) {
		// TODO Auto-generated method stub
		
	}
	
	private void setStatusImage(boolean isConnected) {
		if (!isConnected) {
			mTitleStatusView.setVisibility(View.GONE);
			return;
		}
		String statusMode = PreferenceUtils.getPrefString(getActivity(),
				PreferenceConstants.STATUS_MODE, PreferenceConstants.AVAILABLE);
		int statusId = mStatusMap.get(statusMode);
		if (statusId == -1) {
			mTitleStatusView.setVisibility(View.GONE);
		} else {
			mTitleStatusView.setVisibility(View.VISIBLE);
			mTitleStatusView.setImageResource(statusId);
		}
	}
	
	private boolean isConnected() {
		return mXxService != null && mXxService.isAuthenticated();
	}
	
	private class GetDataTask extends AsyncTask<Void, Void, String[]> {
		@Override
		protected void onPreExecute() {
			super.onPreExecute();
			// if (mPullRefreshScrollView.getState() != State.REFRESHING)
			// mPullRefreshScrollView.setState(State.REFRESHING, true);
		}
		@Override
		protected String[] doInBackground(Void... params) {
			// Simulates a background job.
			if (!isConnected()) {// 如果没有连接重新连接
				String usr = PreferenceUtils.getPrefString(getActivity(),
						PreferenceConstants.ACCOUNT, "");
				String password = PreferenceUtils.getPrefString(
						getActivity(), PreferenceConstants.PASSWORD, "");
				mXxService.Login(usr, password);
			}
			try {
				Thread.sleep(2000);
			} catch (InterruptedException e) {
			}
			return null;
		}
		@Override
		protected void onPostExecute(String[] result) {
			// Do some stuff here
			// Call onRefreshComplete when the list has been refreshed.
			mRosterAdapter.requery();// 重新查询一下数据库
			mPullRefreshScrollView.onRefreshComplete();
			// mPullRefreshScrollView.getLoadingLayoutProxy().setLastUpdatedLabel(
			// "最近更新：刚刚");
			T.showShort(getActivity(), "刷新成功!");
			super.onPostExecute(result);
		}
	}
	
}
