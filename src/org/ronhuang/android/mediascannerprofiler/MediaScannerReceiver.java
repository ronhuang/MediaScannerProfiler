package org.ronhuang.android.mediascannerprofiler;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.provider.MediaStore;
import android.util.Log;

public class MediaScannerReceiver extends BroadcastReceiver {
	private static final String TAG = "MediaScannerReceiver";

	private ResultDatabase db = null;

	@Override
	public void onReceive(final Context context, Intent intent) {
		final Uri uri = intent.getData();
		final long timestamp = System.currentTimeMillis();
		final int type = intent.getAction() == Intent.ACTION_MEDIA_SCANNER_STARTED
				? ResultDatabase.TYPE_STARTED : ResultDatabase.TYPE_FINISHED;

		if (db == null) {
			db = new ResultDatabase(context);
		}

		new Thread(new Runnable() {
			public void run() {
				int count = 0;
				if (type == ResultDatabase.TYPE_FINISHED) {
					Cursor cursor = null;

					cursor = context.getContentResolver().query(
							MediaStore.Audio.Media.INTERNAL_CONTENT_URI,
							new String[] {MediaStore.Audio.Media._ID},
							MediaStore.Audio.Media.DATA + " LIKE ?",
							new String[] {uri.getPath() + "%"},
							null);
					if (cursor != null) {
						count += cursor.getCount();
						cursor.close();
					}

					cursor = context.getContentResolver().query(
							MediaStore.Video.Media.INTERNAL_CONTENT_URI,
							new String[] {MediaStore.Video.Media._ID},
							MediaStore.Video.Media.DATA + " LIKE ?",
							new String[] {uri.getPath() + "%"},
							null);
					if (cursor != null) {
						count += cursor.getCount();
						cursor.close();
					}

					cursor = context.getContentResolver().query(
							MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
							new String[] {MediaStore.Audio.Media._ID},
							MediaStore.Audio.Media.DATA + " LIKE ?",
							new String[] {uri.getPath() + "%"},
							null);
					if (cursor != null) {
						count += cursor.getCount();
						cursor.close();
					}

					cursor = context.getContentResolver().query(
							MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
							new String[] {MediaStore.Video.Media._ID},
							MediaStore.Video.Media.DATA + " LIKE ?",
							new String[] {uri.getPath() + "%"},
							null);
					if (cursor != null) {
						count += cursor.getCount();
						cursor.close();
					}
				}
				long id = db.insert(timestamp, uri, type, count);
				if (id < 0) {
					Log.e(TAG, "failed to insert result");
				}
			}
		}).start();
	}
}
