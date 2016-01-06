package et.naruto.versioner;


public class Outer<RET> {
    public final Handler<RET> in;
    private final Versioner out;
    public Outer(final Outer outer) {
        this.in=new Handler(outer.in);
        this.out=new Versioner(outer.out);
    }
    public Outer() {
        this.in=new Handler();
        this.out=new Versioner();
    }
    public final String toString() {
        return String.format("Outer(in=%s,out=%s,ret=%s)",in.versioner.version(),out.version(),in.result);
    }
    public final void Add(RET ret) {
        in.Add(ret);
    }
    public final RET Out() {
        return in.Output(out);
    }
}
