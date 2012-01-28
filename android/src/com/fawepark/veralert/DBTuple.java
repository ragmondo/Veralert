package com.fawepark.veralert;

import java.sql.Date;
import java.text.DateFormat;

public class DBTuple {
    static private DateFormat dateformatter = DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.MEDIUM);
    static public int SequenceCnt = 1;
    public int ID = SequenceCnt++;
    public String Message;
    public int AlertType;
    public long TimeStamp;
    public boolean Selected = false;

    public String TimeString() {
        return dateformatter.format(new Date(TimeStamp));
    }
}
