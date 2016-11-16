package com.brandonbielicki.spartaride;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.GridView;
import android.widget.TextView;

public class RouteSelect extends Activity {
    private String route;
    private Button cancelButton;
    private Button selectButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_route_select);

        selectButton = (Button)findViewById(R.id.select);
        selectButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent();
                intent.putExtra("route", route);
                setResult(RESULT_OK, intent);
                finish();
            }
        });
        cancelButton = (Button)findViewById(R.id.cancel);
        cancelButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(getApplicationContext(), MainActivity.class);
                startActivity(intent);
            }
        });

        final GridView gridview = (GridView) findViewById(R.id.gridview);
        gridview.setNumColumns(4);
        String[] items = getResources().getStringArray(R.array.routes_array);
        gridview.setAdapter(new ButtonAdapter(this, items));
        gridview.setChoiceMode(AbsListView.CHOICE_MODE_SINGLE);
        gridview.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            public void onItemClick(AdapterView<?> parent, View v, int position, long id) {
                route = (String)((TextView) v).getText();
            }
        });
    }

    public class ButtonAdapter extends BaseAdapter {
        private Context context;
        private final String[] textViewValues;

        public ButtonAdapter(Context context, String[] textViewValues) {
            this.context = context;
            this.textViewValues = textViewValues;
        }

        public View getView(int position, View convertView, ViewGroup parent) {

            LayoutInflater inflater = (LayoutInflater) context
                    .getSystemService(Context.LAYOUT_INFLATER_SERVICE);

            View gridView;

            if (convertView == null) {

                gridView = new View(context);

                gridView = inflater.inflate(R.layout.item, null);

                TextView textView = (TextView) gridView
                        .findViewById(R.id.grid_item_label);
                textView.setText(textViewValues[position]);
            } else {
                gridView = (View) convertView;
            }

            return gridView;
        }

        @Override
        public int getCount() {
            return textViewValues.length;
        }

        @Override
        public Object getItem(int position) {
            return null;
        }

        @Override
        public long getItemId(int position) {
            return 0;
        }
    }
}
