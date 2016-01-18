package et.naruto.election;

import et.naruto.base.Util.ZKArgs;
import et.naruto.process.zk.ZKProcess;
import et.naruto.versioner.Outer;


public class Server extends FollowerServer {
    @Override
    public String toString() {
        return String.format("Server [args=%s,f=%s,m=%s]",args,follower,master);
    }
    
    private final Server server_ref;
    public final Master master;
    
    public Server(final Args args,final ZKArgs zkargs) {
        super(args,zkargs);
        this.server_ref=this;
        this.master=new Master(args,zkprocess) {
            public Outer<Boolean> CloseResolution(long seq,byte[] data) {
                return server_ref.CloseResolution(seq,data);
            }
            public Outer<byte[]> NewResolution(long seq,byte[] data) {
                return server_ref.NewResolution(seq,data);
            }
        };
    }
    public Outer<Boolean> CloseResolution(long seq,byte[] data) {
        Outer<Boolean> o=new Outer();
        o.Add(true);
        return o;
    }
    public Outer<byte[]> NewResolution(long seq,byte[] data) {
        Outer<byte[]> o=new Outer();
        o.Add(("test:"+seq).getBytes());
        return o;
    }
    public void DoRun() {
        if(this.master.Done(this.follower)) {
            this.zkprocess.Tick();
        }
    }
}
