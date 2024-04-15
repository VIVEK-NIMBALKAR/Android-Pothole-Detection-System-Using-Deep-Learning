package com.example.pothole_detection_system;

import static android.content.ContentValues.TAG;

import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.media.ThumbnailUtils;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.Manifest;

import com.example.pothole_detection_system.ml.Model;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;

import org.tensorflow.lite.DataType;
import org.tensorflow.lite.support.tensorbuffer.TensorBuffer;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Objects;

public class detection_model extends AppCompatActivity {

    ImageView picture;
    TextView result, profileName;
    Button form , camera,logout2 ;
    FirebaseAuth mAuth;
    GoogleSignInOptions gso;
    GoogleSignInClient gsc;
    int imageSize = 32;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_detection_model);
        picture = findViewById(R.id.picture);
        result = findViewById(R.id.result);
        form = findViewById(R.id.form);
        camera = findViewById(R.id.btn_camera);
        profileName = findViewById(R.id.profileName2);
        logout2 = findViewById(R.id.LogoutBtn2);


        //email and logout
        GoogleSignInAccount account = GoogleSignIn.getLastSignedInAccount(this);
        mAuth = FirebaseAuth.getInstance();
        gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN).requestEmail().build();
        gsc = GoogleSignIn.getClient(this,gso);

        if (account!=null){
            String personName = account.getEmail();
            profileName.setText(personName);
        }
        // take photo using camera

        logout2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                signout();
            }
        });

        form.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = getIntent();
                String str = intent.getStringExtra("homeAdd");

                Intent b1 = new Intent(detection_model.this,Form.class);
                b1.putExtra("ADDRESS",str);
                startActivity(b1);
            }
        });
    }

    @Override
    protected void onStart() {
        super.onStart();

        camera.setOnClickListener(new View.OnClickListener() {
            @RequiresApi(api = Build.VERSION_CODES.M)
            @Override
            public void onClick(View v) {
                if(checkSelfPermission(Manifest.permission.CAMERA)== PackageManager.PERMISSION_GRANTED){
                    Intent cameraIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                    startActivityForResult(cameraIntent,32);
                }
                else{
                    requestPermissions(new String[]{Manifest.permission.CAMERA},100);
                }
            }
        });
    }

    // classification function
    public void  classifyImage(Bitmap image){
        try {
            Model model = Model.newInstance(getApplicationContext());

            // Creates inputs for reference.
            TensorBuffer inputFeature0 = TensorBuffer.createFixedSize(new int[]{1, 32, 32, 3}, DataType.FLOAT32);
            ByteBuffer byteBuffer = ByteBuffer.allocateDirect(4*imageSize*imageSize*3);
            byteBuffer.order(ByteOrder.nativeOrder());

            int[] intValues = new int[imageSize * imageSize];
            image.getPixels(intValues,0,image.getWidth(),0,0,image.getWidth(),image.getHeight());

            //iterate over each pixel and get RGB values and give to bytebuffer individually.
            int pixel=0;
            for (int i =0;i<imageSize;i++){
                for(int j=0;j<imageSize;j++){
                    int val = intValues[pixel++];
                    //extract values to get rgb that are between 0 and 1
                    // byteBuffer.putFloat(( (val>>16) & 0xFF)*(1.f/ 1)); (1.f/ 1) and (1.f/ 255) if values of pixel are not between 0 and 1
                    byteBuffer.putFloat(( (val>>16) & 0xFF)*(1.f));
                    byteBuffer.putFloat(( (val>>16) & 0xFF)*(1.f));
                    byteBuffer.putFloat( (val & 0xFF)*(1.f));
                }
            }

            inputFeature0.loadBuffer(byteBuffer);

            // Runs model inference and gets result.
            Model.Outputs outputs = model.process(inputFeature0);
            TensorBuffer outputFeature0 = outputs.getOutputFeature0AsTensorBuffer();

            float[] confidence = outputFeature0.getFloatArray();
            int max_position=0;
            float max_confidence=0;
            for (int i =0;i<confidence.length;i++){
                if(confidence[i]>max_confidence){
                    max_confidence = confidence[i];
                    max_position = i ;
                }
            }
            String[] category = {"Pothole Found " , "No Pothole Found"};
            result.setText(category[max_position]);

            // Releases model resources if no longer used.
            model.close();
        } catch (IOException e) {
            // TODO Handle the exception
        }

    }
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
            if(requestCode==32){
                Bitmap image = (Bitmap) Objects.requireNonNull(data).getExtras().get("data");
                int dimensions = Math.min(image.getWidth(),image.getHeight());
                image = ThumbnailUtils.extractThumbnail(image,dimensions, dimensions);
                picture.setImageBitmap(image);

                image = Bitmap.createScaledBitmap(image,imageSize,imageSize,false);
                classifyImage(image);
            }


    }

    void signout(){
        mAuth.signOut();
        gsc.signOut().addOnCompleteListener(new OnCompleteListener<Void>() {
            @Override
            public void onComplete(Task<Void> task) {
                finish();
                startActivity(new Intent(detection_model.this,MainActivity.class));
            }
        });
    }


}