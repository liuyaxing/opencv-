package com.lyx.face;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.widget.ImageView;


public class MainActivity extends Activity {


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

      ImageView  imageView= (ImageView) findViewById(R.id.imageview);


//        imageView.setImageBitmap(FdActivity.bitmap);

        Intent intent=getIntent();
        if(intent !=null)
        {
            byte [] bis=intent.getByteArrayExtra("bitmap");
            Bitmap bitmap= BitmapFactory.decodeByteArray(bis, 0, bis.length);
            imageView.setImageBitmap(bitmap);
        }








    }



}
