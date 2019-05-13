package com.example.bankservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ApplicationContext;

@SpringBootApplication
public class BankserviceApplication {

    public static void main(String[] args) throws Exception {
        ApplicationContext applicationContext=SpringApplication.run(BankserviceApplication.class, args);
        BankService bankService=applicationContext.getBean(BankService.class);
        bankService.consume("bank");
    }

}
