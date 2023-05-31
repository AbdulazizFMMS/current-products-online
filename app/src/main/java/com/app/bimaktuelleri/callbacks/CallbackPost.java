package com.app.bimaktuelleri.callbacks;

import com.app.bimaktuelleri.models.Post;

import java.util.ArrayList;
import java.util.List;

public class CallbackPost {

    public String kind;
    public String nextPageToken;
    public List<Post> items = new ArrayList<>();
    public String etag;

}
