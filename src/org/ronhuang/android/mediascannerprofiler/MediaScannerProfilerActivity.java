package org.ronhuang.android.mediascannerprofiler;

import java.util.List;
import java.util.Map;

import android.app.ListActivity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.Window;
import android.widget.SimpleAdapter;
import android.widget.Toast;

public class MediaScannerProfilerActivity extends ListActivity {
	private static final String TAG = "MediaScannerProfilerActivity";

	private ResultDatabase db;
	private Handler handler;
	private List<Map<String,String>> results;

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
		requestWindowFeature(Window.FEATURE_PROGRESS);

		db = new ResultDatabase(this);
		handler = new Handler();
	}

	@Override
	public void onResume() {
		super.onResume();
		loadResults();

		IntentFilter intentFilter = new IntentFilter(Intent.ACTION_MEDIA_SCANNER_FINISHED);
		intentFilter.addDataScheme("file");
		registerReceiver(mMediaScannerReceiver, intentFilter);
	}

	@Override
	public void onPause() {
		super.onPause();
		unregisterReceiver(mMediaScannerReceiver);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.action, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.refresh:
			loadResults();
			return true;
		case R.id.scan:
			sendBroadcast(new Intent(Intent.ACTION_MEDIA_MOUNTED,
					Uri.parse("file://" + Environment.getExternalStorageDirectory())));
			return true;
		case R.id.clear:
			clearResults();
			return true;
		default:
			return super.onOptionsItemSelected(item);
		}
	}

	private final BroadcastReceiver mMediaScannerReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			Toast.makeText(context, R.string.incoming_new_results, Toast.LENGTH_LONG).show();
		}
	};

	private void loadResults() {
		final Context context = this;

		final Runnable finished = new Runnable() {
			@Override
			public void run() {
				final SimpleAdapter adapter = new SimpleAdapter(context, results,
						android.R.layout.simple_list_item_2,
						new String[] {"title", "summary"},
						new int[] {android.R.id.text1,
						android.R.id.text2});

				setListAdapter(adapter);
				setProgressBarIndeterminateVisibility(false);
				setProgressBarVisibility(false);
			}
		};

		final Runnable loading = new Runnable() {
			@Override
			public void run() {
				List<Map<String,String>> newResults = db.load();
				if (results == null || results.size() != newResults.size()) {
					results = newResults;
				}
				Log.d(TAG, "loaded " + results.size() + " results");
				handler.post(finished);
			}
		};

		setProgressBarIndeterminateVisibility(true);
		setProgressBarVisibility(true);
		new Thread(loading).start();
	}

	private void clearResults() {
		final Runnable finished = new Runnable() {
			@Override
			public void run() {
				setListAdapter(null);
				setProgressBarIndeterminateVisibility(false);
				setProgressBarVisibility(false);
			}
		};

		final Runnable clearing = new Runnable() {
			@Override
			public void run() {
				db.clear();
				if (results != null) {
					results.clear();
					results = null;
				}
				handler.post(finished);
			}
		};

		setProgressBarIndeterminateVisibility(true);
		setProgressBarVisibility(true);
		new Thread(clearing).start();
	}
}