package et.naruto.elect;

import et.naruto.base.Util.DIAG;
import et.naruto.process.base.Processer;
import et.naruto.process.zk.ZKProcess;
import et.naruto.resolutionsurface.Data;
import et.naruto.resolutionsurface.RSArgs;
import et.naruto.resolutionsurface.ResolutionSurface;
import et.naruto.seat.Seat;
import et.naruto.versioner.Dealer;
import et.naruto.versioner.base.Handleable;
import et.naruto.versioner.base.Handler;

public class Master implements Processer, AutoCloseable {
    public static class Result {
        public final Boolean catched;
        public final boolean IsPreLeader() {
            return catched!=null;
        }
        public final boolean IsLeader() {
            return IsPreLeader()&&catched;
        }
        public Result(final Boolean catched) {
            this.catched=catched;
        }
        public final String toString() {
            return String.format("Result(%s)", catched);
        }
    }
    private final Args args;
    private final Seat pre_master_seat;
    private final ResolutionSurface resolution_surface;
    private final ZKProcess zkprocess;
    private final Dealer<Result> dealer=new Dealer();
    public Master(final Args args,final ZKProcess zkprocess) {
        this.args=args;
        this.pre_master_seat=new Seat(args.GetLeaderPath(),args.GetServerId(),zkprocess);
        final Master this_ref=this;
        this.resolution_surface=new ResolutionSurface(zkprocess,new RSArgs(args.GetResolutionsPath(),args.GetServerId())) {
            public Handler<Boolean> CloseingResolution(final long seq,final Data data) {
                return this_ref.CloseingResolution(seq,data);
            }
        };
        this.zkprocess=zkprocess;
        zkprocess.AddProcesser(this);
    }
    public Handler<Boolean> CloseingResolution(final long seq,final Data data) {
        Handler<Boolean> r=new Handler();
        r.Add(true);
        return r;
    }
    public byte[] NewResolution(final long seq,final Data data) {
        return "test new resolution".getBytes();
    }
    public final String toString() {
        return String.format("Master[%s,%s]",this.args.toString(),this.dealer.toString());
    }
    public final void close() {
        this.zkprocess.DelProcesser(this);
        this.resolution_surface.close();
        this.pre_master_seat.close();
    }
    public final Handleable<Result> result_handleable() {
        return dealer.result_handleable();
    }
    public final boolean IsPreLeader() {
        if(result_handleable().result!=null) {
            return result_handleable().result.catched!=null;
        }
        return false;
    }
    public final boolean IsLeader() {
        if(IsPreLeader()) {
            return result_handleable().result.catched;
        }
        return false;
    }
    public final boolean Do() {
        boolean next=false;
        final Handleable<Boolean> pre_master_seat_result_handleable=pre_master_seat.result_handleable();
        final Handleable<ResolutionSurface.Result> resolution_surface_out_handleable=resolution_surface.out_handleable();
        if(dealer.Watch(
            pre_master_seat_result_handleable,
            resolution_surface_out_handleable
        )){
            if(pre_master_seat_result_handleable.result!=null) {
                if(pre_master_seat_result_handleable.result) {
                    if(resolution_surface_out_handleable.result!=null) {
                        if(resolution_surface_out_handleable.result.succ) {
                            
                            DIAG.Log.________________________________.D(
                                "%s leader catched %s.",
                                this.args.toString(),
                                this.resolution_surface.toString());
                            
                            this.dealer.Done(new Result(true));
                        } else {
                            final long next_seq=resolution_surface_out_handleable.result.resolution.seq+1;
                            boolean need_regist=false;
                            if(resolution_surface.in_request()!=null) {
                                if(resolution_surface.in_request().seq<next_seq) {
                                    
                                    DIAG.Log.________________________________.D(
                                        "%s Regist this next_seq %s.",
                                        this.args.toString(),
                                        next_seq);
                                    
                                    need_regist=true;
                                } else {
                                    if(resolution_surface.in_request().seq==next_seq) {
                                        
                                        DIAG.Log.________________________________.D(
                                            "%s Already regist this next_seq %s, Waiting.",
                                            this.args.toString(),
                                            next_seq);
                                        
                                    } else {
                                        
                                        DIAG.Log.________________________________.Error(
                                            "%s Serious ERROR, Resolution_surface.in is %s, But next_seq turn back to %s.",
                                            this.args.toString(),
                                            resolution_surface.in_request().seq,
                                            next_seq);
                                        
                                    }
                                }
                            } else {
                                DIAG.Log.________________________________.D(
                                    "%s First Regist next_seq %s",
                                    this.args.toString(),
                                    next_seq);
                                
                                need_regist=true;
                            }
                            if(need_regist) {
                                final byte[] new_resolution=NewResolution(
                                            next_seq,
                                            resolution_surface_out_handleable.result.resolution.data
                                );
                                if(new_resolution!=null) {
                                    this.resolution_surface.Regist(
                                        new ResolutionSurface.Request(
                                            new_resolution,
                                            next_seq
                                        )
                                    );
                                }
                            }
                            this.dealer.Done(new Result(false));
                        }
                    } else {
                        DIAG.Log.________________________________.D(
                            "%s Surface prepareing, Waiting.",
                            this.args.toString());
                        
                        this.dealer.Done(new Result(false));
                    }
                } else {
                    DIAG.Log.________________________________.D(
                        "%s Lost pre leader, Ignore.",
                        this.args.toString());
                    
                    this.dealer.Done(new Result(null));
                }
            } else {
                DIAG.Log.________________________________.D(
                    "%s pre leader catching, waiting.",
                    this.args.toString());
            }
            next=true;
        }
        return next;
    }
}
