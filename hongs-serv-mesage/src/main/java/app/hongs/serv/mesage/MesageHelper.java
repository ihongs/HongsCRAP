package app.hongs.serv.mesage;

import app.hongs.Core;
import app.hongs.CoreConfig;
import app.hongs.serv.mesage.worker.MesageChatWorker;

/**
 * 消息助手
 * @author Hongs
 */
public class MesageHelper {
    
    public static MesageWorker getWorker() {
        CoreConfig conf = CoreConfig.getInstance("mesage");
        String     defn = MesageChatWorker.class.getName();
        String     clsn = conf.getProperty("core.mesage.worker.class", defn);
        return (MesageWorker) Core.getInstance(clsn);
    }
    
}
