package ibanez.jacob.cat.xtec.ioc.lectorrss.view;

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

import ibanez.jacob.cat.xtec.ioc.lectorrss.R;
import ibanez.jacob.cat.xtec.ioc.lectorrss.model.RssItem;
import ibanez.jacob.cat.xtec.ioc.lectorrss.parser.RssItemParser;
import ibanez.jacob.cat.xtec.ioc.lectorrss.repository.RssItemRepository;
import ibanez.jacob.cat.xtec.ioc.lectorrss.utils.ConnectionUtils;
import ibanez.jacob.cat.xtec.ioc.lectorrss.view.adapter.ItemAdapter;

/**
 * Main Activity
 * <p>
 * Displays a toolbar with a refresh and a search button, and a recycler view fed with a Rss xml from
 * the internet, targeted with a hardcoded url: {@value FEED_CHANNEL}
 *
 * @author <a href="mailto:jacobibanez@jacobibanez.com">Jacob Ibáñez Sánchez</a>.
 */
public class MainActivity extends AppCompatActivity implements View.OnClickListener,
        TextView.OnEditorActionListener {

    //Tag for logging purposes
    private static final String TAG = MainActivity.class.getSimpleName();

    //Rss url
    public static final String FEED_CHANNEL = "http://www.eldiario.es/rss/";

    //class members
    private LinearLayout mSearchBar;
    private EditText mSearchText;
    private ProgressBar mProgressBar;
    private ItemAdapter mItemAdapter;
    private RssItemRepository mItemRepository;

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
        mSearchText = (EditText) findViewById(R.id.et_search);
        mProgressBar = (ProgressBar) findViewById(R.id.pb_loading_indicator);
        mItemAdapter = new ItemAdapter(this);
        mItemRepository = new RssItemRepository(this);

        //set the layout manager and the adapter of the recycler view
        RecyclerView recyclerView = (RecyclerView) findViewById(R.id.recycler_view);
        LinearLayoutManager layoutManager = new LinearLayoutManager(this); //default is set to vertical
        recyclerView.setLayoutManager(layoutManager);
        recyclerView.setAdapter(mItemAdapter);

        //add a decorator to separate items
        DividerItemDecoration decoration = new DividerItemDecoration(this, layoutManager.getOrientation());
        recyclerView.addItemDecoration(decoration);

        //set onClickListener for the search button
        ImageButton searchButton = (ImageButton) findViewById(R.id.ib_search);
        searchButton.setOnClickListener(this);

        //set on action done listener for the search query, so the user can perform search also from the keyboard
        mSearchText.setOnEditorActionListener(this);

        //feed the recycler view
        connectToInternetAndFeedFromRepository();
    }

    private void connectToInternetAndFeedFromRepository() {
        if (ConnectionUtils.hasConnection(this)) { //check for internet connection
            //if there is connection, start the execution of the async task
            new DownloadRssTask(this).execute(FEED_CHANNEL);
        } else {
            //fill adapter list from database
            feedListFromRepository();
            Toast.makeText(this, R.string.toast_offline_load, Toast.LENGTH_SHORT).show();
        }
    }

    private void feedListFromRepository() {
        String keyword = mSearchText.getText().toString();
        mItemAdapter.setItems(mItemRepository.getAllItems(keyword));
    }

    /**
     * Implements behavior for a click on the search button which gets visible in the content layout
     * when the menu search button is clicked
     *
     * @param view The clicked view
     */
    @Override
    public void onClick(View view) {
        feedListFromRepository();
        hideSoftKeyboard(this);
    }

    /**
     * Implements behavior when a key is pressed both on the virtual soft keyboard or an actual
     * keyboard plugged to the device
     *
     * @param textView The text view with the current focus
     * @param actionId The action id
     * @param keyEvent The keyEvent with the info of the pressed key
     * @return Always {@code true}
     */
    @Override
    public boolean onEditorAction(TextView textView, int actionId, KeyEvent keyEvent) {
        //if true, it's an actual keyboard which makes the action
        boolean enterPressedFromKeyboard = keyEvent != null && ((keyEvent.getKeyCode() == KeyEvent.KEYCODE_ENTER)
                && (keyEvent.getAction() == KeyEvent.ACTION_DOWN));
        //if true, it's the virtual keyboard performing the action
        boolean enterPressedFromSoftKeyboard = actionId == EditorInfo.IME_ACTION_DONE;
        if (enterPressedFromKeyboard || enterPressedFromSoftKeyboard) {
            feedListFromRepository();
        }
        hideSoftKeyboard(this);
        return false;
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
     * @return Return {@code false} to allow normal menu processing to proceed, {@code true} to consume it here.
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        //get the item's id
        int id = item.getItemId();

        //we check which button has been pressed based on its id
        switch (id) {
            case R.id.action_refresh:   //refresh button has been pressed
                //check for connection
                if (ConnectionUtils.hasConnection(this)) {
                    //refresh the recycler view content
                    connectToInternetAndFeedFromRepository();
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
     * This class is a task for downloading a Rss file from the internet in a background thread
     */
    private class DownloadRssTask extends AsyncTask<String, Void, List<RssItem>> {

        private Context mContext;

        DownloadRssTask(Context context) {
            this.mContext = context;
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            //set progress bar visible
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
            } catch (IOException | XmlPullParserException ex) {
                Log.e(TAG, "There was an error while downloading the rss file from " + FEED_CHANNEL);
            }

            return result;
        }

        /**
         * Accesses the parser and get the collection of {@link RssItem}s
         *
         * @param url The rss feed url
         * @return The collection of {@link RssItem}s
         * @throws IOException            If there's any Input/Output error
         * @throws XmlPullParserException If the parsing process goes wrong
         */
        private List<RssItem> getRssItems(String url) throws IOException, XmlPullParserException {
            InputStream in = null;
            RssItemParser parser = new RssItemParser(mContext);
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
         * Download the image from the internet and stores it in the app's cache
         *
         * @param items A collection of {@link RssItem}s
         */
        private void cacheImages(List<RssItem> items) {
            for (RssItem item : items) {
                try {
                    //get the bytes of the image from the internet
                    URL imageUrl = new URL(item.getThumbnail());
                    InputStream inputStream = (InputStream) imageUrl.getContent();
                    byte[] bufferImage = new byte[1024];

                    //open a stream to the app's cache
                    OutputStream outputStream = new FileOutputStream(item.getImagePathInCache());

                    int count;
                    while ((count = inputStream.read(bufferImage)) != -1) {
                        outputStream.write(bufferImage, 0, count); //write the bytes from the internet to the cache
                    }

                    //close both input and output streams
                    inputStream.close();
                    outputStream.close();
                } catch (IOException ex) {
                    Log.e(TAG, "Error downloading image from " + item.getThumbnail(), ex);
                }
            }
        }

        @Override
        protected void onPostExecute(List<RssItem> items) {
            //save all the info from the XML file to the database
            for (RssItem item : items) {
                mItemRepository.insertItem(item);
            }

            //hide the progress bar, so the result from the internet has arrived
            mProgressBar.setVisibility(View.INVISIBLE);

            //feed the list of items of the recycler view's adapter
            feedListFromRepository();
        }
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
