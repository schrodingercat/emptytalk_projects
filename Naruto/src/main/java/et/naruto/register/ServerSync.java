package et.naruto.register;

import et.naruto.election.Args;
import et.naruto.process.NodeFetcher;
import et.naruto.process.ValueFetcher;
import et.naruto.process.NodeFetcherX;
import et.naruto.process.ZKProcess;
import et.naruto.versioner.Dealer;

public class ServerSync implements NodeFetcherX.Target {
    private final Args args;
    private final ZKProcess zkprocess;
    private final String node;
    private final String name;
    private final NodeFetcher node_fetcher;
    public final ValueFetcher active_fetcher;
    public ServerSync(final ZKProcess zkprocess,final Args args,final String node,final String name) {
        this.args=args;
        this.zkprocess=zkprocess;
        this.node=node;
        this.name=name;
        this.node_fetcher=new NodeFetcher(zkprocess,args.GetRegistersPath()+"/"+node+"/"+name);
        this.active_fetcher=new ValueFetcher(zkprocess,args.GetRegistersPath()+"/"+node+"/"+name+"/active");
    }
    public String name() {
        return this.name;
    }
    public void Close() {
        node_fetcher.Close();
        active_fetcher.Close();
    }
}
