/*
 * Created by the Dev Team for CSGO Skill.
 * Copyright (c) 2017. All rights reserved.
 */

package net.flare_esports.csgoskill;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.transition.Fade;


// This activity will simply manage the fragments, and pass information between
// them as required.

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        getWindow().setAllowEnterTransitionOverlap(true);
        getWindow().setEnterTransition(new Fade());
    }

    @Override
    public void onBackPressed() {

        //TODO

    }
}
