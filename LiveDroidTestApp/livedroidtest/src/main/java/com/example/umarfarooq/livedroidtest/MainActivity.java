package com.example.umarfarooq.livedroidtest;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

public class MainActivity extends Activity {
    A a, p;
    C c1;
    B b1;
    TextView mTextView;
    int age;
    int x;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mTextView = findViewById(R.id.maintextView);
        mTextView.setText(R.string.app_name);
        Button button = findViewById(R.id.button);
        button.setText(R.string.app_name);

        a = new A();
        p = new A();
        B b = new B();
        a.setB(b);
        B bb = new B();
        button.setOnClickListener(new ClickHandler());
    }


    void onClick1(View view) {
        //a.b = p.b;
        a.getB().f = 10;
        int temp = a.getB().f + c1.e;
        A aa = new A();
        aa.getB().f = temp;
        age = x + 1;
    }

    void onClick2(View view) {
        b1 = a.getB();
        c1 = a.getC();
        a.test();
        int temp = b1.f;
        mTextView.setText(temp + "");
    }

}
