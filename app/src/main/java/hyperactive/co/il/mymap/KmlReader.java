package hyperactive.co.il.mymap;

import android.util.Log;

import com.google.android.gms.maps.model.LatLng;
import com.google.maps.android.PolyUtil;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by Tal on 05/04/2016.
 */
public class KmlReader {
    final private static String MY_LOG="myLog";

    public static List<LatLng> getPolygonPoints(final InputStream kmlStream){
        final List<LatLng> points=new ArrayList<>();
        new Thread(){
            @Override
            public void run() {
                super.run();
                try {
                    XmlPullParserFactory factory=XmlPullParserFactory.newInstance();
                    XmlPullParser parser = factory.newPullParser();
                    parser.setInput(new InputStreamReader(kmlStream));
                    int eventType=parser.getEventType();
                    String tagName;
                    while(eventType!=XmlPullParser.END_DOCUMENT)
                    {
                        if(eventType==XmlPullParser.START_TAG)
                        {
                            tagName=parser.getName();
                            if (tagName.equals("coordinates")){
                                if (parser.next()==XmlPullParser.TEXT){
                                    String coordinates="";
                                    coordinates=parser.getText().trim();
                                    Log.i(MY_LOG, "coordinates="+coordinates);
                                    String[] coords3d=coordinates.split(" ");
                                    String[] tempCoords;
                                    for (int i=0;i<coords3d.length;i++){
                                        Log.i(MY_LOG, "coordinate "+i+" :="+coords3d[i]);
                                        tempCoords=coords3d[i].split(",");
                                        points.add(new LatLng((Double.parseDouble(tempCoords[1])), Double.parseDouble(tempCoords[0])));
                                    }
                                }

                            }
                        }
                        eventType=parser.next();
                    }
                } catch (XmlPullParserException e) {
                    Log.e(MY_LOG, "xml error", e);
                } catch (IOException e) {
                    Log.e(MY_LOG, "IO error", e);
                }
            }
        }.start();
        return points;
    }
}
