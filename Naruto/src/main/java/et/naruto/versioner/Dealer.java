package et.naruto.versioner;


public class Dealer<RET> {
    private final Versioner in;
    public final Handler<RET> handler;
    public Dealer(final Dealer dealer) {
        this.in=new Versioner(dealer.in);
        this.handler=new Handler(dealer.handler);
    }
    public Dealer() {
        this.in=new Versioner();
        this.handler=new Handler();
    }
    public final String toString() {
        return String.format("Dealer(in=%s,out=%s,ret=%s)",in.version(),handler.versioner.version(),handler.result);
    }
    public final boolean Watch(final Versioner...versioners) {
        return in.Watch(versioners);
    }
    public final void Done(final RET ret) {
        this.handler.Add(ret);
    }
}
