package com.example.igo_ai;

import android.app.Application;
import androidx.annotation.CallSuper;
import com.google.firebase.FirebaseApp;

public class Firebaseinit extends Application {

    @Override
    @CallSuper
    public void onCreate() {
        super.onCreate();
        FirebaseApp.initializeApp(this);
    }
}
