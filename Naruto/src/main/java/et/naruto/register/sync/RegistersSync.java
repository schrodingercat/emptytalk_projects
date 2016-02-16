package et.naruto.register.sync;

import org.apache.zookeeper.CreateMode;

import et.naruto.elect.Args;
import et.naruto.process.zk.NodeFetcher;
import et.naruto.process.zk.ValueRegister;
import et.naruto.process.zk.ZKProcess;
import et.naruto.versioner.Dealer;
import et.naruto.versioner.base.Versioner;

public class RegistersSync extends NodeFetcher<NodeSync> {
    private final Args args;
    private final ZKProcess zkprocess;
    public RegistersSync(final ZKProcess zkprocess,final Args args) {
        super(zkprocess,args.GetRegistersPath());
        this.args=args;
        this.zkprocess=zkprocess;
    }
    
    public NodeSync DoCreateX(String child) {
        return new NodeSync(zkprocess,args,child);
    }
}
