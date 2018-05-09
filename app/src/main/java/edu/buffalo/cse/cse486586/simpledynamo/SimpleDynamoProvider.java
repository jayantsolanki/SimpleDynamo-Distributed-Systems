package edu.buffalo.cse.cse486586.simpledynamo;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Formatter;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.database.MergeCursor;
import android.net.Uri;

//SQLite
import android.database.sqlite.SQLiteDatabase;
import android.content.ContentUris;

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

	private MatrixCursor matrixCursor; // stackoverflow.com https://stackoverflow.com/questions/9917935/adding-rows-into-cursor-manually
	private Uri mUri = buildUri("content", AUTHORITY);
	private Handler handler = new Handler();
	private MergeCursor mergeCursor ;
	private Cursor results;
	private String avdId;
	private int FLAG = 0;
	private boolean insertFlag = false;
    private boolean deleteFlag = false;
    private boolean queryFlag = false;
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
        return true;
    }

    @Override

    public int delete(Uri uri, String selection, String[] selectionArgs) {

        SQLiteDatabase db = objDbHelper.getReadableDatabase();
        FLAG = 0;
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
		SQLiteDatabase db = objDbHelper.getReadableDatabase();
		matrixCursor = new MatrixCursor(new String[] { "key", "value" });
		FLAG = 0;
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
			Log.d("queryselect", "Query, I am *");
//            selectionArgs = new String[]{selection};
			Selection = null;
			selectionArgs = null;
//            Selection = "key" + " = ?";

            new ClientTask().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR,"globalData", avdId);
            long startTime = System.currentTimeMillis(); //fetch starting time
            while(false||(System.currentTimeMillis()-startTime)<2000);
            results = db.query(table, projection, Selection, selectionArgs, null, null, null);
            mergeCursor = new MergeCursor(new Cursor[] { matrixCursor, results });
            //you can check for duplicates here before proceeding to return data
            return mergeCursor;


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
			Log.d("queryselect", selection);
			selectionArgs = new String[]{selection};//had to to this coz selection alone was not working
			Selection = "key" + " = ?";
//            if(queryFlag){
//                results = db.query(table, projection, Selection, selectionArgs, null, null, null);
//                return results;
//            }
            try {
                getIndex = findHashIndex(genHash(selection));
                Log.v("Check position", "For key: "+selection+" Index found is "+Integer.toString(getIndex)+" and current Node index is "+Integer.toString(nodeIndex));

            }
            catch(Exception e){
                Log.e("Exception", "NoSuchAlgorithmException");
            }
            if(getIndex==nodeIndex){//data is in same device

                results = db.query(table, projection, Selection, selectionArgs, null, null, null);
                return results;
            }
            else{
                //here the second successor can fail so check for fallback
                results = null;
                if(getIndex==4)
                    new ClientTask().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR,"queryData", avdId, portMap[1], selection);
                else if(getIndex==3)
                    new ClientTask().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR,"queryData", avdId, portMap[0], selection);
                else
                    new ClientTask().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR,"queryData", avdId, portMap[getIndex+2], selection);

                long startTime = System.currentTimeMillis(); //fetch starting time
//                while(FLAG==0);
                while(false||(System.currentTimeMillis()-startTime)<100);
//                while(false||(System.currentTimeMillis()-startTime)<1000);
//                results = db.query(table, projection, Selection, selectionArgs, null, null, null);
                mergeCursor = new MergeCursor(new Cursor[] { matrixCursor });
                viewCursor(mergeCursor);
//
                return mergeCursor;

            }
		}
//        Cursor results = db.query(table, projection, Selection, selectionArgs, null, null, null);
//        while(FLAG==0);
//        results.setNotificationUri(getContext().getContentResolver(), uri);
//        Log.v("query", selection);
		//add while loop here with the FLAG
//        return mergeCursor;
//        return results;
	}

    @Override
    public Uri insert(Uri uri, ContentValues values) {// for insertion of new data
        SQLiteDatabase db = objDbHelper.getWritableDatabase();
        long rowId;
        int getIndex = -1;
        if(insertFlag){
            rowId = db.insertOrThrow(table, null, values);
            getContext().getContentResolver().notifyChange(uri, null);
            Log.v("insert key-val pair virtual copy", values.get("key").toString()+" into successor deviceID: "+portMap[nodeIndex]);
            uri = ContentUris.withAppendedId(Uri.parse("content://" + AUTHORITY + table), rowId);
            return uri;

        }
        try {
            getIndex = findHashIndex(genHash(values.get("key").toString()));
//            Log.v("Check position", "For key: "+genHash(values.get("key").toString())+" Index found is "+Integer.toString(getIndex)+" and current Node index is "+Integer.toString(nodeIndex));

        }
        catch(NoSuchAlgorithmException e){
            Log.e("Exception", "NoSuchAlgorithmException");
        }
        if(getIndex==nodeIndex){//store in the same device and forward it to its two successor devices
            rowId = db.insertOrThrow(table, null, values);
            getContext().getContentResolver().notifyChange(uri, null);
            Log.v("insert key-val pair", values.get("key").toString()+" into deviceID: "+portMap[nodeIndex]);
            uri = ContentUris.withAppendedId(Uri.parse("content://" + AUTHORITY + table), rowId);
            //forward to two successors
            if(getIndex==4)
            {
                new ClientTask().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR,"forwardData", portMap[0], values.get("key").toString(), values.get("value").toString());
                new ClientTask().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR,"forwardData", portMap[1], values.get("key").toString(), values.get("value").toString());

            }
            else if(getIndex==3)
            {
                new ClientTask().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR,"forwardData", portMap[getIndex+1], values.get("key").toString(), values.get("value").toString());
                new ClientTask().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR,"forwardData", portMap[0], values.get("key").toString(), values.get("value").toString());

            }
            else{
                new ClientTask().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR,"forwardData", portMap[getIndex+1], values.get("key").toString(), values.get("value").toString());
                new ClientTask().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR,"forwardData", portMap[getIndex+2], values.get("key").toString(), values.get("value").toString());
            }


        }
        else{
            Log.d(TAG,"cannot be stored into current node, Forwarding Data to corresponding node "+Integer.toString(getIndex)+" and its successors");
            if(getIndex==4)
            {
                new ClientTask().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR,"forwardData", portMap[getIndex], values.get("key").toString(), values.get("value").toString());
                new ClientTask().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR,"forwardData", portMap[0], values.get("key").toString(), values.get("value").toString());
                new ClientTask().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR,"forwardData", portMap[1], values.get("key").toString(), values.get("value").toString());

            }
            else if(getIndex==3)
            {
                new ClientTask().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR,"forwardData", portMap[getIndex], values.get("key").toString(), values.get("value").toString());
                new ClientTask().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR,"forwardData", portMap[getIndex+1], values.get("key").toString(), values.get("value").toString());
                new ClientTask().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR,"forwardData", portMap[0], values.get("key").toString(), values.get("value").toString());

            }
            else
            {
                new ClientTask().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR,"forwardData", portMap[getIndex], values.get("key").toString(), values.get("value").toString());
                new ClientTask().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR,"forwardData", portMap[getIndex+1], values.get("key").toString(), values.get("value").toString());
                new ClientTask().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR,"forwardData", portMap[getIndex+2], values.get("key").toString(), values.get("value").toString());
            }

        }

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

			do {
				try {
					//accept connection from the sender and Initialize Input Stream for the code
					socket = serverSocket.accept();
					ObjectInputStream input = new ObjectInputStream(socket.getInputStream());
					String msg = (String) input.readObject();
					input.close();
					publishProgress(msg);
//                    ObjectInputStream input = new ObjectInputStream(receMsg.getInputStream());//this create byte by byte input stream and is read
//                    inputMsg.append(input.readUTF())
//                    publishProgress(inputMsg.toString());
				} catch (IOException e) {
					Log.e(TAG, "ServerReceive IO Exception");
				}
				catch (ClassNotFoundException e) {
					Log.e(TAG, "ServerTask ClassNotFoundException");
				}
				catch (Exception e) {
					Log.e(TAG, e.getMessage());
				}
			} while(!socket.isInputShutdown());

			return null;
		}

		protected void onProgressUpdate(String...strings) {
            /*
             * The following code displays what is received in doInBackground().
             */
			String strReceived = strings[0].trim();
//            Log.d(TAG,"Msg received "+strReceived);
			String []msgArray = strReceived.split(":");
			try{

				if (msgArray[0].equals("forwardData")){//seek forwarded messages
					Log.d(TAG,"It is a forward message");
//                    Log.d(TAG,"Msg received "+strReceived);
                    Log.d(TAG,"Msg key received "+msgArray[1]);
                    Log.d(TAG,"Msg value received "+msgArray[2]);
					// checking for data entry
					try{
						ContentValues cv = new ContentValues();
						insertFlag = true;
						cv.put("key", msgArray[1]);
						cv.put("value", msgArray[2]);
						Uri cUri = insert(mUri, cv);
						if(cUri != null){
							Log.d(TAG,  "Successfully added copy of data to Content Provider");
						}
						insertFlag = false;
					}
					catch (Exception e) {
						Log.e(TAG, "Something wrong with the Data Entry");
					}
				}
				if (msgArray[0].equals("globalData")){//part of seeking global messages
					Log.d(TAG,"It is a request for globaldata");
					Log.d(TAG,"Msg received "+strReceived);
					Log.d(TAG,"Msg initiated by "+msgArray[1]);
					Log.d(TAG,"Current avd is  "+avdId);
					try{
						Cursor resultCursor = query(mUri, null,
								"@", null, null);
						while (resultCursor.moveToNext()) {
							int keyIndex = resultCursor.getColumnIndex("key");
							int valueIndex = resultCursor.getColumnIndex("value");
							String returnKey = resultCursor.getString(keyIndex);
							String returnValue = resultCursor.getString(valueIndex);
//                    mTextView.append(returnKey+" "+returnValue);
							new ClientTask().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR,"sendingData", msgArray[1], avdId, returnKey, returnValue);//sending data to the request initiater one by one
						}
						resultCursor.close();
					}
					catch (Exception e)
					{
						Log.e(TAG, "Some error occurred");
					}


				}
				if (msgArray[0].equals("sendingData")){//receiving Local Data from remote nodes
					Log.d(TAG,"Local Data received "+strReceived);
					matrixCursor.addRow(new Object[] { msgArray[2], msgArray[3] });


				}
				if (msgArray[0].equals("queryData")){//seek query message
					Log.d(TAG,"Requested key received "+strReceived);
					Log.d(TAG,"Original Msg initiated by "+msgArray[1]);
					Log.d(TAG,"Requested Key is "+msgArray[2]);
					Log.d(TAG,"Current avd is  "+avdId);
					try{
						Cursor resultCursor = query(mUri, null,
								"@", null, null);
						while (resultCursor.moveToNext()) {
							int keyIndex = resultCursor.getColumnIndex("key");
							int valueIndex = resultCursor.getColumnIndex("value");
							String returnKey = resultCursor.getString(keyIndex);
							String returnValue = resultCursor.getString(valueIndex);
                            Log.d(TAG, "Data: "+returnKey);
							if(returnKey.equals(msgArray[2])){
								new ClientTask().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR,"sendingData", msgArray[1], avdId, returnKey, returnValue);
								Log.d(TAG, "Data found "+returnKey+" , returning back");
//								resultCursor.close();
								return;
							}

						}
						resultCursor.close();
					}
					catch (Exception e)
					{
						Log.e(TAG, "Some error occurred");
					}
				}

				if (msgArray[0].equals("globalDeleteData")){//deleting Local Data from remote nodes
					Log.d(TAG,"Deletion Requested "+strReceived);
					Log.d(TAG,"Intitated by device  "+msgArray[1]);
////					if(avdId.equals(msgArray[1])){
////						FLAG = 1;
////						Log.d(TAG,"Flag set");
////						return;
//					}
					delete(mUri, "@", null);
				}
				if (msgArray[0].equals("localDelete")){//request for local delete received
					Log.d(TAG,"Request for delete key received"+strReceived);
					Log.d(TAG,"Msg initiated by "+msgArray[1]);
					Log.d(TAG,"Requested Key is "+msgArray[2]);
					Log.d(TAG,"Current avd is  "+avdId);
					try{
                        deleteFlag = true;
						delete(mUri,msgArray[2], null);
						deleteFlag = false;
					}
					catch (Exception e)
					{
						Log.e(TAG, "Some error occurred during deleting of local key");
					}
				}


			}
			catch(Exception e){
				Log.e(TAG,"NoSuchAlgorithm found");
			}


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
//                receiverPort = msgs[2];
				try {
					Log.d(TAG,"Msg to send "+msgToSend.toString());
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
					Log.e(TAG, "Something wrong real wrong while forearding data");
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
				//you can handle device crashes here

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
}
