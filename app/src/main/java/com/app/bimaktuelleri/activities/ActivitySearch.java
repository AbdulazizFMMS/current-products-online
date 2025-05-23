package com.app.bimaktuelleri.activities;

import static com.app.bimaktuelleri.Config.BANNER_SEARCH;
import static com.app.bimaktuelleri.Config.INTERSTITIAL_RECIPES_LIST;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewStub;
import android.view.WindowManager;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.coordinatorlayout.widget.CoordinatorLayout;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.StaggeredGridLayoutManager;

import com.app.bimaktuelleri.R;
import com.app.bimaktuelleri.adapters.AdapterRecipes;
import com.app.bimaktuelleri.adapters.AdapterSearch;
import com.app.bimaktuelleri.callbacks.CallbackPost;
import com.app.bimaktuelleri.database.prefs.AdsPref;
import com.app.bimaktuelleri.database.prefs.SharedPref;
import com.app.bimaktuelleri.models.Post;
import com.app.bimaktuelleri.rests.RestAdapter;
import com.app.bimaktuelleri.utils.AdsManager;
import com.app.bimaktuelleri.utils.Constant;
import com.app.bimaktuelleri.utils.Tools;
import com.facebook.shimmer.ShimmerFrameLayout;
import com.google.android.material.snackbar.Snackbar;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ActivitySearch extends AppCompatActivity {

    private static final String TAG = "ActivitySearch";
    private EditText etSearch;
    private RecyclerView recyclerView;
    private AdapterRecipes adapterRecipes;
    private RecyclerView recyclerSuggestion;
    private AdapterSearch mAdapterSuggestion;
    private LinearLayout lytSuggestion;
    private ImageButton btnClear;
    private Call<CallbackPost> callbackCall = null;
    List<Post> items = new ArrayList<>();
    private ShimmerFrameLayout lytShimmer;
    CoordinatorLayout parentView;
    SharedPref sharedPref;
    AdsPref adsPref;
    AdsManager adsManager;
    Tools tools;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Tools.getTheme(this);
        setContentView(R.layout.activity_search);
        sharedPref = new SharedPref(this);
        adsPref = new AdsPref(this);
        adsManager = new AdsManager(this);
        tools = new Tools(this);
        Tools.setNavigation(this, sharedPref);
        parentView = findViewById(R.id.coordinatorLayout);
        initComponent();
        adsManager.loadBannerAd(BANNER_SEARCH);
        adsManager.loadInterstitialAd(INTERSTITIAL_RECIPES_LIST, adsPref.getInterstitialAdInterval());
        setupToolbar();
        initShimmerLayout();
    }

    @SuppressLint("ClickableViewAccessibility")
    private void initComponent() {
        lytSuggestion = findViewById(R.id.lyt_suggestion);
        etSearch = findViewById(R.id.et_search);
        btnClear = findViewById(R.id.bt_clear);
        btnClear.setVisibility(View.GONE);
        lytShimmer = findViewById(R.id.shimmer_view_container);
        recyclerView = findViewById(R.id.recyclerView);

        recyclerView = findViewById(R.id.recycler_view);
        recyclerView.setLayoutManager(new StaggeredGridLayoutManager(sharedPref.getRecipesColumns(), StaggeredGridLayoutManager.VERTICAL));

        //set data and list adapter
        adapterRecipes = new AdapterRecipes(this, recyclerView, items);
        recyclerView.setAdapter(adapterRecipes);

        recyclerSuggestion = findViewById(R.id.recyclerSuggestion);
        recyclerSuggestion.setLayoutManager(new LinearLayoutManager(this));

        etSearch.addTextChangedListener(textWatcher);
        etSearch.requestFocus();
        swipeProgress(false);

        adapterRecipes.setOnItemClickListener((view, obj, position) -> {
            Intent intent = new Intent(getApplicationContext(), ActivityRecipesDetail.class);
            intent.putExtra(Constant.EXTRA_OBJC, obj);
            startActivity(intent);
            sharedPref.savePostId(obj.id);

            adsManager.showInterstitialAd();
            adsManager.destroyBannerAd();
        });

        //set data and list adapter suggestion
        mAdapterSuggestion = new AdapterSearch(this);
        recyclerSuggestion.setAdapter(mAdapterSuggestion);
        showSuggestionSearch();
        mAdapterSuggestion.setOnItemClickListener((view, viewModel, pos) -> {
            etSearch.setText(viewModel);
            lytSuggestion.setVisibility(View.GONE);
            hideKeyboard();
            requestAction();
        });

        btnClear.setOnClickListener(view -> etSearch.setText(""));

        etSearch.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                if (etSearch.getText().toString().equals("")) {
                    Snackbar.make(parentView, getString(R.string.msg_search_input), Snackbar.LENGTH_SHORT).show();
                    hideKeyboard();
                    swipeProgress(false);
                } else {
                    adapterRecipes.resetListData();
                    hideKeyboard();
                    requestAction();
                }
                return true;
            }
            return false;
        });

        etSearch.setOnTouchListener((view, motionEvent) -> {
            showSuggestionSearch();
            getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);
            return false;
        });

    }

    public void setupToolbar() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        Tools.setupToolbar(this, toolbar, "", true);
    }

    TextWatcher textWatcher = new TextWatcher() {
        @Override
        public void onTextChanged(CharSequence c, int i, int i1, int i2) {
            if (c.toString().trim().length() == 0) {
                btnClear.setVisibility(View.GONE);
            } else {
                btnClear.setVisibility(View.VISIBLE);
            }
        }

        @Override
        public void beforeTextChanged(CharSequence c, int i, int i1, int i2) {
        }

        @Override
        public void afterTextChanged(Editable editable) {
        }
    };

    private void requestSearchAPI(final String query) {
        List<String> apiKeys = Arrays.asList(sharedPref.getAPIKey().replace(", ", ",").split(","));
        int totalKeys = (apiKeys.size() - 1);
        String apiKey;
        if (sharedPref.getApiKeyPosition() > totalKeys) {
            apiKey = apiKeys.get(0);
            sharedPref.updateApiKeyPosition(0);
        } else {
            apiKey = apiKeys.get(sharedPref.getApiKeyPosition());
        }

        this.callbackCall = RestAdapter.createApiPosts(sharedPref.getBloggerId()).getSearchPosts(query, apiKey);
        this.callbackCall.enqueue(new Callback<CallbackPost>() {
            public void onResponse(Call<CallbackPost> call, Response<CallbackPost> response) {
                CallbackPost resp = response.body();
                if (resp != null) {
                    displayApiResult(resp.items);
                } else {
                    onFailRequest();
                }
            }

            public void onFailure(Call<CallbackPost> call, Throwable th) {
                Log.e("onFailure", "" + th.getMessage());
                if (!call.isCanceled()) {
                    onFailRequest();
                }
            }
        });
    }

    private void displayApiResult(final List<Post> items) {
        adapterRecipes.insertDataWithNativeAd(items);
        swipeProgress(false);
        if (items.size() == 0) {
            showNotFoundView(true);
        }
    }

    private void onFailRequest() {
        adapterRecipes.setLoaded();
        swipeProgress(false);
        if (Tools.isConnect(this)) {
            showFailedView(true, getString(R.string.failed_text));
        } else {
            showFailedView(true, getString(R.string.failed_text));
        }
    }

    private void requestAction() {
        lytSuggestion.setVisibility(View.GONE);
        showFailedView(false, "");
        showNotFoundView(false);
        final String query = etSearch.getText().toString().trim();
        if (!query.equals("")) {
            adapterRecipes.setLoading();
            requestSearchAPI(query);
            swipeProgress(true);
        } else {
            Snackbar.make(parentView, getString(R.string.msg_search_input), Snackbar.LENGTH_SHORT).show();
            swipeProgress(false);
        }
    }

    private void showSuggestionSearch() {
        mAdapterSuggestion.refreshItems();
        lytSuggestion.setVisibility(View.VISIBLE);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
        }
        return super.onOptionsItemSelected(item);
    }

    private void hideKeyboard() {
        View view = this.getCurrentFocus();
        if (view != null) {
            InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
        }
    }

    private void showFailedView(boolean show, String message) {
        View lytFailed = findViewById(R.id.lyt_failed);
        ((TextView) findViewById(R.id.failed_message)).setText(message);
        if (show) {
            recyclerView.setVisibility(View.GONE);
            lytFailed.setVisibility(View.VISIBLE);
        } else {
            recyclerView.setVisibility(View.VISIBLE);
            lytFailed.setVisibility(View.GONE);
        }
        findViewById(R.id.failed_retry).setOnClickListener(view -> requestAction());
    }

    private void showNotFoundView(boolean show) {
        View lytNoItem = findViewById(R.id.lyt_no_item);
        ((TextView) findViewById(R.id.no_item_message)).setText(R.string.no_search_found);
        if (show) {
            recyclerView.setVisibility(View.GONE);
            lytNoItem.setVisibility(View.VISIBLE);
        } else {
            recyclerView.setVisibility(View.VISIBLE);
            lytNoItem.setVisibility(View.GONE);
        }
    }

    private void swipeProgress(final boolean show) {
        if (!show) {
            lytShimmer.setVisibility(View.GONE);
            lytShimmer.stopShimmer();
            return;
        } else {
            lytShimmer.setVisibility(View.VISIBLE);
            lytShimmer.startShimmer();
        }
    }

    @Override
    public void onBackPressed() {
        if (etSearch.length() > 0) {
            etSearch.setText("");
        } else {
            super.onBackPressed();
            adsManager.destroyBannerAd();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        adsManager.resumeBannerAd(BANNER_SEARCH);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        adsManager.destroyBannerAd();
    }

    private void initShimmerLayout() {
        ViewStub stub = findViewById(R.id.lytShimmerView);
        if (sharedPref.getRecipesColumns() == Constant.RECIPES_THREE_COLUMNS) {
            stub.setLayoutResource(R.layout.shimmer_recipes_grid3);
        } else {
            stub.setLayoutResource(R.layout.shimmer_recipes_grid2);
        }
        stub.inflate();
    }

}
