package et.naruto.register;

import org.apache.zookeeper.CreateMode;

import et.naruto.election.Args;
import et.naruto.process.NodeFetcherX;
import et.naruto.process.ValueRegister;
import et.naruto.process.ZKProcess;
import et.naruto.versioner.Dealer;
import et.naruto.versioner.Versioner;

public class RegistersSync extends NodeFetcherX<NodeSync> {
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
