package com.example.pothole_detection_system;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.messaging.FirebaseMessaging;

import java.util.HashMap;
import java.util.Map;

public class registration_more_info extends AppCompatActivity {
    TextView userName;
    Button logout , submit_user, next;
    EditText address;
    EditText password;

    GoogleSignInOptions gso;
    GoogleSignInClient gsc;
    FirebaseFirestore db;
    FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_registration_more_info);
        userName = findViewById(R.id.profileName);
        logout = findViewById(R.id.LogoutBtn);
        address = findViewById(R.id.user_address);
        password = findViewById(R.id.User_Password);
        submit_user = findViewById(R.id.button);
        next = findViewById(R.id.next);


        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();


        gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN).requestEmail().build();
        gsc = GoogleSignIn.getClient(this,gso);

        GoogleSignInAccount account = GoogleSignIn.getLastSignedInAccount(this);


        if (account!=null){
            String personName = account.getEmail();
            userName.setText(personName);
        }

        logout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                signout();
            }
        });

    }
    @Override
    protected void onStart() {
        super.onStart();

        //firebase Authentication


        submit_user.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                createUser();
            }

        });
        next.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(registration_more_info.this,detection_model.class));
            }
        });


        //firebase Datastore
        db = FirebaseFirestore.getInstance();


    }

    private void createUser() {
        GoogleSignInAccount account = GoogleSignIn.getLastSignedInAccount(this);
        assert account != null;
        String email = account.getEmail();
        String pass = password.getText().toString();
        if (TextUtils.isEmpty(pass)){
            password.setError("Password can't be empty");
            password.requestFocus();
        }
        else{
            mAuth.createUserWithEmailAndPassword(email,pass).addOnCompleteListener(new OnCompleteListener<AuthResult>() {
                @Override
                public void onComplete(@NonNull Task<AuthResult> task) {
                    if (task.isSuccessful()){
                        registerUserToken();
                        Toast.makeText(getApplicationContext(),"Successful Registration",Toast.LENGTH_SHORT).show();
                        Intent intent=new Intent(registration_more_info.this,detection_model.class);
                        intent.putExtra("homeAdd",address.getText().toString().trim());
                        startActivity(intent);

                    }else{
                        Toast.makeText(getApplicationContext(),"Registration Error",Toast.LENGTH_SHORT).show();
                    }
                }
            });
        }
    }
    void signout(){
        mAuth.signOut();
        gsc.signOut().addOnCompleteListener(new OnCompleteListener<Void>() {
        @Override
        public void onComplete(Task<Void> task) {
            finish();
            startActivity(new Intent(registration_more_info.this,MainActivity.class));
        }
    });
    }

    //Register user token with uid and token and home address.

    void  registerUserToken(){
        FirebaseMessaging.getInstance().getToken().addOnCompleteListener(new OnCompleteListener<String>() {
            @Override
            public void onComplete(@NonNull Task<String> task) {
                if(!task.isSuccessful()){
                    return;
                }
                String token = task.getResult();
                Map<String,String> maptoken = new HashMap<>();
                maptoken.put("Token",token);
                maptoken.put("home_add",address.getText().toString().trim());
                FirebaseFirestore.getInstance().collection("Tokens").document(FirebaseAuth.getInstance().getCurrentUser().getUid()).set(maptoken);
            }
        });

    }
}