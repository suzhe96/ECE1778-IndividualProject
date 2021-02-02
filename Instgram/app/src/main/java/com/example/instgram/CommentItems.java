package com.example.instgram;

public class CommentItems {
    // Members
    private String userName = null;
    private String userComment = null;
    private byte[] userPic = null;

    public CommentItems(String name, String comment, byte[] pic) {
        this.userName = name;
        this.userComment = comment;
        this.userPic = pic;
    }

    public String getUserName() {
        return this.userName;
    }

    public String getUserComment() {
        return this.userComment;
    }

    public byte[] getUserPic() {
        return this.userPic;
    }
}
