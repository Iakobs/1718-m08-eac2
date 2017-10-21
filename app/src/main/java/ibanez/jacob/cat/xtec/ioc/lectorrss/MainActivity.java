package ibanez.jacob.cat.xtec.ioc.lectorrss;

import android.app.Activity;
import android.content.Context;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.DividerItemDecoration;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import org.xmlpull.v1.XmlPullParserException;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.util.List;

import ibanez.jacob.cat.xtec.ioc.lectorrss.model.RssItem;
import ibanez.jacob.cat.xtec.ioc.lectorrss.utils.ConnectionUtils;

/**
 * Main Activity
 *
 * @author <a href="mailto:jacobibanez@jacobibanez.com">Jacob Ibáñez Sánchez</a>.
 */
public class MainActivity extends AppCompatActivity implements View.OnClickListener,
        TextView.OnEditorActionListener {

    //Tag for logging purposes
    private static final String TAG = MainActivity.class.getSimpleName();

    public static final String FEED_CHANNEL = "http://www.eldiario.es/rss/";

    private LinearLayout mSearchBar;
    private EditText mSearchQuery;
    private ProgressBar mProgressBar;
    private ItemAdapter mItemAdapter;
    private DBInterface mDataBase;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        //Add icon to toolbar title
        getSupportActionBar().setDisplayShowHomeEnabled(true);
        getSupportActionBar().setIcon(R.drawable.ic_rss_feed_white_48dp);

        //Get references to member variables
        mSearchBar = (LinearLayout) findViewById(R.id.search_bar);
        mSearchQuery = (EditText) findViewById(R.id.et_search);
        mProgressBar = (ProgressBar) findViewById(R.id.pb_loading_indicator);
        mItemAdapter = new ItemAdapter(this);
        mDataBase = new DBInterface(this);

        //set the layout manager and the adapter of the recycler view
        RecyclerView recyclerView = (RecyclerView) findViewById(R.id.recycler_view);
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        recyclerView.setLayoutManager(layoutManager);
        recyclerView.setAdapter(mItemAdapter);

        //add a decorator to separate items
        DividerItemDecoration decoration = new DividerItemDecoration(this, layoutManager.getOrientation());
        recyclerView.addItemDecoration(decoration);

        //set onClickListener for the search button
        ImageButton searchButton = (ImageButton) findViewById(R.id.ib_search);
        searchButton.setOnClickListener(this);

        //set on action done listener for the search query
        mSearchQuery.setOnEditorActionListener(this);

        //feed the recycler view, either from the internet or from the database
        feedRecyclerViewFromInternetOrDatabase();
    }

    private void feedRecyclerViewFromInternetOrDatabase() {
        if (ConnectionUtils.hasConnection(this)) { //check for internet connection
            //if there is connection, start the execution of the async task
            new DownloadRssTask().execute(FEED_CHANNEL);
        } else {
            //fill adapter list from database
            feedListFromDataBase();
            Toast.makeText(this, R.string.toast_offline_load, Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onClick(View view) {
        feedListFromDataBase();
        hideSoftKeyboard(this);
    }

    private void feedListFromDataBase() {
        mDataBase.open();
        String keyword = mSearchQuery.getText().toString();
        mItemAdapter.setItems(mDataBase.getAllItems(keyword));
        mDataBase.close();
    }

    @Override
    public boolean onEditorAction(TextView textView, int actionId, KeyEvent keyEvent) {
        boolean enterPressedFromKeyboard = keyEvent != null && ((keyEvent.getKeyCode() == KeyEvent.KEYCODE_ENTER)
                && (keyEvent.getAction() == KeyEvent.ACTION_DOWN));
        boolean enterPressedFromSoftKeyboard = actionId == EditorInfo.IME_ACTION_DONE;
        if (enterPressedFromKeyboard || enterPressedFromSoftKeyboard) {
            feedListFromDataBase();
        }
        hideSoftKeyboard(this);
        return true;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    /**
     * This method handles behavior when a menu item is selected
     *
     * @param item The selected item
     * @return Return false to allow normal menu processing to proceed, true to consume it here.
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //we check which button has been pressed
        switch (id) {
            case R.id.action_refresh:   //refresh button has been pressed
                //check for connection
                if (ConnectionUtils.hasConnection(this)) {
                    //refresh the recycler view content
                    feedRecyclerViewFromInternetOrDatabase();
                } else {
                    //you pressed refresh button but there is no connection
                    Toast.makeText(this, R.string.toast_there_is_no_connection, Toast.LENGTH_SHORT).show();
                }
                return true;
            case R.id.action_search:    //search button has been pressed
                //we only have to toggle the search bar
                toggleSearchBar();
                return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private void toggleSearchBar() {
        //When not visible, the search bar's visibility has to be GONE, so the layout
        //doesn't occupy space in the parent layout
        if (mSearchBar.getVisibility() == View.GONE) {
            mSearchBar.setVisibility(View.VISIBLE);
        } else if (mSearchBar.getVisibility() == View.VISIBLE) {
            mSearchBar.setVisibility(View.GONE);
        }
    }

    /**
     * This class is for downloading a XML file from the internet in a background thread
     */
    private class DownloadRssTask extends AsyncTask<String, Void, List<RssItem>> {
        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            //set progress bar visible and hid recycler view, so we are connecting to the internet
            mProgressBar.setVisibility(View.VISIBLE);
        }

        @Override
        protected List<RssItem> doInBackground(String... strings) {
            List<RssItem> result = null;

            try {
                //get the XML from the feed url and process it
                result = getRssItems(strings[0]);
                //download thumbnails to the cache directory
                cacheImages(result);
            } catch (IOException ex) {

            } catch (XmlPullParserException ex) {

            }

            return result;
        }

        @Override
        protected void onPostExecute(List<RssItem> items) {
            //save to the database all the info of the XML file
            storeResult(items);

            //set progress bar invisible and show recycler view, so the result from the internet has arrived
            mProgressBar.setVisibility(View.INVISIBLE);

            //feed the list of items of the recycler view's adapter
            feedListFromDataBase();
        }
    }

    private void storeResult(List<RssItem> result) {
        mDataBase.open();
        for (RssItem item : result) {
            mDataBase.insertItem(item);
        }
        mDataBase.close();
    }

    /**
     * @param result
     */
    private void cacheImages(List<RssItem> result) {
        for (RssItem item : result) {
            try {
                URL imageUrl = new URL(item.getThumbnail());
                InputStream inputStream = (InputStream) imageUrl.getContent();
                byte[] bufferImage = new byte[1024];

                OutputStream outputStream = new FileOutputStream(item.getImagePathInCache());

                int count;
                while ((count = inputStream.read(bufferImage)) != -1) {
                    outputStream.write(bufferImage, 0, count);
                }

                inputStream.close();
                outputStream.close();
            } catch (IOException ex) {
                Log.e(TAG, "Error downloading image from " + item.getThumbnail(), ex);
            }
        }
    }

    /**
     * @param url
     * @return
     * @throws IOException
     * @throws XmlPullParserException
     */
    private List<RssItem> getRssItems(String url) throws IOException, XmlPullParserException {
        InputStream in = null;
        RssItemParser parser = new RssItemParser(this);
        List<RssItem> result = null;

        try {
            in = ConnectionUtils.openHttpConnection(url);
            result = parser.parse(in);
        } finally {
            if (in != null) {
                in.close();
            }
        }

        return result;
    }

    /**
     * A method for hiding the Android virtual keyboard
     *
     * @param activity The activity which owns the keyboard to hide
     */
    public static void hideSoftKeyboard(Activity activity) {
        InputMethodManager inputManager = (InputMethodManager) activity.getSystemService(Context.INPUT_METHOD_SERVICE);
        View currentFocus = activity.getCurrentFocus();
        if (inputManager != null && currentFocus != null) {
            inputManager.hideSoftInputFromWindow(currentFocus.getWindowToken(), InputMethodManager.HIDE_NOT_ALWAYS);
        }
    }
}
