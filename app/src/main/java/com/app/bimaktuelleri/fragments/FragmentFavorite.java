package com.app.bimaktuelleri.fragments;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.StaggeredGridLayoutManager;

import com.app.bimaktuelleri.R;
import com.app.bimaktuelleri.activities.ActivityRecipesDetail;
import com.app.bimaktuelleri.activities.MainActivity;
import com.app.bimaktuelleri.adapters.AdapterFavorite;
import com.app.bimaktuelleri.database.prefs.SharedPref;
import com.app.bimaktuelleri.database.sqlite.DbFavorite;
import com.app.bimaktuelleri.models.Post;
import com.app.bimaktuelleri.utils.Constant;

import java.util.ArrayList;
import java.util.List;

public class FragmentFavorite extends Fragment {

    List<Post> posts = new ArrayList<>();
    private View rootView;
    LinearLayout lytNoFavorite;
    private RecyclerView recyclerView;
    AdapterFavorite adapterFavorite;
    DbFavorite dbFavorite;
    SharedPref sharedPref;
    Activity activity;

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        activity = (MainActivity) context;
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        rootView = inflater.inflate(R.layout.fragment_favorite, container, false);

        sharedPref = new SharedPref(activity);
        recyclerView = rootView.findViewById(R.id.recyclerView);
        lytNoFavorite = rootView.findViewById(R.id.lyt_no_favorite);
        recyclerView.setLayoutManager(new StaggeredGridLayoutManager(sharedPref.getRecipesColumns(), StaggeredGridLayoutManager.VERTICAL));

        loadDataFromDatabase();

        return rootView;
    }

    @Override
    public void onResume() {
        super.onResume();
        loadDataFromDatabase();
    }

    public void loadDataFromDatabase() {
        dbFavorite = new DbFavorite(activity);
        posts = dbFavorite.getAllData();

        adapterFavorite = new AdapterFavorite(activity, recyclerView, posts);
        recyclerView.setAdapter(adapterFavorite);

        showNoItemView(posts.size() == 0);

        adapterFavorite.setOnItemClickListener((v, obj, position) -> {
            Intent intent = new Intent(activity, ActivityRecipesDetail.class);
            intent.putExtra(Constant.EXTRA_OBJC, obj);
            startActivity(intent);
            sharedPref.savePostId(obj.id);
            
            ((MainActivity) activity).showInterstitialAd();
            ((MainActivity) activity).destroyBannerAd();
        });

    }

    private void showNoItemView(boolean show) {
        ((TextView) rootView.findViewById(R.id.no_item_message)).setText(R.string.no_favorite_found);
        if (show) {
            recyclerView.setVisibility(View.GONE);
            lytNoFavorite.setVisibility(View.VISIBLE);
        } else {
            recyclerView.setVisibility(View.VISIBLE);
            lytNoFavorite.setVisibility(View.GONE);
        }
    }

}
