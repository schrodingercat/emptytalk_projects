package et.naruto;

public class Outer<RET> {
    private final Versioner out;
    private final Fetcher<RET> fetcher;
    public volatile RET result=null;
    public static class Fetcher<RET> {
        private final Versioner.Fetcher fetcher;
        public Fetcher() {
            this.fetcher=new Versioner.Fetcher();
        }
        public Fetcher(final Fetcher fetcher) {
            this.fetcher=new Versioner.Fetcher(fetcher.fetcher);
        }
        public RET Fetch(final Outer<RET> outer) {
            if(fetcher.Fetch(outer.out)) {
                return outer.result;
            }
            return null;
        }
    }
    public Outer(final Outer outer) {
        this.out=new Versioner(outer.out);
        this.fetcher=new Fetcher(outer.fetcher);
    }
    public Outer() {
        this.out=new Versioner();
        this.fetcher=new Fetcher();
    }
    public final void AddOut(final RET ret) {
        result=ret;
        this.out.Add();
    }
    public final void Out(final Versioner.Fetcher doing,final RET ret) {
        result=ret;
        this.out.Assign(doing);
    }
    public final RET Fetch() {
        return fetcher.Fetch(this);
    }
    public final Fetcher<RET> GetFetcher() {
        return new Fetcher<RET>();
    }
}
