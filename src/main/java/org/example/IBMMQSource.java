package org.example;

import com.ibm.mq.jms.MQConnectionFactory;
import org.springframework.jms.connection.CachingConnectionFactory;
import org.springframework.jms.connection.UserCredentialsConnectionFactoryAdapter;
import org.springframework.jms.support.JmsUtils;

import javax.jms.Connection;
import javax.jms.ConnectionFactory;
import javax.jms.JMSException;
import javax.jms.MessageConsumer;
import javax.jms.Queue;
import javax.jms.Session;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import static com.ibm.msg.client.wmq.common.CommonConstants.WMQ_CM_CLIENT;

public class IBMMQSource {

    private IBMCachingConnectionFactory connectionFactory;
    private final ScheduledExecutorService reconnectScheduler = Executors.newScheduledThreadPool(1);
    private static final int NUMBER_OF_CONSUMERS = 4;
    private Connection connection = null;
    private final List<Session> sessions = new ArrayList<>();
    private final List<MessageConsumer> consumers = new ArrayList<>();


    public IBMMQSource(){
        this.connectionFactory = createCachingConnectionFactory();
    }

    public void start(){
        try{
            this.connection = connectionFactory.createConnection();
            startConsumers();
            connection.start();
        }catch(Exception ex){
            throw new RuntimeException(ex);
        }
    }


    public void stop(){
        consumers.forEach(JmsUtils::closeMessageConsumer);
        sessions.forEach(JmsUtils::closeSession);

        JmsUtils.closeConnection(connection);

        this.consumers.clear();
        this.sessions.clear();
        this.connection=null;
    }

    private void startConsumers() throws JMSException {

        for (int i = 0; i < NUMBER_OF_CONSUMERS; i++) {

            Session session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
            this.sessions.add(session);
            Queue queue = session.createQueue("DEV.QUEUE.1");
            MessageConsumer consumer = session.createConsumer(queue);
            this.consumers.add(consumer);
            consumer.setMessageListener(new MyMessageListener(
                    "Consumer " + i));
        }
    }


    private IBMCachingConnectionFactory createCachingConnectionFactory(){
        IBMCachingConnectionFactory cachingConnectionFactory = new IBMCachingConnectionFactory(createIBMConnectionFactory());
        cachingConnectionFactory.setCacheConsumers(true);
        cachingConnectionFactory.setCacheProducers(true);
        cachingConnectionFactory.setSessionCacheSize(25);

        cachingConnectionFactory.setReconnectOnException(true);
        cachingConnectionFactory.setExceptionListener(e-> reconnect(cachingConnectionFactory,5));
        cachingConnectionFactory.afterPropertiesSet();
        return cachingConnectionFactory;
    }

    private ConnectionFactory createIBMConnectionFactory(){
        try {
            MQConnectionFactory factory = new MQConnectionFactory();
            factory.setTransportType(WMQ_CM_CLIENT);
            factory.setHostName("127.0.0.1");
            factory.setPort(1414);
            factory.setQueueManager("QM1");
            factory.setChannel("PASSWORD.SVRCONN");


            UserCredentialsConnectionFactoryAdapter connectionFactoryAdapter = new UserCredentialsConnectionFactoryAdapter();
            connectionFactoryAdapter.setTargetConnectionFactory(factory);
            connectionFactoryAdapter.setUsername("mqm");
            //connectionFactoryAdapter.setPassword("passw0rd");
            return connectionFactoryAdapter;
        }catch(Throwable t){
            throw new RuntimeException(t);
        }

    }

    private synchronized void reconnect(CachingConnectionFactory factory, final int delay) {
        reconnectScheduler.schedule(() -> {
            System.out.println("Reconnecting");
            try {
                this.stop();
                this.connectionFactory.resetConnection();
                this.start();
                System.out.println("Reconnected");
            } catch (Exception e) {
                e.printStackTrace();
                reconnect(factory, delay);
            }

        }, delay, TimeUnit.SECONDS);
    }
}
