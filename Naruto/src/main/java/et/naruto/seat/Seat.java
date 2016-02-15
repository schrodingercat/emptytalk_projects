package et.naruto.seat;

import java.util.Arrays;

import org.apache.zookeeper.CreateMode;

import et.naruto.base.Util;
import et.naruto.base.Util.DIAG;
import et.naruto.process.base.Processer;
import et.naruto.process.zk.ValueFetcher;
import et.naruto.process.zk.ValueRegister;
import et.naruto.process.zk.ZKProcess;
import et.naruto.versioner.Dealer;
import et.naruto.versioner.base.Handleable;

public class Seat implements Processer, AutoCloseable {
    private final Dealer<Boolean> dealer=new Dealer();
    private final ValueFetcher flag_fetcher;
    private final ValueRegister flag_register;
    private final ZKProcess zkprocess;
    public Seat(final String leader_path,final String server_id,final ZKProcess zkprocess) {
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
    public final Handleable<Boolean> result_handleable() {
        return dealer.result_handleable();
    }
    public void close() {
        this.flag_fetcher.Close();
        this.flag_register.Close();
        this.zkprocess.DelProcesser(this);
    }
    public boolean Do() {
        boolean next=false;
        if(this.dealer.Watch(
            flag_fetcher.handleable()
        )) {
            if(flag_fetcher.handleable().result!=null) {
                if(flag_fetcher.handleable().result.value.isEmpty()) {
                    DIAG.Log.________________________________.D(
                        "Seat is empty %s request the seat.",
                        Util.Bytes2String(flag_register.request().want_value));
                    flag_register.ReRequest();
                } else {
                    if(Arrays.equals(flag_fetcher.handleable().result.data,flag_register.request().want_value)) {
                        DIAG.Log.________________________________.D(
                            "%s Get the seat.",
                            Util.Bytes2String(flag_register.request().want_value));
                        this.dealer.Done(true);
                    } else {
                        DIAG.Log.________________________________.D(
                            "%s Lost the seat.",
                            Util.Bytes2String(flag_register.request().want_value));
                        this.dealer.Done(false);
                    }
                }
            } else {
                DIAG.Log.________________________________.D(
                    "%s fetching the seat info.",
                    Util.Bytes2String(flag_register.request().want_value));
            }
            next=true;
        }
        return next;
    }
}
