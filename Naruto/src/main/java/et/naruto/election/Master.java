package et.naruto.election;

import org.apache.zookeeper.CreateMode;

import et.naruto.base.Util;
import et.naruto.base.Util.DIAG;
import et.naruto.process.ValueFetcher;
import et.naruto.process.ValueRegister;
import et.naruto.process.ZKProcess;
import et.naruto.versioner.Dealer;
import et.naruto.versioner.Dealer.IMap;
import et.naruto.versioner.Handler;
import et.naruto.versioner.Outer;
import et.naruto.versioner.Versioner;

class NeedResolution {
    
    public String toString() {
        return String.format(
            "NeedResolution(seq=%s,closed=%d,regist=%d)",
            seq,
            need_closed==null?null:need_closed.length,
            need_registed==null?null:need_registed.length
        );
    }
    
    public final long seq;
    public final byte[] need_closed;
    public final byte[] need_registed;
    
    public NeedResolution(
            final long seq,
            final byte[] need_closed,
            final byte[] need_registed
    ) {
        this.seq=seq;
        this.need_closed=need_closed;
        this.need_registed=need_registed;
    }
    
}

class CurrentNeedResolution {
    
    public final Dealer<NeedResolution> dealer=new Dealer();
    
    public boolean Done(
            Handler<Resolution> follower_resolution_out_handler,
            Handler<ResolutionRegister> resolution_register_handler) {
        if(this.dealer.Watch(
                follower_resolution_out_handler.versionable(),
                resolution_register_handler.versionable(),
                resolution_register_handler.result()==null?null:resolution_register_handler.result().dealer.result_versionable()
        )) {
            if(follower_resolution_out_handler.result()!=null) {
                long seq=-1;
                byte[] need_closed=null;
                byte[] need_regist=null;
                if(resolution_register_handler.result()!=null) {
                    if(resolution_register_handler.result().seq==follower_resolution_out_handler.result().seq) {
                        if(follower_resolution_out_handler.result().closed) {
                            //next seq
                            seq=follower_resolution_out_handler.result().seq+1;
                            need_regist=follower_resolution_out_handler.result().data;
                        } else {
                            do{
                                if(resolution_register_handler.result().dealer.result()!=null) {
                                    if(!resolution_register_handler.result().dealer.result().ok()) {
                                        seq=follower_resolution_out_handler.result().seq+1;
                                        need_closed=follower_resolution_out_handler.result().data;
                                        break;
                                    }
                                }
                                need_regist=follower_resolution_out_handler.result().data;
                            } while(false);
                        }
                    } else {
                        if(resolution_register_handler.result().seq==(follower_resolution_out_handler.result().seq+1)) {
                        } else {
                            seq=follower_resolution_out_handler.result().seq+1;
                        }
                        if(follower_resolution_out_handler.result().closed) {
                            need_regist=follower_resolution_out_handler.result().data;
                        } else {
                            need_closed=follower_resolution_out_handler.result().data;
                        }
                    }
                } else {
                    seq=follower_resolution_out_handler.result().seq+1;
                    if(follower_resolution_out_handler.result().closed) {
                        need_regist=follower_resolution_out_handler.result().data;
                    } else {
                        need_closed=follower_resolution_out_handler.result().data;
                    }
                }
                this.dealer.Done(new NeedResolution(seq,need_closed,need_regist));
                return true;
            }
        }
        return false;
    }
    
}

class LeaderInfo {
    public final byte[] data;
    public LeaderInfo(final byte[] data) {
        this.data=data;
    }
}

class LeaderCatch {
    
    private final Args args;
    protected LeaderCatch(final Args args) {
        this.args=args;
    }
    
    public final Dealer<LeaderInfo> dealer=new Dealer();
    public boolean Done(
        final Handler<ValueFetcher.Result> follower_leader_flag_handler,
        final Handler<Resolution> follower_resolution_handler,
        final Handler<ResolutionRegister> resolution_register_handler
        ) {
        if(this.dealer.Watch(
            follower_leader_flag_handler.versionable(),
            resolution_register_handler.versionable(),
            follower_resolution_handler.versionable(),
            resolution_register_handler.result()==null?null:resolution_register_handler.result().dealer.handler.versionable()
        )) {
            if(follower_leader_flag_handler.result()!=null) {
                if(follower_leader_flag_handler.result().value.equals(args.GetServerId())) {
                    if(resolution_register_handler.result()!=null
                        &&resolution_register_handler.result().dealer.result()!=null
                        &&resolution_register_handler.result().dealer.result().ok()
                    ) {
                        if(follower_resolution_handler.result()!=null) {
                            if(resolution_register_handler.result().seq==follower_resolution_handler.result().seq) {
                                if(follower_resolution_handler.result().data!=null) {
                                    this.dealer.Done(new LeaderInfo(follower_resolution_handler.result().data));
                                    return true;
                                }
                            }
                        }
                    }
                    this.dealer.Done(new LeaderInfo(null));
                    return true;
                }
            }
        }
        return false;
    }
    
}



abstract class ResolutionRegister {
    
    public String toString() {
        return String.format("RR[seq=%s,closing=%s,closed=%s,registing=%s,registed=%s]",seq,closeing,closed,registing,registed);
    }
    
    public final long seq;
    private final ZKProcess zkprocess;
    private final Args args;
    public ResolutionRegister(final long seq,final ZKProcess zkprocess,final Args args) {
        this.seq=seq;
        this.zkprocess=zkprocess;
        this.args=args;
    }
    public void Close() {
        if(this.closed!=null) {
            this.closed.Close();
        }
        if(this.registed!=null) {
            this.registed.Close();
        }
    }
    
    public final Dealer<ValueRegister.Result> dealer=new Dealer();
    private Outer<Boolean> closeing=null;
    private ValueRegister closed=null;
    private Outer<byte[]> registing=null;
    private ValueRegister registed=null;
    public final boolean Done(
        final Handler<NeedResolution> current_need_resolution_handler
    ) {
        boolean next=false;
        if(this.dealer.Watch(
            current_need_resolution_handler.versionable()
        )) {
            if(current_need_resolution_handler.result()!=null) {
                final NeedResolution need_resolution=current_need_resolution_handler.result();
                if(need_resolution.need_closed!=null) {
                    if(this.closeing==null) {
                        this.closeing=this.CloseResolution(this.seq-1,need_resolution.need_closed);
                    }
                }
                if(need_resolution.need_registed!=null) {
                    if(this.registing==null) {
                        this.registing=this.NewResolution(seq,need_resolution.need_registed);
                    }
                }
            }
            next=true;
        }
        
        if(this.closeing!=null) {
            Boolean is_closeing=this.closeing.Out();
            if(is_closeing!=null) {
                ValueClosed();
                next=true;
            }
        }
        if(this.registing!=null) {
            byte[] new_bytes=this.registing.Out();
            if(new_bytes!=null) {
                ValueRegisted(new_bytes);
                next=true;
            }
        }
        
        if(this.registed!=null) {
            ValueRegister.Result result=this.dealer.handler.Sync(this.registed.handler());
            if(result!=null) {
                next=true;
            }
        }
        return next;
    }
    
    public abstract Outer<Boolean> CloseResolution(long seq,byte[] data);
    public abstract Outer<byte[]> NewResolution(long seq,byte[] data);
    
    private void ValueClosed() {
        if(this.closed!=null) {
            DIAG.Get.d.Error(""+seq);
        }
        this.closed=new ValueRegister(
            zkprocess,
            new ValueRegister.Request(
                args.GetResolutionsClosedPath()+"/"+Util.Long2String(seq-1),
                args.GetServerId(),
                CreateMode.PERSISTENT
            )
        );
    }
    private void ValueRegisted(final byte[] data) {
        if(this.registed!=null) {
            DIAG.Get.d.Error(""+seq);
        }
        this.registed=new ValueRegister(
            zkprocess,
            new ValueRegister.Request(
                args.GetResolutionsPath()+"/"+Util.Long2String(seq),
                data,
                CreateMode.PERSISTENT
            )
        );
    }
}

public abstract class Master {
    public String toString() {
        return String.format("Master[pleader=%s,leader=%s,%s]",IsPreLeader(),IsLeader(),resolution_register);
    }
    
    private final Versioner follower_leader_flag_versioner=new Versioner();
    private final Dealer<ResolutionRegister> resolution_register=new Dealer();
    private final CurrentNeedResolution current_need_resolution=new CurrentNeedResolution();
    
    private final Args args;
    private final ZKProcess zkprocess;
    private final ValueRegister leader_flag_register;
    private final LeaderCatch leader_catch;
    
    
    public Master(final Args args,final ZKProcess zkprocess) {
        this.args=args;
        this.zkprocess=zkprocess;
        this.leader_flag_register=new ValueRegister(
            zkprocess,
            new ValueRegister.Request(
                args.GetLeaderPath(),
                args.GetServerId(),
                CreateMode.EPHEMERAL
            )
        );
        this.leader_catch=new LeaderCatch(args);
        
    }
    public boolean Done(final Follower follower) {
        
        boolean next=false;
        final ValueFetcher.Result leader_info=follower.leader_flag_fetcher.handler().Output(this.follower_leader_flag_versioner);
        if(leader_info!=null) {
            if(leader_info.value.isEmpty()) {
                leader_flag_register.ReRequest();
            }
            next=true;
        }
        if(this.leader_catch.Done(
            follower.leader_flag_fetcher.handler(),
            follower.resolution_out.dealer.handler,
            this.resolution_register.handler
        )) {
            next=true;
        }
        
        
        if(IsPreLeader()) {
            final Master master_ref=this;
            if(this.resolution_register.Map(
                new IMap<NeedResolution,ResolutionRegister>(){
                    public ResolutionRegister map(final ResolutionRegister ret,final NeedResolution src) {
                        if(src!=null) {
                            if(src.seq>-1) {
                                if(ret!=null) {
                                    ret.Close();
                                }
                                return new ResolutionRegister(src.seq,zkprocess,args) {
                                    public Outer<Boolean> CloseResolution(long seq,byte[] data) {
                                        return master_ref.CloseResolution(seq,data);
                                    }
                                    public Outer<byte[]> NewResolution(long seq,byte[] data) {
                                        return master_ref.NewResolution(seq,data);
                                    }
                                };
                            }
                        }
                        return null;
                    }
                },
                this.current_need_resolution.dealer.handler
            )) {
                next=true;
            }
        
            if(this.resolution_register.result()!=null) {
                if(this.resolution_register.result().Done(this.current_need_resolution.dealer.handler)) {
                    next=true;
                }
            }
            
            if(this.current_need_resolution.Done(
                follower.resolution_out.dealer.handler,
                this.resolution_register.handler
            )) {
                next=true;
            }
        }
        
        return next;
    }
    
    final public boolean IsPreLeader() {
        if(this.leader_catch.dealer.result()!=null) {
            return true;
        }
        return false;
    }
    final public boolean IsLeader() {
        if(this.leader_catch.dealer.result()!=null) {
            if(this.leader_catch.dealer.result().data!=null) {
                return true;
            }
        }
        return false;
    }
    public abstract Outer<Boolean> CloseResolution(long seq,byte[] data);
    public abstract Outer<byte[]> NewResolution(long seq,byte[] data);

}
