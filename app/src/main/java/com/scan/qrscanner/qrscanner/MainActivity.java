package com.scan.qrscanner.qrscanner;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import com.google.zxing.BinaryBitmap;
import com.google.zxing.LuminanceSource;
import com.google.zxing.MultiFormatReader;
import com.google.zxing.RGBLuminanceSource;
import com.google.zxing.Reader;
import com.google.zxing.Result;
import com.google.zxing.common.HybridBinarizer;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.ArrayList;

public class MainActivity extends Activity {
    Button button;
    ImageView imageView;
    static final int CAM_REQUEST = 1;
    private static String logtag = "QR scanner";
    private Uri imageUri;
    File qr_folder = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES),"QRScanner");

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        try {
            Log.v(logtag, "Starting QR scanner");
            super.onCreate(savedInstanceState);
            setContentView(R.layout.activity_main);
            button = (Button) findViewById(R.id.myButton);
            imageView = (ImageView) findViewById(R.id.image_view);
            button.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Log.v(logtag,"You have clicked a button");
                    Intent camera_intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                    File file = getFile();
                    Log.v(logtag,"Returning from getFile");
                    imageUri = Uri.fromFile(file);
                    Log.v(logtag, imageUri.toString());
                    camera_intent.putExtra(MediaStore.EXTRA_OUTPUT, Uri.fromFile(file));
                    Log.v(logtag, "Starting camera intent");
                    startActivityForResult(camera_intent, CAM_REQUEST);
                    Log.v(logtag,"Success");
                }
            });
        }catch (Exception e){
            Log.e(logtag, e.toString());
        }
    }

    private File getFile(){
        Log.v(logtag, "inside getFile...");
        File folder = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES),"QRScanner");
        if(!folder.exists()){
            Log.v(logtag, "Making directory...");
            boolean make = folder.mkdir();
            Log.v(logtag, "directory selected "+make);
        }
        File image_file = new File(folder, "multipleQR.jpg");
        Log.v(logtag, "Image name selected");
        return image_file;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data){
        if(resultCode == RESULT_OK) {
            Log.v(logtag, "Inside camera intent result");
            Uri selectedImage = imageUri; //name and path of the image
            getContentResolver().notifyChange(selectedImage, null);
            //pull captured image
            ImageView imageView = (ImageView) findViewById(R.id.image_view);
            ContentResolver cr = getContentResolver();
            boolean imgResult;
            //try to get bitmap of the image
            Log.v(logtag, "Getting bitmap of the image");
            try {
                Bitmap bitmap = MediaStore.Images.Media.getBitmap(cr, selectedImage);
                imageView.setImageBitmap(bitmap);
                Log.v(logtag, "Calling the crop function");
                ArrayList<Bitmap> chunkedImages = cropImage(imageView, 4);
                Log.v(logtag, "Size : " + chunkedImages.size());
                if (bitmap != null && !bitmap.isRecycled()) {
                    Log.v(logtag, "recycling bitmap after getting image chunks");
                    bitmap.recycle();
                    bitmap = null;
                }
                for (int i = 0; i < chunkedImages.size(); i++) {
                    Log.v(logtag, "Saving image" + i);
                    imgResult = saveImage(chunkedImages.get(i), i + 1);
                    Log.v(logtag, "Result" + imgResult);
                    InputStream is = new BufferedInputStream(new FileInputStream(String.valueOf(qr_folder) + "/QRCode" + (i + 1) + ".png"));
                    Bitmap bitmap2 = BitmapFactory.decodeStream(is);
                    String decoded = scanImage(bitmap2);
                    Log.i("QrTest", "Decoded string=" + decoded);
                    // The string decoded will contain the value of each QR code
                    if (decoded != null) {
                        Toast.makeText(MainActivity.this, decoded, Toast.LENGTH_LONG).show();
                    }
                    if (bitmap2 != null && !bitmap2.isRecycled()) {
                        Log.v(logtag, "Recycling bitmap2");
                        bitmap2.recycle();
                        bitmap2 = null;
                    }
                    Log.v(logtag, "Scanned an image");
                }
                Log.v(logtag, "Done with scanning");

//            Log.v(logtag, String.valueOf(chunkedImages.size()));
//            for(int i=0;i<chunkedImages.size();i++){
//                Log.v(logtag,"getting "+i+"th image");
//                ByteArrayOutputStream stream = new ByteArrayOutputStream();
//                chunkedImages.get(i).compress(Bitmap.CompressFormat.PNG, 100, stream);
//                byte[] bytes = stream.toByteArray();
//                intent.putExtra("BMP",bytes);
//            }
//
//            //Start a new activity to show these chunks into a grid
//
//            Log.v(logtag,"Starting activity");
//            startActivity(intent);
            } catch (Exception e) {
                Log.e(logtag, e.toString());
            }
        }
        Log.v(logtag, "Calling finish");
//        finish();
    }



    private ArrayList<Bitmap> cropImage(ImageView image, int chunkNumbers) {
        Log.v(logtag,"Inside cropImage");
        //For the number of rows and columns of the grid to be displayed
        int rows,cols;

        //For height and width of the small image chunks
        int chunkHeight,chunkWidth;

        //To store all the small image chunks in bitmap format in this list
        ArrayList<Bitmap> chunkedImages = new ArrayList<Bitmap>(chunkNumbers);
        Log.v(logtag,"created the arrayList");
        Log.v(logtag, String.valueOf(chunkedImages.size()));
        //Getting the scaled bitmap of the source image
        Drawable drawable = image.getDrawable();
        if (drawable != null && drawable instanceof BitmapDrawable) {
            Log.v(logtag, "inside drawable");
            BitmapDrawable bitmapDrawable = (BitmapDrawable) drawable;
            Bitmap bitmap = bitmapDrawable.getBitmap();
            //        bitmapCanvas = new Canvas(bitmap);
            Bitmap scaledBitmap = Bitmap.createScaledBitmap(bitmap, bitmap.getWidth(), bitmap.getHeight(), true);
            Log.v(logtag, "got the bitmap of the image");
            rows = cols = (int) Math.sqrt(chunkNumbers);
            chunkHeight = bitmap.getHeight() / rows;
            chunkWidth = bitmap.getWidth() / cols;
            Log.v(logtag, "chunk width and height are :" + chunkHeight + "and" + chunkWidth);

            //xCoord and yCoord are the pixel positions of the image chunks
            int yCoord = 0;
            for (int x = 0; x < rows; x++) {
                int xCoord = 0;
                for (int y = 0; y < cols; y++) {
                    chunkedImages.add(Bitmap.createBitmap(scaledBitmap, xCoord, yCoord, chunkWidth, chunkHeight));
                    xCoord += chunkWidth;
                }
                yCoord += chunkHeight;
            }

            Log.v(logtag, "Added the chunk images");
            if (bitmap != null && !bitmap.isRecycled()) {
                Log.v(logtag, "recycling bitmap");
                bitmap.recycle();
                bitmap = null;
            }
        }
        return chunkedImages;
    }

    private boolean saveImage(Bitmap image, int index){
        try {
            // Use the compress method on the Bitmap object to write image to
            // the OutputStream
            FileOutputStream fos = new FileOutputStream(new File(String.valueOf(qr_folder)+"/QRCode"+index+".png"), false);

            // Writing the bitmap to the output stream
            image.compress(Bitmap.CompressFormat.PNG, 100, fos);
            fos.close();
            if (image != null && !image.isRecycled()) {
                image.recycle();
                image = null;
            }
            return true;
        } catch (Exception e) {
            Log.e("saveToInternalStorage()", e.getMessage());
            return false;
        }
    }

    private static String scanImage(Bitmap bMap){
        String contents = null;

        int[] intArray = new int[bMap.getWidth()*bMap.getHeight()];
        //copy pixel data from the Bitmap into the 'intArray' array
        bMap.getPixels(intArray, 0, bMap.getWidth(), 0, 0, bMap.getWidth(), bMap.getHeight());

        LuminanceSource source = new RGBLuminanceSource(bMap.getWidth(), bMap.getHeight(), intArray);
        BinaryBitmap bitmp = new BinaryBitmap(new HybridBinarizer(source));

        Reader reader = new MultiFormatReader();
        try {
            Result result = reader.decode(bitmp);
            contents = result.getText();
        }
        catch (Exception e) {
            Log.e("QrTest", "Error decoding barcode", e);
        }
        if (bMap != null && !bMap.isRecycled()) {
            bMap.recycle();
            bMap = null;
        }
        return contents;
    }
}
