package et.naruto.register;

import et.naruto.base.Util.ZKArgs;
import et.naruto.elect.Args;
import et.naruto.process.zk.ZKProcess;
import et.naruto.register.sync.RegistersSync;

public class RegistersClient implements AutoCloseable {
    public String toString() {
        return String.format("RegisterServer(args=%s)",args);
    }
    
    private final RegistersClient server_ref;
    protected final Args args;
    public final ZKProcess zkprocess;
    public final RegistersSync registers_sync;
    
    public RegistersClient(final Args args,final ZKArgs zkargs) {
        this.server_ref=this;
        this.args=args;
        this.zkprocess=new ZKProcess(args.GetServerId(),zkargs) {
            public void DoRun() {
                server_ref.DoRun();
            }
        };
        this.registers_sync=new RegistersSync(this.zkprocess,args);
    }
    final public void Start() {
        this.zkprocess.Start();
    }
    final public void close() {
        this.zkprocess.close();
        this.registers_sync.close();
    }
    
    public void DoRun() {
    }
}
