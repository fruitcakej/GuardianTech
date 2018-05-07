package com.example.android.guardiantech;

import android.app.LoaderManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.Loader;
import android.graphics.BitmapFactory;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.support.customtabs.CustomTabsClient;
import android.support.customtabs.CustomTabsIntent;
import android.support.customtabs.CustomTabsServiceConnection;
import android.support.customtabs.CustomTabsSession;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import java.util.ArrayList;
import java.util.List;

import static android.provider.ContactsContract.CommonDataKinds.Website.URL;

public class MainActivity extends AppCompatActivity implements LoaderManager.LoaderCallbacks<List<Article>> {

    public static final String LOG_TAG = MainActivity.class.getName();
    private static final String API_URL =
            "https://content.guardianapis.com/search?&section=technology&format=json&show-fields=headline,thumbnail&show-tags=contributor&order-by=newest&api-key=test";

    // Custom Tabs variables
    public static final String CUSTOM_TAB_PACKAGE_NAME = "com.android.chrome";
    CustomTabsClient mClient;
    CustomTabsSession mCustomTabsSession;
    CustomTabsServiceConnection mCustomTabsServiceConnection;
    static CustomTabsIntent customTabsIntent;

    private ProgressBar mspinner;
    private ImageView noData;
    RecyclerView recyclerView;

    /**
     * Adapter for the list of articles
     */
    private ArticleAdapter mAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        Log.i(LOG_TAG, "Test OnCreate() called");

        super.onCreate(savedInstanceState);
        setContentView(R.layout.layout_recycler);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setTitle(null);
        getSupportActionBar().setDisplayHomeAsUpEnabled(false);
        findViewById(R.id.appBarLayout).bringToFront();

        //Initialise Custom Tabs
        mCustomTabsServiceConnection = new CustomTabsServiceConnection() {
            @Override
            public void onCustomTabsServiceConnected(ComponentName componentName, CustomTabsClient customTabsClient) {
                mClient = customTabsClient;
                mClient.warmup(0L);
                mCustomTabsSession = mClient.newSession(null);
            }
            @Override
            public void onServiceDisconnected(ComponentName name) {
                mClient = null;
            }
        };
        CustomTabsClient.bindCustomTabsService(MainActivity.this, CUSTOM_TAB_PACKAGE_NAME, mCustomTabsServiceConnection);
        customTabsIntent = new CustomTabsIntent.Builder(mCustomTabsSession)
                .setToolbarColor(ContextCompat.getColor(this, R.color.colorBar))
                .setShowTitle(true)
                .setCloseButtonIcon(BitmapFactory.decodeResource(getResources(), R.drawable.ic_arrow_back))
                .build();

        recyclerView = findViewById(R.id.recyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(MainActivity.this));
        recyclerView.setHasFixedSize(true);

        TextView mEmptyTextView;
        mEmptyTextView = findViewById(R.id.empty_state);
        mspinner = findViewById(R.id.loading_spinner);
        noData = findViewById(R.id.no_data);

        // Create a new adapter that takes an empty list of articles as input
        mAdapter = new ArticleAdapter(this, new ArrayList<Article>());

        // setEmptyView not avail for a recyclerView
        recyclerView.setAdapter(mAdapter);

        Log.i(LOG_TAG, "Calling initLoader()");

        //Check for internet connectivity
        ConnectivityManager cm = (ConnectivityManager) getBaseContext().getSystemService(Context.CONNECTIVITY_SERVICE);

        NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
        boolean isConnected = activeNetwork != null && activeNetwork.isConnectedOrConnecting();

        if (isConnected) {
            getLoaderManager().initLoader(1, null, this).forceLoad();
        } else {
            mspinner.setVisibility(View.GONE);
            mEmptyTextView.setVisibility(View.VISIBLE);
            mEmptyTextView.setText(R.string.no_internet);
        }
    }

    @Override
    public Loader<List<Article>> onCreateLoader(int id, Bundle args) {

        Log.i(LOG_TAG, "Test OnCreateLoader() called");
        return new ArticleLoader(MainActivity.this, API_URL);
    }

    @Override
    public void onLoadFinished(Loader<List<Article>> loader, List<Article> data) {

        //Hide loading spinner as loading is completed
        mspinner.setVisibility(View.GONE);

        Log.i(LOG_TAG, "Test OnLoadFinished() called");

        // Clear the adapter of previous articles
        mAdapter.clearArticles();

        // If there is a valid list of articles, then add them to the adapter's
        // data set.
        if (data != null && !data.isEmpty()) {
            mAdapter.addData(data);
        }

        // Check for data, if none received, enable empty views
        if (mAdapter.getItemCount() == 0) {
            recyclerView.setVisibility(View.GONE);
            noData.setImageResource(R.drawable.nodata);
        }
    }

    @Override
    public void onLoaderReset(Loader<List<Article>> loader) {

        Log.i(LOG_TAG, "Test OnLoadReset() called");

        // Loader reset, so we can clear out our existing data.
        mAdapter.clearArticles();
    }

    /**
     *
     * Method to send intent to browse full article on Guardian website
     */
    public static void onArticleClick(Context context, String webUrl) {
        customTabsIntent.launchUrl(context, Uri.parse(webUrl));
    }

    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.refresh) {
            refreshData();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void refreshData() {
        Toast.makeText(this, R.string.checkNewData, Toast.LENGTH_SHORT).show();
        mAdapter.clearArticles();
        getLoaderManager().initLoader(1, null, this).forceLoad();
    }
}
