package et.naruto.election;

import org.apache.zookeeper.CreateMode;

import et.naruto.base.Util;
import et.naruto.base.Util.DIAG;
import et.naruto.base.Util.Diag.LEVEL;
import et.naruto.process.base.Processer;
import et.naruto.process.zk.ValueFetcher;
import et.naruto.process.zk.ValueRegister;
import et.naruto.process.zk.ZKProcess;
import et.naruto.versioner.Dealer;
import et.naruto.versioner.Dealer.IMap;
import et.naruto.versioner.Outer;
import et.naruto.versioner.base.Handleable;
import et.naruto.versioner.base.Versioner;

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
            final Handleable<Resolution> follower_resolution_out_handler,
            final Handleable<ResolutionRegister> resolution_register_handler) {
        final Handleable<ValueRegister.Result> resolution_register_result_handler=
            resolution_register_handler.result==null?
                null
                :resolution_register_handler.result.dealer.result_handleable();
        if(this.dealer.Watch(
                follower_resolution_out_handler,
                resolution_register_handler,
                resolution_register_result_handler
        )) {
            if(follower_resolution_out_handler.result!=null) {
                long seq=-1;
                byte[] need_closed=null;
                byte[] need_regist=null;
                if(resolution_register_handler.result!=null) {
                    if(resolution_register_handler.result.seq==follower_resolution_out_handler.result.seq) {
                        if(follower_resolution_out_handler.result.closed) {
                            //next seq
                            seq=follower_resolution_out_handler.result.seq+1;
                            need_regist=follower_resolution_out_handler.result.data;
                        } else {
                            do{
                                if(resolution_register_result_handler!=null) {
                                    if(resolution_register_result_handler.result!=null) {
                                        if(!resolution_register_result_handler.result.ok()) {
                                            seq=follower_resolution_out_handler.result.seq+1;
                                            need_closed=follower_resolution_out_handler.result.data;
                                            break;
                                        }
                                    }
                                }
                                need_regist=follower_resolution_out_handler.result.data;
                            } while(false);
                        }
                    } else {
                        if(resolution_register_handler.result.seq==(follower_resolution_out_handler.result.seq+1)) {
                        } else {
                            seq=follower_resolution_out_handler.result.seq+1;
                        }
                        if(follower_resolution_out_handler.result.closed) {
                            need_regist=follower_resolution_out_handler.result.data;
                        } else {
                            need_closed=follower_resolution_out_handler.result.data;
                        }
                    }
                } else {
                    seq=follower_resolution_out_handler.result.seq+1;
                    if(follower_resolution_out_handler.result.closed) {
                        need_regist=follower_resolution_out_handler.result.data;
                    } else {
                        need_closed=follower_resolution_out_handler.result.data;
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
        final Handleable<ValueFetcher.Result> follower_leader_flag_handler,
        final Handleable<Resolution> follower_resolution_handler,
        final Handleable<ResolutionRegister> resolution_register_handler
        ) {
        if(this.dealer.Watch(
            follower_leader_flag_handler.versionable,
            resolution_register_handler.versionable,
            follower_resolution_handler.versionable,
            resolution_register_handler.result==null?null:resolution_register_handler.result.dealer.result_versionable()
        )) {
            if(follower_leader_flag_handler.result!=null) {
                if(follower_leader_flag_handler.result.value.equals(args.GetServerId())) {
                    if(resolution_register_handler.result!=null
                        &&resolution_register_handler.result.dealer.result()!=null
                        &&resolution_register_handler.result.dealer.result().ok()
                    ) {
                        if(follower_resolution_handler.result!=null) {
                            if(resolution_register_handler.result.seq==follower_resolution_handler.result.seq) {
                                if(follower_resolution_handler.result.data!=null) {
                                    this.dealer.Done(new LeaderInfo(follower_resolution_handler.result.data));
                                    DIAG.Log.d.info(String.format("%s leader catched",args.toString()));
                                    return true;
                                }
                            }
                        }
                    }
                    this.dealer.Done(new LeaderInfo(null));
                    DIAG.Log.d.info(String.format("%s preleader catched",args.toString()));
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
    public ResolutionRegister(final long seq,final ZKProcess zkprocess,final Args args,final Dealer<ValueRegister.Result> old) {
        this.seq=seq;
        this.zkprocess=zkprocess;
        this.args=args;
        this.dealer=(old==null)?new Dealer():old;
    }
    public void Close() {
        if(this.closed!=null) {
            this.closed.Close();
        }
        if(this.registed!=null) {
            this.registed.Close();
        }
    }
    
    public final Dealer<ValueRegister.Result> dealer;
    private Outer<Boolean> closeing=null;
    private ValueRegister closed=null;
    private Outer<byte[]> registing=null;
    private ValueRegister registed=null;
    public final boolean Done(
        final Handleable<NeedResolution> current_need_resolution_handler
    ) {
        boolean next=false;
        if(this.dealer.Watch(
            current_need_resolution_handler.versionable
        )) {
            if(current_need_resolution_handler.result!=null) {
                final NeedResolution need_resolution=current_need_resolution_handler.result;
                if(need_resolution.need_closed!=null) {
                    if(this.closeing==null) {
                        this.closeing=this.CloseResolution(this.seq-1,need_resolution.need_closed);
                        DIAG.Log.d.debug(String.format("%s closeing %s resolution",args.toString(),seq));
                    }
                }
                if(need_resolution.need_registed!=null) {
                    if(this.registing==null) {
                        this.registing=this.NewResolution(seq,need_resolution.need_registed);
                        DIAG.Log.d.debug(String.format("%s registing %s resolution",args.toString(),seq));
                    }
                }
            }
            next=true;
        }
        
        if(this.closeing!=null) {
            Boolean is_closeing=this.closeing.Fetch();
            if(is_closeing!=null) {
                ValueClosed();
                DIAG.Log.d.debug(String.format("%s closed %s resolution",args.toString(),seq));
                next=true;
            }
        }
        if(this.registing!=null) {
            byte[] new_bytes=this.registing.Fetch();
            if(new_bytes!=null) {
                ValueRegisted(new_bytes);
                DIAG.Log.d.debug(String.format("%s registed %s resolution",args.toString(),seq));
                next=true;
            }
        }
        
        if(this.registed!=null) {
            ValueRegister.Result result=this.dealer.SyncDone(this.registed.handleable());
            if(result!=null) {
                DIAG.Log.d.debug(String.format("%s output %s resolution",args.toString(),seq));
                next=true;
            }
        }
        return next;
    }
    
    public abstract Outer<Boolean> CloseResolution(long seq,byte[] data);
    public abstract Outer<byte[]> NewResolution(long seq,byte[] data);
    
    private void ValueClosed() {
        if(this.closed!=null) {
            DIAG.Log.d.Error(""+seq);
        }
        this.closed=new ValueRegister(
            zkprocess,
            new ValueRegister.Request(
                args.GetResolutionsClosedPath()+"/"+Util.Long2String(seq-1),
                args.GetServerId(),
                CreateMode.PERSISTENT
            )
        ) {
            public void DoCallback(final Result result) {
                DIAG.Log.d.debug(String.format("%s closed %s resolution callback",args.toString(),seq));
            }
        };
    }
    private void ValueRegisted(final byte[] data) {
        if(this.registed!=null) {
            DIAG.Log.d.Error(""+seq);
        }
        this.registed=new ValueRegister(
            zkprocess,
            new ValueRegister.Request(
                args.GetResolutionsPath()+"/"+Util.Long2String(seq),
                data,
                CreateMode.PERSISTENT
            )
        ) {
            public void DoCallback(final Result result) {
                DIAG.Log.d.debug(String.format("%s registed %s resolution callback",args.toString(),seq));
            }
        };
    }
}

class PreLeaderRegister implements Processer {
    public final ValueFetcher flag_fetcher;
    private final Versioner flag_versioner=new Versioner();
    private final ValueRegister flag_register;
    private final ZKProcess zkprocess;
    public PreLeaderRegister(final String leader_path,final String server_id,final ZKProcess zkprocess) {
        this.zkprocess=zkprocess;
        this.flag_fetcher=new ValueFetcher(zkprocess,leader_path);
        this.flag_register=new ValueRegister(
            zkprocess,
            new ValueRegister.Request(
                leader_path,
                server_id,
                CreateMode.EPHEMERAL
            )
        );
        zkprocess.AddProcesser(this);
    }
    public void Close() {
        this.flag_fetcher.Close();
        this.flag_register.Close();
        this.zkprocess.DelProcesser(this);
    }
    public boolean Do() {
        boolean next=false;
        final ValueFetcher.Result leader_info=this.flag_versioner.Fetch(flag_fetcher.handleable());
        if(leader_info!=null) {
            if(leader_info.value.isEmpty()) {
                flag_register.ReRequest();
            }
            next=true;
        }
        return next;
    }
}

public abstract class Master {
    public String toString() {
        return String.format("Master[pleader=%s,leader=%s,%s]",IsPreLeader(),IsLeader(),resolution_register);
    }
    
    private final PreLeaderRegister pre_leader_register;
    private final Dealer<ResolutionRegister> resolution_register=new Dealer();
    private final CurrentNeedResolution current_need_resolution=new CurrentNeedResolution();
    
    private final Args args;
    private final ZKProcess zkprocess;
    private final LeaderCatch leader_catch;
    
    
    public Master(final Args args,final ZKProcess zkprocess) {
        this.args=args;
        this.zkprocess=zkprocess;
        this.pre_leader_register=new PreLeaderRegister(args.GetLeaderPath(),args.GetServerId(),zkprocess);
        this.leader_catch=new LeaderCatch(args);
        
    }
    public boolean Done(final Follower follower) {
        
        boolean next=false;
        if(this.leader_catch.Done(
            pre_leader_register.flag_fetcher.handleable(),
            follower.resolution_out.dealer.result_handleable(),
            this.resolution_register.result_handleable()
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
                                return new ResolutionRegister(src.seq,zkprocess,args,ret==null?null:ret.dealer) {
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
                this.current_need_resolution.dealer.result_handleable()
            )) {
                next=true;
            }
        
            if(this.resolution_register.result()!=null) {
                if(this.resolution_register.result().Done(this.current_need_resolution.dealer.result_handleable())) {
                    next=true;
                }
            }
            
            if(this.current_need_resolution.Done(
                follower.resolution_out.dealer.result_handleable(),
                this.resolution_register.result_handleable()
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
