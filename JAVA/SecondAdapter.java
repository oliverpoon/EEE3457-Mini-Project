package com.example.project.Activitis;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.example.project.R;
import com.squareup.picasso.Picasso;

import java.util.ArrayList;

public class SecondAdapter extends BaseAdapter {
    private static final String TAG = "SecondAdapter";
    private ArrayList<SecondDomain> items;
    private Context context;
    private LayoutInflater inflater;

    public SecondAdapter(Context context, ArrayList<SecondDomain> items) {
        this.context = context;
        this.items = items;
        this.inflater = LayoutInflater.from(context);
    }

    @Override
    public int getCount() {
        return items.size();
    }

    @Override
    public Object getItem(int position) {
        return items.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder holder;

        if (convertView == null) {
            convertView = inflater.inflate(R.layout.list_item, parent, false);
            holder = new ViewHolder();

            holder.dayTxt = convertView.findViewById(R.id.dayTxt);
            holder.statusTxt = convertView.findViewById(R.id.statusTxt);
            holder.lowTxt = convertView.findViewById(R.id.lowTxt);
            holder.highTxt = convertView.findViewById(R.id.highTxt);
            holder.pic2 = convertView.findViewById(R.id.pic2);

            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }

        SecondDomain item = items.get(position);

        // Set the day of the week
        holder.dayTxt.setText(item.getDay());

        // Set full weather description (not shortened)
        holder.statusTxt.setText(item.getStatus());

        // Set the temperature (without the degree symbol, as the XML already has that)
        holder.highTxt.setText(String.valueOf(item.getHighTemp()));
        holder.lowTxt.setText(String.valueOf(item.getLowTemp()));

        // Use Picasso to load the official Observatory icon
        String iconUrl = item.getPicPath(); // Now this is the URL
        Log.d(TAG, "Loading HKO icon: " + iconUrl + " for day " + item.getDay());

        Picasso.get()
                .load(iconUrl)
                .resize(100, 100)                      // Resize Icons
                .centerInside()                        // Keep Proportion
                .into(holder.pic2);

        return convertView;
    }

    static class ViewHolder {
        TextView dayTxt, statusTxt, lowTxt, highTxt;
        ImageView pic2;
    }
}