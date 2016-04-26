package br.ufmg.dcc.verlab.maglogger;

/**
 * Created by h3ct0r on 25/4/16.
 */
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ReadingParser {
    String msg = null;
    float x = 0;
    float y = 0;
    float z = 0;
    float total = 0;
    float temp = 0;

    ReadingParser(){
        this.msg = "";
    }

    void updateData(String data){
        if(data == null) return;

        this.msg += data;
    }

    boolean isDataReady(){
        int i = this.msg.indexOf("\n");

        if(i > -1){
            String subStr = this.msg.substring(0, i);
            this.msg = this.msg.substring(i + 1, this.msg.length());
            return checkForData(subStr);
        }
        else{
            return false;
        }
    }

    boolean checkForData(String msg){
        Pattern pattern = Pattern.compile("(X|Y|Z|T|Temp)\\s+[\\d\\-\\.]{1,9}", Pattern.CASE_INSENSITIVE);
        Matcher matcher = pattern.matcher(msg);

        Map<String, Float> map = new HashMap<String, Float>();

        while (matcher.find()) {
            String groupStr = matcher.group();
            String g[] = groupStr.split("\\s+");
            if(g.length != 2) return false;

            String key = g[0];
            Float value = Float.parseFloat(g[1]);

            System.out.println("Key:" + key + " Value:" + value);
            map.put(key, value);
        }

        if(map.keySet().size() != 5){
            return false;
        }

        this.x = map.get("X");
        this.y = map.get("Y");
        this.z = map.get("Z");
        this.total = map.get("T");
        this.temp = map.get("Temp");

        return true;
    }

    float[] getData(){
        return new float[]{x, y, z, total, temp};
    }
}
