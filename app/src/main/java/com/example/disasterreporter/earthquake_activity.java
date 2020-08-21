package com.example.disasterreporter;

import android.content.Intent;
import android.net.UrlQuerySanitizer;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebView;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import java.util.ArrayList;

public class earthquake_activity extends AppCompatActivity {
    ArrayList<String> URLs = new ArrayList<>();
    ArrayList<String> Area = new ArrayList<>();
    ArrayList<String> Magnitude = new ArrayList<>();
    ArrayList<String> DateTime = new ArrayList<>();
    String id;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_earthquake_activity);
        ListView view = findViewById(R.id.listView);
        Intent intentReceiver = getIntent();
        id = intentReceiver.getStringExtra("id");
        URLs = intentReceiver.getStringArrayListExtra("URLs");
        Area = intentReceiver.getStringArrayListExtra("Area");
        DateTime = intentReceiver.getStringArrayListExtra("DateTime");
        Magnitude = intentReceiver.getStringArrayListExtra("Magnitude");
        CustomListAdapter customListAdapter = new CustomListAdapter();
        view.setAdapter(customListAdapter);
        view.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                WebView webView=findViewById(R.id.webView);
                webView.getSettings().setJavaScriptEnabled(true);
                webView.loadUrl(URLs.get(i));
            }
        });
    }

    public class CustomListAdapter extends BaseAdapter {
        @Override
        public int getCount() {
            return URLs.size();
        }

        @Override
        public Object getItem(int position) {
            return null;
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int i, View view, ViewGroup viewGroup) {
            view = getLayoutInflater().inflate(R.layout.custom_list_layout, null);
            ImageView imageView = view.findViewById(R.id.icon);
            TextView area = view.findViewById(R.id.area);
            TextView datetime = view.findViewById(R.id.datetime);
            TextView magnitude = view.findViewById(R.id.magnitude);
            switch (id) {
                case "EQ":
                    imageView.setImageResource(R.drawable.earthquake);
                    break;
                case "TC":
                    imageView.setImageResource(R.drawable.cyclone);
                    break;
                case "FL":
                    imageView.setImageResource(R.drawable.flood);
                    break;
                case "VO":
                    imageView.setImageResource(R.drawable.volcano);
                    break;
                default:
                    imageView.setImageResource(R.drawable.drought);
                    break;
            }
            area.setText(Area.get(i));
            datetime.setText(DateTime.get(i));
            if (Magnitude.get(i) != null)
                magnitude.setText(Magnitude.get(i));
            return view;
        }
    }
}