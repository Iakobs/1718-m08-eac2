package ibanez.jacob.cat.xtec.ioc.lectorrss;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;

import ibanez.jacob.cat.xtec.ioc.lectorrss.model.RssItem;

/**
 * Class for operating on the database
 *
 * @author <a href="mailto:jacobibanez@jacobibanez.com">Jacob Ibáñez Sánchez</a>.
 */
public class DBInterface {

    //Tag for logging purposes
    private static final String TAG = DBInterface.class.getSimpleName();

    //Database columns
    private static final String COLUMN_ID = "_id";
    private static final String COLUMN_TITLE = "TITLE";
    private static final String COLUMN_LINK = "LINK";
    private static final String COLUMN_AUTHOR = "AUTHOR";
    private static final String COLUMN_DESCRIPTION = "DESCRIPTION";
    private static final String COLUMN_PUB_DATE = "PUB_DATE";
    private static final String COLUMN_CATEGORIES = "CATEGORIES";
    private static final String COLUMN_THUMBNAIL = "THUMBNAIL";
    private static final String COLUMN_IMAGE_CACHE_PATH = "IMAGE_CACHE_PATH";

    //Database variables
    public static final String DB_NAME = "FEEDS_DB";
    public static final String TABLE_ITEMS = "ITEMS";
    public static final int VERSION = 1;

    //Database queries
    public static final String CREATE_TABLE_ITEMS =
            "CREATE TABLE IF NOT EXISTS " + TABLE_ITEMS + " (" +
                    COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    COLUMN_TITLE + " TEXT NOT NULL UNIQUE," +
                    COLUMN_LINK + " TEXT NOT NULL UNIQUE," +
                    COLUMN_AUTHOR + " TEXT NOT NULL UNIQUE," +
                    COLUMN_DESCRIPTION + " TEXT NOT NULL UNIQUE," +
                    COLUMN_PUB_DATE + " TEXT NOT NULL UNIQUE," +
                    COLUMN_CATEGORIES + " TEXT NOT NULL UNIQUE," +
                    COLUMN_THUMBNAIL + " TEXT NOT NULL UNIQUE," +
                    COLUMN_IMAGE_CACHE_PATH + " TEXT NOT NULL UNIQUE" +
                    ");";

    //class members
    private DBHelp mHelp;
    private SQLiteDatabase mDatabase;

    //Constructor
    public DBInterface(Context context) {
        this.mHelp = new DBHelp(context);
    }

    //Open and close methods

    /**
     * This method opens the connection to the database
     *
     * @return A ready-to-use instance of the {@link DBInterface}
     * @throws SQLException If an error occurs
     */
    public DBInterface open() throws SQLException {
        mDatabase = mHelp.getWritableDatabase();
        return this;
    }

    /**
     * Closes the connection to the database
     */
    public void close() {
        mHelp.close();
    }

    //Methods for manipulating data

    /**
     * @param item
     * @return
     */
    public long insertItem(RssItem item) {
        if (item == null) {
            String msg = "The item must not be null";
            Log.e(TAG, msg);
            throw new IllegalArgumentException(msg);
        }

        long result = -1L;

        if (!existsByTitle(item.getTitle())) {
            ContentValues initialValues = new ContentValues();

            //fill all columns
            initialValues.put(COLUMN_TITLE, item.getTitle());
            initialValues.put(COLUMN_LINK, item.getLink());
            initialValues.put(COLUMN_AUTHOR, item.getAuthor());
            initialValues.put(COLUMN_DESCRIPTION, item.getDescription());
            initialValues.put(COLUMN_PUB_DATE, item.getPubDate());
            initialValues.put(COLUMN_CATEGORIES, item.getCategories());
            initialValues.put(COLUMN_THUMBNAIL, item.getThumbnail());
            initialValues.put(COLUMN_IMAGE_CACHE_PATH, item.getImagePathInCache());

            result = mDatabase.insert(TABLE_ITEMS, null, initialValues);
        }

        return result;
    }

    public boolean existsByTitle(String title) {
        Cursor cursor = mDatabase.query(
                TABLE_ITEMS,
                new String[]{COLUMN_ID},
                COLUMN_TITLE + " = ?",
                new String[]{title},
                null,
                null,
                null
        );

        boolean exists = cursor.moveToFirst();
        cursor.close();
        return exists;
    }

    /**
     * @return
     */
    public List<RssItem> getAllItems(String filter) {
        List<RssItem> items = new ArrayList<>();

        //if the search pattern is an empty string, show all items
        String selection = null;
        String[] selectionArgs = null;
        if (filter != null && !filter.isEmpty()) {
            //search items with the matching pattern
            selection = COLUMN_TITLE + " LIKE ?";
            selectionArgs = new String[]{"%" + filter + "%"};
        }

        Cursor cursor = mDatabase.query(TABLE_ITEMS, null, selection, selectionArgs, null, null, null);

        if (cursor.moveToFirst()) {
            do {
                RssItem item = new RssItem(
                        cursor.getString(cursor.getColumnIndex(COLUMN_TITLE)),
                        cursor.getString(cursor.getColumnIndex(COLUMN_LINK)),
                        cursor.getString(cursor.getColumnIndex(COLUMN_AUTHOR)),
                        cursor.getString(cursor.getColumnIndex(COLUMN_DESCRIPTION)),
                        cursor.getString(cursor.getColumnIndex(COLUMN_PUB_DATE)),
                        cursor.getString(cursor.getColumnIndex(COLUMN_CATEGORIES)),
                        cursor.getString(cursor.getColumnIndex(COLUMN_THUMBNAIL)),
                        cursor.getString(cursor.getColumnIndex(COLUMN_IMAGE_CACHE_PATH))
                );
                items.add(item);
            } while (cursor.moveToNext());

            cursor.close();
        }

        return items;
    }

    //Helper inner class

    /**
     * Helper inner class for accessing the database
     */
    private static class DBHelp extends SQLiteOpenHelper {

        //Constructor
        DBHelp(Context con) {
            super(con, DB_NAME, null, VERSION);
        }

        @Override
        public void onCreate(SQLiteDatabase db) {
            try {
                db.execSQL(CREATE_TABLE_ITEMS);
            } catch (SQLException e) {
                Log.w(TAG, "Error executing statement " + CREATE_TABLE_ITEMS, e);
            }
        }

        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
            Log.w(TAG, "Updating database from " + oldVersion + " to " + newVersion +
                    ". This process wil erase all data");
            db.execSQL("DROP TABLE IF EXISTS " + TABLE_ITEMS);

            onCreate(db);
        }
    }
}
