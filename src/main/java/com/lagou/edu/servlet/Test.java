package com.lagou.edu.servlet;

import com.lagou.edu.factory.BeanFactory;

public class Test {


    public static void main(String[] args) throws Exception {

        TransferController transferController = (TransferController)BeanFactory.getBean("transferController");
        transferController.trans();

    }
}
