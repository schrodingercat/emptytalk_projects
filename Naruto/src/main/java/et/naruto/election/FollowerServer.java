package et.naruto.election;

import et.naruto.base.Util.ZKArgs;
import et.naruto.process.zk.ZKProcess;
import et.naruto.versioner.Outer;

public class FollowerServer {
    @Override
    public String toString() {
        return String.format("FollowerServer [args=%s,f=%s]",args,follower);
    }
    
    private final FollowerServer server_ref;
    protected final Args args;
    protected final ZKProcess zkprocess;
    public final Follower follower;
    
    public FollowerServer(final Args args,final ZKArgs zkargs) {
        this.server_ref=this;
        this.args=args;
        this.zkprocess=new ZKProcess(args.GetServerId(),zkargs) {
            public void DoRun() {
                if(follower.Done()) {
                    zkprocess.Tick();
                }
                server_ref.DoRun();
            }
        };
        this.follower=new Follower(args,zkprocess);
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
