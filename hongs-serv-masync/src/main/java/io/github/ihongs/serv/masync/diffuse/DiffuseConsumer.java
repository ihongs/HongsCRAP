package io.github.ihongs.serv.masync.diffuse;

import io.github.ihongs.Core;
import io.github.ihongs.CoreConfig;
import java.util.Properties;
import java.util.regex.Pattern;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;

/**
 * 数据变更同步
 * @author Hongs
 */
public class DiffuseConsumer implements Runnable {

    @Override
    public void run() {
        Properties props;
        String     topic;
        KafkaConsumer  <String, DiffuseRecord> consumer;
        ConsumerRecords<String, DiffuseRecord> records ;

        props    = CoreConfig.getInstance("masync");
        System.out.println(props.getProperty("bootstrap.servers"));
        topic    = props.getProperty("masync.diffuse.topic", "masync.diffuse");
        topic    = "^"+Pattern.quote(topic)+"(?!"+Pattern.quote(Core.SERVER_ID)+"$)"; // topic 开头, 非 SERVER_ID 结尾
        consumer = new KafkaConsumer(props);        
        consumer.subscribe(Pattern.compile(topic));

        System.out.println("1111111111111111");
        
        records  = consumer.poll(1000);
        for(ConsumerRecord<String, DiffuseRecord> reco : records) {
            DiffuseRecord rec = reco.value();
            System.out.println(rec.toString());
        }
    }

}
