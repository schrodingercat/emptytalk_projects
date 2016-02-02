package et.naruto.register.sync;

import et.naruto.election.Args;
import et.naruto.process.zk.NodeFetcher;
import et.naruto.process.zk.ZKProcess;

public class NodeSync extends NodeFetcher<ServerSync> implements NodeFetcher.Target {
    private final Args args;
    private final ZKProcess zkprocess;
    private final String name;
    public NodeSync(final ZKProcess zkprocess,final Args args,final String name) {
        super(zkprocess,args.GetRegistersPath()+"/"+name);
        this.args=args;
        this.zkprocess=zkprocess;
        this.name=name;
    }
    public String name() {
        return this.name;
    }
    public ServerSync DoCreateX(String child) {
        return new ServerSync(zkprocess,args,name,child);
    }
}
