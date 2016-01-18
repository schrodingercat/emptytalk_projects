package et.naruto.shard.module;

import java.util.ArrayList;

public class Shard {
    private final ArrayList<Range> master;
    private final ArrayList<Range> follower;
    public Shard(final ArrayList<Range> master,final ArrayList<Range> follower) {
        this.master=master;
        this.follower=follower;
    }
}
