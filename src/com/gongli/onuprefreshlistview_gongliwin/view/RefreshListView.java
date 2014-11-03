package com.gongli.onuprefreshlistview_gongliwin.view;

import java.text.SimpleDateFormat;
import java.util.Date;

import com.gongli.onuprefreshlistview_gongliwin.R;
import com.gongli.onuprefreshlistview_gongliwin.R.id;
import com.gongli.onuprefreshlistview_gongliwin.R.layout;

import android.content.Context;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.RotateAnimation;
import android.widget.AbsListView;
import android.widget.AbsListView.OnScrollListener;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;

public class RefreshListView extends ListView implements OnScrollListener {

	private LinearLayout mHeaderViewRoot;
	private int downY = -1;
	private int mPullDownHeaderViewHeight; // ����ͷ���ֵĸ߶�
	private int mFirstVisiblePosition = -1; // ��ǰListView��һ����ʾ��item������
	private View mPullDownHeaderView; // ����ˢ�µ�ͷ����
	
	private final int PULL_DOWN = 0; // ����ˢ��״̬
	private final int RELEASE_REFRESH = 1; // �ͷ�ˢ��״̬
	private final int REFRESHING = 2; // ����ˢ����״̬
	
	private int currentState = PULL_DOWN; // ��ǰ����ͷ��״̬, Ĭ��Ϊ: ����ˢ��״̬
	private RotateAnimation upAnima; // ������ת�Ķ���
	private RotateAnimation downAnima; // ������ת�Ķ���
	private ImageView ivArrow; // ͷ�����еļ�ͷ
	private ProgressBar mProgressBar; // ͷ�����еĽ�����
	private TextView tvState; // ͷ���ֵ�״̬
	private TextView tvDate; // ͷ�������ˢ�µ�ʱ��
	private View mCustomHeaderView; // �û���ӽ�����ͷ�����ļ�(�ֲ�ͼ)
	private OnRefreshListener mOnRefreshListener; // ��ǰListViewˢ�����ݵļ����¼�
	private View mFooterView; // �Ų��ֶ���
	private int mFooterViewHeight; // �Ų��ֵĸ߶�
	private boolean isLoadingMore = false; // �Ƿ����ڼ��ظ���, Ĭ����û�����ڼ���
	
	private boolean isEnablePullDownRefresh = false;	 // �Ƿ���������ˢ�µĹ���, Ĭ��Ϊ: ������
	private boolean isEnableLoadingMore = false;	 // �Ƿ����ü��ظ���Ĺ���, Ĭ��Ϊ: ������

	public RefreshListView(Context context) {
		super(context);
		initHeader();
		initFooter();
		setOnScrollListener(this);
	}
	
	public RefreshListView(Context context, AttributeSet attrs) {
		super(context, attrs);
		initHeader();
		initFooter();
		setOnScrollListener(this);
	}

	/**
	 * ��ʼ���Ų���
	 */
	private void initFooter() {
		mFooterView = View.inflate(getContext(), R.layout.refresh_listview_footer, null);
		mFooterView.measure(0, 0); // �����Ų���
		mFooterViewHeight = mFooterView.getMeasuredHeight();
		mFooterView.setPadding(0, -mFooterViewHeight, 0, 0);
		this.addFooterView(mFooterView);
	}

	/**
	 * ��ʼ������ˢ�µ�ͷ
	 */
	private void initHeader() {
		View mHeaderView = View.inflate(getContext(), R.layout.refresh_listview_header, null);
		mHeaderViewRoot = (LinearLayout) mHeaderView.findViewById(R.id.ll_refresh_listview_header_root);
		mPullDownHeaderView = mHeaderView.findViewById(R.id.ll_pull_down_view);
		ivArrow = (ImageView) mHeaderView.findViewById(R.id.iv_refresh_listview_arrow);
		mProgressBar = (ProgressBar) mHeaderView.findViewById(R.id.pb_refresh_listview);
		tvState = (TextView) mHeaderView.findViewById(R.id.tv_refresh_listview_state);
		tvDate = (TextView) mHeaderView.findViewById(R.id.tv_refresh_listview_last_update_time);

		tvDate.setText("���ˢ��ʱ��: " + getCurrentTime());
		
		// ������ͷ�������ص�.
		mPullDownHeaderView.measure(0, 0);// ��������ͷ����
		mPullDownHeaderViewHeight = mPullDownHeaderView.getMeasuredHeight();
		mPullDownHeaderView.setPadding(0, -mPullDownHeaderViewHeight, 0, 0);
		this.addHeaderView(mHeaderView);
		initAnimation();
	}

	/**
	 * ��ʼ��ͷ���ֵĶ���
	 */
	private void initAnimation() {
		upAnima = new RotateAnimation(
				0, -180, 
				Animation.RELATIVE_TO_SELF, 0.5f, 
				Animation.RELATIVE_TO_SELF, 0.5f);
		upAnima.setDuration(500);
		upAnima.setFillAfter(true); // �ѵ�ǰ�ؼ�ֹͣ�ڶ���������״̬��

		downAnima = new RotateAnimation(
				-180, -360, 
				Animation.RELATIVE_TO_SELF, 0.5f, 
				Animation.RELATIVE_TO_SELF, 0.5f);
		downAnima.setDuration(500);
		downAnima.setFillAfter(true); // �ѵ�ǰ�ؼ�ֹͣ�ڶ���������״̬��
	}

	/**
	 * ���һ���Զ����ͷ���ֶ���
	 * @param v
	 */
	public void addCustomHeaderView(View v) {
		mCustomHeaderView = v;
		mHeaderViewRoot.addView(v);
	}
	
	@Override
	public boolean onTouchEvent(MotionEvent ev) {
		switch (ev.getAction()) {
		case MotionEvent.ACTION_DOWN:
			downY = (int) ev.getY();
			break;
		case MotionEvent.ACTION_MOVE:
			// �ж��Ƿ���������ˢ�µĲ���
			if(!isEnablePullDownRefresh) {
				// ��ǰû������
				break;
			}
			
			// �����ǰ������ˢ���еĲ���, ֱ������, ��ִ�������Ĳ���
			if(currentState == REFRESHING) {
				break;
			}
			
			if(mCustomHeaderView != null) {
				// ����ֲ�ͼ, û����ȫ��ʾ, ��Ӧ�ý��������Ĳ���, ֱ������
				int[] location = new int[2];
				this.getLocationOnScreen(location);  // ȡ����ǰListView����Ļ��x,y���ֵ
				int mListViewLocationOnScreenY = location[1];
				
				// ȡ��mCustomHeaderView(�ֲ�ͼ)����Ļ��y���ֵ.
				mCustomHeaderView.getLocationOnScreen(location);
				int mCustomHeaderViewLocationOnScreenY = location[1];
				
//				System.out.println("Listview��y��: " + mListViewLocationOnScreenY 
//						+ ", �ֲ�ͼ��y��: " + mCustomHeaderViewLocationOnScreenY);
				
				if(mCustomHeaderViewLocationOnScreenY < mListViewLocationOnScreenY) {
//					System.out.println("��ǰ�ֲ�ͼ������һ����, ����������ˢ�µĲ���");
					break;
				}
			}
			
			if(downY == -1) {
				downY = (int) ev.getY();
			}
			
			int moveY = (int) ev.getY();
			
			// ��������������ͷ��paddingtop��ֵ
			int paddingTop = -mPullDownHeaderViewHeight + (moveY - downY);
			
			if(paddingTop > -mPullDownHeaderViewHeight
					&& mFirstVisiblePosition == 0) {
				
				if(paddingTop > 0 && currentState == PULL_DOWN) { // ��ǰ��ͷ������ȫ��ʾ, ���ҵ�ǰ��״̬������״̬
					System.out.println("�ɿ�ˢ��");
					currentState = RELEASE_REFRESH; // �ѵ�ǰ��״̬�޸�Ϊ�ͷ�ˢ�µ�״̬
					refreshPullDownHeaderView();
				} else if(paddingTop < 0 && currentState == RELEASE_REFRESH) {
					System.out.println("����ˢ��");
					currentState = PULL_DOWN; // �ѵ�ǰ��״̬�޸�Ϊ����ˢ�µ�״̬
					refreshPullDownHeaderView();
				}
				mPullDownHeaderView.setPadding(0, paddingTop, 0, 0);
				return true;
			}
			break;
		case MotionEvent.ACTION_UP:
			downY = -1;
			
			if(currentState == RELEASE_REFRESH) {
				// ��ǰ���ɿ�ˢ��, ���뵽����ˢ���еĲ���
				currentState = REFRESHING;
				refreshPullDownHeaderView();
				
				mPullDownHeaderView.setPadding(0, 0, 0, 0);
				
				if(mOnRefreshListener != null) {
					mOnRefreshListener.OnPullDownRefresh(); // �ص��û����¼�
				}
			} else if(currentState == PULL_DOWN) {
				// ��ǰ������ˢ��, ʲô������, ������ͷ������.
				mPullDownHeaderView.setPadding(0, -mPullDownHeaderViewHeight, 0, 0);
			}
			break;
		default:
			break;
		}
		return super.onTouchEvent(ev);
	}
	
	/**
	 * ���ݵ�ǰ��״̬, ˢ������ͷ���ֵ�״̬
	 */
	private void refreshPullDownHeaderView() {
		switch (currentState) {
		case PULL_DOWN: // ��ǰ������ˢ��״̬
			ivArrow.startAnimation(downAnima);
			tvState.setText("����ˢ��");
			break;
		case RELEASE_REFRESH: // ��ǰ���ͷ�ˢ��״̬
			ivArrow.startAnimation(upAnima);
			tvState.setText("�ͷ�ˢ��");
			break;
		case REFRESHING: // ��ǰ������ˢ����
			ivArrow.setVisibility(View.INVISIBLE);
			ivArrow.clearAnimation(); // ������ϵĶ���
			mProgressBar.setVisibility(View.VISIBLE);
			tvState.setText("����ˢ����..");
			break;
		default:
			break;
		}
	}

	/**
	 * ������ʱ�����˷���
	 * 
	 * @param firstVisibleItem ��ǰ����ʱ, ��ʾ�������item������
	 */
	@Override
	public void onScroll(AbsListView view, int firstVisibleItem,
			int visibleItemCount, int totalItemCount) {
		mFirstVisiblePosition = firstVisibleItem;
	}

	/**
	 * SCROLL_STATE_IDLE ����ͣ��
	 * SCROLL_STATE_TOUCH_SCROLL ��ָ��סʱ����
	 * SCROLL_STATE_FLING ���ٵĻ�
	 */
	@Override
	public void onScrollStateChanged(AbsListView view, int scrollState) {
		if(!isEnableLoadingMore) {
			// ��ǰ�����ü��ظ��๦��, ֱ�ӷ���.
			return;
		}
		
		// �������ֹͣ, ���߿��ٻ������ײ�, ������صĲ���
		if(scrollState == SCROLL_STATE_IDLE 
				|| scrollState == SCROLL_STATE_FLING) {
			
			if(this.getLastVisiblePosition() == (getCount() -1)
					&& !isLoadingMore) {
				mFooterView.setPadding(0, 0, 0, 0);
				// ��Listview�������ײ�
				this.setSelection(getCount());
				isLoadingMore = true;
				
				// �����û��Ļص��¼�, ȥ���ظ��������.
				if(mOnRefreshListener != null) {
					mOnRefreshListener.onLoadingMore();
				}
			}
		}
	}
	
	public void setOnRefreshListener(OnRefreshListener listener) {
		this.mOnRefreshListener = listener;
	}
	
	/**
	 * ���û�ˢ��������ɺ�, ���ô˷���, ��ͷ�������ص�.
	 */
	public void onRefreshFinish() {
		if(currentState == REFRESHING) {
			currentState = PULL_DOWN;
			mProgressBar.setVisibility(View.INVISIBLE);
			ivArrow.setVisibility(View.VISIBLE);
			tvState.setText("����ˢ��");
			tvDate.setText("���ˢ��ʱ��: " + getCurrentTime());
			
			mPullDownHeaderView.setPadding(0, -mPullDownHeaderViewHeight, 0, 0);
		} else if(isLoadingMore) {
			// ��ǰ�Ǽ��ظ������, �����ѽŲ������ص�
			isLoadingMore = false;
			mFooterView.setPadding(0, -mFooterViewHeight, 0, 0);
		}
	}
	
	/**
	 * ��õ�ǰ��ʱ��: 2014-10-21 16:17:22
	 */
	private String getCurrentTime() {
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		return sdf.format(new Date());
	}
	
	/**
	 * �����Ƿ���������ˢ�¹���
	 * @param isEnablePullDownRefresh true������
	 */
	public void setEnablePullDownRefresh(boolean isEnablePullDownRefresh) {
		this.isEnablePullDownRefresh = isEnablePullDownRefresh;
	}
	
	/**
	 * �����Ƿ����ü��ظ��๦��
	 * @param isEnableLoadingMore true������
	 */
	public void setEnableLoadingMore(boolean isEnableLoadingMore) {
		this.isEnableLoadingMore = isEnableLoadingMore;
	}
}
