package com.scan.qrscanner.qrscanner;

import android.app.Activity;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.widget.GridView;
import android.util.Log;
import java.util.ArrayList;

//This activity will display the small image chunks into a grid view
public class ChunkedActivity extends Activity {
    private static String logtag = "QRscanner";

    public void onCreate(Bundle bundle){

        super.onCreate(bundle);
        setContentView(R.layout.activity_chunked);
        Log.v(logtag,"inside image chunks");
        //Getting the image chunks sent from the previous activity
        ArrayList<Bitmap> imageChunks = getIntent().getParcelableArrayListExtra("image chunks");

        //Getting the grid view and setting an adapter to it
        GridView grid = (GridView) findViewById(R.id.gridview);
        grid.setAdapter(new ImageAdapter(this, imageChunks));
        grid.setNumColumns((int) Math.sqrt(imageChunks.size()));
    }
}
