package ibanez.jacob.cat.xtec.ioc.lectorrss.utils;

import android.app.Activity;
import android.content.Context;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkInfo;
import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.ProtocolException;
import java.net.URL;

import static android.net.ConnectivityManager.TYPE_MOBILE;
import static android.net.ConnectivityManager.TYPE_WIFI;

/**
 * Class with connection utils
 *
 * @author <a href="mailto:jacobibanez@jacobibanez.com">Jacob Ibáñez Sánchez</a>.
 */
public class ConnectionUtils {

    private static final String TAG = ConnectionUtils.class.getSimpleName();

    /**
     * This method checks for {@link ConnectivityManager#TYPE_MOBILE} and
     * {@link ConnectivityManager#TYPE_WIFI} connectivity and tells if there's one of both connected
     *
     * @param activity The {@link Activity} which holds the {@link ConnectivityManager}
     */
    public static boolean hasConnection(Activity activity) {
        boolean hasConnection = false;

        //Get the connectivity manager
        ConnectivityManager connMgr = (ConnectivityManager) activity.getSystemService(Context.CONNECTIVITY_SERVICE);

        //Get all networks
        Network[] networks = connMgr.getAllNetworks();

        //iterate for all networks checking if mobile or wifi is connected
        for (Network network : networks) {
            //Get the network info for each network
            NetworkInfo networkInfo = connMgr.getNetworkInfo(network);

            //If the network is either mobile or wifi and is connected, we change the default false
            //value of the return variable
            if (networkInfo != null) {
                boolean networkIsMobileOrWifi = networkInfo.getType() == TYPE_MOBILE ||
                        networkInfo.getType() == TYPE_WIFI;
                boolean networkIsConnected = networkInfo.isConnected();
                hasConnection = networkIsConnected && networkIsMobileOrWifi;
            }
        }

        return hasConnection;
    }

    /**
     * Opens an HTTP connection to the given url address
     *
     * @param urlAddress The string representing a url address
     * @return The input stream associated to the connection
     * @throws IOException If an error occurs
     */
    public static InputStream openHttpConnection(String urlAddress) throws IOException {
        InputStream in;
        int responseCode;

        //Get a URL object from provided String
        URL url = new URL(urlAddress);

        //Get new http connection from given URL
        HttpURLConnection httpConn = (HttpURLConnection) url.openConnection();

        try {
            //prepare connection and connect
            prepareConnection(httpConn);
            httpConn.connect();

            //We get the http response code
            responseCode = httpConn.getResponseCode();

            //Check if the response is OK
            if (responseCode == HttpURLConnection.HTTP_OK) {
                //We get the input stream from the connection
                in = httpConn.getInputStream();
            } else {
                //There was en error on the response, log it
                String message = "Response code not OK. Response code: " + responseCode;
                Log.e(TAG, message);
                throw new IOException(message);
            }
        } catch (Exception ex) {
            //There was en error connecting, log it
            String message = "Error connecting";
            Log.e(TAG, message);
            throw new IOException(message);
        }

        return in;
    }

    /**
     * Gives basic configuration to the http connection.
     *
     * @param httpConn The connection
     * @throws ProtocolException If the http verb is not allowed
     */
    private static void prepareConnection(HttpURLConnection httpConn) throws ProtocolException {
        httpConn.setReadTimeout(10000);
        httpConn.setConnectTimeout(15000);
        httpConn.setRequestMethod("GET");
        httpConn.setDoInput(true);
    }
}
