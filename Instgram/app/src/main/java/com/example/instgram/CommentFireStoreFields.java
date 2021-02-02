package com.example.instgram;

import java.util.Map;

public class CommentFireStoreFields {
    private final String photoRef;
    private final String commentOwner;
    private final String commentOwnerName;
    private final String commentWords;
    private final String commentTimestamp;

    public CommentFireStoreFields(String photoRef, String commentOwner, String commentOwnerName,
                                  String commentWords, String commentTimestamp) {
        this.photoRef = photoRef;
        this.commentOwner = commentOwner;
        this.commentOwnerName = commentOwnerName;
        this.commentWords = commentWords;
        this.commentTimestamp =commentTimestamp;
    }

    public CommentFireStoreFields(Map<String, Object> map) {
        this.photoRef = (String)map.get("photoRef");
        this.commentOwner = (String)map.get("commentOwner");
        this.commentOwnerName = (String)map.get("commentOwnerName");
        this.commentWords = (String)map.get("commentWords");
        this.commentTimestamp = (String)map.get("commentTimestamp");
    }

    public String getCommentOwner() {
        return this.commentOwner;
    }

    public String getCommentOwnerName() {
        return this.commentOwnerName;
    }

    public String getCommentWords() {
        return this.commentWords;
    }

    public String getPhotoRef() {
        return this.photoRef;
    }

    public String getCommentTimestamp() {
        return this.commentTimestamp;
    }
}
