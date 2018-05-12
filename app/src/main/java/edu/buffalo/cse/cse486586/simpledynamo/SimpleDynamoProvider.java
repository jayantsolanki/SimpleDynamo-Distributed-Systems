package edu.buffalo.cse.cse486586.simpledynamo;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Formatter;
import java.util.concurrent.ExecutionException;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.database.MergeCursor;
import android.net.Uri;

//SQLite
import android.database.sqlite.SQLiteDatabase;

//others
import android.os.AsyncTask;
import android.os.Handler;
import android.telephony.TelephonyManager;
import android.util.Log;



public class SimpleDynamoProvider extends ContentProvider {
	private sQLiteDbHelper objDbHelper;
	public static final String AUTHORITY = "edu.buffalo.cse.cse486586.simpledynamo.provider";
	public static final String table = "dynamo";
	static final String TAG = SimpleDynamoProvider.class.getSimpleName();
	private String portStr;
	private String nodeId;
	private String []nodeHashMap = {"177ccecaec32c54b82d5aaafc18a2dadb753e3b1", "208f7f72b198dadd244e61801abe1ec3a4857bc9","33d6357cfaaf0f72991b0ecd8c56da066613c089",
            "abf0fd8db03e5ecb199a9b82929e9db79b909643","c25ddd596aa7c81fa12378fa725f706d54325d12"};
	private String []portMap = {"5562","5556","5554","5558","5560"};

//	private MatrixCursor matrixCursor = new MatrixCursor(new String[] { "key", "value" }); // stackoverflow.com https://stackoverflow.com/questions/9917935/adding-rows-into-cursor-manually
    private MatrixCursor matrixCursor; // stackoverflow.com https://stackoverflow.com/questions/9917935/adding-rows-into-cursor-manually
    private Uri mUri = buildUri("content", AUTHORITY);
	private Handler handler = new Handler();
	private MergeCursor mergeCursor ;
	private Cursor results;
	private String avdId;
	private int FLAG = 1;
	private int RESTOREFLAG = 1;
    private boolean deleteFlag = false;
	private int nodeIndex = -1;
	static final int SERVER_PORT = 10000;
	//build URI
	private Uri buildUri(String scheme, String authority) {
		Uri.Builder uriBuilder = new Uri.Builder();
		uriBuilder.authority(authority);
		uriBuilder.scheme(scheme);
		return uriBuilder.build();
	}
//
//    public void broadCastUpdatedNodes() {
//        new ClientTask().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR,"nodeUpdate");
//    }

    //helper function for finding the index of the particular deviceid
    public int findIndex(String node) {
        for (int i=0; i<5 ; i++)
        {
            if(node.compareToIgnoreCase(portMap[i])==0)
                return i;
        }
        return -1;
    }

    //helper function for finding the index of the particular deviceid
    public void viewCursor(Cursor res) {
        Log.e("Iterating Viwing Cursor results","Iterating");
        while (res.moveToNext()) {
            int keyIndex = res.getColumnIndex("key");
            int valueIndex = res.getColumnIndex("value");
            String returnKey = res.getString(keyIndex);
            String returnValue = res.getString(valueIndex);
//                    mTextView.append(returnKey+" "+returnValue);
            Log.v("Viwing Cursor results","Key: "+returnKey+" Value: "+returnValue);
        }
        res.close();
    }

    //helper function for checking if the device is head, successor or 2nd successor
    public Boolean checkPosition(int index) {
        if(index==nodeIndex)
            return true;
        else{
            int div = nodeIndex-index;
            int temp = 0;
            Log.d("Diff", Integer.toString(div));
            if(div<=2 && index<=nodeIndex)
                return true;
            temp = index+1;//first successor
            if(temp==5)
                temp=0;
            if(nodeIndex==temp)
                return true;
            temp = index+2;
            if(temp==5)
                temp=0;
            if(temp==6)
                temp=1;
            if(nodeIndex==temp)
                return true;
//            else if(index==4)
//            {
//                if(nodeIndex==0)
//                    return true;
//                if(nodeIndex==1)
//                    return true;
//            }
//            else if(index==3)
//            {
//                if(nodeIndex==4)
//                    return true;
//                if(nodeIndex==0)
//                    return true;
//            }
        }
        return false;
    }


    //helper function for finding the index where insertion of query has to take place
    public int findHashIndex(String hashkey) {
       for( int i =0;i<5;i++)
        {
            try {
            if(i>0){

                    if((hashkey.compareToIgnoreCase(nodeHashMap[i-1])> 0 || hashkey.compareToIgnoreCase(nodeHashMap[i])<0) && nodeHashMap[i].compareToIgnoreCase(nodeHashMap[i-1])<0 ){//its the current node, store there
                        return (i);
                    }

                    else if(hashkey.compareToIgnoreCase(nodeHashMap[i-1])> 0 && hashkey.compareToIgnoreCase(nodeHashMap[i])<0 ){//its the current node, store there

                        return (i);
                    }
                    else{

//                        Log.d(TAG,"Forward Data if");
                    }

                }
                else{//fifth node is the predessessor of the first node

                    if((hashkey.compareToIgnoreCase(nodeHashMap[4])> 0 || hashkey.compareToIgnoreCase(nodeHashMap[i])<0)){//its the current node, store there

                        return (i);
                    }
                    else{

//                        Log.d(TAG,"Forward Data else");
                    }
                }

            }
            catch(Exception e){
                Log.e("Exception", "NoSuchAlgorithmException");
            }

        }
        return (-1);
    }



	@Override
	public String getType(Uri uri) {
		// TODO Auto-generated method stub
		return null;
	}




    @Override
    public boolean onCreate() {
        // If you need to perform any one-time initialization task, please do it here.
        objDbHelper = new sQLiteDbHelper(getContext());
        Log.v("intialise", "Database initialisation");
        TelephonyManager tel = (TelephonyManager) getContext().getSystemService(Context.TELEPHONY_SERVICE);
        avdId = tel.getLine1Number().substring(tel.getLine1Number().length() - 4);
        portStr = String.valueOf((Integer.parseInt(avdId) * 2));
        try {
            nodeId = genHash(avdId);
        }
        catch(NoSuchAlgorithmException e){
            Log.e("Exception", "NoSuchAlgorithmException");
        }
        Log.d(TAG, avdId);
        Log.d(TAG, portStr);
        Log.d(TAG, nodeId);
        nodeIndex = findIndex(avdId);
        Log.d(TAG, Integer.toString(nodeIndex));
        new ClientRestoreTask().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR,"restore");
        // starting server listener
        try {
            /*
             * Create a server socket as well as a thread (AsyncTask) that listens on the server
             * port.
             *
             * AsyncTask is a simplified thread construct that Android provides. Please make sure
             * you know how it works by reading
             * http://developer.android.com/reference/android/os/AsyncTask.html
             */
            ServerSocket serverSocket = new ServerSocket(SERVER_PORT);
            new ServerTask().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, serverSocket);
        } catch (IOException e) {
            /*
             * Log is a good way to debug your code. LogCat prints out all the messages that
             * Log class writes.
             *
             * Please read http://developer.android.com/tools/debugging/debugging-projects.html
             * and http://developer.android.com/tools/debugging/debugging-log.html
             * for more information on debugging.
             */
            Log.e(TAG, "Can't create a ServerSocket");
            return false;
        }
//        long startTime = System.currentTimeMillis(); //fetch starting time
//        while(false||(System.currentTimeMillis()-startTime)<1000);


        return true;
    }

    @Override

    public int delete(Uri uri, String selection, String[] selectionArgs) {

        SQLiteDatabase db = objDbHelper.getReadableDatabase();
//        FLAG = 0;
        int rowsDeleted = 0;
        //no particular order of  results, or columns defined
        String Selection;
        if(selection == null) {
            Log.d("delete all", "Deleting data");
            db.execSQL("delete from "+table);
            return 0;
        }
        else if(selection.equals("*")){
            Log.d("deleteselectglobal", "I am *, deleting");
//            selectionArgs = new String[]{selection};
            db.execSQL("delete from "+table);
            new ClientTask().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR,"globalDeleteData", avdId);
            long startTime = System.currentTimeMillis(); //fetch starting time
            while(false||(System.currentTimeMillis()-startTime)<2000);
            return 0;


        }
        else if(selection.equals("@")){
            Log.d("queryselect", "I am @, deleting");
            db.execSQL("delete from "+table);
            return 0;
        }
        else{//deletion for particular key


            selectionArgs = new String[]{selection};//had to to this coz selection alone was not working
            Selection = "key" + " = ?";
            int getIndex = -1;
            try {
                getIndex = findHashIndex(genHash(selection));
                Log.v("Check position", "For key: "+selection+" Index found is "+Integer.toString(getIndex)+" and current Node index is "+Integer.toString(nodeIndex));
            }
            catch(Exception e){
                Log.e("Exception", "NoSuchAlgorithmException");
            }
            if(deleteFlag){
                rowsDeleted = db.delete(table, Selection, selectionArgs);
                String rowCount = Integer.toString(rowsDeleted);
                Log.d("row deleted in ", "deviceid: "+avdId+" rowsdeleted are "+rowCount);
                return 0;
            }
            rowsDeleted = db.delete(table, Selection, selectionArgs);
            String rowCount = Integer.toString(rowsDeleted);
            Log.d("row deleted in ", "deviceid: "+avdId+" rowsdeleted are "+rowCount);
            if(getIndex==nodeIndex){//data is in same device


                //forward delete request to two successors
                long startTime = System.currentTimeMillis(); //fetch starting time
                if(getIndex==4)
                {
                    new ClientTask().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR,"localDelete", avdId, portMap[0], selection);
                    new ClientTask().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR,"localDelete", avdId, portMap[1], selection);

                }
                else if(getIndex==3)
                {
                    new ClientTask().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR,"localDelete", avdId, portMap[getIndex+1], selection);
                    new ClientTask().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR,"localDelete", avdId, portMap[0], selection);

                }
                else{
                    new ClientTask().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR,"localDelete", avdId, portMap[getIndex+1], selection);
                    new ClientTask().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR,"localDelete", avdId, portMap[getIndex+2], selection);
                }

                while(false||(System.currentTimeMillis()-startTime)<500);



            }
            else{
                Log.d(TAG,"localDelete Data to corresponding nodes");
                long startTime = System.currentTimeMillis(); //fetch starting time
                if(getIndex==4)
                {
                    new ClientTask().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR,"localDelete", avdId, portMap[getIndex], selection);
                    new ClientTask().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR,"localDelete", avdId, portMap[0], selection);
                    new ClientTask().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR,"localDelete", avdId, portMap[1], selection);

                }
                else if(getIndex==3)
                {
                    new ClientTask().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR,"localDelete", avdId, portMap[getIndex], selection);
                    new ClientTask().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR,"localDelete", avdId, portMap[getIndex+1], selection);
                    new ClientTask().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR,"localDelete", avdId, portMap[0], selection);

                }
                else
                {
                    new ClientTask().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR,"localDelete", avdId, portMap[getIndex], selection);
                    new ClientTask().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR,"localDelete", avdId, portMap[getIndex+1],selection);
                    new ClientTask().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR,"localDelete", avdId, portMap[getIndex+2], selection);
                }

                while(false||(System.currentTimeMillis()-startTime)<500);

            }

            return 0;
        }

    }

	@Override
	public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs,
						String sortOrder) {
//	    while(RESTOREFLAG==0);//wait till restoration is complete
        SQLiteDatabase db = objDbHelper.getReadableDatabase();
//        if(mode==0)
//		    matrixCursor = new MatrixCursor(new String[] { "key", "value" });
////		FLAG = 0;
//        if(FLAG==0)
//            Log.d("queryselect on hold", selection);
////        while(FLAG==0);//hold incoming queries
//        if(FLAG==1)
//            Log.d("queryselect released", selection);
//        FLAG=0;
        int getIndex = -1;
		//no particular order of  results, or columns defined
		String Selection;
		if(selection == null) {
			Selection = null;
			selectionArgs = null;
			Log.d("query", "Selection is null, fetching all data");
			results = db.query(table, projection, Selection, selectionArgs, null, null, null);
			return results;
		}
		else if(selection.equals("*")){
            FLAG=0;
//            matrixCursor = new MatrixCursor(new String[] { "key", "value" });
			Log.d("queryselect", "Query, I am *");
//            selectionArgs = new String[]{selection};
			Selection = null;
			selectionArgs = null;
//            Selection = "key" + " = ?";

//            new ClientTask().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR,"globalData", avdId);
//            long startTime = System.currentTimeMillis(); //fetch starting time
//            while(FLAG==0);
//            while(false||(System.currentTimeMillis()-startTime)<200);
            try {
                results = new ClientFetchGlobalTask().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR, "globalData", avdId, portMap[1], selection).get();
            }
            catch (InterruptedException e) {
                Log.e(TAG, "Interruption occurrred during Global selection query");
            }
            catch (ExecutionException e) {
                Log.e(TAG, "Execution stoppage occurrred during Global selection query");
            }
            catch (Exception e) {
                Log.e(TAG, "Unknown exception occurrred during Global selection query");
            }
            return results;


		}
		else if(selection.equals("@")){
			Log.d("queryselect", "Query, I am @");
//            selectionArgs = new String[]{selection};
			Selection = null;
			selectionArgs = null;
//            Selection = "key" + " = ?";
			results = db.query(table, projection, Selection, selectionArgs, null, null, null);
			return results;
		}
		else{// for particular key
			Log.d("queryselectinside", selection);
			selectionArgs = new String[]{selection};//had to to this coz selection alone was not working
			Selection = "key" + " = ?";

            try {
                getIndex = findHashIndex(genHash(selection));
//                Log.v("Check position", "For key: "+selection+" Index found is "+Integer.toString(getIndex)+" and current Node index is "+Integer.toString(nodeIndex));

            }
            catch(Exception e){
                Log.e("Exception", "NoSuchAlgorithmException");
            }
            if(getIndex==nodeIndex){//data is in same device

                results = db.query(table, projection, Selection, selectionArgs, null, null, null);
                return results;
            }

            else{
                if(getIndex==4){
                    if(1==nodeIndex){
                        results = db.query(table, projection, Selection, selectionArgs, null, null, null);
                        return results;
                    }
                }
                else if(getIndex==3){
                    if(0==nodeIndex){
                        results = db.query(table, projection, Selection, selectionArgs, null, null, null);
                        return results;
                    }
                }
                else{
                    if(getIndex+2==nodeIndex){
                        results = db.query(table, projection, Selection, selectionArgs, null, null, null);
                        return results;
                    }
                }

                FLAG=0;
                try {
                    if (getIndex == 4) {
                        results = new ClientFetchQuery().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR, "queryData", avdId, portMap[1], selection).get();
                        if(results.getCount()==0) {
                            Log.e(TAG, "No row returned, asking to first successor");
                            results = new ClientFetchQuery().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR, "queryData", avdId, portMap[0], selection).get();
                        }
                        if(results.getCount()==0) {
                            Log.e(TAG, "No row returned, asking to original node");
                            results = new ClientFetchQuery().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR, "queryData", avdId, portMap[4], selection).get();
                        }

//                    new ClientTask().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR,"queryData", avdId, portMap[1], selection);
                    } else if (getIndex == 3) {
                        results = new ClientFetchQuery().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR, "queryData", avdId, portMap[0], selection).get();
                        if(results.getCount()==0) {
                            Log.e(TAG, "No row returned, asking to first successor");
                            results = new ClientFetchQuery().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR, "queryData", avdId, portMap[3 + 1], selection).get();
                        }
                        if(results.getCount()==0) {
                            Log.e(TAG, "No row returned, asking to original node");
                            results = new ClientFetchQuery().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR, "queryData", avdId, portMap[3], selection).get();
                        }
//                    new ClientTask().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR,"queryData", avdId, portMap[0], selection);

                    } else {
                        results = new ClientFetchQuery().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR, "queryData", avdId, portMap[getIndex + 2], selection).get();
                        if(results.getCount()==0) {
                            Log.e(TAG, "No row returned, asking to first successor");
                            results = new ClientFetchQuery().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR, "queryData", avdId, portMap[getIndex + 1], selection).get();
                        }
                        if(results.getCount()==0) {
                            Log.e(TAG, "No row returned, asking to original node");
                            results = new ClientFetchQuery().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR, "queryData", avdId, portMap[getIndex], selection).get();
                        }

//                    new ClientTask().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR,"queryData", avdId, portMap[getIndex+2], selection);

                    }
                }
                catch (InterruptedException e) {
                    Log.e(TAG, "Interruption occurrred during selection query");
                }
                catch (ExecutionException e) {
                    Log.e(TAG, "Execution stoppage occurrred during selection query");
                }
                catch (Exception e) {
                    Log.e(TAG, "Unknown exception occurrred during selection query");
                }
//                while(FLAG==0);
//                long startTime = System.currentTimeMillis(); //fetch starting time
//                while(false||(System.currentTimeMillis()-startTime)<100);
                return results;


            }
		}

	}

    @Override
    public Uri insert(Uri uri, ContentValues values) {// for insertion of new data
//        SQLiteDatabase db = objDbHelper.getWritableDatabase();
//        long rowId;
        int getIndex = -1;
        try {
            getIndex = findHashIndex(genHash(values.get("key").toString()));
            Log.v("Check position", "For key: "+values.get("key").toString()+" Index found is "+Integer.toString(getIndex)+" and current Node index is "+Integer.toString(nodeIndex));

        }
        catch(NoSuchAlgorithmException e){
            Log.e("Exception", "NoSuchAlgorithmException");
        }
        if(getIndex==nodeIndex){//store in the same device and forward it to its two successor devices
//            rowId = db.insertOrThrow(table, null, values);
            SQLiteDatabase db = objDbHelper.getWritableDatabase();
            db.execSQL("INSERT OR REPLACE INTO "+table+" (key,value) VALUES(\""+values.get("key").toString()+"\",\""+values.get("value").toString()+"\")");
            getContext().getContentResolver().notifyChange(uri, null);
            Log.v("Inserted key-val pair", values.get("key").toString()+" into deviceID: "+portMap[nodeIndex]);
//            uri = ContentUris.withAppendedId(Uri.parse("content://" + AUTHORITY + table), rowId);
            //forward to two successors
            if(getIndex==4)
            {
                new ClientTask().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR,"forwardData", portMap[1],avdId, values.get("key").toString(), values.get("value").toString());
                new ClientTask().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR,"forwardData", portMap[0], avdId, values.get("key").toString(), values.get("value").toString());

            }
            else if(getIndex==3)
            {
                new ClientTask().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR,"forwardData", portMap[0], avdId, values.get("key").toString(), values.get("value").toString());
                new ClientTask().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR,"forwardData", portMap[getIndex+1], avdId, values.get("key").toString(), values.get("value").toString());

            }
            else{
                new ClientTask().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR,"forwardData", portMap[getIndex+2], avdId, values.get("key").toString(), values.get("value").toString());
                new ClientTask().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR,"forwardData", portMap[getIndex+1], avdId, values.get("key").toString(), values.get("value").toString());
            }


        }
        else{
            Log.d(TAG,values.get("key").toString()+"cannot be stored into current node, Forwarding Data to corresponding node "+Integer.toString(getIndex)+" and its successors");
            if(getIndex==4)//successors, 0, 1
            {
//                if(nodeIndex==1){
//                    db.execSQL("INSERT OR REPLACE INTO "+table+" (key,value) VALUES(\""+values.get("key").toString()+"\",\""+values.get("value").toString()+"\")");
//                    getContext().getContentResolver().notifyChange(uri, null);
//                    Log.v("Inserted key-val pair into successor", values.get("key").toString()+" into deviceID: "+portMap[nodeIndex]);
//                    new ClientTask().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR,"forwardData", portMap[0], avdId, values.get("key").toString(), values.get("value").toString());
//                    new ClientTask().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR,"forwardData", portMap[getIndex], avdId, values.get("key").toString(), values.get("value").toString());
//                }
//                if(nodeIndex==0){
//                    db.execSQL("INSERT OR REPLACE INTO "+table+" (key,value) VALUES(\""+values.get("key").toString()+"\",\""+values.get("value").toString()+"\")");
//                    getContext().getContentResolver().notifyChange(uri, null);
//                    Log.v("Inserted key-val pair into successor", values.get("key").toString()+" into deviceID: "+portMap[nodeIndex]);
//                    new ClientTask().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR,"forwardData", portMap[1], avdId, values.get("key").toString(), values.get("value").toString());
//                    new ClientTask().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR,"forwardData", portMap[getIndex], avdId, values.get("key").toString(), values.get("value").toString());
//                }
                new ClientTask().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR,"forwardData", portMap[1], avdId, values.get("key").toString(), values.get("value").toString());
                new ClientTask().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR,"forwardData", portMap[0], avdId, values.get("key").toString(), values.get("value").toString());
                new ClientTask().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR,"forwardData", portMap[getIndex], avdId, values.get("key").toString(), values.get("value").toString());

            }
            else if(getIndex==3)//successors, 4, 0
            {
//                if(nodeIndex==0){//0
//                    db.execSQL("INSERT OR REPLACE INTO "+table+" (key,value) VALUES(\""+values.get("key").toString()+"\",\""+values.get("value").toString()+"\")");
//                    getContext().getContentResolver().notifyChange(uri, null);
//                    Log.v("Inserted key-val pair into successor", values.get("key").toString()+" into deviceID: "+portMap[nodeIndex]);
//                    new ClientTask().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR,"forwardData", portMap[getIndex+1], avdId, values.get("key").toString(), values.get("value").toString());
//                    new ClientTask().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR,"forwardData", portMap[getIndex], avdId, values.get("key").toString(), values.get("value").toString());
//
//                }
//                if(nodeIndex==getIndex+1){//4
//                    db.execSQL("INSERT OR REPLACE INTO "+table+" (key,value) VALUES(\""+values.get("key").toString()+"\",\""+values.get("value").toString()+"\")");
//                    getContext().getContentResolver().notifyChange(uri, null);
//                    Log.v("Inserted key-val pair into successor", values.get("key").toString()+" into deviceID: "+portMap[nodeIndex]);
//                    new ClientTask().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR,"forwardData", portMap[0], avdId, values.get("key").toString(), values.get("value").toString());
//                    new ClientTask().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR,"forwardData", portMap[getIndex], avdId, values.get("key").toString(), values.get("value").toString());
//
//                }
                new ClientTask().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR,"forwardData", portMap[0], avdId, values.get("key").toString(), values.get("value").toString());
                new ClientTask().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR,"forwardData", portMap[getIndex+1], avdId, values.get("key").toString(), values.get("value").toString());
                new ClientTask().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR,"forwardData", portMap[getIndex], avdId, values.get("key").toString(), values.get("value").toString());

            }
            else//0,1,2,, 1,2,3,, 2,3,4
            {
//                if(getIndex+2==nodeIndex){
//                    db.execSQL("INSERT OR REPLACE INTO "+table+" (key,value) VALUES(\""+values.get("key").toString()+"\",\""+values.get("value").toString()+"\")");
//                    getContext().getContentResolver().notifyChange(uri, null);
//                    Log.v("insert key-val pair", values.get("key").toString()+" into deviceID: "+portMap[nodeIndex]);
//                }
                new ClientTask().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR,"forwardData", portMap[getIndex+2], avdId, values.get("key").toString(), values.get("value").toString());
                new ClientTask().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR,"forwardData", portMap[getIndex+1], avdId, values.get("key").toString(), values.get("value").toString());
                new ClientTask().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR,"forwardData", portMap[getIndex], avdId, values.get("key").toString(), values.get("value").toString());
            }

        }
//        long startTime = System.currentTimeMillis(); //fetch starting time
//        while(false||(System.currentTimeMillis()-startTime)<1000);
        return uri;
    }

	@Override
	public int update(Uri uri, ContentValues values, String selection,
			String[] selectionArgs) {
		// TODO Auto-generated method stub
		return 0;
	}

    private String genHash(String input) throws NoSuchAlgorithmException {
        MessageDigest sha1 = MessageDigest.getInstance("SHA-1");
        byte[] sha1Hash = sha1.digest(input.getBytes());
        Formatter formatter = new Formatter();
        for (byte b : sha1Hash) {
            formatter.format("%02x", b);
        }
        return formatter.toString();
    }

    private class ServerTask extends AsyncTask<ServerSocket, String, Void> {
		@Override
		protected Void doInBackground(ServerSocket... sockets) {
			ServerSocket serverSocket = sockets[0];

            /*
             * TODO: Fill in your server code that receives messages and passes them
             * to onProgressUpdate().
             */
			Socket socket = null;

			while(true) {
				try {

                        //accept connection from the sender and Initialize Input Stream for the code
    //                    Log.e("Server Task request", "Blocked");
                    socket = serverSocket.accept();
                    if (socket.isConnected()) {//if connected

    //                    Log.e("Server Task request", "Unblocked");
                        ObjectInputStream input = new ObjectInputStream(socket.getInputStream());
                        String msg = (String) input.readObject();
    //					input.close();

                        String strReceived = msg;
                        String[] msgArray = strReceived.split(":");

                        if (msgArray[0].equals("forwardData")) {//seek forwarded messages
    //                        Log.d(TAG,"It is a forward message");
    //                        Log.d(TAG,"Msg key received "+msgArray[1]);
    //                        Log.d(TAG,"Msg value received "+msgArray[2]);
                            // checking for data entry
                            try {
                                SQLiteDatabase db = objDbHelper.getWritableDatabase();
                                db.execSQL("INSERT OR REPLACE INTO " + table + " (key,value) VALUES(\"" + msgArray[2] + "\",\"" + msgArray[3] + "\")");
    //                            getContext().getContentResolver().notifyChange(uri, null);
    //                            cv.put("key", msgArray[2]);
    //                            cv.put("value", msgArray[3]);
                                Log.d(TAG, "Successfully added copy " + msgArray[2] + " of data to Content Provider, asked by deviceid: " + msgArray[1]);
                            } catch (Exception e) {
                                Log.e(TAG, "Something wrong with the Data Entry");
                            }
                        }
                        if (msgArray[0].equals("sendingData")) {//receiving Local Data from remote nodes
                            Log.d(TAG, "Local Data received " + strReceived);
                            Log.d(TAG, "Adding key: " + msgArray[2] + " to matrixcursor");
                            matrixCursor.addRow(new Object[]{msgArray[2], msgArray[3]});
                            FLAG = 1;
                        }
                        if (msgArray[0].equals("globalData")) {//part of seeking global messages
                            Log.d(TAG, "It is a request for globaldata");
                            Log.d(TAG, "Msg received " + strReceived);
                            Log.d(TAG, "Msg initiated by " + msgArray[1]);
                            Log.d(TAG, "Current avd is  " + avdId);
                            try {
                                Cursor resultCursor = query(mUri, null,
                                        "@", null, null);
                                while (resultCursor.moveToNext()) {
                                    int keyIndex = resultCursor.getColumnIndex("key");
                                    int valueIndex = resultCursor.getColumnIndex("value");
                                    String returnKey = resultCursor.getString(keyIndex);
                                    String returnValue = resultCursor.getString(valueIndex);
    //                    mTextView.append(returnKey+" "+returnValue);
                                    new ClientTask().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR, "sendingData", msgArray[1], avdId, returnKey, returnValue);//sending data to the request initiater one by one
                                }
                                resultCursor.close();
                            } catch (Exception e) {
                                Log.e(TAG, "Some error occurred while sending the Global Data");
                            }


                        }
                        if (msgArray[0].equals("queryData")) {//seek query message
                            Log.d(TAG, "Requested key received " + strReceived);
                            Log.d(TAG, "Original Msg initiated by " + msgArray[1]);
                            Log.d(TAG, "Requested Key is " + msgArray[2]);
                            Log.d(TAG, "Current avd is  " + avdId);

                            SQLiteDatabase db = objDbHelper.getReadableDatabase();
                            Log.d("queryselectoutside", msgArray[2]);
                            String[] selectionArgs = new String[]{msgArray[2]};//had to to this coz selection alone was not working
                            String Selection = "key" + " = ?";
                            Cursor resultCursor = db.query(table, null, Selection, selectionArgs, null, null, null);
                            if(resultCursor.getCount()==0){
                                StringBuffer msgToSend = new StringBuffer("Null");
                                ObjectOutputStream output = new ObjectOutputStream(socket.getOutputStream());//creating the outputstream for sending the objectified version of the string message received from the textbox
                                output.writeObject(msgToSend.toString());//sending the string object
                                output.flush();
                                Log.d("Data not Found", "Data not found for key" + msgArray[2] + " , returning back");
                            }
                            else {
                                try {
                                    while (resultCursor.moveToNext()) {
                                        int keyIndex = resultCursor.getColumnIndex("key");
                                        int valueIndex = resultCursor.getColumnIndex("value");
                                        String returnKey = resultCursor.getString(keyIndex);
                                        String returnValue = resultCursor.getString(valueIndex);

                                        if (returnKey.equals(msgArray[2])) {
                                            Log.d(TAG, "Data found: " + returnKey);
                                            Log.d(TAG, "Sending local Data from DHT to " + msgArray[1]);
                                            StringBuffer msgToSend = new StringBuffer("sendingData");
                                            msgToSend.append(":" + msgArray[1]);
                                            msgToSend.append(":" + returnKey);
                                            msgToSend.append(":" + returnValue);
                                            ObjectOutputStream output = new ObjectOutputStream(socket.getOutputStream());//creating the outputstream for sending the objectified version of the string message received from the textbox
                                            output.writeObject(msgToSend.toString());//sending the string object
                                            output.flush();
                                            Log.d("Data Found", "Data found " + returnKey + " , returning back");
                                            break;
                                        }

                                    }

                                } catch (Exception e) {
                                    Log.e("Exception in query", "Exception occurred while sending queried data back, key " + msgArray[2]);
                                }
                            }

                        }

                        if (msgArray[0].equals("restoreData")) {//seek query message
                            Log.d(TAG, "Restore message initiated by " + msgArray[1]);
                            Cursor resultCursor = query(mUri, null,
                                    "@", null, null);
                            if(resultCursor.getCount()==0){
                                StringBuffer msgToSend = new StringBuffer("Null");
                                ObjectOutputStream output = new ObjectOutputStream(socket.getOutputStream());//creating the outputstream for sending the objectified version of the string message received from the textbox
                                output.writeObject(msgToSend.toString());//sending the string object
                                output.flush();
                                Log.d("Restore Data not Found", "Restore Data not found for key, returning back");
                            }
                            //else
                            try {
                                ObjectOutputStream output = new ObjectOutputStream(socket.getOutputStream());
                                while (resultCursor.moveToNext()) {
                                    int keyIndex = resultCursor.getColumnIndex("key");
                                    int valueIndex = resultCursor.getColumnIndex("value");
                                    String returnKey = resultCursor.getString(keyIndex);
                                    String returnValue = resultCursor.getString(valueIndex);
                                    Log.d(TAG, "Transmitting local Data from DHT to " + msgArray[1]+" "+returnKey);
                                    StringBuffer msgToSend = new StringBuffer("sendingData");
                                    msgToSend.append(":" + avdId);
                                    msgToSend.append(":" + returnKey);
                                    msgToSend.append(":" + returnValue);
                                    //creating the outputstream for sending the objectified version of the string message received from the textbox
                                    output.writeObject(msgToSend.toString());//sending the string object
//                                    output.flush();
                                }
                            resultCursor.close();
                                output.writeObject("Null");//end of keys
                            } catch (Exception e) {
                                Log.e("Exception in query", "Exception occurred while sending restore data back, key");
                            }

                        }

                        if (msgArray[0].equals("globalDeleteData")) {//deleting Local Data from remote nodes
                            Log.d(TAG, "Deletion Requested " + strReceived);
                            Log.d(TAG, "Intitated by device  " + msgArray[1]);
    ////					if(avdId.equals(msgArray[1])){
    ////						FLAG = 1;
    ////						Log.d(TAG,"Flag set");
    ////						return;
    //					}
                            delete(mUri, "@", null);
                        }
                        if (msgArray[0].equals("localDelete")) {//request for local delete received
                            Log.d(TAG, "Request for delete key received" + strReceived);
                            Log.d(TAG, "Msg initiated by " + msgArray[1]);
                            Log.d(TAG, "Requested Key is " + msgArray[2]);
                            Log.d(TAG, "Current avd is  " + avdId);
                            try {
                                deleteFlag = true;
                                delete(mUri, msgArray[2], null);
                                deleteFlag = false;
                            } catch (Exception e) {
                                Log.e(TAG, "Some error occurred during deleting of local key");
                            }
                        }

    //                    ObjectInputStream input = new ObjectInputStream(receMsg.getInputStream());//this create byte by byte input stream and is read
    //                    inputMsg.append(input.readUTF())
    //                    publishProgress(inputMsg.toString());
                    }//if connected
                    else{
                        Log.d("Listerner Server no connection", "Cannot established connection");
                    }

				}//end of try
				catch (IOException e) {
					Log.e("Listerner Server Error", "Listener ServerReceive IO Exception");
				}
				catch (ClassNotFoundException e) {
					Log.e("Listerner Server Error", "ServerTask ClassNotFoundException");
				}
                catch(Exception e){
                    Log.e(TAG,"Unknown Exception in Server Task");
                }
			} //while(!socket.isInputShutdown());, end of while

//			return null;
		}

		protected  void onProgressUpdate(String...strings) {
            /*
             * The following code displays what is received in doInBackground().
             */


//            if()

			return;
		}

	}


	/***
	 * ClientTask is an AsyncTask that should send a string over the network.
	 * It is created by ClientTask.executeOnExecutor() call whenever OnKeyListener.onKey() detects
	 * an enter key press event.
	 *
	 * @author stevko
	 *
	 */
	private class ClientTask extends AsyncTask<String, Void, Void> {

		@Override
		protected Void doInBackground(String... msgs) {
			String msg1 = msgs[0];
			StringBuffer msgToSend = new StringBuffer(msg1);
			String receiverPort = new String();

			if (msg1.equals("forwardData")) {//create packet for forwarding data, forwardData:key:value
				msgToSend.append(":"+msgs[2]);
				msgToSend.append(":"+msgs[3]);
                msgToSend.append(":"+msgs[4]);
//                receiverPort = msgs[2];
				try {
					Log.d(TAG,"Msg to send to avd: "+msgs[1]+": "+msgToSend.toString());
					Socket socket = new Socket(InetAddress.getByAddress(new byte[]{10, 0, 2, 2}),
							Integer.parseInt(msgs[1])*2);

					ObjectOutputStream output = new ObjectOutputStream(socket.getOutputStream());//creating the outputstream for sending the objectified version of the string message received from the textbox
					output.writeObject(msgToSend.toString());//sending the string object
					output.flush();
//                        output.close();//this is causing IOexception in server side
//                    socket.close();//bug bug bug

				} catch (UnknownHostException e) {
					Log.e(TAG, "ClientTask UnknownHostException");
				} catch (IOException e) {
					Log.e(TAG, "ClientTask socket IOException");
				}
				catch (Exception e) {
					Log.e(TAG, "Something wrong real wrong while forwarding data");
				}
			}
			if (msg1.equals("globalData")) {
				msgToSend.append(":"+msgs[1]);
//                msgToSend.append(":"+msgs[2]);
//                receiverPort = msgs[2];

					Log.d(TAG,"Requesting Global Data from all DHT nodes "+msgToSend.toString());
					for(int i = 0; i<5; i++)
                    {
                        try {
                            if (portMap[i].compareToIgnoreCase(msgs[1]) == 0)
                                continue;
                            Socket socket = new Socket(InetAddress.getByAddress(new byte[]{10, 0, 2, 2}),
                                    Integer.parseInt(portMap[i]) * 2);
                            ObjectOutputStream output = new ObjectOutputStream(socket.getOutputStream());//creating the outputstream for sending the objectified version of the string message received from the textbox
                            output.writeObject(msgToSend.toString());//sending the string object
                            output.flush();
                            long startTime = System.currentTimeMillis(); //fetch starting time
                            while(false||(System.currentTimeMillis()-startTime)<20);

//                        output.close();//this is causing IOexception in server side
//                            socket.close();//bug bug bug
                        } catch (UnknownHostException e) {
                            Log.e(TAG, "ClientTask UnknownHostException");
                        } catch (IOException e) {
                            Log.e(TAG, "ClientTask socket IOException");
                        }
                        catch (Exception e) {
                            Log.e(TAG, "Something wrong real wrong while requesting global data");
                        }
                    }


			}
            if (msg1.equals("sendingData")) {//sending the localdata to the called avd
                msgToSend.append(":"+msgs[2]);
                msgToSend.append(":"+msgs[3]);
                msgToSend.append(":"+msgs[4]);

                try {
                    Log.d(TAG,"Sending local Data from DHT to "+msgs[1]);
                    Socket socket = new Socket(InetAddress.getByAddress(new byte[]{10, 0, 2, 2}),
                            Integer.parseInt(msgs[1])*2);

                    ObjectOutputStream output = new ObjectOutputStream(socket.getOutputStream());//creating the outputstream for sending the objectified version of the string message received from the textbox
                    output.writeObject(msgToSend.toString());//sending the string object
                    output.flush();
//                        output.close();//this is causing IOexception in server side
//                    socket.close();//bug bug bug

                } catch (UnknownHostException e) {
                    Log.e(TAG, "ClientTask UnknownHostException");
                } catch (IOException e) {
                    Log.e(TAG, "ClientTask socket IOException");
                }
                catch (Exception e) {
                    Log.e(TAG, "Something wrong real wrong while requesting global data");
                }
            }
			if (msg1.equals("globalDeleteData")) {//sending the localdata to the called avd
					Log.d(TAG,"Requesting deletion of data from DHT globally, initiated from device "+msgs[1].toString());
					msgToSend.append(":"+msgs[1]);
                    for(int i = 0; i<5; i++)
                    {
                        try {
                            if (portMap[i].compareToIgnoreCase(msgs[1]) == 0)
                                continue;
                            Socket socket = new Socket(InetAddress.getByAddress(new byte[]{10, 0, 2, 2}),
                                    Integer.parseInt(portMap[i]) * 2);
                            ObjectOutputStream output = new ObjectOutputStream(socket.getOutputStream());//creating the outputstream for sending the objectified version of the string message received from the textbox
                            output.writeObject(msgToSend.toString());//sending the string object
                            output.flush();
                            long startTime = System.currentTimeMillis(); //fetch starting time
                            while(false||(System.currentTimeMillis()-startTime)<20);
//                        output.close();//this is causing IOexception in server side
//                            socket.close();//bug bug bug
                        } catch (UnknownHostException e) {
                            Log.e(TAG, "ClientTask UnknownHostException");
                        } catch (IOException e) {
                            Log.e(TAG, "ClientTask socket IOException");
                        }
                        catch (Exception e) {
                            Log.e(TAG, "Something wrong real wrong while requesting global data");
                        }
                    }
			}
			if (msg1.equals("localDelete")) {//Requesting query key Data from DHT
				msgToSend.append(":"+msgs[1]);
				msgToSend.append(":"+msgs[3]);
				Log.d(TAG,"Deletion Msg initiated by "+msgs[1]);
				try {
					Log.d(TAG,"Requesting for deleting key from DHT "+msgs[2]+" "+msgToSend.toString());
					Log.d(TAG,"Requested Key is "+msgs[2]);
					Socket socket = new Socket(InetAddress.getByAddress(new byte[]{10, 0, 2, 2}),
							Integer.parseInt(msgs[2])*2);

					ObjectOutputStream output = new ObjectOutputStream(socket.getOutputStream());//creating the outputstream for sending the objectified version of the string message received from the textbox
					output.writeObject(msgToSend.toString());//sending the string object
					output.flush();
					long startTime = System.currentTimeMillis(); //fetch starting time
					while(false||(System.currentTimeMillis()-startTime)<20);
//                        output.close();//this is causing IOexception in server side
					socket.close();//bug bug bug

				} catch (UnknownHostException e) {
					Log.e(TAG, "ClientTask UnknownHostException");
				} catch (IOException e) {
					Log.e(TAG, "ClientTask socket IOException");
				}
				catch (Exception e) {
					Log.e(TAG, "Something wrong real wrong while requesting global data");
				}
			}

			return null;
		}
	}

    /***
     * ClientTask is an AsyncTask that should send a string over the network.
     * It is created by ClientTask.executeOnExecutor() call whenever OnKeyListener.onKey() detects
     * an enter key press event.
     *
     * @author stevko
     *
     */

    private class ClientFetchQuery extends AsyncTask<String, Void, MatrixCursor> {

        @Override
        protected MatrixCursor doInBackground(String... msgs) {
            String msg1 = msgs[0];
            StringBuffer msgToSend = new StringBuffer(msg1);
            String receiverPort = new String();
            MatrixCursor tempCursor = new MatrixCursor(new String[] { "key", "value" });
            if (msg1.equals("queryData")) {//Requesting query key Data from DHT
                msgToSend.append(":"+msgs[1]);
//				msgToSend.append(":"+msgs[2]);//miss this, its the receiver port
                msgToSend.append(":"+msgs[3]);

                try {
                    Log.d(TAG,"Requesting query key Data from DHT "+msgs[2]+" "+msgToSend.toString());
                    Log.d(TAG,"Requested Key is "+msgs[3]);
                    Log.d(TAG,"Asked by Avd "+msgs[1]);
                    Socket socket = new Socket(InetAddress.getByAddress(new byte[]{10, 0, 2, 2}),
                            Integer.parseInt(msgs[2])*2);

                    ObjectOutputStream output = new ObjectOutputStream(socket.getOutputStream());//creating the outputstream for sending the objectified version of the string message received from the textbox
                    output.writeObject(msgToSend.toString());//sending the string object
                    ObjectInputStream input = new ObjectInputStream(socket.getInputStream());
                    String msg = (String) input.readObject();

                    Log.d(TAG,"Queried Data received "+msg);
                    String []msgArray = msg.split(":");
                    Log.d(TAG,"Adding key: "+msgArray[2]+" to matrixcursor");
                    tempCursor.addRow(new Object[] { msgArray[2], msgArray[3] });
                    FLAG = 1;
                    return tempCursor;
//                    output.flush();

//					long startTime = System.currentTimeMillis(); //fetch starting time
//					while(false||(System.currentTimeMillis()-startTime)<20);
//					socket.close();//bug bug bug

                } catch (UnknownHostException e) {
                    Log.e(TAG, "ClientFetchQuery UnknownHostException in Fetching key "+msgs[3]);
                    return tempCursor;
                }
                catch (SocketTimeoutException e) {
                    Log.e(TAG, "ClientFetchQuery socket SocketTimeoutException in Fetching key "+msgs[3]);
                    return tempCursor;
                }
                catch (IOException e) {
                    Log.e(TAG, "ClientFetchQuery socket IOException in Fetching key "+msgs[3]);
                    return tempCursor;
                }
                catch (Exception e) {
                    Log.e(TAG, "Something wrong real wrong while requesting query data");
                    return tempCursor;
                }
                //you can handle device crashes here

            }
            Log.d("querycount for key "+msgs[3], "tempcursor "+Integer.toString(tempCursor.getCount()));
            return null;
        }
    }
    /***
     * ClientTask is an AsyncTask that should send a string over the network.
     * It is created by ClientTask.executeOnExecutor() call whenever OnKeyListener.onKey() detects
     * an enter key press event.
     *
     * @author stevko
     *
     */
    private class ClientRestoreTask extends AsyncTask<String, Void, Void> {

        @Override
        protected Void doInBackground(String... msgs) {
            String msg1 = msgs[0];

            if (msg1.equals("restore")) {//create packet for forwarding data, forwardData:key:value
                delete(mUri,"@", null);

//                try {
                    int getIndex = -1;
                    Log.d(TAG, "Starting restoration ");
                    StringBuffer msgToSend = new StringBuffer("restoreData");
                    msgToSend.append(":"+avdId);
                    SQLiteDatabase db = objDbHelper.getWritableDatabase();//important
                    for(int i = 0; i<5; i++)
                    {
                        Log.d("Iterating" ,"Port "+ portMap[i]);
                        try {
                            if (portMap[i].compareToIgnoreCase(avdId) == 0)
                                continue;
                            Socket socket = new Socket(InetAddress.getByAddress(new byte[]{10, 0, 2, 2}),
                                    Integer.parseInt(portMap[i]) * 2);
                            ObjectOutputStream output = new ObjectOutputStream(socket.getOutputStream());//creating the outputstream for sending the objectified version of the string message received from the textbox
                            output.writeObject(msgToSend.toString());//sending the string object
                            ObjectInputStream input = new ObjectInputStream(socket.getInputStream());
                            try{
                                while(true){
                                    String msg = (String) input.readObject();
//                                    Log.d(TAG,"Restored Data received "+msg+" from");
                                    String []msgArray = msg.split(":");
                                    if(msgArray[0].compareToIgnoreCase("Null")!=0)
                                    {
                                        getIndex = findHashIndex(genHash(msgArray[2]));
                                        Boolean check = checkPosition(getIndex);
//                                        Log.v("Check position status: "+Boolean.toString(checkPosition(getIndex)), "For key: "+msgArray[2]+" Index found is "+Integer.toString(getIndex)+" and current Node index is "+Integer.toString(nodeIndex));
                                        Log.d(TAG, "Returned values for restoration "+msgArray[2]+" "+msgArray[3]);

                                        if(check){//main node

                                            db.execSQL("INSERT OR REPLACE INTO " + table + " (key,value) VALUES(\"" + msgArray[2] + "\",\"" + msgArray[3] + "\")");

                                        }

                                        getIndex = -1;
                                    }
                                    else//get out of loop and move to other port
                                        break;
                                }
                            }
                            catch (Exception e){
                                Log.e("Coming out of loop","yeah");
                            }



//                            long startTime = System.currentTimeMillis(); //fetch starting time
//                            while(false||(System.currentTimeMillis()-startTime)<20);
//                        output.close();//this is causing IOexception in server side
//                            socket.close();//bug bug bug
                        } catch (UnknownHostException e) {
                            Log.e(TAG, "ClientRestoreTask UnknownHostException");
                        } catch (IOException e) {
                            Log.e(TAG, "ClientRestoreTask socket IOException");
                        }

                    }

            }
            return null;
        }
    }
    /***
     * ClientTask is an AsyncTask that should send a string over the network.
     * It is created by ClientTask.executeOnExecutor() call whenever OnKeyListener.onKey() detects
     * an enter key press event.
     *
     * @author stevko
     *
     */
    private class ClientFetchGlobalTask extends AsyncTask<String, Void, MatrixCursor> {

        @Override
        protected MatrixCursor doInBackground(String... msgs) {
            String msg1 = msgs[0];
            MatrixCursor tempCursor = new MatrixCursor(new String[] { "key", "value" });
            if (msg1.equals("globalData")) {//create packet for forwarding data, forwardData:key:value

//                try {
                int getIndex = -1;

                Log.d(TAG, "Starting Global Fetching ");
                StringBuffer msgToSend = new StringBuffer("restoreData");
                msgToSend.append(":"+avdId);
                SQLiteDatabase db = objDbHelper.getWritableDatabase();//important
                for(int i = 0; i<5; i++)
                {
                    Log.d("Iterating" ,"Port "+ portMap[i]);
                    try {
//                        if (portMap[i].compareToIgnoreCase(avdId) == 0)
//                            continue;
                        Socket socket = new Socket(InetAddress.getByAddress(new byte[]{10, 0, 2, 2}),
                                Integer.parseInt(portMap[i]) * 2);
                        ObjectOutputStream output = new ObjectOutputStream(socket.getOutputStream());//creating the outputstream for sending the objectified version of the string message received from the textbox
                        output.writeObject(msgToSend.toString());//sending the string object
                        ObjectInputStream input = new ObjectInputStream(socket.getInputStream());
                        try{
                            while(true){
                                String msg = (String) input.readObject();
//                                    Log.d(TAG,"Restored Data received "+msg+" from");
                                String []msgArray = msg.split(":");
                                if(msgArray[0].compareToIgnoreCase("Null")!=0)
                                {
                                    getIndex = findHashIndex(genHash(msgArray[2]));
                                    Boolean check = checkPosition(getIndex);
//                                        Log.v("Check position status: "+Boolean.toString(checkPosition(getIndex)), "For key: "+msgArray[2]+" Index found is "+Integer.toString(getIndex)+" and current Node index is "+Integer.toString(nodeIndex));
                                    Log.d(TAG, "Returned values for global query "+msgArray[2]+" "+msgArray[3]);

                                    if(true){//main node
                                        tempCursor.addRow(new Object[]{msgArray[2], msgArray[3]});
                                    }

                                    getIndex = -1;
                                }
                                else//get out of loop and move to other port
                                    break;
                            }
                        }
                        catch (Exception e){
                            Log.e("Coming out of loop","yeah");
                        }



//                            long startTime = System.currentTimeMillis(); //fetch starting time
//                            while(false||(System.currentTimeMillis()-startTime)<20);
//                        output.close();//this is causing IOexception in server side
//                            socket.close();//bug bug bug
                    } catch (UnknownHostException e) {
                        Log.e(TAG, "ClientGlobalFetchTask UnknownHostException");
                    } catch (IOException e) {
                        Log.e(TAG, "ClientGlobalFetchTask socket IOException");
                    }

                }

            }
            return tempCursor;
        }
    }
}
