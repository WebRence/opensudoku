package org.moire.opensudoku;

import android.app.Application;

import patch.TraceAspect;

/**
 * Created by muditmathur on 25/10/16.
 */

public class MyApplication extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        System.out.println("IN MyApplication");
        TraceAspect.setContext(getApplicationContext());
    }
}
