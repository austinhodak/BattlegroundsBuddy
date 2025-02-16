package com.brokenstrawapps.battlebuddy.info;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.fragment.app.Fragment;
import butterknife.ButterKnife;
import com.brokenstrawapps.battlebuddy.R;

public class ControlsFragment extends Fragment {

    public ControlsFragment() {
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_controls, container, false);
        ButterKnife.bind(this, view);

        return view;
    }

}
