package et.naruto;

import java.nio.file.Paths;

import org.apache.zookeeper.CreateMode;

public class Server {
    @Override
    public String toString() {
        return "Server [args=" + args + "]";
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
    
    private final Args args;
    private final ZKProcess zkprocess;
    private final ValueFetcher leader_flag_fetcher;
    private final ValueRegister leader_flag_register;
    private String leader_server_id="";
    
    public Server(final Args args) {
        this.args=args;
        this.zkprocess=new ZKProcess(args.GetServerId()) {
            public void DoRun() {
                MyServer_DoRun();
            }
        };
        this.leader_flag_fetcher=new ValueFetcher(args.GetLeaderPath());
        this.leader_flag_register=new ValueRegister(
            new ValueRegister.Request(
                args.GetLeaderPath(),
                args.GetServerId(),
                CreateMode.EPHEMERAL
            )
        );
        
        this.zkprocess.AddProcesser(leader_flag_fetcher);
        this.zkprocess.AddProcesser(leader_flag_register);
    }
    private void MyServer_DoRun() {
        final ValueFetcher.Result leader_flag_result=this.leader_flag_fetcher.Fetch();
        if(leader_flag_result!=null) {
            if(leader_flag_result.value.isEmpty()) {
                leader_flag_register.ReRequest();
            }
            if(!leader_server_id.equals(leader_flag_result.value)) {
                leader_server_id=leader_flag_result.value;
            }
        }
    }
    public boolean IsLeader() {
        return leader_server_id.equals(args.GetServerId());
    }
    public void Start() {
        this.zkprocess.Start();
    }
    public void Stop() {
        this.zkprocess.Stop();
    }
}
