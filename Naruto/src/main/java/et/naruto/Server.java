package et.naruto;

import java.nio.file.Paths;

import org.apache.zookeeper.AsyncCallback.DataCallback;
import org.apache.zookeeper.AsyncCallback.StringCallback;
import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.WatchedEvent;
import org.apache.zookeeper.Watcher;
import org.apache.zookeeper.ZooDefs.Ids;
import org.apache.zookeeper.data.Stat;

import et.naruto.Server.Args;
import et.naruto.Util.DIAG;


class LeaderFetcher extends StatusFetcher {
    private final Args args;
    private boolean changed;
    public LeaderFetcher(final Args args) {
        super(null);
        this.args=args;
    }
    public boolean IsChanged() {
        if(changed) {
            changed=false;
            return true;
        }
        return false;
    }
    public void DoFetching(final Status status) {
        ZooKeeperInstance.Get.zk.getData(
            this.args.GetLeaderPath(),
            new Watcher() {
                public void process(WatchedEvent event) {
                    changed=true;
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
                        }
                        break;
                    default:
                        status.value="";
                        break;
                    }
                }
            },
            null
        );
    }
}

class LeaderRegister extends StatusFetcher {
    private final Args args;
    public LeaderRegister(final Args args) {
        super(null);
        this.args=args;
    }
    public void DoFetching(final Status status) {
        ZooKeeperInstance.Get.zk.create(
            this.args.GetLeaderPath(),
            this.args.GetServerId().getBytes(),
            Ids.OPEN_ACL_UNSAFE,
            CreateMode.EPHEMERAL,
            new StringCallback() {
                public void processResult(int rc, String path, Object ctx, String name) {
                    switch(rc) {
                    case 0:
                        status.value="OK";
                        break;
                    default:
                        status.value="";
                        break;
                    }
                }
            },
            null
        );
    }
}

public class Server {
    @Override
	public String toString() {
		return "Server[args="+args+",leader_fetcher="+leader_fetcher
				+",leader_register="+leader_register+",running="
				+running+"]";
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
        public String GetServerId() {
            return node_path+":"+server_num;
        }
    }
    
    private final Server this_ref;
    private final Args args;
    private final Thread post_thread;
    private final LeaderFetcher leader_fetcher;
    private final LeaderRegister leader_register;
    private boolean running=false;
    public boolean IsLeader() {
        if(leader_fetcher.IsKnow()) {
            if(leader_fetcher.status.value.equals(this.args.GetServerId())) {
                return true;
            }
        }
        return false;
    }
    
    public Server(final Args args) {
        this.this_ref=this;
        this.args=args;
        this.leader_fetcher=new LeaderFetcher(args);
        this.leader_register=new LeaderRegister(args);
        this.post_thread=new Thread(args.server_num+"::post_thread"){
            public void run() {
                while(true) {
                    try {
                        boolean need_fetch_leader=false;
                        boolean need_regist_leader=false;
                        boolean leader_node_changed=this_ref.leader_fetcher.IsChanged();
                        
                        if(leader_node_changed) {
                            need_fetch_leader=true;
                        } else {
                            if(this_ref.leader_fetcher.IsUnknow()) {
                                need_fetch_leader=true;
                            } else {
                                if(this_ref.leader_fetcher.IsKnowButEmpty()) {
                                    if(this_ref.leader_register.IsKnow()) {
                                        need_fetch_leader=true;
                                    }
                                }
                            }
                        }
                        
                        if(leader_node_changed) {
                            need_regist_leader=true;
                        } else {
                            if(this_ref.leader_fetcher.IsKnowButEmpty()) {
                                if(this_ref.leader_register.IsUnknow()) {
                                    need_regist_leader=true;
                                }
                            }
                        }
                        
                        boolean need_quick_next=false;
                        if(need_fetch_leader) {
                            this_ref.leader_fetcher.Fetch();
                            this_ref.leader_fetcher.Fetch();
                            need_quick_next=true;
                        }
                        if(need_regist_leader) {
                            this_ref.leader_register.Fetch();
                            need_quick_next=true;
                        }
                        if(!this_ref.running) {
                            if(this_ref.IsLeader()) {
                                ZooKeeperInstance.Get.zk.delete(this_ref.args.GetLeaderPath(),-1);
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
    }
    public void Start() {
        this.running=true;
    	this.post_thread.start();
    }
    public void Stop() {
        this.running=false;
        try {
            this.post_thread.join();
        } catch (Exception e) {
            DIAG.Get.d.pass_error("",e);
        }
    }
}
