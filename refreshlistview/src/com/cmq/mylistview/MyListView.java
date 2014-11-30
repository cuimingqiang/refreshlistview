package com.cmq.mylistview;

import java.text.SimpleDateFormat;
import java.util.Date;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.AsyncTask;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.RotateAnimation;
import android.widget.AbsListView;
import android.widget.AbsListView.OnScrollListener;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;

public class MyListView extends ListView implements OnScrollListener {
	private static final String TAG = "MyListView";
	private View mHeaderView;//刷新布局
	private int mHeaderHeight;//刷新布局的高度
	private View mFooterView;//加载更多布局
	private int mFooterHeight;//加载更多布局的高度
	private ImageView mArrow;//下拉刷新，指示箭头
	private ProgressBar mProgressbar;//刷新布局，刷新状态
	private TextView mStateTV;//刷新布局，文字提示
	private TextView mLastUpdateTime;//上次更新时间
	private RotateAnimation mUpAnim;
	private RotateAnimation mDownAnim;
	private OnRefreshListener mOnRefreshListener;//异步回调
	private int mActionState;//刷新、加载更多、空三种状态。
	private static final int ACTION_ADIL = 0;//空闲
	private static final int ACTION_REFRESH = 1;//刷新动作
	private static final int ACTION_LOAD = 2;//加载更多动作

	private int mCurrentSatet;//当前动作的状态
	private static final int REFRESH_PULL_DOWN = 0;//下拉但未到可以刷新的状态。
	private static final int REFRESH_RELEASE = 1;//可以刷新。
	private static final int REFRESH_DOING = 2;//正在刷新
	
	private static final int LOAD_UP = 3;//上拉但未到可以加载更多的状态。
	private static final int LOAD_RELEASE = 4;//达到可以加载更多的状态。
	private static final int LOAD_DOING = 5;//正在加载更多
	public MyListView(Context context, AttributeSet attrs) {
		super(context, attrs);
		initView();
	}

	private void initView() {
		initHeader();
		initFooter();
		this.setOnScrollListener(this);
	}

	private void initHeader() {
		mHeaderView = View.inflate(getContext(),
				R.layout.refresh_header_view, null);
		mArrow = (ImageView) mHeaderView
				.findViewById(R.id.iv_refresh_header_view_pull_down_arrow);
		mProgressbar = (ProgressBar) mHeaderView
				.findViewById(R.id.pb_refresh_header_view_pull_down);
		mStateTV = (TextView) mHeaderView
				.findViewById(R.id.tv_refresh_header_view_pull_down_state);
		mLastUpdateTime = (TextView) mHeaderView
				.findViewById(R.id.tv_refresh_header_view_pull_down_last_update_time);

		mLastUpdateTime.setText("最后刷新时间:" + getCurrentTime());

		// 测量下拉刷新头的高度.
		mHeaderView.measure(0, 0);
		// 得到下拉刷新头布局的高度
		mHeaderHeight = mHeaderView.getMeasuredHeight();

		// 隐藏头布局
		mHeaderView.setPadding(0, -mHeaderHeight, 0, 0);

		this.addHeaderView(mHeaderView);
//		this.setSelection(0);
		// 初始化动画
		initAnimation();
	}
	private void initFooter(){
		mFooterView = View.inflate(getContext(), R.layout.refresh_footer_view, null);
		mFooterView.measure(0, 0);
		mFooterHeight = mFooterView.getMeasuredHeight();	
		mFooterView.setPadding(0, -mFooterHeight, 0, 0);
		this.addFooterView(mFooterView);
	}
	private void initAnimation() {
		mUpAnim = new RotateAnimation(0, -180, Animation.RELATIVE_TO_SELF,
				0.5f, Animation.RELATIVE_TO_SELF, 0.5f);
		mUpAnim.setDuration(500);
		mUpAnim.setFillAfter(true);

		mDownAnim = new RotateAnimation(-180, -360, Animation.RELATIVE_TO_SELF,
				0.5f, Animation.RELATIVE_TO_SELF, 0.5f);
		mDownAnim.setDuration(500);
		mDownAnim.setFillAfter(true);

	}

	private int mDownY;//记录当下下拉刷新、上拉加载更多时的初始位置。
	private int mCurrY;//移动中的位置
	private int mDiffY;//移动量

	@Override
	public boolean onTouchEvent(MotionEvent ev) {
		switch (ev.getAction()) {
		case MotionEvent.ACTION_DOWN:
			mDownY = (int) ev.getY();
			break;
		case MotionEvent.ACTION_MOVE:
			mCurrY = (int) ev.getY();
			mDiffY = mCurrY - mDownY;
			if(mActionState==ACTION_ADIL){
				mDownY = mCurrY;
			}
			if(mDiffY>0){
				if(mActionState == ACTION_REFRESH){
					mDiffY -= mHeaderHeight;
					mHeaderView.setPadding(0, mDiffY, 0, 0);
					if (mDiffY > 0 && mCurrentSatet != REFRESH_RELEASE) {
						mCurrentSatet = REFRESH_RELEASE;
						refreshPullDownHeaderState();
					} else if (mDiffY < 0 && mCurrentSatet != REFRESH_PULL_DOWN) {
						mCurrentSatet = REFRESH_PULL_DOWN;
						refreshPullDownHeaderState();
					}
					return true;
				}
			}
			else{
				if(mActionState==ACTION_LOAD){
					mDiffY  = -mFooterHeight - mDiffY;
					mFooterView.setPadding(0, 0, 0, mDiffY);
					if(mDiffY>0&&mCurrentSatet!=LOAD_RELEASE){
						mCurrentSatet = LOAD_RELEASE;
					}
					else if(mDiffY<0&&mCurrentSatet!=LOAD_UP){
						mCurrentSatet = LOAD_UP;
					}
				}
			}
			
			break;
		case MotionEvent.ACTION_UP:
			mDownY = -1;
			if (mCurrentSatet == REFRESH_PULL_DOWN) {
				// 当前状态是下拉刷新状态, 把头布局隐藏.
				mHeaderView.setPadding(0, -mHeaderHeight, 0, 0);
			} else if (mCurrentSatet == REFRESH_RELEASE) {
				// 当前状态是释放刷新, 把头布局完全显示, 并且进入到正在刷新中状态
				mHeaderView.setPadding(0, 0, 0, 0);
				mCurrentSatet = REFRESH_DOING;
				refreshPullDownHeaderState();
				new AsyncTask<Void, Void, Void>() {

					@Override
					protected Void doInBackground(Void... params) {
						// 调用用户的回调接口
						if (mOnRefreshListener != null) {
							mOnRefreshListener.onRefresh();
						}
						return null;
					}

					@Override
					protected void onPostExecute(Void result) {
						if (mOnRefreshListener != null) {
							mOnRefreshListener.onComplete(true);
						}
						onRefreshOrLoadFinish();
					}

				}.execute();
			}
			else if(mCurrentSatet == LOAD_UP){
				mFooterView.setPadding(0, -mFooterHeight, 0, 0);
			}
			else if(mCurrentSatet == LOAD_RELEASE){
				mFooterView.setPadding(0, 0, 0, 0);
				mCurrentSatet = LOAD_DOING;
				new AsyncTask<Void, Void, Void>() {

					@Override
					protected Void doInBackground(Void... params) {
						// 调用用户的回调接口
						if (mOnRefreshListener != null) {
							mOnRefreshListener.onLoadMore();;
						}
						return null;
					}

					@Override
					protected void onPostExecute(Void result) {
						if (mOnRefreshListener != null) {
							mOnRefreshListener.onComplete(false);
						}
						onRefreshOrLoadFinish();
					}

				}.execute();
			}
			
			break;
		default:
			break;
		}
		return super.onTouchEvent(ev);
	}

	/**
	 * 当数据刷新完成时调用此方法
	 */
	private void onRefreshOrLoadFinish() {
		if (mCurrentSatet == REFRESH_DOING) {
			// 当前是下拉刷新的操作, 隐藏头布局和复位变量.
			mHeaderView.setPadding(0, -mHeaderHeight, 0, 0);
			mProgressbar.setVisibility(View.INVISIBLE);
			mArrow.setVisibility(View.VISIBLE);
			mStateTV.setText("下拉刷新");
			mLastUpdateTime.setText("最后刷新时间: " + getCurrentTime());
		}
		else {
			mFooterView.setPadding(0, -mFooterHeight, 0, 0);
		}

	}

	/**
	 * 根据currentState当前的状态, 来刷新头布局的状态
	 */
	private void refreshPullDownHeaderState() {
		switch (mCurrentSatet) {
		case REFRESH_PULL_DOWN: // 下拉刷新状态
			mArrow.startAnimation(mDownAnim);
			mStateTV.setText("下拉刷新");
			break;
		case REFRESH_RELEASE: // 释放刷新状态
			mArrow.startAnimation(mUpAnim);
			mStateTV.setText("释放刷新");
			break;
		case REFRESH_DOING: // 正在刷新中
			mArrow.clearAnimation();
			mArrow.setVisibility(View.INVISIBLE);
			mProgressbar.setVisibility(View.VISIBLE);
			mStateTV.setText("正在刷新中..");
			break;
		default:
			break;
		}
	}

	private int mScrollState;//滑动状态

	@Override
	public void onScrollStateChanged(AbsListView view, int scrollState) {
		mScrollState = scrollState;
		Log.i(TAG, "scroll state:" + mScrollState);
	}

	@Override
	public void onScroll(AbsListView view, int firstVisibleItem,
			int visibleItemCount, int totalItemCount) {
		if (mScrollState == OnScrollListener.SCROLL_STATE_TOUCH_SCROLL) {
			if (firstVisibleItem < 1) {
				mActionState = ACTION_REFRESH;
				this.setSelection(0);
			} else if (firstVisibleItem + visibleItemCount == totalItemCount) {
				mActionState = ACTION_LOAD;
				this.setSelection(getCount());
			} else {
				mActionState = ACTION_ADIL;
			}
		} else {
			if (firstVisibleItem == 0) {//初始化，应认为可刷新状态
				mActionState = ACTION_REFRESH;
			}
			else if(firstVisibleItem+visibleItemCount==totalItemCount){
				mActionState = ACTION_LOAD;
			}
		}
	}

	@SuppressLint("SimpleDateFormat")
	private String getCurrentTime() {
		SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		return format.format(new Date());
	}

	public void setOnRefreshListener(OnRefreshListener listener) {
		this.mOnRefreshListener = listener;
	}

	public static interface OnRefreshListener {

		/**
		 * 异步方法，刷新数据。
		 */
		public void onRefresh();

		/**
		 * 异步方法，加载更多。
		 */
		public void onLoadMore();

		/**
		 * 完成异步操作之后，更新UI。
		 */
		public void onComplete(Boolean isRefresh);
	}

}
