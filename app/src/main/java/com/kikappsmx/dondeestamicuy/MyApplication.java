package com.kikappsmx.dondeestamicuy;

import android.app.Application;
import android.content.Intent;

import com.kikappsmx.dondeestamicuy.services.MyService;

public class MyApplication extends Application {

    @Override
    public void onCreate() {
        super.onCreate();

        Intent intent = new Intent(this, MyService.class);
        startService(intent);
    }



}
