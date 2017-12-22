/*
 * Created by the Dev Team for CSGO Skill.
 * Copyright (c) 2017. All rights reserved.
 */

package net.flare_esports.csgoskill;

//import android.app.Activity;
import android.app.Fragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.transition.Fade;
import android.view.Window;

import net.flare_esports.csgoskill.IntroFrags.*;

public class Introduction extends AppCompatActivity {

    Fragment slide1;
    FragmentManager fragmentManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_ACTIVITY_TRANSITIONS);
        setContentView(R.layout.activity_intro);

        slide1 = new Frag1();
        fragmentManager = getFragmentManager();
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
        Fade fade = new Fade();
        fade.setDuration(1000);
        slide1.setEnterTransition(fade);
        fragmentTransaction.replace(R.id.intro_frag_container, slide1);
        fragmentTransaction.commitAllowingStateLoss();

    }
}
