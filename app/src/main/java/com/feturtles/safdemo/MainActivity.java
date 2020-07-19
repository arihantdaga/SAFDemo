package com.feturtles.safdemo;

import androidx.appcompat.app.AppCompatActivity;

import android.app.Activity;
import android.content.ClipData;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Base64;
import android.util.Log;
import android.view.View;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

public class MainActivity extends AppCompatActivity {
    private static final int CREATE_REQUEST_CODE = 40;
    private static final int OPEN_REQUEST_CODE = 41;
    private static final int SAVE_REQUEST_CODE = 42;
    private String TAG = "SAFDEMO";
    public Boolean multiple = true;
    private Map<String, Integer> fileNames = new HashMap<String, Integer>();
    private Cursor imagecursor, actualimagecursor;
    private int maxImages = 20;
    private int maxImageCount = 20;

    private int desiredWidth = 800;
    private int desiredHeight = 800;
    private int quality = 80;
    private OutputType outputType = OutputType.fromValue(0);


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
    }

    public void newFile(View view) {
        Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);

        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("text/plain");
        intent.putExtra(Intent.EXTRA_TITLE, "newfile.txt");

        startActivityForResult(intent, CREATE_REQUEST_CODE);
    }

    public void openFile(View view) {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("image/*");
        if (this.multiple) {
            intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
        }
        startActivityForResult(intent, OPEN_REQUEST_CODE);
    }

    public void saveFile(View view) {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("text/plain");

        startActivityForResult(intent, SAVE_REQUEST_CODE);
    }


    public void onActivityResult(int requestCode, int resultCode,
                                 Intent resultData) {
        super.onActivityResult(requestCode, resultCode, resultData);
        if (resultCode == Activity.RESULT_OK) {
            if (requestCode == CREATE_REQUEST_CODE) {
                if (resultData != null) {
                    Log.d(TAG, "CREATE_REQUEST_CODE Received");
                }
            } else if (requestCode == SAVE_REQUEST_CODE) {

                if (resultData != null) {
                    Uri currentUri =
                            resultData.getData();
//                    writeFileContent(currentUri);
                    Log.d(TAG, "Content URI: " + currentUri.toString());
                }
            } else if (requestCode == OPEN_REQUEST_CODE) {

                if (resultData != null) {
                    if (this.multiple) {
                        final ClipData clipData = resultData.getClipData();
                        int takeFlags = resultData.getFlags();
                        takeFlags &= (Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
                        ArrayList<Uri> uris = new ArrayList<Uri>();
                        for (int i = 0; i < clipData.getItemCount(); i++) {
                            ClipData.Item item = clipData.getItemAt(i);
                            Uri dataUri = item.getUri();
                            if (dataUri != null) {
                                uris.add(dataUri);
                                Log.d(TAG, "File URI : " + dataUri.toString());
                            }
                        }
                        processFiles(uris);
                    } else {
                        Uri currentUri = resultData.getData();
                        if (currentUri != null) {
                            Log.d(TAG, "Open File, Content URI: " + currentUri.toString());
                        } else {
                            Log.e(TAG, "ContentUri is NUll");
                        }
                    }
                }


//                    try {
//                        String content =
//                                readFileContent(currentUri);
//                        textView.setText(content);
//                    } catch (IOException e) {
//                        // Handle error here
//                    }

            } else {
                Log.d(TAG, "Received Code : " + String.valueOf(requestCode));
            }

        }
    }

    private void processFiles(ArrayList<Uri> uris) {
        if (uris.isEmpty()) {
//            setResult(RESULT_CANCELED);
//            progress.dismiss();
//            finish();
        } else {
            setRequestedOrientation(getResources().getConfiguration().orientation); //prevent orientation changes during processing
//            fileNames.clear();
//            for (int i = 0; i < uris.size(); i++) {
//                fileNames.put(uris.get(i), 0);
//            }
            new ResizeImagesTask().execute(uris);
        }
    }


    private class ResizeImagesTask extends AsyncTask<ArrayList<Uri>, Void, ArrayList<String>> {
        private Exception asyncTaskError = null;

        @Override
        protected ArrayList<String> doInBackground(ArrayList<Uri>... fileSets) {
//            Set<Map.Entry<String, Integer>> fileNames = fileSets[0];
//            Uri filenames = fileSets[0];
            ArrayList<String> al = new ArrayList<String>();
            try {
//                Iterator<Map.Entry<String, Integer>> i = fileNames.iterator();
//                Iterator<Uri> i = fileSets
//                fileSets.
//                ArrayList<Uri> list
//                        = new ArrayList<>();
//                list.iterator();
                ArrayList<Uri> urisCopy = fileSets[0];
                Iterator<Uri> i = urisCopy.iterator();
                Bitmap bmp;
                while (i.hasNext()) {
                    Uri fileuri = i.next();
//                    String fileuri = imageInfo;
                    File file = new File(fileuri.toString()); // TODO: IT WONT WORK.
                    int rotate = 0; // TODO:
                    BitmapFactory.Options options = new BitmapFactory.Options();
                    options.inSampleSize = 1;
                    options.inJustDecodeBounds = true;
                    InputStream is = getContentResolver().openInputStream(fileuri);
                    BitmapFactory.decodeStream(is); // TODO: Earlier it was options.
                    int width = options.outWidth;
                    int height = options.outHeight;
                    float scale = calculateScale(width, height);

                    if (scale < 1) {
                        int finalWidth = (int) (width * scale);
                        int finalHeight = (int) (height * scale);
                        int inSampleSize = calculateInSampleSize(options, finalWidth, finalHeight);
                        options = new BitmapFactory.Options();
                        options.inSampleSize = inSampleSize;

                        try {
                            bmp = this.tryToGetBitmap(fileuri, options, rotate, true);
                        } catch (OutOfMemoryError e) {
                            options.inSampleSize = calculateNextSampleSize(options.inSampleSize);
                            try {
                                bmp = this.tryToGetBitmap(fileuri, options, rotate, false);
                            } catch (OutOfMemoryError e2) {
                                throw new IOException("Unable to load image into memory.");
                            }
                        }
                    } else {
                        try {
                            bmp = this.tryToGetBitmap(fileuri, null, rotate, false);
                        } catch (OutOfMemoryError e) {
                            options = new BitmapFactory.Options();
                            options.inSampleSize = 2;

                            try {
                                bmp = this.tryToGetBitmap(fileuri, options, rotate, false);
                            } catch (OutOfMemoryError e2) {
                                options = new BitmapFactory.Options();
                                options.inSampleSize = 4;

                                try {
                                    bmp = this.tryToGetBitmap(fileuri, options, rotate, false);
                                } catch (OutOfMemoryError e3) {
                                    throw new IOException("Unable to load image into memory.");
                                }
                            }
                        }
                    }

                    if (outputType == OutputType.FILE_URI) {
                        file = storeImage(bmp, file.getName(), "");
                        al.add(Uri.fromFile(file).toString());

                    } else if (outputType == OutputType.BASE64_STRING) {
                        al.add(getBase64OfImage(bmp));
                    }
                }
                return al;
            } catch (IOException e) {
                try {
                    asyncTaskError = e;
                    for (int i = 0; i < al.size(); i++) {
                        URI uri = new URI(al.get(i));
                        File file = new File(uri);
                        file.delete();
                    }
                } catch (Exception ignore) {
                }

                return new ArrayList<String>();
            }
        }

        @Override
        protected void onPostExecute(ArrayList<String> al) {
            Intent data = new Intent();

            if (asyncTaskError != null) {
                Bundle res = new Bundle();
                res.putString("ERRORMESSAGE", asyncTaskError.getMessage());
                data.putExtras(res);
                setResult(RESULT_CANCELED, data);

            } else if (al.size() > 0) {
                Bundle res = new Bundle();
                res.putStringArrayList("MULTIPLEFILENAMES", al);

                if (imagecursor != null) {
                    res.putInt("TOTALFILES", imagecursor.getCount());
                }

                int sync = ResultIPC.get().setLargeData(res);
                data.putExtra("bigdata:synccode", sync);
                setResult(RESULT_OK, data);

            } else {
                setResult(RESULT_CANCELED, data);
            }
            Log.d(TAG, "TASK FINISH");

//            progress.dismiss();
//            finish();
        }

        private Bitmap tryToGetBitmap(Uri fileuri,
                                      BitmapFactory.Options options,
                                      int rotate,
                                      boolean shouldScale) throws IOException, OutOfMemoryError {
            Bitmap bmp;
//            Uri temp3 = Uri.parse(file.getPath());
//            Uri.Builder c = new Uri.Builder();
//            c.appendPath(file.getPath());
//            Uri temp3 = c.build();
//            InputStream inputStream =
//                    getContentResolver().openInputStream(temp3);
//            BufferedReader reader =
//                    new BufferedReader(new InputStreamReader(
//                            inputStream));
//            StringBuilder stringBuilder = new StringBuilder();
//            String currentline;
//            while ((currentline = reader.readLine()) != null) {
//                stringBuilder.append(currentline + "\n");
//            }
//            inputStream.close();
//            String something = stringBuilder.toString();

            InputStream is = getContentResolver().openInputStream(fileuri);
            if (options == null) {
//                String temp = file.getAbsolutePath();
//                String temp2 = file.getPath();
//                bmp = BitmapFactory.decodeFile(fileuri);
                String Hello = "content:////com.android.providers.media.documents/document/image%3A245176";
                bmp = BitmapFactory.decodeStream(is);
            } else {
                bmp = BitmapFactory.decodeStream(is);
//                bmp = BitmapFactory.decodeFile(fileuri, options);
            }

            if (bmp == null) {
                throw new IOException("The image file could not be opened.");
            }

            if (options != null && shouldScale) {
                float scale = calculateScale(options.outWidth, options.outHeight);
                bmp = this.getResizedBitmap(bmp, scale);
            }

            if (rotate != 0) {
                Matrix matrix = new Matrix();
                matrix.setRotate(rotate);
                bmp = Bitmap.createBitmap(bmp, 0, 0, bmp.getWidth(), bmp.getHeight(), matrix, true);
            }

            return bmp;
        }

        /*
         * The following functions are originally from
         * https://github.com/raananw/PhoneGap-Image-Resizer
         *
         * They have been modified by Andrew Stephan for Sync OnSet
         *
         * The software is open source, MIT Licensed.
         * Copyright (C) 2012, webXells GmbH All Rights Reserved.
         */
        private File storeImage(Bitmap bmp, String fileName, String ext) throws IOException {
//            RealPathUtil.getRealPath(fileName)
//            int index = fileName.lastIndexOf('.');
//            String name = fileName.substring(0, index);
//            String ext = fileName.substring(index);
            String name = fileName;
            File file = File.createTempFile("tmp_" + name, ext);
            OutputStream outStream = new FileOutputStream(file);

            if (ext.compareToIgnoreCase(".png") == 0) {
                bmp.compress(Bitmap.CompressFormat.PNG, quality, outStream);
            } else {
                bmp.compress(Bitmap.CompressFormat.JPEG, quality, outStream);
            }

            outStream.flush();
            outStream.close();
            return file;
        }

        private Bitmap getResizedBitmap(Bitmap bm, float factor) {
            int width = bm.getWidth();
            int height = bm.getHeight();
            // create a matrix for the manipulation
            Matrix matrix = new Matrix();
            // resize the bit map
            matrix.postScale(factor, factor);
            // recreate the new Bitmap
            return Bitmap.createBitmap(bm, 0, 0, width, height, matrix, false);
        }

        private String getBase64OfImage(Bitmap bm) {
            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            bm.compress(Bitmap.CompressFormat.JPEG, quality, byteArrayOutputStream);
            byte[] byteArray = byteArrayOutputStream.toByteArray();
            return Base64.encodeToString(byteArray, Base64.NO_WRAP);
        }
    }

    private int calculateInSampleSize(BitmapFactory.Options options, int reqWidth, int reqHeight) {
        // Raw height and width of image
        final int height = options.outHeight;
        final int width = options.outWidth;
        int inSampleSize = 1;

        if (height > reqHeight || width > reqWidth) {
            final int halfHeight = height / 2;
            final int halfWidth = width / 2;

            // Calculate the largest inSampleSize value that is a power of 2 and keeps both
            // height and width larger than the requested height and width.
            while ((halfHeight / inSampleSize) > reqHeight && (halfWidth / inSampleSize) > reqWidth) {
                inSampleSize *= 2;
            }
        }

        return inSampleSize;
    }

    private int calculateNextSampleSize(int sampleSize) {
        double logBaseTwo = (int) (Math.log(sampleSize) / Math.log(2));
        return (int) Math.pow(logBaseTwo + 1, 2);
    }

    private float calculateScale(int width, int height) {
        float widthScale = 1.0f;
        float heightScale = 1.0f;
        float scale = 1.0f;
        if (desiredWidth > 0 || desiredHeight > 0) {
            if (desiredHeight == 0 && desiredWidth < width) {
                scale = (float) desiredWidth / width;

            } else if (desiredWidth == 0 && desiredHeight < height) {
                scale = (float) desiredHeight / height;

            } else {
                if (desiredWidth > 0 && desiredWidth < width) {
                    widthScale = (float) desiredWidth / width;
                }

                if (desiredHeight > 0 && desiredHeight < height) {
                    heightScale = (float) desiredHeight / height;
                }

                if (widthScale < heightScale) {
                    scale = widthScale;
                } else {
                    scale = heightScale;
                }
            }
        }

        return scale;
    }

    enum OutputType {

        FILE_URI(0), BASE64_STRING(1);

        int value;

        OutputType(int value) {
            this.value = value;
        }

        public static OutputType fromValue(int value) {
            for (OutputType type : OutputType.values()) {
                if (type.value == value) {
                    return type;
                }
            }
            throw new IllegalArgumentException("Invalid enum value specified");
        }
    }
}
