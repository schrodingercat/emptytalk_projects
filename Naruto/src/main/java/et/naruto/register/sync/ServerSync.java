package et.naruto.register.sync;

import et.naruto.elect.Args;
import et.naruto.process.zk.NodeFetcher;
import et.naruto.process.zk.ValueFetcher;
import et.naruto.process.zk.ZKProcess;

public class ServerSync implements NodeFetcher.Target {
    private final Args args;
    private final ZKProcess zkprocess;
    private final String node;
    private final String name;
    public final ValueFetcher active_fetcher;
    public ServerSync(final ZKProcess zkprocess,final Args args,final String node,final String name) {
        this.args=args;
        this.zkprocess=zkprocess;
        this.node=node;
        this.name=name;
        this.active_fetcher=new ValueFetcher(zkprocess,args.GetRegistersPath()+"/"+node+"/"+name+"/active");
    }
    public String name() {
        return this.name;
    }
    public void close() {
        active_fetcher.close();
    }
}
