package edu.buffalo.cse.cse486586.simpledynamo;

import android.os.Bundle;
import android.app.Activity;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.Menu;
import android.widget.TextView;

import android.content.ContentResolver;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.view.View;
//other
import java.io.File;

public class SimpleDynamoActivity extends Activity {
	static final String TAG = SimpleDynamoActivity.class.getSimpleName();
	private Uri mUri;
	private static final String KEY_FIELD = "key";
	private static final String VALUE_FIELD = "value";

	private Uri buildUri(String scheme, String authority) {
		Uri.Builder uriBuilder = new Uri.Builder();
		uriBuilder.authority(authority);
		uriBuilder.scheme(scheme);
		return uriBuilder.build();
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_simple_dynamo);

		//this code deletes the app data everytime the app is initialised
//		File cache = getCacheDir();
//		File appDir = new File(cache.getParent());
//		if (appDir.exists()) {
//			String[] children = appDir.list();
//			for (String s : children) {
//				if (!s.equals("lib")) {
//					deleteDir(new File(appDir, s));Log.i("TAG", "**************** File /data/data/APP_PACKAGE/" + s + " DELETED *******************");
//				}
//			}
//		}
		/////////file deletion done///////////

		TextView mTextView = (TextView) findViewById(R.id.textView1);
		mUri = buildUri("content", "edu.buffalo.cse.cse486586.simpledynamo.provider");
		mTextView.setMovementMethod(new ScrollingMovementMethod());
		// for Test button
		findViewById(R.id.button3).setOnClickListener(
				new OnTestClickListener(mTextView, getContentResolver()));
		// for LDump button
		findViewById(R.id.button1).setOnClickListener(new View.OnClickListener()
		{
			public void onClick(View v)
			{
				Log.d(TAG, "LDump Button Clicked");
				new Task1().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
			}
		});
		// for GDump button
		findViewById(R.id.button2).setOnClickListener(new View.OnClickListener()
		{
			public void onClick(View v)
			{
				Log.d(TAG, "GDump Button Clicked");
				new Task2().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
			}
		});
		// for Local Delete button
		findViewById(R.id.button4).setOnClickListener(new View.OnClickListener()
		{
			public void onClick(View v)
			{
				Log.d(TAG, "Delete Button Clicked");
				new Task3().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
			}
		});

		// for Global Delete button
		findViewById(R.id.button5).setOnClickListener(new View.OnClickListener()
		{
			public void onClick(View v)
			{
				Log.d(TAG, "Global Delete Button Clicked");
				new Task4().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
			}
		});
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.simple_dynamo, menu);
		return true;
	}
	
	public void onStop() {
        super.onStop();
	    Log.v("Test", "onStop()");
	}

	// referenced from https://stackoverflow.com/questions/10934304/clear-android-application-user-data
	//below method deletes the app data everytime the app is initialised
//	public static boolean deleteDir(File dir) {
//		if (dir != null && dir.isDirectory()) {
//			String[] children = dir.list();
//			for (int i = 0; i < children.length; i++) {
//				boolean success = deleteDir(new File(dir, children[i]));
//				if (!success) {
//					return false;
//				}
//			}
//		}
//
//		return dir.delete();
//	}

	private class Task1 extends AsyncTask<Void, String, Void> {

		@Override
		protected Void doInBackground(Void... params) {
			testQuery(getContentResolver());
			publishProgress("Fetched");
			return null;
		}

		protected void onProgressUpdate(String...result) {
//            mTextView.append(result.toString());
			return;
		}


		private void testQuery(ContentResolver cr) {
			try{
				Cursor resultCursor = cr.query(mUri, null,
						"@", null, null);
				while (resultCursor.moveToNext()) {
					int keyIndex = resultCursor.getColumnIndex(KEY_FIELD);
					int valueIndex = resultCursor.getColumnIndex(VALUE_FIELD);
					String returnKey = resultCursor.getString(keyIndex);
					String returnValue = resultCursor.getString(valueIndex);
//                    mTextView.append(returnKey+" "+returnValue);
					Log.d(TAG, returnKey+" "+returnValue);
				}
				resultCursor.close();
			}
			catch (Exception e)
			{
				Log.e(TAG, "Some error occurred");
			}

		}
	}
	private class Task2 extends AsyncTask<Void, String, Void> {

		@Override
		protected Void doInBackground(Void... params) {
			testQuery(getContentResolver());
			publishProgress("Fetched");
			return null;
		}

		protected void onProgressUpdate(String...result) {
//            mTextView.append(result.toString());
			return;
		}


		private void testQuery(ContentResolver cr) {
			try{
				Cursor resultCursor = cr.query(mUri, null,
						"*", null, null);
				while (resultCursor.moveToNext()) {
					int keyIndex = resultCursor.getColumnIndex(KEY_FIELD);
					int valueIndex = resultCursor.getColumnIndex(VALUE_FIELD);
					String returnKey = resultCursor.getString(keyIndex);
					String returnValue = resultCursor.getString(valueIndex);
//                    mTextView.append(returnKey+" "+returnValue);
					Log.d(TAG, "Returned values "+returnKey+" "+returnValue);
				}
				resultCursor.close();
			}
			catch (Exception e)
			{
				Log.e(TAG, "Some error occurred");
			}

		}
	}
	private class Task3 extends AsyncTask<Void, String, Void> {

		@Override
		protected Void doInBackground(Void... params) {
			testQuery(getContentResolver());
			publishProgress("Deleted");
			return null;
		}

		protected void onProgressUpdate(String...result) {
//            mTextView.append(result.toString());
			return;
		}


		private void testQuery(ContentResolver cr) {
			try{
				int resultCursor = cr.delete(mUri,
						"@", null);
			}
			catch (Exception e)
			{
				Log.e(TAG, "Some error occurred");
			}

		}
	}

	private class Task4 extends AsyncTask<Void, String, Void> {

		@Override
		protected Void doInBackground(Void... params) {
			testQuery(getContentResolver());
			publishProgress("Deleted");
			return null;
		}

		protected void onProgressUpdate(String...result) {
//            mTextView.append(result.toString());
			return;
		}


		private void testQuery(ContentResolver cr) {
			try{
				int resultCursor = cr.delete(mUri,
						"*", null);
			}
			catch (Exception e)
			{
				Log.e(TAG, "Some error occurred");
			}

		}
	}

}
