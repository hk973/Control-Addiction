package com.genzopia.addiction;

import android.util.Log;
import android.view.View;
import android.widget.Button;

public class menu {
    Button bb;
    public menu(View view) {
        bb=view.findViewById(R.id.btn_option1);

    }

    public void initializeview() {
        bb.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Log.e("test234","pressed");
            }
        });
    }
}
