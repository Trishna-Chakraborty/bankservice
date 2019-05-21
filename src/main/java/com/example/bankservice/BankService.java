package com.example.bankservice;


import com.rabbitmq.client.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Service
public class BankService {

    @Autowired
    BankRepository bankRepository;


    public  boolean addBalance(Double money){
        Bank bank =new Bank();
        bank.setId(UUID.randomUUID());
        bank.setBalance(money);
        bankRepository.save(bank);
        return true;

    }


    public void consume(String name) throws Exception{

        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost("localhost");
        Connection connection=factory.newConnection();
        Channel channel=connection.createChannel();
        //channel.queueDeclare(name, false, false, false, null);
        Map<String, Object> args = new HashMap<String, Object>();
        args.put("x-dead-letter-exchange", "dead_exchange");
        args.put("x-message-ttl", 60000);
        channel.queueDeclare(name, false, false, false, args);
        channel.exchangeDeclare(name+"_exchange", "direct");;
        channel.queueBind(name, name+"_exchange", "");
        channel.basicQos(1);



        System.out.println(" [x] Awaiting RPC requests from " + name);

        Object monitor = new Object();
        DeliverCallback deliverCallback = (consumerTag, delivery) -> {
            AMQP.BasicProperties replyProps = new AMQP.BasicProperties
                    .Builder()
                    .correlationId(delivery.getProperties().getCorrelationId())
                    .build();

            String response = "";

            try {
                String message = new String(delivery.getBody(), "UTF-8");

                System.out.println("Got message from " + name +" : " + message);
                response= String.valueOf(addBalance(Double.parseDouble(message)));

            } catch (Exception e) {
                System.out.println(" [.] " + e.toString());
            } finally {
                channel.basicPublish("", delivery.getProperties().getReplyTo(), replyProps, response.getBytes("UTF-8"));
                channel.basicAck(delivery.getEnvelope().getDeliveryTag(), false);
                // RabbitMq consumer worker thread notifies the RPC server owner thread
                synchronized (monitor) {
                    monitor.notify();
                }
            }
        };

        channel.basicConsume(name, false, deliverCallback, (consumerTag -> { }));
        // Wait and be prepared to consume the message from RPC client.
        while (true) {
            synchronized (monitor) {
                try {
                    monitor.wait();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }

    }

}
