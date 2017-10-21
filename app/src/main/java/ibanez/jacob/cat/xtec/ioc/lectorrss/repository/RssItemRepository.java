package ibanez.jacob.cat.xtec.ioc.lectorrss.repository;

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
 * Class for manipulating {@link RssItem} objects from the database.
 * <p>
 * It encapsulates all access to the database, so it manages creating and closing connections itself.
 *
 * @author <a href="mailto:jacobibanez@jacobibanez.com">Jacob Ibáñez Sánchez</a>.
 */
public class RssItemRepository {

    //Tag for logging purposes
    private static final String TAG = RssItemRepository.class.getSimpleName();

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
    private static final String DB_NAME = "FEEDS_DB";
    private static final String TABLE_ITEMS = "ITEMS";
    private static final int VERSION = 1;

    //Database queries
    private static final String CREATE_TABLE_ITEMS =
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
    public RssItemRepository(Context context) {
        this.mHelp = new DBHelp(context);
    }

    //Open and close methods

    /**
     * This method opens the connection to the database.
     * <p>
     * You can specify if you want only read access or both read/write access
     *
     * @param withWriteAccess If {@code true}, the connection can write to the database
     * @return A ready-to-use connection to the database
     * @throws SQLException If an error occurs
     */
    private RssItemRepository open(boolean withWriteAccess) throws SQLException {
        if (withWriteAccess) {
            mDatabase = mHelp.getWritableDatabase();
        } else {
            mDatabase = mHelp.getReadableDatabase();
        }
        return this;
    }

    /**
     * Closes the connection to the database
     */
    private void close() {
        mHelp.close();
    }

    //Methods for manipulating data

    /**
     * Creates a new {@link RssItem} in the database.
     * <p>
     * If the item already exists, it does nothing
     *
     * @param item The item to create
     * @return The number of rows affected or -1 if an error occurs
     */
    public long insertItem(RssItem item) {
        //Check that the item is not null
        if (item == null) {
            String msg = "The item must not be null";
            Log.e(TAG, msg);
            throw new IllegalArgumentException(msg);
        }

        //default return value
        long result = 0L;

        if (!existsByTitle(item.getTitle())) { //first check if the item already exists
            //first open database with write permissions
            open(true);

            //fill all columns
            ContentValues initialValues = new ContentValues();

            initialValues.put(COLUMN_TITLE, item.getTitle());
            initialValues.put(COLUMN_LINK, item.getLink());
            initialValues.put(COLUMN_AUTHOR, item.getAuthor());
            initialValues.put(COLUMN_DESCRIPTION, item.getDescription());
            initialValues.put(COLUMN_PUB_DATE, item.getPubDate());
            initialValues.put(COLUMN_CATEGORIES, item.getCategories());
            initialValues.put(COLUMN_THUMBNAIL, item.getThumbnail());
            initialValues.put(COLUMN_IMAGE_CACHE_PATH, item.getImagePathInCache());

            //create the item
            result = mDatabase.insert(TABLE_ITEMS, null, initialValues);

            //database access is not needed anymore
            close();
        }

        return result;
    }

    /**
     * Checks if a {@link RssItem} already exists, searching by its {@link RssItem#title}
     *
     * @param title The {@link RssItem#title}
     * @return {@code true} if the item exists in the database. {@code false} otherwise.
     */
    public boolean existsByTitle(String title) {
        //first open database with only read access
        open(false);

        Cursor cursor = mDatabase.query(
                TABLE_ITEMS,
                new String[]{COLUMN_ID},
                COLUMN_TITLE + " = ?",
                new String[]{title},
                null,
                null,
                null
        );

        //if there is a single coincidence, it means that the item already exists
        boolean exists = cursor.moveToFirst();

        //close cursor so it's not needed anymore, and so the connection to the database
        cursor.close();
        close();

        return exists;
    }

    /**
     * This method retrieve all elements from the database, with an optional keyword.
     * <p>
     * If the keyword is informed, it searches any coincidence in the {@link RssItem#title} with the
     * given keyword passed as parameter, not only exact matches.
     *
     * @param keyword The keyword to look up after
     * @return A collection of {@link RssItem}s, matching criteria if any, or all items if there's no keyword
     */
    public List<RssItem> getAllItems(String keyword) {
        List<RssItem> items = new ArrayList<>();

        //if the search pattern is an empty string, show all items
        String selection = null;
        String[] selectionArgs = null;

        if (keyword != null && !keyword.isEmpty()) { //if there's a keyword, let's make a where filter
            //search items with the matching pattern
            selection = COLUMN_TITLE + " LIKE ?";
            selectionArgs = new String[]{"%" + keyword + "%"};
        }

        //open read access to the database
        open(false);

        //perform query to the database
        Cursor cursor = mDatabase.query(TABLE_ITEMS, null, selection, selectionArgs, null, null, null);

        if (cursor.moveToFirst()) { //check if it have any result first
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
            } while (cursor.moveToNext()); //continue adding while there are more items in the cursor

            //close the cursor, it's not needed anymore
            cursor.close();
        }
        close();

        return items;
    }

    /**
     * Helper inner class for encapsulating low level access to the database
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
