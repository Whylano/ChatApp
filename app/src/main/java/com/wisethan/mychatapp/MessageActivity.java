package com.wisethan.mychatapp;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.rengwuxian.materialedittext.MaterialEditText;
import com.wisethan.mychatapp.Adapters.MessageAdapter;
import com.wisethan.mychatapp.model.Chats;
import com.wisethan.mychatapp.model.Users;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import de.hdodenhof.circleimageview.CircleImageView;

public class MessageActivity extends AppCompatActivity {
    String friendid, message, myid;
    CircleImageView imageViewOnToolbar;
    TextView usernameOnToolbar;
    Toolbar toolbar;

    FirebaseUser firebaseUser;

    DatabaseReference reference;

    EditText et_message;

    MessageAdapter messageAdapter;

    Button send;

    List<Chats> chatsList;

    MessageActivity messageActivity;

    RecyclerView recyclerView;

    ValueEventListener seenlistener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_message);

        toolbar = findViewById(R.id.toolbar_message);
        setSupportActionBar(toolbar);
        getSupportActionBar().setTitle("");
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        imageViewOnToolbar = findViewById(R.id.profile_image_toolbar_message);
        usernameOnToolbar = findViewById(R.id.username_ontoolbar_message);

        recyclerView = findViewById(R.id.recyclerview_messages);
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        layoutManager.setStackFromEnd(true);
        recyclerView.setLayoutManager(layoutManager);

        send = findViewById(R.id.send_messsage_btn);
        et_message = findViewById(R.id.edit_message_text);

        firebaseUser = FirebaseAuth.getInstance().getCurrentUser();
        myid = firebaseUser.getUid();// my id or the one who is loggin


        friendid = getIntent().getStringExtra("friendid"); // retreive the friendid when we click on the item

        firebaseUser = FirebaseAuth.getInstance().getCurrentUser();

        DatabaseReference reference = FirebaseDatabase.getInstance().getReference("Users").child(friendid);
        reference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                Users users = snapshot.getValue(Users.class);

                // set the text of the user on textview in toolbar
                usernameOnToolbar.setText(users.getUsername());

                if (users.getImageURL().equals("default")) {
                    imageViewOnToolbar.setImageResource(R.drawable.user);
                } else {
                    Glide.with(getApplicationContext()).load(users.getImageURL()).into(imageViewOnToolbar);
                }

                readMessages(myid, friendid, users.getImageURL());

            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });

        seenMessage(friendid);

        et_message.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int i, int i1, int i2) {
                if (s.toString().length() > 0) {
                    send.setEnabled(true);
                } else {
                    send.setEnabled(false);
                }
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

                String text = et_message.getText().toString();
                if (!text.startsWith(" ")) {
                    et_message.getText().insert(0, " ");

                }
            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        });
        send.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                message = et_message.getText().toString();

                sendMessage(myid, friendid, message);

                et_message.setText("");

            }
        });
    }

    private void seenMessage(String friendid) {

        reference = FirebaseDatabase.getInstance().getReference("Chats");

        seenlistener = reference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                for (DataSnapshot ds : snapshot.getChildren()) {
                    Chats chats = ds.getValue(Chats.class);
                    if (chats.getReciever().equals(myid) && chats.getSender().equals(friendid)) {
                        HashMap<String, Object> hashMap = new HashMap<>();
                        hashMap.put("isseen", true);
                        ds.getRef().updateChildren(hashMap);
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });
    }

    private void readMessages(String myid, String friendid, String imageURL) {
        chatsList = new ArrayList<>();
        DatabaseReference reference = FirebaseDatabase.getInstance().getReference("Chats");

        reference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                chatsList.clear();
                for (DataSnapshot ds : snapshot.getChildren()) {
                    Chats chats = ds.getValue(Chats.class);
                    if (chats.getSender().equals(myid) && chats.getReciever().equals(friendid) ||
                            chats.getSender().equals(friendid) && chats.getReciever().equals(myid)) {
                        chatsList.add(chats);
                    }
                    messageAdapter = new MessageAdapter(MessageActivity.this, chatsList, imageURL);
                    recyclerView.setAdapter(messageAdapter);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });
    }

    private void sendMessage(String myid, String friendid, String message) {
        final DatabaseReference reference = FirebaseDatabase.getInstance().getReference();

//        System.out.println("전송 완료");

        HashMap<String, Object> hashMap = new HashMap<>();
        hashMap.put("sender", myid);
        hashMap.put("reciever", friendid);
        hashMap.put("message", message);
        hashMap.put("isseen", false);

        reference.child("Chats").push().setValue(hashMap);

        DatabaseReference reference1 = FirebaseDatabase.getInstance().getReference("Chatslists").child(myid).child(friendid);

        reference1.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {

                if (!snapshot.exists()) {
                    reference1.child("id").setValue(friendid);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });
    }

    private void Status(final String status) {
        final DatabaseReference reference = FirebaseDatabase.getInstance().getReference("Users").child(firebaseUser.getUid());

        HashMap<String, Object> hashMap = new HashMap<>();
        hashMap.put("status", status);

        reference.updateChildren(hashMap);
    }


    @Override
    protected void onResume() {
        super.onResume();
        Status("online");
    }

    @Override
    protected void onPause() {
        super.onPause();
        Status("offline");
        reference.removeEventListener(seenlistener);
    }
}

