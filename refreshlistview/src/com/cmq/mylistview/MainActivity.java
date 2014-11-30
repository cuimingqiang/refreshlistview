package com.cmq.mylistview;

import com.cmq.mylistview.MyListView.OnRefreshListener;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;
import android.widget.Toast;


public class MainActivity extends Activity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		MyListView listview = (MyListView) findViewById(R.id.list);
		listview.setAdapter(new CustomAdapter());
		listview.setOnRefreshListener(new OnRefreshListener() {
			
			@Override
			public void onRefresh() {
				try {
					Thread.sleep(2000);
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
			
			@Override
			public void onLoadMore() {
				try {
					Thread.sleep(2000);
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				
			}
			
			@Override
			public void onComplete(Boolean isRefresh) {
				String str = "执行了刷新操作";
				if(!isRefresh){
					str = "执行了加载更多操作";
				}
				Toast.makeText(MainActivity.this, str, Toast.LENGTH_SHORT).show();
			}
		});
	}
	private class CustomAdapter extends BaseAdapter{

		@Override
		public int getCount() {
			// TODO Auto-generated method stub
			return 30;
		}

		@Override
		public Object getItem(int position) {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public long getItemId(int position) {
			// TODO Auto-generated method stub
			return 0;
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			TextView view = null;
			if(convertView instanceof TextView){
				view = (TextView) convertView;
			}
			else{
				view = new TextView(MainActivity.this);		
			}
			view.setPadding(20, 20, 20, 20);
			view.setText("ListView:"+position);
			return view;
		}
		
	}
}
