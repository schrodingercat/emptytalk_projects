package et.naruto;

import java.nio.file.Paths;
import java.util.List;

import org.apache.zookeeper.AsyncCallback.ChildrenCallback;
import org.apache.zookeeper.AsyncCallback.DataCallback;
import org.apache.zookeeper.AsyncCallback.StringCallback;
import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.WatchedEvent;
import org.apache.zookeeper.Watcher;
import org.apache.zookeeper.ZooDefs.Ids;
import org.apache.zookeeper.data.Stat;

import et.naruto.Server.Args;
import et.naruto.Util.DIAG;

/*
class NodeChildFetcher extends Thread {
    private final String path;
    private boolean changed;
    private List<String> childs;
    public NodeChildFetcher(final String path) {
        this.path=path;
    }
    public boolean IsChanged() {
        if(changed) {
            changed=false;
            return true;
        }
        return false;
    }
    public void DoChanged() {}
    public void DoFetching(final Status status) {
        ZooKeeperInstance.Get.zk.getChildren(
            this.path,
            new Watcher() {
                public void process(WatchedEvent event) {
                    changed=true;
                    DoChanged();
                }
            },
            new ChildrenCallback() {
                public void processResult(
                        int rc,
                        String path,
                        Object ctx,
                        List<String> children) {
                    switch(rc) {
                    case 0:
                        childs=children;
                        status.value="OK";
                        DoChanged();
                        break;
                    default:
                        status.value="";
                        DoChanged();
                        break;
                    }
                }
            },
            null
        );
    }
}
*/
class NodeFetcher extends StatusFetcher {
    private final String path;
    private boolean changed;
    public NodeFetcher(final String path) {
        super(null);
        this.path=path;
    }
    public boolean IsChanged() {
        if(changed) {
            changed=false;
            return true;
        }
        return false;
    }
    public void DoChanged() {}
    public void DoFetching(final Status status) {
        ZooKeeperInstance.Get.zk.getData(
            this.path,
            new Watcher() {
                public void process(WatchedEvent event) {
                    changed=true;
                    DoChanged();
                }
            },
            new DataCallback() {
                public void processResult(
                            int rc,
                            String path,
                            Object ctx,
                            byte data[],
                            Stat stat) {
                    switch(rc) {
                    case 0:
                        try {
                            status.value=new String(data,"UTF-8");
                        } catch (Exception e) {
                            DIAG.Get.d.dig_error("",e);
                            status.value="";
                        } finally {
                            DoChanged();
                        }
                        break;
                    default:
                        status.value="";
                        DoChanged();
                        break;
                    }
                }
            },
            null
        );
    }
}

class NodeRegister extends StatusFetcher {
    private final String path;
    private final String data;
    public NodeRegister(final String path,final String data) {
        super(null);
        this.path=path;
        this.data=data;
    }
    public void DoChanged() {}
    public void DoFetching(final Status status) {
        ZooKeeperInstance.Get.zk.create(
            this.path,
            this.data.getBytes(),
            Ids.OPEN_ACL_UNSAFE,
            CreateMode.EPHEMERAL,
            new StringCallback() {
                public void processResult(int rc, String path, Object ctx, String name) {
                    switch(rc) {
                    case 0:
                        status.value="OK";
                        DoChanged();
                        break;
                    default:
                        status.value="";
                        DoChanged();
                        break;
                    }
                }
            },
            null
        );
    }
}

class Poster {
    private final NodeFetcher leader_fetcher;
    private final NodeRegister leader_register;
    private Thread thread=null;
    private final Args args;
    private final Poster poster_ref;
    public Poster(final Args args) {
        this.poster_ref=this;
        this.leader_fetcher=new NodeFetcher(args.GetLeaderPath()) {
            public void DoChanged() {
                poster_ref.DoChanged();
            }
        };
        this.leader_register=new NodeRegister(args.GetLeaderPath(),args.GetServerId()) {
            public void DoChanged() {
                poster_ref.DoChanged();
            }
        };
        this.args=args;
    }
    public void DoChanged() {}
    public boolean IsLeader() {
        if(leader_fetcher.IsKnow()) {
            if(leader_fetcher.status.value.equals(this.args.GetServerId())) {
                return true;
            }
        }
        return false;
    }
    public void Start() {
        this.thread=new Thread(this.args.server_num+"::post_thread"){
            public void run() {
                while(true) {
                    try {
                        boolean need_quick_next=poster_ref.Tick();
                        if(poster_ref.thread!=this) {
                            if(poster_ref.IsLeader()) {
                                ZooKeeperInstance.Get.zk.delete(poster_ref.args.GetLeaderPath(),-1);
                            }
                            break;
                        }
                        if(need_quick_next) {
                            Thread.sleep(100);
                        } else {
                            Thread.sleep(1000);
                        }
                    } catch (Exception e) {
                        DIAG.Get.d.dig_error("",e);
                    }
                }
            }
        };
        this.thread.start();
    }
    public void Stop() {
        Thread temp=this.thread;
        this.thread=null;
        try {
            temp.join();
        } catch (Exception e) {
            DIAG.Get.d.pass_error("",e);
        }
    }
    private boolean Tick() {
        boolean need_fetch_leader=false;
        boolean need_regist_leader=false;
        boolean leader_node_changed=this.leader_fetcher.IsChanged();
                        
        if(leader_node_changed) {
            need_fetch_leader=true;
        } else {
            if(this.leader_fetcher.IsUnknow()) {
                need_fetch_leader=true;
            } else {
                if(this.leader_fetcher.IsKnowButEmpty()) {
                    if(this.leader_register.IsKnow()) {
                        need_fetch_leader=true;
                    }
                }
            }
        }
                        
        if(leader_node_changed) {
            need_regist_leader=true;
        } else {
            if(this.leader_fetcher.IsKnowButEmpty()) {
                if(this.leader_register.IsUnknow()) {
                    need_regist_leader=true;
                }
            }
        }
                        
        boolean need_quick_next=false;
        if(need_fetch_leader) {
            this.leader_fetcher.Fetch();
            this.leader_fetcher.Fetch();
            need_quick_next=true;
        }
        if(need_regist_leader) {
            this.leader_register.Fetch();
            need_quick_next=true;
        }
        return need_quick_next;
    }
}

public class Server {
	@Override
	public String toString() {
		return "Server [args=" + args + ", poster=" + poster + "]";
	}
	public static class Args {
        @Override
        public String toString() {
            return "Args [cluster_path="+cluster_path+"," +
                "node_path="+node_path+",server_num="+server_num+"]";
        }
        public Args(
            String cluster_path,
            String node_path,
            String server_num
        ) {
            this.cluster_path=cluster_path;
            this.node_path=node_path;
            this.server_num=server_num;
        }
        public final String cluster_path;
        public final String node_path;
        public final String server_num;
        public String GetLeaderPath() {
            return Paths.get(cluster_path).resolve("Leader").toString();
        }
        public String GetLeaderVerPath() {
            return Paths.get(cluster_path).resolve("LeaderVer").toString();
        }
        public String GetServerId() {
            return node_path+":"+server_num;
        }
    }
    
    private final Server this_ref;
    private final Args args;
    private final Poster poster;
    private boolean is_leader=false;
    
    public boolean IsLeader() {
        return is_leader;
    }
    public Server(final Args args) {
        this.this_ref=this;
        this.args=args;
        this.poster=new Poster(args) {
            public void DoChanged() {
                if(poster.IsLeader()!=is_leader) {
                    is_leader=poster.IsLeader();
                }
            }
        };
    }
    public void Start() {
    	this.poster.Start();
    }
    public void Stop() {
        this.poster.Stop();
    }
}
