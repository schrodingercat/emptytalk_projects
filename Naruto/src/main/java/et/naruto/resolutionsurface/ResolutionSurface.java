package et.naruto.resolutionsurface;

import java.util.TimerTask;

import org.apache.zookeeper.CreateMode;

import et.naruto.base.Util;
import et.naruto.base.Util.DIAG;
import et.naruto.process.base.Processer;
import et.naruto.process.zk.ValueRegister;
import et.naruto.process.zk.ZKProcess;
import et.naruto.versioner.Dealer;
import et.naruto.versioner.Outer;
import et.naruto.versioner.base.Handleable;
import et.naruto.versioner.base.Handler;

class RegistHandler implements AutoCloseable, Processer {
    public final long seq;
    private final byte[] byte_data;
    private final RSArgs args;
    private final ZKProcess zkprocess;
    private final ResolutionSync resolution_sync;
    private final Dealer<Boolean> dealer;
    
    private final ValueRegister closed;
    private final ValueRegister registed;
    private Handler<Boolean> closeing=null;
    public RegistHandler(final ZKProcess zkprocess,final ResolutionSync resolution_sync,final long seq,final byte[] byte_data,final RSArgs args) {
        this.dealer=new Dealer();
        this.seq=seq;
        this.byte_data=byte_data;
        this.args=args;
        this.zkprocess=zkprocess;
        this.resolution_sync=resolution_sync;
        this.closed=new ValueRegister(zkprocess,null);
        this.registed=new ValueRegister(zkprocess,null);
        this.zkprocess.AddProcesser(this);
        
        if(seq==10) {
            if(this.resolution_sync.current_resolution_handleable().result.seq==8) {
                int i=0;
                i++;
            }
        }
    }
    public final String toString() {
        return String.format("RegistHandler(%s,%s)",seq,args);
    }
    public final Handleable<Boolean> result_handleable() {
        return dealer.result_handleable();
    }
    public void close() {
        this.zkprocess.DelProcesser(this);
        this.closed.close();
        this.registed.close();
    }
    public boolean Do() {
        boolean next=false;
        Handleable<Resolution> current_resolution_handleable=this.resolution_sync.current_resolution_handleable();
        if(dealer.Watch(
            current_resolution_handleable.versionable,
            closed.result_versionable(),
            registed.result_versionable(),
            closeing==null?null:closeing.versionable()
        )) {
            if(current_resolution_handleable.result.seq+1==this.seq) {
                if(current_resolution_handleable.result.closed) {
                    DIAG.Log.________________________________________________________________.D(
                        "%s cseq:%s closed, need regist.",
                        this,
                        current_resolution_handleable.result);
                    
                    if(registed.request()!=null) {
                        //go wait registed end or failed
                        if(registed.result()!=null) {
                            
                            
                            if(registed.result().succ==null) {
                                //exception
                                if(!registed.doing()) {
                                    DIAG.Log.________________________________________________________________.D(
                                        "%s Regist seq, but exception arisesed, schedule rerequest after 1s, wait.",
                                        this);
                                    
                                    final RegistHandler this_ref=this;
                                    final long thread_id_ref=Thread.currentThread().getId();
                                    this.zkprocess.tm.schedule(
                                        new TimerTask() {
                                            public void run() {
                                                DIAG.Log.________________________________________________________________.D(
                                                    "%s schedule thread is [TID=%s], registed rerequest, wait.",
                                                    this_ref,
                                                    thread_id_ref);
                                                
                                                registed.ReRequest();
                                            }
                                        },
                                        1000
                                    );
                                } else {
                                    DIAG.Log.________________________________________________________________.D(
                                        "%s Regist seq, exception, bug registed is doing, wait.",
                                        this);
                                }
                            } else {
                                //failed or ok
                                DIAG.Log.________________________________________________________________.D(
                                    "%s Registed seq, succ=%s, wait for current resolution sync.",
                                    this,
                                    registed.result().succ);
                            }
                        } else {
                            DIAG.Log.________________________________________________________________.D(
                                "%s Registing seq, waiting.",
                                this);
                        }
                    } else {
                        final Data data=new Data(this.byte_data==null?new byte[0]:this.byte_data,this.args.token);
                        
                        DIAG.Log.________________________________________________________________.D(
                            "%s Regist next seq, %s.",
                            this,
                            data);
                        
                        this.registed.Request(
                            new ValueRegister.Request(
                                args.path+"/"+Util.Long2String(this.seq),
                                data.data,
                                CreateMode.PERSISTENT
                            )
                        );
                    }
                } else {
                    if(closed.request()!=null) {
                        //go wait closed end or failed
                        if(closed.result()!=null) {
                            if(closed.result().succ==null) {
                                //exception
                                if(!closed.doing()) {
                                    DIAG.Log.________________________________________________________________.D(
                                        "%s closed exception, so schedule closed Rerequest, wait.",
                                        this);
                                    
                                    final RegistHandler this_ref=this;
                                    final long thread_id_ref=Thread.currentThread().getId();
                                    this.zkprocess.tm.schedule(
                                        new TimerTask() {
                                            public void run() {
                                                DIAG.Log.________________________________________________________________.D(
                                                    "%s schedule thread is [TID=%s], closed rerequest, wait.",
                                                    this_ref,
                                                    thread_id_ref);
                                                
                                                closed.ReRequest();
                                            }
                                        },
                                        1000
                                    );
                                } else {
                                    DIAG.Log.________________________________________________________________.D(
                                        "%s closed exception, but closed is doing, wait.",
                                        this);
                                }
                            } else {
                                DIAG.Log.________________________________________________________________.D(
                                    "%s closed request is ok or failed, ignore.",
                                    this);
                            }
                        } else {
                            DIAG.Log.________________________________________________________________.D(
                                "%s wait for closed, ignore.",
                                this);
                        }
                    } else {
                        if(closeing!=null) {
                            if(this.closeing.result()!=null) {
                                DIAG.Log.________________________________________________________________.D(
                                    "%s Resolution seq is closeing complete, call closed request.",
                                    this);
                                
                                closed.Request(new ValueRegister.Request(args.closed_path()+"/"+Util.Long2String(current_resolution_handleable.result.seq),args.token,CreateMode.PERSISTENT));
                            } else {
                                //wait closeing end or failed
                                DIAG.Log.________________________________________________________________.D(
                                    "%s Wait closeing end or failed, ignore.",
                                    this);
                            }
                        } else {
                            final Boolean is_determined_for_closed=current_resolution_handleable.result.data.is_determined_for_closed();
                            if(is_determined_for_closed!=null) {
                                if(!is_determined_for_closed) {
                                    DIAG.Log.________________________________________________________________.D(
                                        "%s Closeing cseq:%s.",
                                        this,
                                        current_resolution_handleable.result);
                                    
                                    this.closeing=CloseingResolution(current_resolution_handleable.result.seq,current_resolution_handleable.result.data);
                                } else {
                                    DIAG.Log.________________________________________________________________.D(
                                        "%s Surface is closed by cseq:%s, Done false.",
                                        this,
                                        current_resolution_handleable.result);
                                    
                                    dealer.Done(false);
                                }
                            } else {
                                //wait for resolution determined
                                DIAG.Log.________________________________________________________________.D(
                                    "%s cseq:% undetermined, wait.",
                                    this,
                                    current_resolution_handleable.result);
                            }
                        }
                    }
                }
            } else {
                if(current_resolution_handleable.result.seq>=this.seq) {
                    DIAG.Log.________________________________________________________________.D(
                        "%s cseq:%s arrive seq, done.",
                        this,
                        current_resolution_handleable.result);
                    
                    dealer.Done(true);
                } else {
                    DIAG.Log.________________________________________________________________.Error(
                        "%s cseq:%s and seq is except, failed.",
                        this,
                        current_resolution_handleable.result);
                }
            }
            next=true;
        }
        return next;
    }
    public Handler<Boolean> CloseingResolution(final long seq,final Data data) {
        Handler<Boolean> r=new Handler();
        r.Add(true);
        return r;
    }
}


public class ResolutionSurface implements Processer ,AutoCloseable {
    public static class Request {
        public final byte[] data;
        public final long seq;
        public Request(final byte[] data,final long seq) {
            this.data=data;
            this.seq=seq;
        }
        public final String toString() {
            return String.format("Request(l:%s,seq:%s)",data==null?null:data.length,seq);
        }
    }
    public static class Result {
        public final boolean succ;
        public final Resolution resolution;
        public Result(final Resolution resolution,final boolean succ) {
            this.resolution=resolution;
            this.succ=succ;
        }
        public final String toString() {
            return String.format("Result(%s,%s)",succ,resolution);
        }
    }
    private final Outer<Request> request=new Outer();
    private final Dealer<Result> dealer=new Dealer();
    
    
    public final RSArgs args;
    private final ZKProcess zkprocess;
    private final ResolutionSync resolution_sync;
    private RegistHandler regist_handler=null;
    public ResolutionSurface(final ZKProcess zkprocess,final RSArgs args) {
        this.args=args;
        this.zkprocess=zkprocess;
        final ResolutionSurface this_ref=this;
        this.resolution_sync=new ResolutionSync(args,zkprocess) {
            public Handler<Boolean> CloseingResolution(final long seq,final Data data) {
                return this_ref.CloseingResolution(seq, data);
            }
        };
        this.zkprocess.AddProcesser(this);
    }
    public final String toString() {
        return String.format("ResolutionSF(%s,%s,%s)",args,request,dealer);
    }
    public Handler<Boolean> CloseingResolution(final long seq,final Data data) {
        Handler<Boolean> r=new Handler();
        r.Add(true);
        return r;
    }
    public final Request in_request() {
        return request.set_handleable().result;
    }
    public final Handleable<Result> out_handleable() {
        return dealer.result_handleable();
    }
    public void close() {
        this.zkprocess.DelProcesser(this);
        this.resolution_sync.close();
        if(this.regist_handler!=null) {
            this.regist_handler.close();
        }
    }
    public void Regist(final Request request) {
        this.request.Add(request);
        this.zkprocess.Tick();
    }
    public boolean Do() {
        boolean next=false;
        
        final Handleable<Resolution> current_resolution=resolution_sync.current_resolution_handleable();
        if(current_resolution.result!=null) {
            {
                final Request request=this.request.Fetch();
                if(request!=null) {
                    if(current_resolution.result.seq+1==request.seq) {
                        boolean need_new_handler=false;
                        if(regist_handler!=null) {
                            if(regist_handler.seq!=request.seq) {
                                need_new_handler=true;
                                regist_handler.close();
                            } else {
                                DIAG.Log.________________________________________________________________.D(
                                    "%s Regist handler existed and hseq is same to rseq.",
                                    this);
                            }
                        } else {
                            need_new_handler=true;
                        }
                        if(need_new_handler) {
                            DIAG.Log.________________________________________________________________.D(
                                "%s New regist handler.",
                                this);
                            
                            regist_handler=new RegistHandler(
                                this.zkprocess,
                                resolution_sync,
                                request.seq,
                                request.data,
                                args
                            );
                        } else {
                            //is doing
                        }
                    } else {
                        DIAG.Log.________________________________________________________________.D(
                            "%s rseq is not the next of cseq:%s.",
                            this,
                            current_resolution.result);
                    }
                    next=true;
                }
            }
            if(this.dealer.Watch(
                resolution_sync.current_resolution_handleable(),
                regist_handler==null?null:regist_handler.result_handleable(),
                this.request.set_handleable()
            )) {
                if((this.request.set_handleable().result==null)||(current_resolution.result.seq>=this.request.set_handleable().result.seq)) {
                    DIAG.Log.________________________________________________________________.D(
                        "%s cseq:%s arrival rseq.",
                        this,
                        current_resolution.result);
                    
                    String determined_token=current_resolution.result.data.get_determined_token();
                    if(determined_token!=null) {
                        if(determined_token.equals(this.args.token)) {
                            DIAG.Log.________________________________________________________________.D(
                                "%s Token is determined, register is me, Done %s.",
                                this,
                                current_resolution.result);
                            
                            this.dealer.Done(new Result(current_resolution.result,true));
                        } else {
                            DIAG.Log.________________________________________________________________.D(
                                "%s Token is determined, register is not me, Done %s.",
                                this,
                                current_resolution.result);
                            
                            this.dealer.Done(new Result(current_resolution.result,false));
                        }
                    } else {
                        if(current_resolution.result.seq==-1) {
                            DIAG.Log.________________________________________________________________.D(
                                "%s Token is not determined bug cseq is -1, Done %s.",
                                this,
                                current_resolution.result);
                            
                            this.dealer.Done(new Result(current_resolution.result,false));
                        } else {
                            DIAG.Log.________________________________________________________________.D(
                                "%s cseq:%s wait for current_resolution token determined.",
                                this,
                                current_resolution.result);
                        }
                    }
                } else {
                    DIAG.Log.________________________________________________________________.D(
                        "%s cseq:%s not later to rseq, ignore.",
                        this,
                        current_resolution.result);
                }
                next=true;
            }
        }
        
        return next;
    }
}
