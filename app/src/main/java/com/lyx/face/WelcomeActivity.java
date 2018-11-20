package com.lyx.face;

import android.app.Activity;

import android.content.Intent;

import android.os.Bundle;

import android.view.View;


public class WelcomeActivity extends Activity {


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        findViewById(R.id.tv_welcome).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                Intent intent=new Intent(WelcomeActivity.this,FdActivity.class);
                startActivity(intent);


            }
        });
    }





}