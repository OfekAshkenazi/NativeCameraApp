package com.example.ofek.nativecameraapp;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.ImageView;

import com.squareup.picasso.Picasso;

import java.io.File;

public class ShowPicture extends AppCompatActivity {

    private String imagePath;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_show_picture);
        ImageView imageView=findViewById(R.id.imageView);
        if (getIntent().hasExtra(MainActivity.FILE_PATH_TAG)) {
            imagePath = (String) getIntent().getExtras().get(MainActivity.FILE_PATH_TAG);
            File file=new File(imagePath);
            Picasso.with(this).load(file).into(imageView);
        }

    }

    @Override
    public void onBackPressed() {
        finish();
    }
}
