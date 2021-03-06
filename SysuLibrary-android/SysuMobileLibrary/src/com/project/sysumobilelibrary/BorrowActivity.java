package com.project.sysumobilelibrary;

import java.util.ArrayList;
import java.util.HashMap;











import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.android.volley.VolleyError;
import com.android.volley.Response.ErrorListener;
import com.android.volley.Response.Listener;
import com.project.sysumobilelibrary.adapter.MyBorrowListViewAdapter;
import com.project.sysumobilelibrary.entity.BorrowBook;
import com.project.sysumobilelibrary.global.MyApplication;
import com.project.sysumobilelibrary.global.MyURL;

import dmax.dialog.SpotsDialog;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.ListActivity;
import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

public class BorrowActivity extends Activity {
	private static final String TAG = "BorrowActivity";
	private static Context context;
	
	private ImageView ivBack;
	private TextView tvTitle;
	private Button btAllReBor;
	private static TextView tvEmpty;
	private static ListView listView;
	private TextView tvFlush;

	private static ArrayList<BorrowBook> borrowBooks = new ArrayList<BorrowBook>();
	private static MyBorrowListViewAdapter adapter;
	private static AlertDialog loading;
	
	private final static int CODE_TOAST_MSG = 0;

	static Handler handler = new Handler() {
		public void handleMessage(android.os.Message msg) {
			switch (msg.what) {
			case CODE_TOAST_MSG:
				Toast.makeText(context, msg.obj.toString(),
						Toast.LENGTH_SHORT).show();
				break;
			}
		};
	};

	public static void myToast(String msg) {
		handler.obtainMessage(CODE_TOAST_MSG, msg).sendToTarget();
	}
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_NO_TITLE);// 去掉标题栏
		setContentView(R.layout.activity_borrow);

		initView();
		initListView();
	}

	private void initView() {
		loading = new SpotsDialog(this);
		context = BorrowActivity.this;
		ivBack = (ImageView) findViewById(R.id.iv_back);
		tvTitle = (TextView) findViewById(R.id.tv_title);
		tvTitle.setText("我的外借");
		tvEmpty = (TextView) findViewById(R.id.empty);
		tvFlush = (TextView)findViewById(R.id.tv_flush);
		tvFlush.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View arg0) {
				updateBorrowBooks();
			}
		});
		tvEmpty.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View arg0) {
				updateBorrowBooks();
			}
		});
		btAllReBor = (Button)findViewById(R.id.bt_all_re_bor);
		ivBack.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View arg0) {
				BorrowActivity.this.finish();
			}
		});
		btAllReBor.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View arg0) {
				re_borrow_all();
			}
			private void re_borrow_all() {
				Listener<String> listener = new Listener<String>() {
					@Override
					public void onResponse(String response) {
						try {
							JSONObject jsonObject = new JSONObject(response);
							int code = jsonObject.getInt("code");
							if (code == 1) {
								Log.e(TAG, jsonObject.getString("msg"));
								Toast.makeText(context, jsonObject.getString("msg"), Toast.LENGTH_SHORT)
										.show();
								updateBorrowBooks();
							} else {
								Log.e(TAG, "续借失败");
								Toast.makeText(context, "续借失败",
										Toast.LENGTH_SHORT).show();
							}
						} catch (JSONException e) {
							e.printStackTrace();
							Log.e(TAG, "get book info json new error");
							Toast.makeText(context, "续借失败,有bug",
									Toast.LENGTH_SHORT).show();
						}
					}
				};
				ErrorListener errorListener = new ErrorListener() {

					@Override
					public void onErrorResponse(VolleyError error) {
						Log.e(TAG,
								"get book order info error; "
										+ error.toString());
						Toast.makeText(context, "是不是网络出问题了呢？",
								Toast.LENGTH_SHORT).show();
					}
				};
				HashMap<String, String> map = new HashMap<String, String>();
				map.put("token", MyApplication.getUser().getToken());
				MyApplication.getMyVolley().addPostStringRequest(
						MyURL.BOR_RENEW_ALL_URL, listener, errorListener, map,
						TAG);
			}
		});
	}

	private void initListView() {
		listView = (ListView)findViewById(R.id.listview);
		listView.setEmptyView(tvEmpty);
		if (adapter == null){
			adapter = new MyBorrowListViewAdapter(BorrowActivity.this, borrowBooks);
		}
		listView.setAdapter(adapter);
		
		if (borrowBooks.size() == 0){
			updateBorrowBooks();
		}else if (borrowBooks.size() != Integer.parseInt(MyApplication.getUser().getBorrow_num())){
			updateBorrowBooks();
		}
	}
	
	public static void updateBorrowBooks() {
		loading.show();
		Listener<String> listener = new Listener<String>() {
			@Override
			public void onResponse(String response) {
				try {
					JSONObject jsonObject = new JSONObject(response);
					Log.e(TAG, jsonObject.getString("msg"));
					int code = jsonObject.getInt("code");
					//Log.e(TAG, response);
					if (code == 1){
						updateBorrowBooksFromJSONObject(jsonObject);
						Log.e(TAG, "获取外借成功"); 
						tvEmpty.setText("你没有正在借的书呐...");
					}else{
						Log.e(TAG, "获取外借失败");
						tvEmpty.setText("点击重新加载");
					}
				} catch (JSONException e) {
					e.printStackTrace();
					Log.e(TAG, "get borrow books json error");
					myToast("是不是网络出问题了呢？");
					tvEmpty.setText("点击重新加载");
				}
				loading.dismiss();
			}
		};
		ErrorListener errorListener = new ErrorListener() {

			@Override
			public void onErrorResponse(VolleyError error) {
				Log.e(TAG, "get borrow books error; "+error.toString());
				myToast("是不是网络出问题了呢？");
				tvEmpty.setText("点击重新加载");
				loading.dismiss();
			}
		};
		HashMap<String, String> map = new HashMap<String, String>();
		map.put("token", MyApplication.getUser().getToken());
		MyApplication.getMyVolley().addPostStringRequest(MyURL.GET_MY_BOR_LOAN_URL, listener, errorListener, map, TAG);
	}
 
	protected static void updateBorrowBooksFromJSONObject(JSONObject jsonObject) throws JSONException {
		borrowBooks.clear();
		JSONArray jsonArray= jsonObject.getJSONArray("loans");
		for (int i=0; i<jsonArray.length(); ++i){
			BorrowBook book = new BorrowBook();
			book.getFromJSONObject(jsonArray.getJSONObject(i));
			borrowBooks.add(book);
		}
		if (adapter!=null){
			adapter.notifyDataSetChanged();
		}
		MyApplication.getUser().setBorrow_num(jsonArray.length()+"");
	}
}
