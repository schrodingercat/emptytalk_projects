package et.naruto.election;

import java.nio.file.Paths;

public class Args {
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
    public String GetResolutionsPath() {
        return Paths.get(cluster_path).resolve("Resolutions").toString();
    }
    public String GetResolutionsClosedPath() {
        return Paths.get(cluster_path).resolve("ResolutionsClosed").toString();
    }
    public String GetRegistersPath() {
        return Paths.get(cluster_path).resolve("Registers").toString();
    }
    public String GetNodePath() {
        return GetRegistersPath()+"/"+node_path;
    }
    public String GetServerPath() {
        return GetNodePath()+"/"+server_num;
    }
    public String GetActivePath() {
        return GetServerPath()+"/active";
    }
    public String GetServerId() {
        return node_path+":"+server_num;
    }
}
