package et.naruto;

import org.apache.zookeeper.CreateMode;

import et.naruto.Util.DIAG;
import et.naruto.Util.ZKArgs;

class NeedResolution {
    @Override
	public String toString() {
		return "NeedResolution [seq=" + seq + ", need_closed=" + need_closed
				+ ", need_regist=" + need_regist + "]";
	}
	public final long seq;
    public final boolean need_closed;
    public final boolean need_regist;
    public NeedResolution(final long seq,final boolean need_closed,final boolean need_regist) {
        this.seq=seq;
        this.need_closed=need_closed;
        this.need_regist=need_regist;
    }
}

public class Server {
    private class ResolutionRegister {
        public final long seq;
        public ValueRegister closed=null;
        public ValueRegister resolution=null;
        public ResolutionRegister(final long seq) {
            this.seq=seq;
        }
        public void RegistClosed(final ZKProcess zkprocess) {
            if(this.closed!=null) {
                this.closed.Close();
            }
            this.closed=new ValueRegister(
                zkprocess,
                new ValueRegister.Request(
                    args.GetResolutionsClosedPath()+"/"+(seq-1),
                    args.GetServerId(),
                    CreateMode.PERSISTENT
                )
            );
        }
        public void RegistResolution(final ZKProcess zkprocess) {
            if(this.resolution!=null) {
                this.resolution.Close();
            }
            this.resolution=new ValueRegister(
                zkprocess,
                new ValueRegister.Request(
                    args.GetResolutionsPath()+"/"+seq,
                    args.GetServerId(),
                    CreateMode.PERSISTENT
                )
            );
        }
        public void Close() {
            if(this.closed!=null) {
                this.closed.Close();
            }
            if(this.resolution!=null) {
                this.resolution.Close();
            }
        }
    }
    @Override
    public String toString() {
        return String.format("Server [args=%s,is_pre_leader=%s,is_leader=%s,seq=%s]",args,is_pre_leader_out.result,is_leader_out.result,resolution_register.resolution.request().GetRegisterName());
    }
    
    private final Args args;
    private final ZKProcess zkprocess;
    private final Follower follower;
    private final ValueRegister leader_flag_register;
    private ResolutionRegister resolution_register=null;
    private final FlowOut<NeedResolution> need_resolution_out=new FlowOut();
    public final Outer<Boolean> is_pre_leader_out=new Outer();
    public final FlowOut<Boolean> is_leader_out=new FlowOut();
    
    public Server(final Args args,final ZKArgs zkargs) {
        this.args=args;
        this.zkprocess=new ZKProcess(args.GetServerId(),zkargs) {
            public void DoRun() {
                Server_DoRun();
            }
        };
        this.follower=new Follower(args,zkprocess);
        this.leader_flag_register=new ValueRegister(
            zkprocess,
            new ValueRegister.Request(
                args.GetLeaderPath(),
                args.GetServerId(),
                CreateMode.EPHEMERAL
            )
        );
        
    }
    private NeedResolution GetNeedResolution() {
        Resolution fetched_resolution=follower.resolution_out.result;
        if(fetched_resolution!=null) {
            long seq=-1;
            boolean need_closed=false;
            boolean need_regist=false;
            if(this.resolution_register!=null) {
                if(this.resolution_register.seq==fetched_resolution.seq) {
                    if(fetched_resolution.closed) {
                        //next seq
                        seq=fetched_resolution.seq+1;
                    }
                    need_regist=true;
                } else {
                    if(this.resolution_register.seq==(fetched_resolution.seq+1)) {
                    } else {
                        seq=fetched_resolution.seq+1;
                    }
                    if(fetched_resolution.closed) {
                        need_regist=true;
                    } else {
                        need_closed=true;
                    }
                }
            } else {
                seq=fetched_resolution.seq+1;
                if(fetched_resolution.closed) {
                    need_regist=true;
                } else {
                    need_closed=true;
                }
            }
            return new NeedResolution(seq,need_closed,need_regist);
        }
        return null;
    }
    private void Server_DoRun() {
        follower.Follower_DoRun();
        
        final ValueFetcher.Result leader_info=this.follower.leader_flag_fetcher.Fetch();if(leader_info!=null) {
            if(IsPreLeader()) {
                int i=0;
                i++;
            }
            if(leader_info.value.isEmpty()) {
                leader_flag_register.ReRequest();
            }
            is_leader_out.AddIn();
            is_pre_leader_out.AddOut(IsPreLeader());
        }
        
        
        if(is_pre_leader_out.result!=null&&is_pre_leader_out.result) {
            Resolution fetched_resolution=follower.resolution_out.Fetch();if(fetched_resolution!=null) {
                is_leader_out.AddIn();
                need_resolution_out.AddIn();
            }
        }
        
        NeedResolution need_resolution=need_resolution_out.Fetch();if(need_resolution!=null) {
            if(need_resolution.seq>-1) {
                if(this.resolution_register!=null) {
                    this.resolution_register.Close();
                }
                this.resolution_register=new ResolutionRegister(need_resolution.seq);
                need_resolution_out.AddIn();
                is_leader_out.AddIn();
            }
            if(need_resolution.need_closed) {
                if(this.resolution_register.closed!=null) {
                } else {
                    this.resolution_register.RegistClosed(this.zkprocess);
                }
                need_resolution_out.AddIn();
                is_leader_out.AddIn();
            }
            if(need_resolution.need_regist) {
                if(this.resolution_register.resolution!=null) {
                } else {
                    this.resolution_register.RegistResolution(this.zkprocess);
                }
                need_resolution_out.AddIn();
                is_leader_out.AddIn();
            }
        }
        
        if(this.resolution_register!=null) {
            if(this.resolution_register.closed!=null) {
                if(this.resolution_register.closed.Fetch()!=null) {
                    is_leader_out.AddIn();
                }
            }
            if(this.resolution_register.resolution!=null) {
                if(this.resolution_register.resolution.Fetch()!=null) {
                    is_leader_out.AddIn();
                }
            }
        }
        
        if(this.need_resolution_out.Doing()) {
            NeedResolution temp_need_resolution=GetNeedResolution();
            if(temp_need_resolution!=null) {
                need_resolution_out.Out(temp_need_resolution);
            }
        }
        
        if(is_leader_out.Doing()) {
            is_leader_out.Out(IsLeader());
        }
    }
    final private boolean IsPreLeader() {
        if(this.follower.leader_flag_fetcher.result()!=null) {
                return this.follower.leader_flag_fetcher.result().value.equals(args.GetServerId());
        }
        return false;
    }
    final private boolean IsLeader() {
        if(IsPreLeader()) {
            if(resolution_register!=null) {
                if(resolution_register.resolution!=null) {
                    if(resolution_register.resolution.result()!=null) {
                        if(follower.resolution_out.result!=null) {
                            if(resolution_register.seq==follower.resolution_out.result.seq) {
                                return true;
                            }
                        }
                    }
                }
            }
        }
        return false;
    }
    final public void Start() {
        this.zkprocess.Start();
    }
    final public void Stop() {
        this.zkprocess.Stop();
    }
}
