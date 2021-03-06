package com.project.sysumobilelibrary.adapter;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.regex.Pattern;

import org.json.JSONException;
import org.json.JSONObject;

import com.android.volley.VolleyError;
import com.android.volley.Response.ErrorListener;
import com.android.volley.Response.Listener;
import com.jauker.widget.BadgeView;
import com.project.sysumobilelibrary.BookDetailActivity;
import com.project.sysumobilelibrary.BorrowActivity;
import com.project.sysumobilelibrary.R;
import com.project.sysumobilelibrary.UserFragment;
import com.project.sysumobilelibrary.entity.BorrowBook;
import com.project.sysumobilelibrary.global.MyApplication;
import com.project.sysumobilelibrary.global.MyURL;
import com.project.sysumobilelibrary.utils.MyAlgorithm;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

public class MyBorrowListViewAdapter extends BaseAdapter {
	private static final String TAG = "MyBorrowListViewAdapter";
	private Context context;
	private ArrayList<BorrowBook> borrowBooks;

	public MyBorrowListViewAdapter(Context context,
			ArrayList<BorrowBook> borrowBooks) {
		this.context = context;
		this.borrowBooks = borrowBooks;
	}

	@Override
	public int getCount() {
		return borrowBooks.size();
	}

	@Override
	public Object getItem(int i) {
		return borrowBooks.get(i);
	}

	@Override
	public long getItemId(int arg0) {
		return arg0;
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		ViewHolder viewHolder = null;
		if (convertView == null) {
			convertView = LayoutInflater.from(context).inflate(
					R.layout.borrow_list_item, null);
			viewHolder = new ViewHolder();
			initViewHolder(viewHolder, convertView);
			convertView.setTag(viewHolder);
		} else {
			viewHolder = (ViewHolder) convertView.getTag();
		}

		final BorrowBook borrowBook = borrowBooks.get(position);
		viewHolder.tvIndex.setText((position + 1) + "");
		viewHolder.tvDueDate.setText(borrowBook.getDue_date());

		String due_date_str = Pattern.compile("[^0-9]")
				.matcher(borrowBook.getDue_date()).replaceAll("");
		// Log.e("due_data", due_date_str);
		String pat = "yyyyMMdd";
		SimpleDateFormat sdf = new SimpleDateFormat(pat);
		try {
			Date due_date = sdf.parse(due_date_str);
			Date now_date = new Date();// 获取当前时间
			String now_date_str = sdf.format(now_date);
			// Log.e("now_date", now_date_str);
			int due = Integer.parseInt(due_date_str);
			int now = Integer.parseInt(now_date_str);
			int diff = MyAlgorithm.getGapCount(now_date, due_date);
			if (diff > 10) {
				viewHolder.badgeView.setVisibility(View.GONE);
			} else if (diff > 3) {
				viewHolder.badgeView.setVisibility(View.VISIBLE);
				viewHolder.badgeView.setTargetView(viewHolder.tvDueDate);
				viewHolder.badgeView.setBadgeGravity(Gravity.TOP
						| Gravity.RIGHT);
				viewHolder.badgeView.setTypeface(Typeface.create(
						Typeface.SANS_SERIF, Typeface.ITALIC));
				viewHolder.badgeView.setShadowLayer(2, -1, -1, Color.GREEN);
				viewHolder.badgeView.setBadgeCount(diff);
			} else {
				viewHolder.badgeView.setVisibility(View.VISIBLE);
				viewHolder.badgeView.setTargetView(viewHolder.tvDueDate);
				viewHolder.badgeView.setTypeface(Typeface.create(
						Typeface.SANS_SERIF, Typeface.ITALIC));
				viewHolder.badgeView.setShadowLayer(2, -1, -1, Color.RED);
				viewHolder.badgeView.setBadgeGravity(Gravity.TOP
						| Gravity.RIGHT);
				viewHolder.badgeView.setBadgeCount(diff);
			}
			// Log.e("diff", diff+"");
		} catch (ParseException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}

		viewHolder.tvName.setText(borrowBook.getName());
		viewHolder.tvReBor.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View arg0) {
				re_borrow_part();

			}

			private void re_borrow_part() {
				Listener<String> listener = new Listener<String>() {
					@Override
					public void onResponse(String response) {
						try {
							JSONObject jsonObject = new JSONObject(response);
							int code = jsonObject.getInt("code");
							if (code == 1) {
								// Log.e(TAG, response);
								String msg = jsonObject.getJSONArray("loans")
										.getJSONObject(0)
										.getString("fail_reason");
								Log.e(TAG, msg);
								msg = msg.trim().isEmpty() ? "续借失败" : msg;
								Toast.makeText(context, msg, Toast.LENGTH_SHORT)
										.show();
								BorrowActivity.updateBorrowBooks();
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
				map.put("select_nos", borrowBook.getSelect_no());
				map.put("cnt", borrowBook.getNo());
				MyApplication.getMyVolley().addPostStringRequest(
						MyURL.BOR_RENEW_PART_URL, listener, errorListener, map,
						TAG);
			}
		});
		viewHolder.tvDetail.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View arg0) {
				// getDetail();
				Intent intent = new Intent(context, BookDetailActivity.class);
				intent.putExtra("doc_number", borrowBook.getDoc_number());
				context.startActivity(intent);
			}

		});
		// Log.e(TAG, borrowBooks.size()+"");
		return convertView;
	}

	private void initViewHolder(ViewHolder viewHolder, View convertView) {
		viewHolder.tvIndex = (TextView) convertView.findViewById(R.id.tv_index);
		viewHolder.tvDueDate = (TextView) convertView
				.findViewById(R.id.tv_due_date);
		viewHolder.tvName = (TextView) convertView.findViewById(R.id.tv_name);
		viewHolder.tvReBor = (TextView) convertView
				.findViewById(R.id.tv_re_bor);
		viewHolder.tvDetail = (TextView) convertView
				.findViewById(R.id.tv_detail);
		viewHolder.badgeView = new BadgeView(context);

	}

	class ViewHolder {
		public TextView tvIndex;
		public TextView tvDueDate;
		public TextView tvName;
		public TextView tvReBor;
		public TextView tvDetail;
		public BadgeView badgeView;
	}

}
