package edu.buffalo.cse.cse486586.simpledynamo;

/**
 * Created by jayant on 5/8/18.
 */

// referenced from
// https://www.youtube.com/watch?v=9Ias8Tp-yGI

//import android.content.ContentValues;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

public class sQLiteDbHelper extends SQLiteOpenHelper {


    // Database name
    private final static String DB_NAME = "dynamo";
    static final String TAG = SQLiteOpenHelper.class.getSimpleName();

    public sQLiteDbHelper(Context context) {
        super(context, DB_NAME, null, 1);
    }
    @Override
    public void onCreate(SQLiteDatabase db) {
        String keyCol = "key";
        String valCol = "value";
        String createTable ="CREATE TABLE "+DB_NAME+ " (" +keyCol  + " TEXT PRIMARY KEY, " +valCol + " TEXT "+");";
        db.execSQL(createTable);
        Log.v(TAG, DB_NAME+" table created");
    }
    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {

        db.execSQL("DROP TABLE IF EXISTS " + 1);
        onCreate(db);
    }
}
