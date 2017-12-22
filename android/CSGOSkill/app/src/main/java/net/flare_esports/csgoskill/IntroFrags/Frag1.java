/*
 * Created by the Dev Team for CSGO Skill.
 * Copyright (c) 2017. All rights reserved.
 */

package net.flare_esports.csgoskill.IntroFrags;

import android.app.Fragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import net.flare_esports.csgoskill.R;

public class Frag1 extends Fragment {

    View view;

    public Frag1() {}

    public static Frag1 newInstance() { return new Frag1(); }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        view = inflater.inflate(R.layout.fragment_intro1, container, false);

        return view;
    }

}
