package com.mycompany.app;

import org.eclipse.paho.mqttv5.client.IMqttToken;
import org.eclipse.paho.mqttv5.client.MqttCallback;
import org.eclipse.paho.mqttv5.client.MqttClient;
import org.eclipse.paho.mqttv5.client.MqttConnectionOptions;
import org.eclipse.paho.mqttv5.client.MqttDisconnectResponse;
import org.eclipse.paho.mqttv5.common.MqttException;
import org.eclipse.paho.mqttv5.common.MqttMessage;
import org.eclipse.paho.mqttv5.common.packet.MqttProperties;

public class App {
    private static volatile boolean stop = false; // Volatile to ensure visibility across threads

    public static void main(String[] args) {
        // Debug print to see the arguments passed
        System.out.println("Arguments passed: ");
        for (String arg : args) {
            System.out.println(arg);
        }

        if (args.length < 4) {
            System.out.println("Usage: java DummySensor <publish_topic> <subscribe_topic> <delay> <mqtt_broker_host>");
            return;
        }
        
        // Default topics
        String defaultPubTopic = "sensoren/java1";
        String defaultSubTopic = "feedback/java1";
        int defaultDelay = 1000;

        // Topics from command-line arguments or environment variables
        String pubTopic = args.length > 0 ? args[0] : System.getenv().getOrDefault("PUB_TOPIC", defaultPubTopic);
        String subTopic = args.length > 1 ? args[1] : System.getenv().getOrDefault("SUB_TOPIC", defaultSubTopic);
        Long delay = args.length > 2 ? Long.parseLong(args[2]) : defaultDelay; // Add default value for delay
        String mqttBrokerHost = args.length > 3 ? args[3] : System.getenv("MQTT_BORKER_HOST");
        String clientId = pubTopic;
        String broker = "tcp://" + mqttBrokerHost + ":1883";

        int pubQos = 1;
        double counter = 0.0;

        try {
            Thread.sleep(delay);
        } catch (InterruptedException e) {
            System.err.println("Initial sleep interrupted");
        }

        try {
            MqttClient client = new MqttClient(broker, clientId);
            MqttConnectionOptions options = new MqttConnectionOptions();

            client.setCallback(new MqttCallback() {
                public void connectComplete(boolean reconnect, String serverURI) {
                    System.out.println("Connected to: " + serverURI);
                }

                public void disconnected(MqttDisconnectResponse disconnectResponse) {
                    System.out.println("Disconnected: " + disconnectResponse.getReasonString());
                }

                public void deliveryComplete(IMqttToken token) {
                    System.out.println("Delivery complete: " + token.isComplete());
                }

                public void messageArrived(String topic, MqttMessage message) throws Exception {
                    String messageContent = new String(message.getPayload());
                    System.out.println("Received message on topic " + topic + ": " + messageContent);

                    if (messageContent.equalsIgnoreCase("stop")) {
                        stop = true;
                    }
                }

                public void mqttErrorOccurred(MqttException exception) {
                    System.out.println("MQTT error occurred: " + exception.getMessage());
                }

                public void authPacketArrived(int reasonCode, MqttProperties properties) {
                    System.out.println("Auth packet arrived");
                }
            });

            client.connect(options);
            client.subscribe(subTopic, pubQos); // Subscribe to the feedback topic

            while (!stop) {
                double sinValue = Math.sin(counter);
                System.out.println("Sin(" + counter + ") = " + sinValue);

                String messageContent = Double.toString(sinValue);
                MqttMessage message = new MqttMessage(messageContent.getBytes());
                message.setQos(pubQos);
                client.publish(pubTopic, message);

                counter += 0.1;

                try {
                    Thread.sleep(1000); // Wait for the specified delay
                } catch (InterruptedException e) {
                    System.err.println("Sleep interrupted");
                }
            }

            client.disconnect();
            client.close();
        } catch (MqttException e) {
            e.printStackTrace();
        }
    }
}
