package et.naruto.register;

import java.util.TimerTask;

import org.apache.zookeeper.CreateMode;

import et.naruto.base.Util.DIAG;
import et.naruto.election.Args;
import et.naruto.process.base.Processer;
import et.naruto.process.zk.ValueRegister;
import et.naruto.process.zk.ZKProcess;
import et.naruto.versioner.Dealer;
import et.naruto.versioner.base.Versioner;

public abstract class RegistersUpdater implements Processer {
    private class Checking extends TimerTask {
        public void run() {
            zkprocess.zk.setData(args.GetActivePath(),DoGetActiveInfo(),-1,null,null);
        }
    }
    protected abstract byte[] DoGetActiveInfo();
    private final ZKProcess zkprocess;
    private final Args args;
    private final ValueRegister registers_register;
    public RegistersUpdater(final ZKProcess zkprocess,final Args args) {
        this.zkprocess=zkprocess;
        this.args=args;
        this.registers_register=new ValueRegister(
            this.zkprocess,
            new ValueRegister.Request(
                args.GetRegistersPath(),
                args.GetServerId(),
                CreateMode.PERSISTENT
            )
        );
        this.zkprocess.AddProcesser(this);
    }
    public void Close() {
        this.zkprocess.DelProcesser(this);
        this.registers_register.Close();
        if(this.node_register!=null) {
            this.node_register.Close();
        }
        if(this.server_register!=null) {
            this.server_register.Close();
        }
        if(this.active_register!=null) {
            this.active_register.Close();
        }
    }
    
    private ValueRegister node_register=null;
    private ValueRegister server_register=null;
    private ValueRegister active_register=null;
    private final Versioner flow_check=new Versioner();
    public boolean Do() {
        boolean next=false;
        if(flow_check.Watch(
            null,
            registers_register.result_versionable(),
            node_register==null?null:node_register.result_versionable(),
            server_register==null?null:server_register.result_versionable(),
            active_register==null?null:active_register.result_versionable()
        )) {
            if(active_register==null) {
                if(server_register==null) {
                    if(node_register==null) {
                        if(registers_register.result()!=null) {
                            if(registers_register.result().succ!=null) {
                                node_register=new ValueRegister(
                                    this.zkprocess,
                                    new ValueRegister.Request(
                                        args.GetNodePath(),
                                        args.GetServerId(),
                                        CreateMode.PERSISTENT
                                    )
                                );
                            } else {
                                this.zkprocess.tm.schedule(
                                    new TimerTask() {
                                        public void run() {
                                            registers_register.ReRequest();
                                        }
                                    },
                                    1000
                                );
                            }
                        }
                    } else {
                        if(node_register.result()!=null) {
                            if(node_register.result().succ!=null) {
                                server_register=new ValueRegister(
                                    this.zkprocess,
                                    new ValueRegister.Request(
                                        args.GetServerPath(),
                                        args.GetServerId(),
                                        CreateMode.PERSISTENT
                                    )
                                );
                            } else {
                                this.zkprocess.tm.schedule(
                                    new TimerTask() {
                                        public void run() {
                                            node_register.ReRequest();
                                        }
                                    },
                                    1000
                                );
                            }
                        }
                    }
                } else {
                    if(server_register.result()!=null) {
                        if(server_register.result().succ!=null) {
                            active_register=new ValueRegister(
                                this.zkprocess,
                                new ValueRegister.Request(
                                    args.GetActivePath(),
                                    DoGetActiveInfo(),
                                    CreateMode.EPHEMERAL
                                )
                            );
                        } else {
                            this.zkprocess.tm.schedule(
                                new TimerTask() {
                                    public void run() {
                                        server_register.ReRequest();
                                    }
                                },
                                1000
                            );
                        }
                    }
                }
            } else {
                if(active_register.result()!=null) {
                    do {
                        if(active_register.result().succ!=null) {
                            if(active_register.result().succ) {
                                this.zkprocess.tm.schedule(
                                    new Checking(),
                                    1000*30,
                                    1000*30
                                );
                                break;
                            }
                        }
                        this.zkprocess.tm.schedule(
                            new TimerTask() {
                                public void run() {
                                    DIAG.Log.d.info(String.format("active has been created, but not %s",args.GetServerId()));
                                    active_register.ReRequest();
                                }
                            },
                            1000
                        );
                    } while(false);
                }
            }
        }
        return next;
    }
    
}
