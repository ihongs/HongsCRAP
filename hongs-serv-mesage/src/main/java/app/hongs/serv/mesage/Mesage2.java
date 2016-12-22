package app.hongs.serv.mesage;

import java.io.Serializable;
import java.util.Set;

/**
 * 再分发消息
 * @author Hongs
 */
public class Mesage2 implements Serializable {
    
    public final Mesage     message;
    public final Set<String> onlines;
    
    /**
     * 
     * @param message 消息结构体
     * @param onlines 在线用户ID(Keep)或接收用户ID(Push)
     */
    public Mesage2 (Mesage message, Set<String> onlines) {
        this.message = message;
        this.onlines = onlines;
    }
    
}
