package org.example;

import org.springframework.jms.connection.CachingConnectionFactory;

import javax.jms.ConnectionFactory;

public class IBMCachingConnectionFactory extends CachingConnectionFactory {

    public IBMCachingConnectionFactory(ConnectionFactory targetConnectionFactory){
        super(targetConnectionFactory);
    }

}