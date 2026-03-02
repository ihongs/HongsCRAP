package io.github.ihongs.serv.masync.diffuse;

import io.github.ihongs.Core;
import io.github.ihongs.CoreConfig;
import java.util.Properties;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import io.github.ihongs.serv.matrix.util.DataDiffuser;

/**
 * 数据变更广播
 * @author Hongs
 */
@Core.Singleton
public class DiffuseProducer implements DataDiffuser {

    private final String topic;
    private final KafkaProducer<String, DiffuseRecord> producer;

    private DiffuseProducer () {
        Properties props;
        props    = CoreConfig.getInstance("masync");
        topic    = props.getProperty("masync.diffuse.topic", "masync.diffuse");
        producer = new KafkaProducer(props);
    }

    public static DiffuseProducer getInstance() {
        return Core.getInterior().got(DiffuseProducer.class.getName(), ()-> new DiffuseProducer());
    }

    @Override
    public void update(String conf, String form, String id) {
        this.producer.send(new ProducerRecord<String, DiffuseRecord>(this.topic+"."+Core.SERVER_ID, new DiffuseRecord (true , conf, form, id, Core.SERVER_ID)));
    }

    @Override
    public void delete(String conf, String form, String id) {
        this.producer.send(new ProducerRecord<String, DiffuseRecord>(this.topic+"."+Core.SERVER_ID, new DiffuseRecord (false, conf, form, id, Core.SERVER_ID)));
    }

}
