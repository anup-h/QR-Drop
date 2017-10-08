package com.example.anuphiremath.testing;

import android.app.Activity;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.DataFormatException;
import java.util.zip.Deflater;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;
import java.util.zip.Inflater;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;

import static android.R.attr.name;
import static android.R.attr.text;

public class MainActivity extends Activity {
    List<String> strings;
    List<Bitmap> QRcodes;
    int count = 0;
    public static String toHexString(byte[] bytes) {
        StringBuilder hexString = new StringBuilder();

        for (int i = 0; i < bytes.length; i++) {
            String hex = Integer.toHexString(0xFF & bytes[i]);
            if (hex.length() == 1) {
                hexString.append('0');
            }
            hexString.append(hex);
        }

        return hexString.toString();
    }

    private static int IMG_RESULT = 1;
    String ImageDecode;
    ImageView compressed;
    Button LoadImage;
    Button NextCode;
    Intent intent;
    String[] FILE;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        compressed = (ImageView) findViewById(R.id.compressed);
        LoadImage = (Button) findViewById(R.id.button1);
        //NextCode = (Button)findViewById(R.id.button2);
        LoadImage.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                //NextCode.setEnabled(true);
                intent = new Intent(Intent.ACTION_PICK,
                        android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);

                startActivityForResult(intent, IMG_RESULT);

            }
        });

        /*NextCode.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                imageViewLoad.setImageBitmap(QRcodes.get(count));
                count++;
                if(count==QRcodes.size()){
                    count=0;
                    NextCode.setEnabled(false);
                }
            }
        });*/
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        try {

            if (requestCode == IMG_RESULT && resultCode == RESULT_OK
                    && null != data) {


                final Uri imageUri = data.getData();
                final InputStream imageStream = getContentResolver().openInputStream(imageUri);
                final Bitmap selectedImage = BitmapFactory.decodeStream(imageStream);

                String encoded = Base64.encodeToString(bitmapToByteArray(selectedImage, 40), Base64.DEFAULT);

                strings = new ArrayList<String>();
                QRcodes = new ArrayList<Bitmap>();
                int numCharsPerImage = 2950;
                for (int i = 0; i < encoded.length(); i += numCharsPerImage) {
                    strings.add(encoded.substring(i, Math.min(i + numCharsPerImage, encoded.length())));
                }

                for(int i = 0; i<strings.size(); i++){
                    QRcodes.add(encodeAsBitmap(strings.get(i)));
                }

                EditText textView = (EditText) findViewById(R.id.textView);
                textView.setText("Total codes: "+Integer.toString(strings.size()));
                Bitmap bm = decodeToBitmap(encoded);

                compressed.setImageBitmap(bm);
                LoadImage.setText("Next code: "+(count+2));
                LoadImage.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        count++;
                        LoadImage.setText("Next code: "+(count+2));
                        if(!(count>=strings.size())){

                            compressed.setImageBitmap(QRcodes.get(count));

                        }
                    }
                });
            }
        } catch (Exception e) {
            Toast.makeText(this, "Please try again", Toast.LENGTH_LONG)
                    .show();
        }

    }

    byte[] bitmapToByteArray(Bitmap b, int percent){
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        b.compress(Bitmap.CompressFormat.WEBP, percent, byteArrayOutputStream);
        return byteArrayOutputStream.toByteArray();
    }

    Bitmap encodeAsBitmap(String str) throws WriterException {
        BitMatrix result;
        try {
            result = new MultiFormatWriter().encode(str, BarcodeFormat.QR_CODE, 1000, 1000, null);
        } catch (IllegalArgumentException iae) {
            // Unsupported format
            return null;
        }

        int width = result.getWidth();
        int height = result.getHeight();
        int[] pixels = new int[width * height];
        for (int y = 0; y < height; y++) {
            int offset = y * width;
            for (int x = 0; x < width; x++) {
                pixels[offset + x] = result.get(x, y) ? 0xFF000000 : 0xFFFFFFFF;
            }
        }

        Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        bitmap.setPixels(pixels, 0, width, 0, 0, width, height);
        return bitmap;
    }

    Bitmap decodeToBitmap(String input) throws IOException, DataFormatException {
        byte[] uncompressedBytes = Base64.decode(input, Base64.DEFAULT);
        Bitmap bitmap = BitmapFactory.decodeByteArray(uncompressedBytes, 0, uncompressedBytes.length);
        return bitmap;
    }

    Bitmap decodeToBitmapBytes(byte[] b) throws IOException, DataFormatException {
        Bitmap bitmap = BitmapFactory.decodeByteArray(b, 0, b.length);
        return bitmap;
    }
}
