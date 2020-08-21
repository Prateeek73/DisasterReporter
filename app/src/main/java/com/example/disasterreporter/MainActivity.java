package com.example.disasterreporter;

import android.Manifest;
import android.app.AlertDialog;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteStatement;
import android.os.AsyncTask;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.example.disasterreporter.notificationChannel.CHANNEL;

public class MainActivity extends AppCompatActivity {

    SharedPreferences sharedPreferences;
    NotificationManagerCompat notificationManager;
    LocationListener locationListener;
    LocationManager locationManager;
    static SQLiteDatabase drDB;
    DownloadTask task;
    EditText range;

    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 1)
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED)
                if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED)
                    locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, locationListener);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_resource_file, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == R.id.refresh) {
            drDB.execSQL("DROP TABLE events");
            drDB.execSQL("CREATE TABLE IF NOT EXISTS events (type VARCHAR," +
                    "url VARCHAR," +
                    " area VARCHAR," +
                    " datetime VARCHAR," +
                    " magnitude VARCHAR," +
                    " latitude VARCHAR," +
                    " longitude VARCHAR" +
                    ")");
            Intent intent = new Intent(getApplicationContext(), MainActivity.class);
            startActivity(intent);
        } else if (item.getItemId() == R.id.range) {
            android.app.AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
            LayoutInflater inflater = MainActivity.this.getLayoutInflater();
            View view = inflater.inflate(R.layout.dialog_box, null);
            builder.setView(view).
                    setTitle("Enter Radius in kilometers")
                    .setPositiveButton("Change", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            sharedPreferences.edit().putInt("range", Integer.parseInt(range.getText().toString())).apply();
                        }
                    }).show();
            range = view.findViewById(R.id.range);
        }
        return super.onOptionsItemSelected(item);
    }

    public class DownloadTask extends AsyncTask<String, Void, String> {
        @Override
        protected String doInBackground(String... urls) {
            String result = "";
            URL url;
            HttpURLConnection urlConnection;
            try {
                url = new URL(urls[0]);
                urlConnection = (HttpURLConnection) url.openConnection();
                BufferedReader br = new BufferedReader(new InputStreamReader(urlConnection.getInputStream()));
                String line;
                while ((line = br.readLine()) != null)
                    result = result.concat(line);
                urlConnection.disconnect();
                String[] splitResult = result.split("<div id=\"virtual_osocc_title_bar\">");
                Pattern p1 = Pattern.compile("class=\"alert_item\"><a href=\"(.*?)<div class=\"alert_icon\">");
                Pattern p2 = Pattern.compile("<div class=\"alert_item_name(.*?)</div>");
                Pattern p3 = Pattern.compile("<span class=\"magnitude(.*?)</span>");
                Pattern p4 = Pattern.compile("class=\"alert_date(.*?)</span>");
                Matcher m1 = p1.matcher(splitResult[0]);
                Matcher m2 = p2.matcher(splitResult[0]);
                Matcher m3 = p3.matcher(splitResult[0]);
                Matcher m4 = p4.matcher(splitResult[0]);

                while (m1.find() && m2.find() && m3.find() && m4.find()) {

                    String link = m1.group(1).substring(0, m1.group(1).indexOf('"'));
                    link = link.replace(link.substring(link.indexOf(';'), link.lastIndexOf(';')), "");
                    String eventType = link.substring(link.lastIndexOf('=') + 1);
                    String eventID = link.substring(link.indexOf('=') + 1, link.indexOf('&'));
                    link = link.substring(0, link.indexOf('?') + 1) + link.substring(link.indexOf(';') + 1) + '&' + link.substring(link.indexOf('?') + 1, link.indexOf('&'));
                    String URLs = link;
                    result = "";
                    url = new URL("https://gdacs.org/report.aspx?eventtype=" + eventType + "&eventid=" + eventID);
                    urlConnection = (HttpURLConnection) url.openConnection();
                    br = new BufferedReader(new InputStreamReader(urlConnection.getInputStream()));
                    while ((line = br.readLine()) != null)
                        result = result.concat(line);
                    String[] splitResultURL = result.split("jksfd,msdfsdkjfsn,dmdgfhj,gdmsm,");
                    Pattern LatitudePattern = Pattern.compile("reportLat =(.*?);");
                    Pattern LongitudePattern = Pattern.compile("reportLon =(.*?);");
                    Matcher LatitudeMatcher = LatitudePattern.matcher(splitResultURL[0]);
                    Matcher LongitudeMatcher = LongitudePattern.matcher(splitResultURL[0]);
                    String Latitude = "";
                    String Longitude = "";
                    while (LatitudeMatcher.find())
                        Latitude = LatitudeMatcher.group(1).trim();
                    while (LongitudeMatcher.find())
                        Longitude = LongitudeMatcher.group(1).trim();
                    String Area = (m2.group(1).replace("_past\">", "").replace(">", "").replace("\"", "").trim());
                    String Magnitude = m3.group(1).replace("_past\">", "").replace(">", "").replace("\"", "").trim();
                    String DateTime = m4.group(1).replace("_past\">", "").replace(">", "").replace("\"", "").replace('-', ' ').trim().replace("  ", "").replace("  ", "").replace("  ", "");
                    String type = "";
                    if (URLs.contains("EQ"))
                        type = "Earthquake";
                    else if (URLs.contains("VO"))
                        type = "Volcano";
                    else if (URLs.contains("FL"))
                        type = "Flood";
                    else if (URLs.contains("DR"))
                        type = "Drought";
                    else if (URLs.contains("TC"))
                        type = "Cyclone";
                    String sql = "INSERT INTO events (type, url, area, datetime, magnitude, latitude, longitude) VALUES (?, ?, ?, ?, ?, ?, ?)";
                    SQLiteStatement statement = drDB.compileStatement(sql);
                    Cursor check = drDB.rawQuery("SELECT * FROM events WHERE url LIKE '" + URLs + "'", null);
                    if (check.getCount() == 0) {
                        statement.bindString(1, type);
                        statement.bindString(2, URLs);
                        statement.bindString(3, Area);
                        statement.bindString(4, DateTime);
                        statement.bindString(5, Magnitude);
                        statement.bindString(6, Latitude);
                        statement.bindString(7, Longitude);
                        statement.execute();
                    }
                    check.close();
                }
            } catch (IOException ex) {
                ex.printStackTrace();
            }
            return result;
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        sharedPreferences = this.getSharedPreferences("com.example.disasterreporter", Context.MODE_PRIVATE);
        drDB = this.openOrCreateDatabase("users", MODE_PRIVATE, null);
        notificationManager = NotificationManagerCompat.from(this);
//        drDB.execSQL("DROP TABLE events");
        drDB.execSQL("CREATE TABLE IF NOT EXISTS events (type VARCHAR," +
                "url VARCHAR," +
                " area VARCHAR," +
                " datetime VARCHAR," +
                " magnitude VARCHAR," +
                " latitude VARCHAR," +
                " longitude VARCHAR" +
                ")");
        task = new DownloadTask();
        task.execute("https://gdacs.org");
        locationListener = new LocationListener() {
            @Override
            public void onLocationChanged(Location location) {
                showNotification(location);
            }

            @Override
            public void onStatusChanged(String s, int i, Bundle bundle) {
            }

            @Override
            public void onProviderEnabled(String s) {
            }

            @Override
            public void onProviderDisabled(String s) {
            }
        };
        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED)
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 1);

        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 30000, 0, locationListener);
        Location lastKnownLocation = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
        if (lastKnownLocation != null) {
            showNotification(lastKnownLocation);
        }
    }

    public void showList(View view) {

        Intent intentSender = new Intent(getApplicationContext(), earthquake_activity.class);
        ArrayList<String> IntentURLs = new ArrayList<>();
        ArrayList<String> IntentArea = new ArrayList<>();
        ArrayList<String> IntentMagnitude = new ArrayList<>();
        ArrayList<String> IntentDateTime = new ArrayList<>();

        Cursor c = drDB.rawQuery("SELECT * FROM events", null);
        int urlId = c.getColumnIndex("url");
        int areaId = c.getColumnIndex("area");
        int datetimeId = c.getColumnIndex("datetime");
        int magnitudeId = c.getColumnIndex("magnitude");

        String id = getResources().getResourceName(view.getId()).substring(getResources().getResourceName(view.getId()).lastIndexOf('/') + 1);
        if (c.moveToFirst())
            while (c.moveToNext()) {
                if (c.getString(urlId).substring(c.getString(urlId).indexOf('=') + 1, c.getString(urlId).indexOf('=') + 3).equals(id)) {
                    IntentURLs.add(c.getString(urlId));
                    IntentArea.add(c.getString(areaId));
                    IntentDateTime.add(c.getString(datetimeId));
                    IntentMagnitude.add(c.getString(magnitudeId));
                }
            }
        c.close();
        intentSender.putExtra("id", id);
        intentSender.putExtra("URLs", IntentURLs);
        intentSender.putExtra("Area", IntentArea);
        intentSender.putExtra("DateTime", IntentDateTime);
        intentSender.putExtra("Magnitude", IntentMagnitude);
        if (IntentURLs.size() == 0)
            Toast.makeText(MainActivity.this, "No Data Available!!!", Toast.LENGTH_SHORT).show();
        else
            startActivity(intentSender);
    }

    public void showMap(View view) {
        Intent intentSender = new Intent(getApplicationContext(), MapsActivity.class);
        startActivity(intentSender);
    }

    public void showNotification(Location location) {
        double latitude = location.getLatitude();
        double longitude = location.getLongitude();
        Cursor c = drDB.rawQuery("SELECT type, area,latitude, longitude FROM events", null);
        int typeId = c.getColumnIndex("type");
        int areaId = c.getColumnIndex("area");
        int latitudeId = c.getColumnIndex("latitude");
        int longitudeId = c.getColumnIndex("longitude");
        if (c.moveToFirst())
            while (c.moveToNext()) {
                int dist = (int) distance(Double.parseDouble(c.getString(latitudeId)), latitude, Double.parseDouble(c.getString(longitudeId)), longitude);
                int str = sharedPreferences.getInt("range", 0);
                if (str == 0)
                    str = 1000;
                if (dist < str) {
                    Intent myIntent = new Intent(getApplicationContext(), MapsActivity.class);
                    myIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                    PendingIntent pendingIntent = PendingIntent.getActivity(getApplicationContext(), 0, myIntent, 0);
                    Notification notification = new NotificationCompat.Builder(this, CHANNEL)
                            .setSmallIcon(R.drawable.logo)
                            .setPriority(NotificationCompat.PRIORITY_HIGH)
                            .setCategory(NotificationCompat.CATEGORY_MESSAGE)
                            .setDefaults(Notification.DEFAULT_SOUND)
                            .setAutoCancel(true)
                            .setContentIntent(pendingIntent)
                            .setTicker("Notification!")
                            .setContentTitle("Disaster Alert")
                            .setContentText(c.getString(typeId) + " in " + c.getString(areaId))
                            .build();
                    notificationManager.notify(dist, notification);
                }
            }
    }
    public static double distance(double lat1, double lat2, double lon1, double lon2) {
        lon1 = Math.toRadians(lon1);
        lon2 = Math.toRadians(lon2);
        lat1 = Math.toRadians(lat1);
        lat2 = Math.toRadians(lat2);
        double dlon = lon2 - lon1;
        double dlat = lat2 - lat1;
        double a = Math.pow(Math.sin(dlat / 2), 2) + Math.cos(lat1) * Math.cos(lat2) * Math.pow(Math.sin(dlon / 2), 2);
        double c = 2 * Math.asin(Math.sqrt(a));
        return (c * 6371);
    }
}
