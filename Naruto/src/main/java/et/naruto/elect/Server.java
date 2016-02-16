package et.naruto.elect;

import et.naruto.base.Util.ZKArgs;
import et.naruto.process.zk.ZKProcess;

public class Server {
    @Override
    public String toString() {
        return String.format("FollowerServer [args=%s,f=%s]",args,master);
    }
    
    private final Server server_ref;
    protected final Args args;
    protected final ZKProcess zkprocess;
    public final Master master;
    
    public Server(final Args args,final ZKArgs zkargs) {
        this.server_ref=this;
        this.args=args;
        this.zkprocess=new ZKProcess(args.GetServerId(),zkargs) {
            public void DoRun() {
                server_ref.DoRun();
            }
        };
        this.master=new Master(args,zkprocess);
    }
    final public void Start() {
        this.zkprocess.Start();
    }
    final public void Stop() {
        this.zkprocess.Stop();
    }
    
    public void DoRun() {
    }

}
