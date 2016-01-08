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
    
    
    public void Close() {
        super.Close();
        if(registers_register!=null) {
            registers_register.Close();
        }
        if(node_register!=null) {
            node_register.Close();
        }
        if(server_register!=null) {
            server_register.Close();
        }
    }
    
    private ValueRegister registers_register=null;
    private ValueRegister node_register=null;
    private NodeSync this_node_sync=null;
    private ValueRegister server_register=null;
    private final Versioner error_versioner=new Versioner();
    public final Dealer<ServerSync> dealer=new Dealer();
    public boolean Do() {
        boolean next=false;
        if(super.Do()) {
            next=true;
        }
        if(dealer.Watch(
            super.nodes_fetcher.dealer.result_versionable(),
            super.dealer.result_versionable(),
            this_node_sync==null?null:this_node_sync.dealer.result_versionable()
        )) {
            if(super.nodes_fetcher.dealer.result()!=null) {
                if(!super.nodes_fetcher.dealer.result().exist()) {
                    if(registers_register==null) {
                        registers_register=new ValueRegister(
                            this.zkprocess,
                            new ValueRegister.Request(args.GetRegistersPath(),args.GetServerId(),CreateMode.PERSISTENT)
                        );
                    }
                }
            }
            if(super.dealer.result()!=null) {
                if(this_node_sync==null) {
                    if(!super.dealer.result().containsKey(args.node_path)) {
                        if(node_register==null) {
                            node_register=new ValueRegister(
                                this.zkprocess,
                                new ValueRegister.Request(args.GetNodePath(),args.GetServerId(),CreateMode.PERSISTENT)
                            );
                        }
                    } else {
                        this_node_sync=super.dealer.result().get(args.node_path);
                    }
                }
            }
            if(this_node_sync!=null) {
                if(this_node_sync.dealer.result()!=null) {
                    if(this.dealer.result()==null) {
                        if(!this_node_sync.dealer.result().containsKey(args.server_num)) {
                            if(server_register==null) {
                                server_register=new ValueRegister(
                                    this.zkprocess,
                                    new ValueRegister.Request(args.GetServerPath(),args.GetServerId(),CreateMode.PERSISTENT)
                                );
                            }
                        } else {
                            this.dealer.Done(this_node_sync.dealer.result().get(args.server_num));
                        }
                    }
                }
            }
            next=true;
        }
        if(error_versioner.Watch(
            null,
            this.registers_register==null?null:this.registers_register.handler().versionable(),
            this.node_register==null?null:this.node_register.handler().versionable(),
            this.server_register==null?null:this.server_register.handler().versionable())
        ) {
            if(this.registers_register!=null) {
                if(this.registers_register.result()!=null) {
                    if(this.registers_register.result().succ==null) {
                        if(super.nodes_fetcher.dealer.result()!=null) {
                            if(!super.nodes_fetcher.dealer.result().exist()) {
                                if(!this.registers_register.doing()) {
                                    this.registers_register.ReRequest();
                                }
                            }
                        }
                    }
                }
            }
            if(this.node_register!=null) {
                if(this.node_register.result()!=null) {
                    if(this.node_register.result().succ==null) {
                        if(super.dealer.result()!=null) {
                            if(!super.dealer.result().containsKey(args.node_path)) {
                                if(!this.node_register.doing()) {
                                    this.node_register.ReRequest();
                                }
                            }
                        }
                    }
                }
            }
            if(this.server_register!=null) {
                if(this.server_register.result()!=null) {
                    if(this.server_register.result().succ==null) {
                        if(this.this_node_sync!=null) {
                            if(this.this_node_sync.dealer.result()!=null) {
                                if(!this.this_node_sync.dealer.result().containsKey(args.server_num)) {
                                    if(!this.server_register.doing()) {
                                        this.server_register.ReRequest();
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        return next;
    }
}
