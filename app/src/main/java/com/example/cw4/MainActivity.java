package com.example.cw4;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.os.Bundle;
import android.util.Log;

import com.google.api.client.extensions.android.http.AndroidHttp;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.vision.v1.Vision;
import com.google.api.services.vision.v1.VisionRequestInitializer;
import com.google.api.services.vision.v1.model.AnnotateImageRequest;
import com.google.api.services.vision.v1.model.BatchAnnotateImagesRequest;
import com.google.api.services.vision.v1.model.BatchAnnotateImagesResponse;
import com.google.api.services.vision.v1.model.EntityAnnotation;
import com.google.api.services.vision.v1.model.Feature;
import com.google.api.services.vision.v1.model.Image;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.List;

import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {
    private ArrayList<FoodItem> data;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        data = new ArrayList<>();
        FoodListAdapter adapter = new FoodListAdapter(this, R.layout.list_item, data);
        data.add(new FoodItem(R.drawable.p, ""));
        data.add(new FoodItem(R.drawable.b, ""));
        data.add(new FoodItem(R.drawable.c, ""));


        ListView lv = findViewById(R.id.my_list);
        lv.setAdapter(adapter);

        /*This is another way to create threads.*/
        for (int i = 0; i < data.size(); i++) {
            int index = i; // so we can pass in the image to the tester
            new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        myVisionTester(index, new VisionCallback() {
                            @Override
                            public void onResult(String result) {
                                data.get(index).tagText = result;
                                runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        adapter.notifyDataSetChanged();
                                    }
                                });
                            }
                        });
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }).start();
        }
    }
    void myVisionTester(int index, VisionCallback callback) throws IOException {
        // Ensure index is within bounds
        if (index >= data.size()) {
            return;
        }

        FoodItem item = data.get(index);

        // Convert the image resource to a Bitmap
        Bitmap bitmap = ((BitmapDrawable) getResources().getDrawable(item.imageResource)).getBitmap();
        ByteArrayOutputStream bout = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, 90, bout);
        Image myimage = new Image();
        myimage.encodeContent(bout.toByteArray());

        // PREPARE AnnotateImageRequest
        AnnotateImageRequest annotateImageRequest = new AnnotateImageRequest();
        annotateImageRequest.setImage(myimage);
        Feature f = new Feature();
        f.setType("LABEL_DETECTION");
        f.setMaxResults(5);
        List<Feature> lf = new ArrayList<>();
        lf.add(f);
        annotateImageRequest.setFeatures(lf);

        // BUILD the Vision
        HttpTransport httpTransport = AndroidHttp.newCompatibleTransport();
        GsonFactory jsonFactory = GsonFactory.getDefaultInstance();
        Vision.Builder builder = new Vision.Builder(httpTransport, jsonFactory, null);
        builder.setVisionRequestInitializer(new VisionRequestInitializer(""));
        Vision vision = builder.build();

        // CALL Vision.Images.Annotate
        BatchAnnotateImagesRequest batchAnnotateImagesRequest = new BatchAnnotateImagesRequest();
        List<AnnotateImageRequest> list = new ArrayList<>();
        list.add(annotateImageRequest);
        batchAnnotateImagesRequest.setRequests(list);
        Vision.Images.Annotate task = vision.images().annotate(batchAnnotateImagesRequest);
        BatchAnnotateImagesResponse response = task.execute();
        Log.v("MYTAG", response.toPrettyString());

        // Tags are filled sometimes in a weird order based on the ordering of the actual files in the drawable folder.
        // Depending on the naming of the file, will determine what index gets assigned to it.
        String tagText = "";
        if (response != null && response.getResponses() != null && !response.getResponses().isEmpty()) {
            List<EntityAnnotation> annotations = response.getResponses().get(0).getLabelAnnotations();
            if (annotations != null && !annotations.isEmpty()) {
                StringBuilder tagsBuilder = new StringBuilder();
                for (EntityAnnotation annotation : annotations) {
                    if (annotation.getScore() != null && annotation.getScore() >= 0.85) {
                        tagsBuilder.append(annotation.getDescription()).append(", ");
                    }
                    if (annotation.getScore() < 0.85) {
                        tagText = annotations.get(0).getDescription();
                    }
                }
                if (tagsBuilder.length() > 0) {
                    // Remove the trailing comma and space
                    tagText = tagsBuilder.substring(0, tagsBuilder.length() - 2);
                }
            }
        }
        // Use callback to send the result
        callback.onResult(tagText);
    }
}