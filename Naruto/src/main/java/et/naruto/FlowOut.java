package et.naruto;



public class FlowOut<RET> extends Outer<RET> {
    private final Versioner in;
    private final Versioner.Fetcher doing;
    public FlowOut(final FlowOut flow) {
        super(flow);
        this.doing=new Versioner.Fetcher(flow.doing);
        this.in=new Versioner(flow.in);
    }
    public FlowOut() {
        super();
        this.in=new Versioner();
        this.doing=new Versioner.Fetcher();
    }
    public final void AddIn() {
        this.in.Add();
    }
    public final boolean Doing() {
        if(this.doing.Fetch(this.in)) {
            return true;
        }
        return false;
    }
    public final void Out(final RET ret) {
        this.Out(doing,ret);
    }
}
