package et.naruto.resolutionsurface;

import et.naruto.base.Util.DIAG;

public class Data {
    public final byte[] data;
    public final int split;
    private static int FindSplit(final byte[] data) {
        for(int i=0;i<data.length;i++) {
            if(data[i]=='@') {
                return i;
            }
        }
        DIAG.Log.d.Error();
        return -1;
    }
    public Data() {
        this.data=new byte[]{'@'};
        this.split=0;
    }
    public Data(final byte[] data) {
        this.data=data;
        this.split=FindSplit(data);
    }
    public Data(final byte[] data,final String token) {
        if(token.indexOf('@')>-1) {
            DIAG.Log.d.Error();
        }
        this.data=new byte[data.length+token.length()+1];
        this.split=token.length();
        try {
            System.arraycopy((token+'@').getBytes("UTF-8"),0,this.data,0,token.length()+1);
            System.arraycopy(data,0,this.data,token.length()+1,data.length);
        } catch (Exception e) {
            DIAG.Log.d.pass_error("",e);
        }
    }
    public final Boolean is_determined_for_closed() {
        if(token_length()>0) {
            if(data_length()==0) {
                return true;
            } else {
                return false;
            }
        }
        return null;
    }
    public final String get_determined_token() {
        if(token_length()>0) {
            try {
                return new String(this.data,0,this.split,"UTF-8");
            } catch (Exception e) {
                DIAG.Log.d.pass_error("",e);
            }
        }
        return null;
    }
    public final int token_length() {
        return split;
    }
    public final int data_length() {
        return data.length-split-1;
    }
}
