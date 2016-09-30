package com.yeleman.mmtkrelay;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;

public class MMTKBaseActivity extends AppCompatActivity {
    public Session session;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // load existing preferences into Session
        session = new Session(this);
    }
}
