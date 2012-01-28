package com.fawepark.veralert;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import android.app.Activity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.TextView;

public class DBViewAdapter extends ArrayAdapter<DBTuple> {
    static public final int BY_MESSAGE = 1;
    static public final int BY_DATE = 2;
    
    private List<DBTuple> values;
    private final Activity context;
    private int SortOrder;
    private DBSource dbSource;

    public boolean EditState;

    static public DBViewAdapter GetAdapter(Activity context) {
        DBSource src = new DBSource(context);
        src.open(false);
        List<DBTuple> vals = src.getAllData();
        src.close();
        DBViewAdapter Result = new DBViewAdapter(context, vals);
        Result.dbSource = src;
        return(Result);
    }

    private DBViewAdapter(Activity context, List<DBTuple> values) {
        super(context, R.layout.rowlayout, values);
        this.context = context;
        this.values = values;
        SetSortOrder(BY_DATE);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        LayoutInflater inflater = context.getLayoutInflater();
        DBTuple t = values.get(position);
        View rowView;
        if (convertView == null) {
            rowView = inflater.inflate(R.layout.rowlayout, null);
        } else {
            rowView = convertView;
        }
        TextView tv = (TextView) rowView.findViewById(R.id.row_message);
        tv.setText(t.Message);
        tv = (TextView) rowView.findViewById(R.id.row_alerttype);
        tv.setText("" + t.AlertType);
        tv = (TextView) rowView.findViewById(R.id.row_timestamp);
        tv.setText(t.TimeString());
        CheckBox cb = (CheckBox) rowView.findViewById(R.id.row_check);
        cb.setVisibility(EditState ? View.VISIBLE : View.GONE);
        cb.setTag(t);
        cb.setOnCheckedChangeListener(null);
        cb.setChecked(t.Selected);
        cb.setOnCheckedChangeListener(new OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton mcb, boolean ischecked) {
                DBTuple tag = (DBTuple) mcb.getTag(); 
                tag.Selected = ischecked;               
            }
        });
        
        return rowView;
    }
    
    public void SetEdit(boolean EditState) {
        this.EditState = EditState;
    }
    
    public List<DBTuple> GetValues() {
        return values;
    }
    
    public DBTuple Get(int i) {
        return values.get(i);
    }
    
    public void SetSortOrder(int v) {
        SortOrder = v;
        switch (SortOrder) {
          case BY_MESSAGE:
            Collections.sort(values, new Comparator<DBTuple>() {
              @Override
              public int compare(DBTuple t1, DBTuple t2) {
                  int c = t1.Message.compareTo(t2.Message);
                  if (c == 0) {
                      if (t1.TimeStamp == t2.TimeStamp) return 0;
                      if (t1.TimeStamp > t2.TimeStamp) { 
                          return -1;
                      } else {
                          return 1;
                      }
                  }
                  return c;
              }
            });
            break;
          case BY_DATE:
            Collections.sort(values, new Comparator<DBTuple>() {
                @Override
                public int compare(DBTuple t1, DBTuple t2) {
                    if (t1.TimeStamp == t2.TimeStamp) return 0;
                    if (t1.TimeStamp > t2.TimeStamp) { 
                        return -1;
                    } else {
                        return 1;
                    }
                }
            });
            break;
        }
    }
    
    public void DeleteSelected() {
        dbSource.open(true);
        for (int i = 0; i < values.size(); i++) {
            DBTuple t = values.get(i);
            if (t.Selected) {
                dbSource.Remove(t);
                this.remove(t);
                i--;
            }
        }
        dbSource.close();
    }
    
    public void Refresh() {
        dbSource.open(false);
        dbSource.GetNewData(values);
        dbSource.close();
    }
}
