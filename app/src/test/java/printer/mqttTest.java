package printer;

import org.eclipse.paho.client.mqttv3.MqttClient;

public class mqttTest {

    public static void main(String[] args) throws Exception{
        String broker = "tcp://localhost:1883";
        String clientId = "JavaSample";

        MqttClient client = new MqttClient(broker, clientId);

        client.connect();
        System.out.println("Connected to broker: " + broker);

        client.subscribe("test",(topic,letter) -> {
            System.out.println("Received message: " + new String(letter.getPayload()));
        });
    }
}