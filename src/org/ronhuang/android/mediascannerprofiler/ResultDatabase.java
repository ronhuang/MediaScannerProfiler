package org.ronhuang.android.mediascannerprofiler;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.net.Uri;
import android.util.Log;

public class ResultDatabase {
	public static final int TYPE_STARTED = 0;
	public static final int TYPE_FINISHED = 1;

	private static final String TAG = "ResultDatabase";

	private static final String DATABASE_NAME = "result";
	private static final int DATABASE_VERSION = 1;

	private static final String RESULT_TABLE_NAME = "result";

	private static final String KEY_TIMESTAMP = "timestamp";
	private static final String KEY_TYPE = "type";
	private static final String KEY_URI = "uri";
	private static final String KEY_COUNT = "count";

	private static final String[] ALL_COLUMNS = {KEY_TIMESTAMP, KEY_TYPE, KEY_URI, KEY_COUNT};

	private Context mContext;

	ResultDatabase(Context context) {
		mContext = context;
	}

	public long insert(long timestamp, Uri uri, int type, int count) {
		Log.d(TAG, "insert(" + timestamp + ", " + uri.toString() + ", " + type + ", " + count + ")");

		ResultOpenHelper helper = new ResultOpenHelper(mContext);
		SQLiteDatabase db = helper.getWritableDatabase();
		ContentValues values = new ContentValues();
		values.put(KEY_TIMESTAMP, timestamp);
		values.put(KEY_URI, uri.toString());
		values.put(KEY_TYPE, type);
		values.put(KEY_COUNT, count);
		long id = db.insert(RESULT_TABLE_NAME, null, values);
		helper.close();
		return id;
	}

	public List<Map<String,String>> load() {
		List<Map<String,String>> items = new ArrayList<Map<String, String>>();

		ResultOpenHelper helper = new ResultOpenHelper(mContext);
		SQLiteDatabase db = helper.getReadableDatabase();
		Cursor cursor = db.query(RESULT_TABLE_NAME, ALL_COLUMNS,
				null, null,	null, null, KEY_TIMESTAMP + " ASC");

		String startedUri = null;
		long startedTimestamp = 0;

		cursor.moveToFirst();
		while (!cursor.isAfterLast()) {
			long timestamp = cursor.getLong(0);
			int type = cursor.getInt(1);
			String uri = cursor.getString(2);
			int count = cursor.getInt(3);

			switch (type) {
			case TYPE_STARTED:
				startedUri = uri;
				startedTimestamp = timestamp;
				break;
			case TYPE_FINISHED:
				if (startedUri == null || startedTimestamp == 0) {
					Log.e(TAG, "no started intent");
					break;
				}
				if (!startedUri.equals(uri)) {
					Log.e(TAG, "uri not matched. expected: " + startedUri + ", got: " + uri);
					break;
				}
				if (timestamp < startedTimestamp) {
					Log.e(TAG, "timestamp not matched. expected: " + startedTimestamp + " <= " + timestamp);
					break;					
				}

				Map<String,String> map = new HashMap<String,String>();
				map.put("title", Uri.parse(uri).getPath() + " (" + count + " items)");
				long duration = timestamp - startedTimestamp;
				String summary = "Total " + duration + " ms";
				if (count > 0) {
					summary += ", average " + duration/count + " ms/item";
				}
				map.put("summary", summary);
				items.add(0, map);

				startedUri = null;
				startedTimestamp = 0;
				break;
			}

			cursor.moveToNext();
		}
		cursor.close();

		db.close();		
		return items;
	}

	public boolean clear() {
		ResultOpenHelper helper = new ResultOpenHelper(mContext);
		SQLiteDatabase db = helper.getWritableDatabase();
		db.delete(RESULT_TABLE_NAME, null, null);
		db.close();
		return true;
	}

	public static class ResultOpenHelper extends SQLiteOpenHelper {
		private static final String RESULT_TABLE_CREATE =
				"CREATE TABLE " + RESULT_TABLE_NAME + " (" +
						KEY_TIMESTAMP + " INTEGER, " +
						KEY_URI + " TEXT, " +
						KEY_COUNT + " INTEGER, " +
						KEY_TYPE + " INTEGER);";

		ResultOpenHelper(Context context) {
			super(context, DATABASE_NAME, null, DATABASE_VERSION);
		}

		@Override
		public void onCreate(SQLiteDatabase db) {
			db.execSQL(RESULT_TABLE_CREATE);
		}

		@Override
		public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
			Log.w(TAG, "Upgrading database from version " + oldVersion + " to "
					+ newVersion + ", which will destroy all old data");
			db.execSQL("DROP TABLE IF EXISTS " + RESULT_TABLE_NAME);
			onCreate(db);
		}
	}
}
