package et.naruto.elect;

import java.nio.file.Paths;

import static et.naruto.base.Util.Path2UnixStr;

public class Args {
    @Override
    public String toString() {
        return String.format("Arg(c=%s,n=%s,s=%s)",cluster_path,node_path,server_num);
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
        return Path2UnixStr(Paths.get(cluster_path).resolve("Leader"));
    }
    public String GetResolutionsPath() {
        return Path2UnixStr(Paths.get(cluster_path).resolve("Resolutions"));
    }
    public String GetResolutionsClosedPath() {
        return Path2UnixStr(Paths.get(cluster_path).resolve("ResolutionsClosed"));
    }
    public String GetRegistersPath() {
        return Path2UnixStr(Paths.get(cluster_path).resolve("Registers"));
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
