package com.example.pothole_detection_system;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.messaging.FirebaseMessaging;

import java.util.HashMap;
import java.util.Map;

public class Form extends AppCompatActivity {
    EditText pothole_location ,ans1,ans2,ans3;
    Button submit;
    FirebaseFirestore db;
    FirebaseAuth mauth;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_form);
        pothole_location = findViewById(R.id.found_pothole_address);
        ans1 = findViewById(R.id.edanswer1);
        ans2 = findViewById(R.id.edanswer2);
        ans3 = findViewById(R.id.edanswer3);
        submit = findViewById(R.id.btnupload);

        FirebaseMessaging.getInstance().subscribeToTopic("all");
        submit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                insertData();
            }
        });

        db = FirebaseFirestore.getInstance();
        mauth = FirebaseAuth.getInstance();
    }
    public void insertData(){

        Intent a1 = getIntent();
        String home_address = a1.getStringExtra("ADDRESS");

        String uid = mauth.getCurrentUser().getUid();

        GoogleSignInAccount account = GoogleSignIn.getLastSignedInAccount(this);
        String email= account.getEmail();


        Map<String, Object> items = new HashMap<>();
        items.put("uid",uid);
        items.put("gmail",email);
        items.put("home_address",home_address);
        items.put("pothole_address",pothole_location.getText().toString().trim());
        items.put("ans1",Integer.parseInt(ans1.getText().toString().trim()));
        items.put("ans2",Integer.parseInt(ans2.getText().toString().trim()));
        items.put("ans3",Integer.parseInt(ans3.getText().toString().trim()));

        db.collection("Database").add(items).addOnCompleteListener(new OnCompleteListener<DocumentReference>() {
            @Override
            public void onComplete(@NonNull Task<DocumentReference> task) {

                Toast.makeText(getApplicationContext(),"Submitted Successfully",Toast.LENGTH_SHORT).show();

//                String title = "Pothole reported at " + pothole_location.getText().toString().trim().toUpperCase();
//                String message = "Please fill the form to provide more details on Pothole";
//                FcmNotificationsSender notificationsSender = new FcmNotificationsSender("/topics/all",title , message ,getApplicationContext(),Form.this);
//                notificationsSender.SendNotifications();
                fetchData();
            }
        });


    }

    // fetch data for neighbour user in area and send notification.
    public void fetchData(){
        String title = "Pothole reported at " + pothole_location.getText().toString().toUpperCase();
        String message = "Please fill the form to provide more details on Pothole ";
        db.collection("Tokens").whereEqualTo("home_add",pothole_location.getText().toString().trim()).get().addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
            @Override
            public void onComplete(@NonNull Task<QuerySnapshot> task) {
                if(task.isSuccessful()){
                    Log.d("usertoken0", "onComplete: "+task);
                    for(DocumentSnapshot document: task.getResult()){
                        if(document.exists()){

                            Map<String, Object> usertoken = document.getData();
                            Log.d("u1", document.getId() + " => " + document.getData());
                            String utoken = usertoken.get("Token").toString();
                            Log.d("usertok", "onComplete: "+utoken);
                            FcmNotificationsSender notificationsSender = new FcmNotificationsSender(utoken,title,message ,getApplicationContext(),Form.this);
                            notificationsSender.SendNotifications();
                        }else{
                            Log.d("nodoc", "onComplete: Nod document");
                        }

                    }
                    startActivity(new Intent(Form.this,detection_model.class));
                    pothole_location.setText("");
                    ans1.setText("");
                    ans2.setText("");
                    ans3.setText("");
                }
                else{
                    Log.d("how", "onComplete: Error");
                }
            }
        });
    }
}