package com.gongli.onuprefreshlistview_gongliwin.view;

public interface OnRefreshListener {

	/**
	 * 当是下拉刷新时, 回调此方法, 进行刷新数据的操作
	 */
	public void OnPullDownRefresh();

	/**
	 * 当前是加载更多的操作.
	 */
	public void onLoadingMore();
}
