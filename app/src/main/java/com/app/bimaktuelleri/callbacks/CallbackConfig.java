package com.app.bimaktuelleri.callbacks;

import com.app.bimaktuelleri.models.Ads;
import com.app.bimaktuelleri.models.App;
import com.app.bimaktuelleri.models.Blog;
import com.app.bimaktuelleri.models.Category;
import com.app.bimaktuelleri.models.Notification;

import java.util.ArrayList;
import java.util.List;

public class CallbackConfig {

    public Blog blog = null;
    public App app = null;
    public Notification notification = null;
    public Ads ads = null;
    public List<Category> labels = new ArrayList<>();

}
