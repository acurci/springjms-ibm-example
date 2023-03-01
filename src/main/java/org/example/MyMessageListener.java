package org.example;

import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageListener;
import javax.jms.TextMessage;

public class MyMessageListener implements MessageListener {
    private String messageListenerName;

    public MyMessageListener(String consumerName) {
        this.messageListenerName = consumerName;
    }

    public void onMessage(Message message) {
        TextMessage textMessage = (TextMessage) message;
        try {
            System.out.println(messageListenerName + " received " + textMessage.getText());
        } catch (JMSException e) {
            e.printStackTrace();
        }
    }
}
